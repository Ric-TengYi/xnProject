package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.system.Menu;
import com.xngl.manager.menu.MenuService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
public class MenusController {

  private final MenuService menuService;

  public MenusController(MenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping("/tree")
  public ApiResult<List<MenuTreeNodeDto>> tree(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status) {
    if (tenantId == null) return ApiResult.ok(List.of());
    List<Menu> list = menuService.listTree(tenantId, keyword, status);
    List<MenuTreeNodeDto> tree = buildTree(list, 0L);
    return ApiResult.ok(tree);
  }

  @GetMapping("/{id}")
  public ApiResult<MenuDetailDto> get(@PathVariable Long id) {
    Menu m = menuService.getById(id);
    if (m == null) return ApiResult.fail(404, "菜单不存在");
    return ApiResult.ok(toDetail(m));
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody MenuCreateUpdateDto dto) {
    Menu m = new Menu();
    mapToEntity(dto, m);
    m.setStatus("ENABLED");
    m.setParentId(dto.getParentId() != null && !dto.getParentId().isEmpty() ? Long.parseLong(dto.getParentId()) : 0L);
    long id = menuService.create(m);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody MenuCreateUpdateDto dto) {
    Menu m = menuService.getById(id);
    if (m == null) return ApiResult.fail(404, "菜单不存在");
    mapToEntity(dto, m);
    m.setId(id);
    if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
      m.setParentId(Long.parseLong(dto.getParentId()));
    }
    menuService.update(m);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    Menu m = menuService.getById(id);
    if (m == null) return ApiResult.fail(404, "菜单不存在");
    menuService.delete(id);
    return ApiResult.ok();
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
