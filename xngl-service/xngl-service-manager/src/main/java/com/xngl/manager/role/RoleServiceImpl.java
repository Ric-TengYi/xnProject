package com.xngl.manager.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.UserRoleRel;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import com.xngl.infrastructure.persistence.entity.system.RolePermissionRel;
import com.xngl.infrastructure.persistence.entity.system.RoleMenuRel;
import com.xngl.infrastructure.persistence.mapper.DataScopeRuleMapper;
import com.xngl.infrastructure.persistence.mapper.RoleMapper;
import com.xngl.infrastructure.persistence.mapper.RoleMenuRelMapper;
import com.xngl.infrastructure.persistence.mapper.RolePermissionRelMapper;
import com.xngl.infrastructure.persistence.mapper.UserRoleRelMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl implements RoleService {

  private final RoleMapper roleMapper;
  private final RolePermissionRelMapper rolePermissionRelMapper;
  private final RoleMenuRelMapper roleMenuRelMapper;
  private final DataScopeRuleMapper dataScopeRuleMapper;
  private final UserRoleRelMapper userRoleRelMapper;

  public RoleServiceImpl(
      RoleMapper roleMapper,
      RolePermissionRelMapper rolePermissionRelMapper,
      RoleMenuRelMapper roleMenuRelMapper,
      DataScopeRuleMapper dataScopeRuleMapper,
      UserRoleRelMapper userRoleRelMapper) {
    this.roleMapper = roleMapper;
    this.rolePermissionRelMapper = rolePermissionRelMapper;
    this.roleMenuRelMapper = roleMenuRelMapper;
    this.dataScopeRuleMapper = dataScopeRuleMapper;
    this.userRoleRelMapper = userRoleRelMapper;
  }

  @Override
  public Role getById(Long id) {
    return roleMapper.selectById(id);
  }

  @Override
  public IPage<Role> page(
      String keyword, Long tenantId, String roleScope, String status, int pageNo, int pageSize) {
    LambdaQueryWrapper<Role> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(Role::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(Role::getRoleCode, keyword)
                  .or()
                  .like(Role::getRoleName, keyword));
    }
    if (StringUtils.hasText(roleScope)) q.eq(Role::getRoleScope, roleScope);
    if (StringUtils.hasText(status)) q.eq(Role::getStatus, status);
    return roleMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  public IPage<Role> pageWithPermissionFilter(
      String keyword, Long tenantId, String roleScope, String status, int pageNo, int pageSize, Role currentUserRole) {
    LambdaQueryWrapper<Role> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(Role::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      q.and(
          w ->
              w.like(Role::getRoleCode, keyword)
                  .or()
                  .like(Role::getRoleName, keyword));
    }
    if (StringUtils.hasText(roleScope)) q.eq(Role::getRoleScope, roleScope);
    if (StringUtils.hasText(status)) q.eq(Role::getStatus, status);

    if ("TENANT".equals(currentUserRole.getRoleScope())) {
      q.eq(Role::getRoleScope, "TENANT");
    }

    return roleMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long create(Role role) {
    if (!StringUtils.hasText(role.getStatus())) {
      role.setStatus("ENABLED");
    }
    if (!StringUtils.hasText(role.getDataScopeTypeDefault())) {
      role.setDataScopeTypeDefault("ORG_AND_CHILDREN");
    }
    roleMapper.insert(role);
    ensureDefaultDataScopeRule(role);
    return role.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void update(Role role) {
    roleMapper.updateById(role);
    ensureDefaultDataScopeRule(role);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    dataScopeRuleMapper.deletePhysicalByRoleId(id);
    roleMapper.deleteById(id);
  }

  @Override
  public List<Role> listByIds(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    return roleMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getId, ids));
  }

  @Override
  public List<Long> listPermissionIdsByRoleId(Long roleId) {
    return rolePermissionRelMapper
        .selectList(new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId))
        .stream()
        .map(RolePermissionRel::getPermissionId)
        .toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updatePermissions(Long roleId, List<Long> menuIds, List<Long> permissionIds) {
    Role role = roleMapper.selectById(roleId);
    if (role == null) return;
    Long tenantId = role.getTenantId();
    roleMenuRelMapper.delete(new LambdaQueryWrapper<RoleMenuRel>().eq(RoleMenuRel::getRoleId, roleId));
    if (!CollectionUtils.isEmpty(menuIds)) {
      for (Long menuId : menuIds) {
        RoleMenuRel rel = new RoleMenuRel();
        rel.setTenantId(tenantId);
        rel.setRoleId(roleId);
        rel.setMenuId(menuId);
        roleMenuRelMapper.insert(rel);
      }
    }
    rolePermissionRelMapper.delete(
        new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId));
    if (!CollectionUtils.isEmpty(permissionIds)) {
      for (Long permId : permissionIds) {
        RolePermissionRel rel = new RolePermissionRel();
        rel.setTenantId(tenantId);
        rel.setRoleId(roleId);
        rel.setPermissionId(permId);
        rolePermissionRelMapper.insert(rel);
      }
    }
  }

  @Override
  public List<DataScopeRule> listDataScopeRulesByRoleId(Long roleId) {
    return dataScopeRuleMapper.selectList(
        new LambdaQueryWrapper<DataScopeRule>().eq(DataScopeRule::getRoleId, roleId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateDataScopeRules(Long roleId, List<DataScopeRule> rules) {
    Role role = roleMapper.selectById(roleId);
    if (role == null) return;
    dataScopeRuleMapper.deletePhysicalByRoleId(roleId);
    if (!CollectionUtils.isEmpty(rules)) {
      for (DataScopeRule r : rules) {
        r.setId(null);
        r.setTenantId(role.getTenantId());
        r.setRoleId(roleId);
        r.setRuleType(null);
        r.setRuleValue(null);
        r.setResourceCode(null);
        dataScopeRuleMapper.insert(r);
      }
    }
  }

  @Override
  public List<Long> listMenuIdsByRoleId(Long roleId) {
    return roleMenuRelMapper
        .selectList(new LambdaQueryWrapper<RoleMenuRel>().eq(RoleMenuRel::getRoleId, roleId))
        .stream()
        .map(RoleMenuRel::getMenuId)
        .toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateMenus(Long roleId, List<Long> menuIds) {
    Role role = roleMapper.selectById(roleId);
    if (role == null) return;
    Long tenantId = role.getTenantId();
    roleMenuRelMapper.delete(
        new LambdaQueryWrapper<RoleMenuRel>().eq(RoleMenuRel::getRoleId, roleId));
    if (!CollectionUtils.isEmpty(menuIds)) {
      for (Long menuId : menuIds) {
        RoleMenuRel rel = new RoleMenuRel();
        rel.setTenantId(tenantId);
        rel.setRoleId(roleId);
        rel.setMenuId(menuId);
        roleMenuRelMapper.insert(rel);
      }
    }
  }

  private void ensureDefaultDataScopeRule(Role role) {
    if (role.getId() == null || role.getTenantId() == null || !StringUtils.hasText(role.getDataScopeTypeDefault())) {
      return;
    }
    long count =
        dataScopeRuleMapper.selectCount(
            new LambdaQueryWrapper<DataScopeRule>().eq(DataScopeRule::getRoleId, role.getId()));
    if (count > 0) {
      return;
    }
    DataScopeRule rule = new DataScopeRule();
    rule.setTenantId(role.getTenantId());
    rule.setRoleId(role.getId());
    rule.setBizModule("ALL");
    rule.setScopeType(role.getDataScopeTypeDefault());
    rule.setScopeValue("[]");
    dataScopeRuleMapper.insert(rule);
  }

  @Override
  public List<Role> listByRoleCode(Long tenantId, String roleCode) {
    if (tenantId == null || !StringUtils.hasText(roleCode)) return List.of();
    return roleMapper.selectList(
        new LambdaQueryWrapper<Role>()
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getRoleCode, roleCode));
  }

  @Override
  public void validateRoleCreation(Role currentUserRole, Role newRole) {
    if ("TENANT".equals(currentUserRole.getRoleScope())) {
      if ("SYSTEM".equals(newRole.getRoleScope())) {
        throw new RuntimeException("租户用户不能创建系统角色");
      }
      newRole.setRoleScope("TENANT");
    }
    if (!canAssignDataScope(currentUserRole.getDataScopeTypeDefault(),
        newRole.getDataScopeTypeDefault())) {
      throw new RuntimeException("数据范围不能超过自己的权限");
    }
  }

  @Override
  public boolean canAssignDataScope(String userScope, String newScope) {
    Map<String, Integer> scopeLevel = Map.of(
        "ALL", 4,
        "ORG_AND_CHILDREN", 3,
        "CUSTOM_ORG_SET", 2,
        "SELF", 1
    );
    return scopeLevel.getOrDefault(userScope, 0) >= scopeLevel.getOrDefault(newScope, 0);
  }

  public boolean canAccessOrganization(Long userId, Long organizationId, String requiredScope) {
    List<UserRoleRel> relations = userRoleRelMapper.selectByUserId(userId);
    for (UserRoleRel rel : relations) {
      if (rel.getOrganizationId() != null && rel.getOrganizationId().equals(organizationId)) {
        Role role = roleMapper.selectById(rel.getRoleId());
        if (role != null && canAccessWithScope(role.getDataScopeTypeDefault(), requiredScope)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean canAccessWithScope(String roleScope, String requiredScope) {
    Map<String, Integer> scopeLevel = Map.of(
        "ALL", 4,
        "ORG_AND_CHILDREN", 3,
        "CUSTOM_ORG_SET", 2,
        "SELF", 1
    );
    return scopeLevel.getOrDefault(roleScope, 0) >= scopeLevel.getOrDefault(requiredScope, 0);
  }
}
