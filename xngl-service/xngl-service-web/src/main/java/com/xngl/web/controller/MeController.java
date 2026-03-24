package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.Tenant;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.manager.menu.MenuService;
import com.xngl.manager.permission.PermissionService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.tenant.TenantService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.dto.CurrentUserDto;
import com.xngl.web.auth.dto.CurrentUserPermissionsDto;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.MenuTreeNodeDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

  private static final long PERMISSION_VERSION = 1L;

  private final UserService userService;
  private final UserContext userContext;
  private final RoleService roleService;
  private final TenantService tenantService;
  private final MenuService menuService;
  private final PermissionService permissionService;

  public MeController(
      UserService userService,
      UserContext userContext,
      RoleService roleService,
      TenantService tenantService,
      MenuService menuService,
      PermissionService permissionService) {
    this.userService = userService;
    this.userContext = userContext;
    this.roleService = roleService;
    this.tenantService = tenantService;
    this.menuService = menuService;
    this.permissionService = permissionService;
  }

  @GetMapping("/me")
  public ApiResult<CurrentUserDto> me(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Role> roles = getCurrentRoles(user.getId());
    Tenant tenant = user.getTenantId() == null ? null : tenantService.getById(user.getTenantId());
    return ApiResult.ok(
        new CurrentUserDto(
            String.valueOf(user.getId()),
            user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null,
            user.getMainOrgId() != null ? String.valueOf(user.getMainOrgId()) : null,
            user.getUsername(),
            user.getName(),
            user.getUserType(),
            roles.stream().map(Role::getRoleCode).filter(Objects::nonNull).toList(),
            tenant != null ? tenant.getTenantType() : null));
  }

  @GetMapping("/me/permissions")
  public ApiResult<CurrentUserPermissionsDto> permissions(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Permission> permissions = getCurrentPermissions(user.getId());
    List<String> buttonCodes =
        permissions.stream()
            .filter(this::isButtonPermission)
            .map(Permission::getPermissionCode)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    List<String> apiCodes =
        permissions.stream()
            .filter(permission -> !isButtonPermission(permission))
            .map(Permission::getPermissionCode)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return ApiResult.ok(
        new CurrentUserPermissionsDto(buttonCodes, apiCodes, List.of(), PERMISSION_VERSION));
  }

  @GetMapping("/me/menus")
  public ApiResult<List<MenuTreeNodeDto>> menus(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Long> roleIds = userService.listRoleIdsByUserId(user.getId());
    Set<Long> menuIds = new LinkedHashSet<>();
    for (Long roleId : roleIds) {
      menuIds.addAll(roleService.listMenuIdsByRoleId(roleId));
    }
    List<Menu> menus =
        menuService.listByIds(List.copyOf(menuIds)).stream()
            .filter(menu -> !"DISABLED".equalsIgnoreCase(menu.getStatus()))
            .sorted(Comparator.comparing(Menu::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    return ApiResult.ok(buildMenuTree(menus, 0L));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private List<Role> getCurrentRoles(Long userId) {
    return roleService.listByIds(userService.listRoleIdsByUserId(userId));
  }

  private List<Permission> getCurrentPermissions(Long userId) {
    List<Long> roleIds = userService.listRoleIdsByUserId(userId);
    Set<Long> permissionIds = new LinkedHashSet<>();
    for (Long roleId : roleIds) {
      permissionIds.addAll(roleService.listPermissionIdsByRoleId(roleId));
    }
    return permissionService.listByIds(List.copyOf(permissionIds));
  }

  private boolean isButtonPermission(Permission permission) {
    String type = permission.getResourceType();
    return type == null
        || "BUTTON".equalsIgnoreCase(type)
        || "ACTION".equalsIgnoreCase(type);
  }

  private List<MenuTreeNodeDto> buildMenuTree(List<Menu> menus, Long parentId) {
    return menus.stream()
        .filter(menu -> Objects.equals(menu.getParentId(), parentId))
        .map(
            menu ->
                new MenuTreeNodeDto(
                    String.valueOf(menu.getId()),
                    menu.getMenuCode(),
                    menu.getMenuName(),
                    String.valueOf(menu.getParentId()),
                    menu.getMenuType(),
                    menu.getRoutePath(),
                    menu.getIcon(),
                    menu.getSortOrder(),
                    String.valueOf(menu.getVisibleFlag()),
                    menu.getStatus(),
                    (int) menus.stream().filter(child -> Objects.equals(child.getParentId(), menu.getId())).count(),
                    buildMenuTree(menus, menu.getId())))
        .collect(Collectors.toList());
  }
}
