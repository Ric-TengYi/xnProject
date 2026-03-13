package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.manager.permission.PermissionService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
public class PermissionsController {

  private final PermissionService permissionService;

  public PermissionsController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GetMapping
  public ApiResult<PageResult<PermissionListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long menuId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    IPage<Permission> page =
        permissionService.page(tenantId, menuId, keyword, pageNo, pageSize);
    List<PermissionListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<PermissionDetailDto> get(@PathVariable Long id) {
    Permission p = permissionService.getById(id);
    if (p == null) return ApiResult.fail(404, "权限不存在");
    return ApiResult.ok(toDetail(p));
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody PermissionCreateUpdateDto dto) {
    Permission p = new Permission();
    mapToEntity(dto, p);
    p.setStatus("ENABLED");
    long id = permissionService.create(p);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody PermissionCreateUpdateDto dto) {
    Permission p = permissionService.getById(id);
    if (p == null) return ApiResult.fail(404, "权限不存在");
    mapToEntity(dto, p);
    p.setId(id);
    permissionService.update(p);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    Permission p = permissionService.getById(id);
    if (p == null) return ApiResult.fail(404, "权限不存在");
    permissionService.delete(id);
    return ApiResult.ok();
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
