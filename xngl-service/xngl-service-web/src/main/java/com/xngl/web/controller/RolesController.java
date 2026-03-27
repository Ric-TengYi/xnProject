package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.DataScopeRule;
import com.xngl.infrastructure.persistence.entity.system.Permission;
import com.xngl.manager.permission.PermissionService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
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
  private final PermissionService permissionService;
  private final UserContext userContext;
  private final UserService userService;

  public RolesController(RoleService roleService, PermissionService permissionService, UserContext userContext, UserService userService) {
    this.roleService = roleService;
    this.permissionService = permissionService;
    this.userContext = userContext;
    this.userService = userService;
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
    User currentUser = userContext.requireCurrentUser(request);
    List<Long> roleIds = userService.listRoleIdsByUserId(currentUser.getId());
    Role currentUserRole = roleIds.isEmpty() ? null : roleService.getById(roleIds.get(0));
    IPage<Role> page = currentUserRole != null
        ? roleService.pageWithPermissionFilter(keyword, tenantId, roleScope, status, pageNo, pageSize, currentUserRole)
        : roleService.page(keyword, tenantId, roleScope, status, pageNo, pageSize);
    List<RoleListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<RoleDetailDto> get(@PathVariable Long id) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
    return ApiResult.ok(toDetail(r));
  }

  @GetMapping("/{id}/permissions")
  public ApiResult<RolePermissionsDto> listPermissions(@PathVariable Long id) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
    List<Long> menuIds = roleService.listMenuIdsByRoleId(id);
    List<Long> permissionIds = roleService.listPermissionIdsByRoleId(id);
    List<Permission> permissions = permissionService.listByIds(permissionIds);
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
      @PathVariable Long id, @RequestBody java.util.Map<String, List<Long>> body) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
    List<Long> menuIds = body.get("menuIds");
    List<Long> permissionIds = body.get("permissionIds");
    roleService.updatePermissions(id, menuIds, permissionIds);
    return ApiResult.ok();
  }

  @GetMapping("/{id}/data-scope-rules")
  public ApiResult<List<DataScopeRuleDto>> listDataScopeRules(@PathVariable Long id) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
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
      @PathVariable Long id, @RequestBody List<DataScopeRuleDto> dtos) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
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
    User currentUser = userContext.requireCurrentUser(request);
    List<Long> roleIds = userService.listRoleIdsByUserId(currentUser.getId());
    Role currentUserRole = roleIds.isEmpty() ? null : roleService.getById(roleIds.get(0));
    if (currentUserRole == null) return ApiResult.fail(403, "无法获取当前用户角色");

    Role r = new Role();
    mapToEntity(dto, r);
    roleService.validateRoleCreation(currentUserRole, r);
    long id = roleService.create(r);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody RoleCreateUpdateDto dto, HttpServletRequest request) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");

    User currentUser = userContext.requireCurrentUser(request);
    List<Long> roleIds = userService.listRoleIdsByUserId(currentUser.getId());
    Role currentUserRole = roleIds.isEmpty() ? null : roleService.getById(roleIds.get(0));
    if (currentUserRole == null) return ApiResult.fail(403, "无法获取当前用户角色");

    mapToEntity(dto, r);
    roleService.validateRoleCreation(currentUserRole, r);
    r.setId(id);
    roleService.update(r);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    Role r = roleService.getById(id);
    if (r == null) return ApiResult.fail(404, "角色不存在");
    roleService.delete(id);
    return ApiResult.ok();
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
