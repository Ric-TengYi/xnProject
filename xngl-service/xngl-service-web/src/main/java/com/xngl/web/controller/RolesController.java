package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.manager.menu.MenuService;
import com.xngl.manager.permission.PermissionService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.TenantManagementAccessGuard;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RolesController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final RoleService roleService;
  private final MenuService menuService;
  private final PermissionService permissionService;
  private final UserContext userContext;
  private final UserService userService;
  private final TenantManagementAccessGuard accessGuard;

  public RolesController(
      RoleService roleService,
      MenuService menuService,
      PermissionService permissionService,
      UserContext userContext,
      UserService userService,
      TenantManagementAccessGuard accessGuard) {
    this.roleService = roleService;
    this.menuService = menuService;
    this.permissionService = permissionService;
    this.userContext = userContext;
    this.userService = userService;
    this.accessGuard = accessGuard;
  }

  @GetMapping
  public ApiResult<PageResult<RoleListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String roleScope,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    IPage<Role> page =
        roleService.page(
            keyword, currentUser.getTenantId(), roleScope, status, pageNo, pageSize);
    List<RoleListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<RoleDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Role r = requireRole(id, currentUser);
    return ApiResult.ok(toDetail(r));
  }

  @GetMapping("/{id}/permissions")
  public ApiResult<RolePermissionsDto> listPermissions(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireRole(id, currentUser);
    List<Long> menuIds =
        filterMenuIdsByTenant(roleService.listMenuIdsByRoleId(id), currentUser.getTenantId());
    List<Permission> permissions =
        filterPermissionsByTenant(
            roleService.listPermissionIdsByRoleId(id), currentUser.getTenantId());
    List<Long> permissionIds = permissions.stream().map(Permission::getId).toList();
    List<String> buttonCodes =
        permissions.stream()
            .filter(
                permission ->
                    permission.getResourceType() == null
                        || "BUTTON".equalsIgnoreCase(permission.getResourceType())
                        || "ACTION".equalsIgnoreCase(permission.getResourceType()))
            .map(Permission::getPermissionCode)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    List<String> apiCodes =
        permissions.stream()
            .filter(
                permission ->
                    permission.getResourceType() != null
                        && "API".equalsIgnoreCase(permission.getResourceType()))
            .map(Permission::getPermissionCode)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return ApiResult.ok(
        new RolePermissionsDto(
            menuIds.stream().map(String::valueOf).toList(),
            permissionIds.stream().map(String::valueOf).toList(),
            buttonCodes,
            apiCodes));
  }

  @PutMapping("/{id}/permissions")
  public ApiResult<Void> updatePermissions(
      @PathVariable Long id,
      @RequestBody java.util.Map<String, List<Long>> body,
      HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireRole(id, currentUser);
    List<Long> menuIds = body.get("menuIds");
    List<Long> permissionIds = body.get("permissionIds");
    ensureMenusBelongToTenant(menuIds, currentUser.getTenantId());
    ensurePermissionsBelongToTenant(permissionIds, currentUser.getTenantId());
    roleService.updatePermissions(id, menuIds, permissionIds);
    return ApiResult.ok();
  }

  @GetMapping("/{id}/data-scope-rules")
  public ApiResult<List<DataScopeRuleDto>> listDataScopeRules(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireRole(id, currentUser);
    List<DataScopeRule> rules = roleService.listDataScopeRulesByRoleId(id);
    List<DataScopeRuleDto> list =
        rules.stream()
            .map(
                rule ->
                    new DataScopeRuleDto(
                        rule.getScopeType(), rule.getScopeValue(), rule.getBizModule()))
            .collect(Collectors.toList());
    return ApiResult.ok(list);
  }

  @PutMapping("/{id}/data-scope-rules")
  public ApiResult<Void> updateDataScopeRules(
      @PathVariable Long id, @RequestBody List<DataScopeRuleDto> dtos, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireRole(id, currentUser);
    List<DataScopeRule> rules =
        dtos.stream()
            .map(
                dto -> {
                  DataScopeRule rule = new DataScopeRule();
                  rule.setScopeType(dto.getRuleType());
                  rule.setScopeValue(dto.getRuleValue());
                  rule.setBizModule(dto.getResourceCode());
                  return rule;
                })
            .collect(Collectors.toList());
    roleService.updateDataScopeRules(id, rules);
    return ApiResult.ok();
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody RoleCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Role currentUserRole = resolveEffectiveCurrentUserRole(currentUser);
    if (currentUserRole == null) return ApiResult.fail(403, "无法获取当前用户角色");

    Role r = new Role();
    mapToEntity(dto, r);
    r.setTenantId(currentUser.getTenantId());
    roleService.validateRoleCreation(currentUserRole, r);
    long id = roleService.create(r);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id, @RequestBody RoleCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Role r = requireRole(id, currentUser);

    Role currentUserRole = resolveEffectiveCurrentUserRole(currentUser);
    if (currentUserRole == null) return ApiResult.fail(403, "无法获取当前用户角色");

    mapToEntity(dto, r);
    roleService.validateRoleCreation(currentUserRole, r);
    r.setId(id);
    r.setTenantId(currentUser.getTenantId());
    roleService.update(r);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireRole(id, currentUser);
    roleService.delete(id);
    return ApiResult.ok();
  }

  private Role requireRole(Long id, User currentUser) {
    Role role = roleService.getById(id);
    if (role == null) {
      throw new BizException(404, "角色不存在");
    }
    accessGuard.ensureSameTenant(role.getTenantId(), currentUser.getTenantId(), "角色");
    return role;
  }

  private Role resolveEffectiveCurrentUserRole(User currentUser) {
    List<Role> currentUserRoles = resolveCurrentUserRoles(currentUser);
    if (!currentUserRoles.isEmpty()) {
      return currentUserRoles.get(0);
    }
    return null;
  }

  private List<Role> resolveCurrentUserRoles(User currentUser) {
    List<Long> roleIds = userService.listRoleIdsByUserId(currentUser.getId());
    if (roleIds == null || roleIds.isEmpty()) {
      return List.of();
    }
    List<Role> loadedRoles = new ArrayList<>();
    List<Role> batchRoles = roleService.listByIds(roleIds);
    if (batchRoles != null && !batchRoles.isEmpty()) {
      loadedRoles.addAll(batchRoles);
    }
    java.util.Set<Long> loadedRoleIds =
        loadedRoles.stream().map(Role::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    for (Long roleId : roleIds) {
      if (roleId == null || loadedRoleIds.contains(roleId)) {
        continue;
      }
      Role role = roleService.getById(roleId);
      if (role != null) {
        loadedRoles.add(role);
      }
    }
    return loadedRoles.stream()
        .filter(role -> Objects.equals(role.getTenantId(), currentUser.getTenantId()))
        .sorted(
            Comparator.comparingInt(this::roleScopePriority)
                .thenComparingInt(role -> dataScopePriority(role.getDataScopeTypeDefault()))
                .reversed())
        .toList();
  }

  private int roleScopePriority(Role role) {
    if (role == null || role.getRoleScope() == null) {
      return 0;
    }
    return switch (role.getRoleScope().trim().toUpperCase()) {
      case "SYSTEM" -> 3;
      case "TENANT" -> 2;
      case "ORG" -> 1;
      default -> 0;
    };
  }

  private int dataScopePriority(String dataScopeTypeDefault) {
    if (dataScopeTypeDefault == null) {
      return 0;
    }
    return switch (dataScopeTypeDefault.trim().toUpperCase()) {
      case "ALL" -> 4;
      case "ORG_AND_CHILDREN" -> 3;
      case "CUSTOM_ORG_SET" -> 2;
      case "SELF" -> 1;
      default -> 0;
    };
  }

  private List<Long> filterMenuIdsByTenant(List<Long> menuIds, Long tenantId) {
    if (menuIds == null || menuIds.isEmpty()) {
      return List.of();
    }
    java.util.Set<Long> allowedMenuIds =
        menuService.listByIds(menuIds).stream()
            .filter(menu -> Objects.equals(menu.getTenantId(), tenantId))
            .map(Menu::getId)
            .collect(Collectors.toSet());
    return menuIds.stream().filter(allowedMenuIds::contains).distinct().toList();
  }

  private List<Permission> filterPermissionsByTenant(List<Long> permissionIds, Long tenantId) {
    if (permissionIds == null || permissionIds.isEmpty()) {
      return List.of();
    }
    java.util.Map<Long, Permission> allowedPermissionMap =
        permissionService.listByIds(permissionIds).stream()
            .filter(permission -> Objects.equals(permission.getTenantId(), tenantId))
            .collect(Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> left));
    return permissionIds.stream()
        .distinct()
        .map(allowedPermissionMap::get)
        .filter(Objects::nonNull)
        .toList();
  }

  private void ensureMenusBelongToTenant(List<Long> menuIds, Long tenantId) {
    if (menuIds == null || menuIds.isEmpty()) {
      return;
    }
    List<Menu> menus = menuService.listByIds(menuIds);
    if (menus.size() != menuIds.size()
        || menus.stream().anyMatch(menu -> !Objects.equals(menu.getTenantId(), tenantId))) {
      throw new BizException(400, "存在不属于当前租户的菜单");
    }
  }

  private void ensurePermissionsBelongToTenant(List<Long> permissionIds, Long tenantId) {
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    List<Permission> permissions = permissionService.listByIds(permissionIds);
    if (permissions.size() != permissionIds.size()
        || permissions.stream().anyMatch(permission -> !Objects.equals(permission.getTenantId(), tenantId))) {
      throw new BizException(400, "存在不属于当前租户的权限点");
    }
  }

  private RoleListItemDto toListItem(Role r) {
    return new RoleListItemDto(
        String.valueOf(r.getId()),
        r.getRoleCode(),
        r.getRoleName(),
        r.getRoleScope(),
        r.getRoleCategory(),
        r.getStatus(),
        null);
  }

  private RoleDetailDto toDetail(Role r) {
    return new RoleDetailDto(
        String.valueOf(r.getId()),
        String.valueOf(r.getTenantId()),
        r.getRoleCode(),
        r.getRoleName(),
        r.getRoleScope(),
        r.getRoleCategory(),
        r.getDescription(),
        r.getDataScopeTypeDefault(),
        r.getStatus(),
        null);
  }

  private void mapToEntity(RoleCreateUpdateDto dto, Role r) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      r.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    r.setRoleCode(dto.getRoleCode());
    r.setRoleName(dto.getRoleName());
    r.setRoleScope(
        org.springframework.util.StringUtils.hasText(dto.getRoleScope())
            ? dto.getRoleScope()
            : "TENANT");
    r.setRoleCategory(
        org.springframework.util.StringUtils.hasText(dto.getRoleCategory())
            ? dto.getRoleCategory()
            : "CUSTOM");
    r.setDescription(dto.getDescription());
    r.setDataScopeTypeDefault(
        org.springframework.util.StringUtils.hasText(dto.getDataScopeTypeDefault())
            ? dto.getDataScopeTypeDefault()
            : "ORG_AND_CHILDREN");
  }
}
