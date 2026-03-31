package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.manager.menu.MenuService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.*;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.TenantManagementAccessGuard;
import com.xngl.infrastructure.persistence.entity.organization.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
public class MenusController {

  private final MenuService menuService;
  private final TenantManagementAccessGuard accessGuard;

  public MenusController(MenuService menuService, TenantManagementAccessGuard accessGuard) {
    this.menuService = menuService;
    this.accessGuard = accessGuard;
  }

  @GetMapping("/tree")
  public ApiResult<List<MenuTreeNodeDto>> tree(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    List<Menu> list = menuService.listTree(currentUser.getTenantId(), keyword, status);
    List<MenuTreeNodeDto> tree = buildTree(list, 0L);
    return ApiResult.ok(tree);
  }

  @GetMapping("/{id}")
  public ApiResult<MenuDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Menu m = requireMenu(id, currentUser);
    return ApiResult.ok(toDetail(m));
  }

  @PostMapping
  public ApiResult<String> create(
      @RequestBody MenuCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Menu m = new Menu();
    mapToEntity(dto, m);
    m.setTenantId(currentUser.getTenantId());
    m.setStatus("ENABLED");
    m.setParentId(dto.getParentId() != null && !dto.getParentId().isEmpty() ? Long.parseLong(dto.getParentId()) : 0L);
    ensureParentBelongsToTenant(m.getParentId(), currentUser.getTenantId());
    long id = menuService.create(m);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id, @RequestBody MenuCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    Menu m = requireMenu(id, currentUser);
    mapToEntity(dto, m);
    m.setId(id);
    m.setTenantId(currentUser.getTenantId());
    if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
      m.setParentId(Long.parseLong(dto.getParentId()));
    }
    ensureParentBelongsToTenant(m.getParentId(), currentUser.getTenantId());
    menuService.update(m);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = accessGuard.requireTenantManagementUser(request);
    requireMenu(id, currentUser);
    menuService.delete(id);
    return ApiResult.ok();
  }

  private Menu requireMenu(Long id, User currentUser) {
    Menu menu = menuService.getById(id);
    if (menu == null) {
      throw new BizException(404, "菜单不存在");
    }
    accessGuard.ensureSameTenant(menu.getTenantId(), currentUser.getTenantId(), "菜单");
    return menu;
  }

  private void ensureParentBelongsToTenant(Long parentId, Long tenantId) {
    if (parentId == null || parentId <= 0) {
      return;
    }
    Menu parent = menuService.getById(parentId);
    if (parent == null || !Objects.equals(parent.getTenantId(), tenantId)) {
      throw new BizException(400, "父级菜单不存在或不属于当前租户");
    }
  }

  private List<MenuTreeNodeDto> buildTree(List<Menu> list, Long parentId) {
    return list.stream()
        .filter(m -> Objects.equals(m.getParentId(), parentId))
        .map(
            m -> {
              List<MenuTreeNodeDto> children = buildTree(list, m.getId());
              return new MenuTreeNodeDto(
                  String.valueOf(m.getId()),
                  m.getMenuCode(),
                  m.getMenuName(),
                  String.valueOf(m.getParentId()),
                  m.getMenuType(),
                  m.getRoutePath(),
                  m.getIcon(),
                  m.getSortOrder(),
                  String.valueOf(m.getVisibleFlag()),
                  m.getStatus(),
                  children.size(),
                  children);
            })
        .collect(Collectors.toList());
  }

  private MenuDetailDto toDetail(Menu m) {
    return new MenuDetailDto(
        String.valueOf(m.getId()),
        String.valueOf(m.getTenantId()),
        String.valueOf(m.getParentId()),
        m.getMenuCode(),
        m.getMenuName(),
        m.getMenuType(),
        m.getRoutePath(),
        m.getComponentPath(),
        m.getIcon(),
        m.getSortOrder(),
        String.valueOf(m.getVisibleFlag()),
        m.getStatus());
  }

  private void mapToEntity(MenuCreateUpdateDto dto, Menu m) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      m.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    m.setMenuCode(dto.getMenuCode());
    m.setMenuName(dto.getMenuName());
    m.setMenuType(dto.getMenuType() != null ? dto.getMenuType() : "MENU");
    m.setRoutePath(dto.getPath());
    m.setComponentPath(dto.getComponent());
    m.setIcon(dto.getIcon());
    if (dto.getSortOrder() != null) m.setSortOrder(dto.getSortOrder());
    m.setVisibleFlag(dto.getVisible() != null && "1".equals(dto.getVisible()) ? 1 : 0);
  }
}
