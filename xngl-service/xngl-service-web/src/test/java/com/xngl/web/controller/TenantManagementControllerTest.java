package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.manager.menu.MenuService;
import com.xngl.manager.permission.PermissionService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.MenuCreateUpdateDto;
import com.xngl.web.dto.user.PermissionCreateUpdateDto;
import com.xngl.web.dto.user.RoleCreateUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.TenantManagementAccessGuard;
import com.xngl.web.support.UserContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class TenantManagementControllerTest {

  @Mock
  private RoleService roleService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private UserContext userContext;

  @Mock
  private UserService userService;

  @Mock
  private MenuService menuService;

  @Mock
  private TenantManagementAccessGuard accessGuard;

  @Test
  void rolesListShouldRejectOrgAdmin() {
    RolesController controller =
        new RolesController(
            roleService, menuService, permissionService, userContext, userService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    when(accessGuard.requireTenantManagementUser(request))
        .thenThrow(new BizException(403, "当前用户无权操作租户级权限配置"));

    assertThatThrownBy(() -> controller.list(null, 999L, null, null, 1, 20, request))
        .isInstanceOf(BizException.class)
        .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(403));

    verifyNoInteractions(roleService);
  }

  @Test
  void roleCreateShouldBindCurrentUserTenant() {
    RolesController controller =
        new RolesController(
            roleService, menuService, permissionService, userContext, userService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(7L, 1L, "TENANT_ADMIN");
    Role currentRole = new Role();
    currentRole.setId(11L);
    currentRole.setTenantId(1L);
    currentRole.setRoleScope("TENANT");
    currentRole.setDataScopeTypeDefault("ALL");
    RoleCreateUpdateDto dto = new RoleCreateUpdateDto();
    dto.setTenantId("999");
    dto.setRoleCode("ROLE_TEST");
    dto.setRoleName("Role Test");
    dto.setRoleScope("TENANT");
    dto.setDataScopeTypeDefault("ORG_AND_CHILDREN");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(userService.listRoleIdsByUserId(7L)).thenReturn(List.of(11L));
    when(roleService.getById(11L)).thenReturn(currentRole);
    when(roleService.create(any(Role.class))).thenReturn(55L);

    ApiResult<String> result = controller.create(dto, request);

    ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
    verify(roleService).create(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
    assertThat(result.getData()).isEqualTo("55");
  }

  @Test
  void roleCreateShouldUseTenantScopedRoleWhenOrgRoleAppearsFirst() {
    RolesController controller =
        new RolesController(
            roleService, menuService, permissionService, userContext, userService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(21L, 1L, "TENANT_ADMIN");

    Role orgRole = new Role();
    orgRole.setId(16L);
    orgRole.setTenantId(1L);
    orgRole.setRoleScope("ORG");
    orgRole.setDataScopeTypeDefault("SELF");

    Role tenantRole = new Role();
    tenantRole.setId(1L);
    tenantRole.setTenantId(1L);
    tenantRole.setRoleScope("TENANT");
    tenantRole.setDataScopeTypeDefault("ALL");

    RoleCreateUpdateDto dto = new RoleCreateUpdateDto();
    dto.setRoleCode("ROLE_PRIORITY_TEST");
    dto.setRoleName("Role Priority Test");
    dto.setRoleScope("TENANT");
    dto.setDataScopeTypeDefault("ALL");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(userService.listRoleIdsByUserId(21L)).thenReturn(List.of(16L, 1L));
    when(roleService.listByIds(List.of(16L, 1L))).thenReturn(List.of(orgRole, tenantRole));
    when(roleService.create(any(Role.class))).thenReturn(99L);

    ApiResult<String> result = controller.create(dto, request);

    verify(roleService, never()).validateRoleCreation(eq(orgRole), any(Role.class));
    verify(roleService).validateRoleCreation(eq(tenantRole), any(Role.class));
    assertThat(result.getData()).isEqualTo("99");
  }

  @Test
  void roleCreateShouldFallbackToIndividualRoleLookupWhenBatchLookupIsEmpty() {
    RolesController controller =
        new RolesController(
            roleService, menuService, permissionService, userContext, userService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(22L, 1L, "TENANT_ADMIN");

    Role orgRole = new Role();
    orgRole.setId(16L);
    orgRole.setTenantId(1L);
    orgRole.setRoleScope("ORG");
    orgRole.setDataScopeTypeDefault("ORG");

    Role tenantRole = new Role();
    tenantRole.setId(1L);
    tenantRole.setTenantId(1L);
    tenantRole.setRoleScope("TENANT");
    tenantRole.setDataScopeTypeDefault("ALL");

    RoleCreateUpdateDto dto = new RoleCreateUpdateDto();
    dto.setRoleCode("ROLE_FALLBACK_TEST");
    dto.setRoleName("Role Fallback Test");
    dto.setRoleScope("TENANT");
    dto.setDataScopeTypeDefault("ALL");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(userService.listRoleIdsByUserId(22L)).thenReturn(List.of(16L, 1L));
    when(roleService.listByIds(List.of(16L, 1L))).thenReturn(List.of());
    when(roleService.getById(16L)).thenReturn(orgRole);
    when(roleService.getById(1L)).thenReturn(tenantRole);
    doAnswer(
            invocation -> {
              Role validationRole = invocation.getArgument(0);
              if (validationRole == orgRole) {
                throw new RuntimeException("数据范围不能超过自己的权限");
              }
              return null;
            })
        .when(roleService)
        .validateRoleCreation(any(Role.class), any(Role.class));
    when(roleService.create(any(Role.class))).thenReturn(100L);

    ApiResult<String> result = controller.create(dto, request);

    verify(roleService).validateRoleCreation(eq(tenantRole), any(Role.class));
    assertThat(result.getData()).isEqualTo("100");
  }

  @Test
  void menuTreeShouldUseCurrentUserTenantInsteadOfRequestParameter() {
    MenusController controller = new MenusController(menuService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(9L, 1L, "TENANT_ADMIN");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(menuService.listTree(1L, "settings", "ENABLED")).thenReturn(List.of());

    controller.tree(999L, "settings", "ENABLED", request);

    verify(menuService).listTree(1L, "settings", "ENABLED");
  }

  @Test
  void permissionCreateShouldRejectForeignTenantMenu() {
    PermissionsController controller =
        new PermissionsController(permissionService, menuService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(10L, 1L, "TENANT_ADMIN");
    Menu foreignMenu = new Menu();
    foreignMenu.setId(88L);
    foreignMenu.setTenantId(2L);
    PermissionCreateUpdateDto dto = new PermissionCreateUpdateDto();
    dto.setMenuId("88");
    dto.setPermissionCode("settings:edit");
    dto.setPermissionName("编辑配置");
    dto.setResourceType("BUTTON");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(menuService.getById(88L)).thenReturn(foreignMenu);

    assertThatThrownBy(() -> controller.create(dto, request))
        .isInstanceOf(BizException.class)
        .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(400));

    verifyNoInteractions(permissionService);
  }

  @Test
  void menuCreateShouldRejectForeignTenantParentMenu() {
    MenusController controller = new MenusController(menuService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(12L, 1L, "TENANT_ADMIN");
    Menu foreignParent = new Menu();
    foreignParent.setId(66L);
    foreignParent.setTenantId(2L);
    MenuCreateUpdateDto dto = new MenuCreateUpdateDto();
    dto.setParentId("66");
    dto.setMenuCode("settings:test");
    dto.setMenuName("测试菜单");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(menuService.getById(66L)).thenReturn(foreignParent);

    assertThatThrownBy(() -> controller.create(dto, request))
        .isInstanceOf(BizException.class)
        .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(400));

    verify(menuService, never()).create(any(Menu.class));
  }

  @Test
  void rolePermissionsShouldFilterForeignTenantRelationsOnRead() {
    RolesController controller =
        new RolesController(
            roleService, menuService, permissionService, userContext, userService, accessGuard);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(13L, 1L, "TENANT_ADMIN");
    Role role = new Role();
    role.setId(77L);
    role.setTenantId(1L);

    Menu localMenu = new Menu();
    localMenu.setId(1001L);
    localMenu.setTenantId(1L);
    Menu foreignMenu = new Menu();
    foreignMenu.setId(2002L);
    foreignMenu.setTenantId(2L);

    com.xngl.infrastructure.persistence.entity.system.Permission localPermission =
        new com.xngl.infrastructure.persistence.entity.system.Permission();
    localPermission.setId(3003L);
    localPermission.setTenantId(1L);
    localPermission.setPermissionCode("roles:view");
    localPermission.setResourceType("BUTTON");
    com.xngl.infrastructure.persistence.entity.system.Permission foreignPermission =
        new com.xngl.infrastructure.persistence.entity.system.Permission();
    foreignPermission.setId(4004L);
    foreignPermission.setTenantId(2L);
    foreignPermission.setPermissionCode("admin:export");
    foreignPermission.setResourceType("API");

    when(accessGuard.requireTenantManagementUser(request)).thenReturn(currentUser);
    when(roleService.getById(77L)).thenReturn(role);
    when(roleService.listMenuIdsByRoleId(77L)).thenReturn(List.of(1001L, 2002L));
    when(roleService.listPermissionIdsByRoleId(77L)).thenReturn(List.of(3003L, 4004L));
    when(menuService.listByIds(List.of(1001L, 2002L))).thenReturn(List.of(localMenu, foreignMenu));
    when(permissionService.listByIds(List.of(3003L, 4004L)))
        .thenReturn(List.of(localPermission, foreignPermission));

    ApiResult<com.xngl.web.dto.user.RolePermissionsDto> result =
        controller.listPermissions(77L, request);

    assertThat(result.getData().getMenuIds()).containsExactly("1001");
    assertThat(result.getData().getPermissionIds()).containsExactly("3003");
    assertThat(result.getData().getButtonCodes()).containsExactly("roles:view");
    assertThat(result.getData().getApiCodes()).isEmpty();
  }

  private User buildUser(Long id, Long tenantId, String userType) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    user.setUserType(userType);
    return user;
  }
}
