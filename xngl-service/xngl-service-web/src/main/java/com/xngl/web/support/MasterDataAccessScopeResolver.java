package com.xngl.web.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import com.xngl.infrastructure.persistence.mapper.DataScopeRuleMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MasterDataAccessScopeResolver {

  public static final String BIZ_MODULE_UNIT = "UNIT";
  public static final String BIZ_MODULE_PROJECT = "PROJECT";
  public static final String BIZ_MODULE_DISPOSAL_PERMIT = "DISPOSAL_PERMIT";
  public static final String BIZ_MODULE_SITE = "SITE";
  public static final String BIZ_MODULE_VEHICLE = "VEHICLE";
  public static final String BIZ_MODULE_CONTRACT = "CONTRACT";

  private static final Set<String> TENANT_WIDE_USER_TYPES =
      Set.of("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN");
  private static final Set<String> TENANT_WIDE_ROLE_SCOPES = Set.of("SYSTEM", "TENANT");
  private static final Set<String> TENANT_WIDE_DATA_SCOPES = Set.of("ALL", "TENANT");
  private static final Pattern LONG_PATTERN = Pattern.compile("\\d+");

  private final UserService userService;
  private final RoleService roleService;
  private final OrgMapper orgMapper;
  private final DataScopeRuleMapper dataScopeRuleMapper;

  public MasterDataAccessScopeResolver(
      UserService userService,
      RoleService roleService,
      OrgMapper orgMapper,
      DataScopeRuleMapper dataScopeRuleMapper) {
    this.userService = userService;
    this.roleService = roleService;
    this.orgMapper = orgMapper;
    this.dataScopeRuleMapper = dataScopeRuleMapper;
  }

  public MasterDataAccessScope resolve(User currentUser, String bizModule) {
    if (currentUser == null || currentUser.getTenantId() == null) {
      return MasterDataAccessScope.none();
    }
    List<Org> tenantOrgs =
        orgMapper.selectList(
            new LambdaQueryWrapper<Org>().eq(Org::getTenantId, currentUser.getTenantId()));
    LinkedHashSet<Long> tenantOrgIds = toOrgIdSet(tenantOrgs);
    if (tenantOrgIds.isEmpty()) {
      return MasterDataAccessScope.none();
    }
    if (isTenantWideUser(currentUser)) {
      return MasterDataAccessScope.tenantWide(tenantOrgIds);
    }

    List<Long> roleIds = userService.listRoleIdsByUserId(currentUser.getId());
    List<Role> roles =
        roleIds == null || roleIds.isEmpty()
            ? List.of()
            : roleService.listByIds(roleIds).stream()
                .filter(role -> Objects.equals(role.getTenantId(), currentUser.getTenantId()))
                .toList();
    if (roles.stream().anyMatch(this::isTenantWideRole)) {
      return MasterDataAccessScope.tenantWide(tenantOrgIds);
    }

    LinkedHashSet<Long> assignedOrgIds = loadAssignedOrgIds(currentUser, tenantOrgIds);
    List<DataScopeRule> rules = loadRules(currentUser.getTenantId(), roleIds, bizModule);

    LinkedHashSet<Long> allowedOrgIds = new LinkedHashSet<>();
    LinkedHashSet<Long> allowedProjectIds = new LinkedHashSet<>();
    if (!rules.isEmpty()) {
      for (DataScopeRule rule : rules) {
        applyScopeRule(rule.getScopeType(), rule.getScopeValue(), assignedOrgIds, tenantOrgs, allowedOrgIds, allowedProjectIds);
      }
    } else {
      for (Role role : roles) {
        applyScopeRule(
            role.getDataScopeTypeDefault(), null, assignedOrgIds, tenantOrgs, allowedOrgIds, allowedProjectIds);
      }
    }

    if (allowedOrgIds.isEmpty() && allowedProjectIds.isEmpty()) {
      allowedOrgIds.addAll(assignedOrgIds);
    }
    return MasterDataAccessScope.scoped(allowedOrgIds, allowedProjectIds);
  }

  private boolean isTenantWideUser(User currentUser) {
    return TENANT_WIDE_USER_TYPES.contains(normalize(currentUser.getUserType()));
  }

  private boolean isTenantWideRole(Role role) {
    return TENANT_WIDE_ROLE_SCOPES.contains(normalize(role.getRoleScope()))
        || TENANT_WIDE_DATA_SCOPES.contains(normalize(role.getDataScopeTypeDefault()));
  }

  private List<DataScopeRule> loadRules(Long tenantId, List<Long> roleIds, String bizModule) {
    if (tenantId == null || roleIds == null || roleIds.isEmpty()) {
      return List.of();
    }
    return dataScopeRuleMapper.selectList(
        new LambdaQueryWrapper<DataScopeRule>()
            .eq(DataScopeRule::getTenantId, tenantId)
            .in(DataScopeRule::getRoleId, roleIds)
            .and(
                wrapper ->
                    wrapper
                        .eq(DataScopeRule::getBizModule, "ALL")
                        .or()
                        .eq(DataScopeRule::getBizModule, bizModule)));
  }

  private LinkedHashSet<Long> loadAssignedOrgIds(User currentUser, Set<Long> tenantOrgIds) {
    LinkedHashSet<Long> assignedOrgIds = new LinkedHashSet<>();
    if (currentUser.getMainOrgId() != null && tenantOrgIds.contains(currentUser.getMainOrgId())) {
      assignedOrgIds.add(currentUser.getMainOrgId());
    }
    List<Long> userOrgIds = userService.listOrgIdsByUserId(currentUser.getId());
    if (userOrgIds != null) {
      for (Long orgId : userOrgIds) {
        if (orgId != null && tenantOrgIds.contains(orgId)) {
          assignedOrgIds.add(orgId);
        }
      }
    }
    return assignedOrgIds;
  }

  private void applyScopeRule(
      String scopeType,
      String scopeValue,
      Set<Long> assignedOrgIds,
      List<Org> tenantOrgs,
      Set<Long> allowedOrgIds,
      Set<Long> allowedProjectIds) {
    String normalizedScope = normalize(scopeType);
    switch (normalizedScope) {
      case "SELF" -> allowedOrgIds.addAll(assignedOrgIds);
      case "ORG_AND_CHILDREN" -> allowedOrgIds.addAll(expandOrgIds(assignedOrgIds, tenantOrgs));
      case "CUSTOM_ORG_SET" -> allowedOrgIds.addAll(filterToTenantOrgIds(extractIds(scopeValue), tenantOrgs));
      case "CUSTOM_PROJECT_SET" -> allowedProjectIds.addAll(extractIds(scopeValue));
      case "ALL", "TENANT" -> allowedOrgIds.addAll(toOrgIdSet(tenantOrgs));
      default -> {
        if (!assignedOrgIds.isEmpty()) {
          allowedOrgIds.addAll(assignedOrgIds);
        }
      }
    }
  }

  private LinkedHashSet<Long> expandOrgIds(Set<Long> rootOrgIds, List<Org> tenantOrgs) {
    LinkedHashSet<Long> expanded = new LinkedHashSet<>();
    if (rootOrgIds == null || rootOrgIds.isEmpty()) {
      return expanded;
    }
    for (Org org : tenantOrgs) {
      if (org.getId() == null) {
        continue;
      }
      for (Long rootOrgId : rootOrgIds) {
        if (Objects.equals(org.getId(), rootOrgId) || pathContainsOrg(org.getOrgPath(), rootOrgId)) {
          expanded.add(org.getId());
          break;
        }
      }
    }
    return expanded;
  }

  private LinkedHashSet<Long> filterToTenantOrgIds(Set<Long> candidateOrgIds, List<Org> tenantOrgs) {
    Set<Long> tenantOrgIds = toOrgIdSet(tenantOrgs);
    LinkedHashSet<Long> filtered = new LinkedHashSet<>();
    for (Long candidateOrgId : candidateOrgIds) {
      if (tenantOrgIds.contains(candidateOrgId)) {
        filtered.add(candidateOrgId);
      }
    }
    return filtered;
  }

  private boolean pathContainsOrg(String orgPath, Long orgId) {
    if (orgPath == null || orgId == null) {
      return false;
    }
    String token = "/" + orgId;
    return orgPath.equals(token)
        || orgPath.startsWith(token + "/")
        || orgPath.contains(token + "/")
        || orgPath.endsWith(token);
  }

  private LinkedHashSet<Long> extractIds(String rawValue) {
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    if (rawValue == null || rawValue.isBlank()) {
      return ids;
    }
    Matcher matcher = LONG_PATTERN.matcher(rawValue);
    while (matcher.find()) {
      ids.add(Long.parseLong(matcher.group()));
    }
    return ids;
  }

  private LinkedHashSet<Long> toOrgIdSet(List<Org> orgs) {
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    if (orgs != null) {
      for (Org org : orgs) {
        if (org != null && org.getId() != null) {
          ids.add(org.getId());
        }
      }
    }
    return ids;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
