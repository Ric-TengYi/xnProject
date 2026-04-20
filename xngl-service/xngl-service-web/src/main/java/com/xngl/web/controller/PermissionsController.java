package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.manager.menu.MenuService;
import com.xngl.manager.permission.PermissionService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.TenantManagementAccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
public class PermissionsController {

  private final PermissionService permissionService;
  private final MenuService menuService;
  private final TenantManagementAccessGuard accessGuard;

  public PermissionsController(
      PermissionService permissionService,
      MenuService menuService,
      TenantManagementAccessGuard accessGuard) {
    this.permissionService = permissionService;
    this.menuService = menuService;
    this.accessGuard = accessGuard;
  }

  @GetMapping
  public ApiResult<PageResult<PermissionListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long menuId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    IPage<Permission> page =
        permissionService.page(currentUser.getTenantId(), menuId, keyword, pageNo, pageSize);
    List<PermissionListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<PermissionDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Permission p = requirePermission(id, currentUser);
    return ApiResult.ok(toDetail(p));
  }

  @PostMapping
  public ApiResult<String> create(
      @RequestBody PermissionCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Permission p = new Permission();
    mapToEntity(dto, p);
    p.setTenantId(currentUser.getTenantId());
    ensureMenuBelongsToTenant(p.getMenuId(), currentUser.getTenantId());
    p.setStatus("ENABLED");
    long id = permissionService.create(p);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id, @RequestBody PermissionCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Permission p = requirePermission(id, currentUser);
    mapToEntity(dto, p);
    p.setId(id);
    p.setTenantId(currentUser.getTenantId());
    ensureMenuBelongsToTenant(p.getMenuId(), currentUser.getTenantId());
    permissionService.update(p);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requirePermission(id, currentUser);
    permissionService.delete(id);
    return ApiResult.ok();
  }

  private Permission requirePermission(Long id, User currentUser) {
    Permission permission = permissionService.getById(id);
    if (permission == null) {
      throw new BizException(404, "权限不存在");
    }
    accessGuard.ensureSameTenant(permission.getTenantId(), currentUser.getTenantId(), "权限");
    return permission;
  }

  private void ensureMenuBelongsToTenant(Long menuId, Long tenantId) {
    if (menuId == null) {
      return;
    }
    Menu menu = menuService.getById(menuId);
    if (menu == null || !Objects.equals(menu.getTenantId(), tenantId)) {
      throw new BizException(400, "关联菜单不存在或不属于当前租户");
    }
  }

  private PermissionListItemDto toListItem(Permission p) {
    return new PermissionListItemDto(
        String.valueOf(p.getId()),
        p.getPermissionCode(),
        p.getPermissionName(),
        p.getMenuId() != null ? String.valueOf(p.getMenuId()) : null,
        p.getResourceType(),
        p.getStatus());
  }

  private PermissionDetailDto toDetail(Permission p) {
    return new PermissionDetailDto(
        String.valueOf(p.getId()),
        String.valueOf(p.getTenantId()),
        p.getMenuId() != null ? String.valueOf(p.getMenuId()) : null,
        p.getPermissionCode(),
        p.getPermissionName(),
        p.getResourceType(),
        p.getStatus());
  }

  private void mapToEntity(PermissionCreateUpdateDto dto, Permission p) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      p.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    if (dto.getMenuId() != null && !dto.getMenuId().isEmpty()) {
      p.setMenuId(Long.parseLong(dto.getMenuId()));
    }
    p.setPermissionCode(dto.getPermissionCode());
    p.setPermissionName(dto.getPermissionName());
    p.setResourceType(dto.getResourceType());
  }
}
