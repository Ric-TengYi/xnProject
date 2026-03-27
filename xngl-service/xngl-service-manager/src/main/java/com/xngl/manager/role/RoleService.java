package com.xngl.manager.role;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import java.util.List;

public interface RoleService {

  Role getById(Long id);

  IPage<Role> page(String keyword, Long tenantId, String roleScope, String status, int pageNo, int pageSize);

  long create(Role role);

  void update(Role role);

  void delete(Long id);

  List<Role> listByIds(List<Long> ids);

  List<Long> listPermissionIdsByRoleId(Long roleId);

  void updatePermissions(Long roleId, List<Long> menuIds, List<Long> permissionIds);

  List<DataScopeRule> listDataScopeRulesByRoleId(Long roleId);

  void updateDataScopeRules(Long roleId, List<DataScopeRule> rules);

  List<Long> listMenuIdsByRoleId(Long roleId);

  void updateMenus(Long roleId, List<Long> menuIds);

  List<Role> listByRoleCode(Long tenantId, String roleCode);

  void validateRoleCreation(Role currentUserRole, Role newRole);

  boolean canAssignDataScope(String userScope, String newScope);
}
