package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.Role;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.org.OrgService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsersController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final String DEFAULT_INITIAL_PASSWORD = "123456";

  private final UserService userService;
  private final OrgService orgService;
  private final RoleService roleService;

  public UsersController(UserService userService, OrgService orgService, RoleService roleService) {
    this.userService = userService;
    this.orgService = orgService;
    this.roleService = roleService;
  }

  @GetMapping
  public ApiResult<PageResult<UserListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    IPage<User> page =
        userService.page(tenantId, keyword, orgId, status, pageNo, pageSize);
    List<UserListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<UserDetailDto> get(@PathVariable Long id) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    return ApiResult.ok(toDetail(u));
  }

  @GetMapping("/{id}/roles")
  public ApiResult<List<RoleOptionDto>> listRoles(@PathVariable Long id) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    return ApiResult.ok(toRoleOptions(userService.listRoleIdsByUserId(id)));
  }

  @GetMapping("/{id}/orgs")
  public ApiResult<List<OrgOptionDto>> listOrgs(@PathVariable Long id) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    return ApiResult.ok(toOrgOptions(userService.listOrgIdsByUserId(id)));
  }

  @PostMapping
  @Transactional(rollbackFor = Exception.class)
  public ApiResult<String> create(@RequestBody UserCreateUpdateDto dto) {
    if (!StringUtils.hasText(dto.getTenantId())
        || !StringUtils.hasText(dto.getUsername())
        || !StringUtils.hasText(dto.getName())
        || !StringUtils.hasText(dto.getUserType())
        || !StringUtils.hasText(dto.getMainOrgId())) {
      return ApiResult.fail(400, "租户、账号、姓名、用户类型、主组织必填");
    }
    User u = new User();
    mapToEntity(dto, u);
    u.setStatus("ENABLED");
    u.setLockStatus(0);
    u.setAuthSource("LOCAL");
    if (!StringUtils.hasText(dto.getPassword())) {
      u.setPasswordEncrypted(DEFAULT_INITIAL_PASSWORD);
      u.setNeedResetPassword(1);
    } else {
      u.setNeedResetPassword(0);
    }
    long id = userService.create(u);

    if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
      userService.updateRoles(
          id,
          dto.getRoleIds().stream()
              .map(Long::parseLong)
              .collect(Collectors.toList()));
    }

    Long mainOrgId = parseNullableLong(dto.getMainOrgId());
    if (mainOrgId != null || (dto.getOrgIds() != null && !dto.getOrgIds().isEmpty())) {
      List<Long> orgIds =
          dto.getOrgIds() == null
              ? Collections.emptyList()
              : dto.getOrgIds().stream().map(Long::parseLong).collect(Collectors.toList());
      userService.updateOrgs(id, mainOrgId, orgIds);
    }
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  @Transactional(rollbackFor = Exception.class)
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody UserCreateUpdateDto dto) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    mapToEntity(dto, u);
    u.setId(id);
    userService.update(u);

    if (dto.getRoleIds() != null) {
      userService.updateRoles(
          id,
          dto.getRoleIds().stream()
              .map(Long::parseLong)
              .collect(Collectors.toList()));
    }
    if (dto.getOrgIds() != null || parseNullableLong(dto.getMainOrgId()) != null) {
      Long mainOrgId = parseNullableLong(dto.getMainOrgId());
      List<Long> orgIds =
          dto.getOrgIds() == null
              ? userService.listOrgIdsByUserId(id)
              : dto.getOrgIds().stream().map(Long::parseLong).collect(Collectors.toList());
      userService.updateOrgs(id, mainOrgId, orgIds);
    }
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDto dto) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    userService.updateStatus(id, dto.getStatus());
    return ApiResult.ok();
  }

  @PutMapping("/{id}/password")
  public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordDto dto) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty()) {
      return ApiResult.fail(400, "新密码必填");
    }
    userService.resetPassword(id, dto.getNewPassword());
    return ApiResult.ok();
  }

  @PutMapping("/{id}/roles")
  public ApiResult<Void> updateRoles(@PathVariable Long id, @RequestBody UserRolesUpdateDto dto) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    List<Long> roleIds =
        dto.getRoleIds() == null
            ? Collections.emptyList()
            : dto.getRoleIds().stream().map(Long::parseLong).collect(Collectors.toList());
    userService.updateRoles(id, roleIds);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/orgs")
  public ApiResult<Void> updateOrgs(@PathVariable Long id, @RequestBody UserOrgsUpdateDto dto) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    Long mainOrgId =
        dto.getMainOrgId() != null && !dto.getMainOrgId().isEmpty()
            ? Long.parseLong(dto.getMainOrgId())
            : null;
    List<Long> orgIds =
        dto.getOrgIds() == null
            ? Collections.emptyList()
            : dto.getOrgIds().stream().map(Long::parseLong).collect(Collectors.toList());
    userService.updateOrgs(id, mainOrgId, orgIds);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    User u = userService.getById(id);
    if (u == null) return ApiResult.fail(404, "用户不存在");
    userService.delete(id);
    return ApiResult.ok();
  }

  private UserListItemDto toListItem(User u) {
    Org mainOrg = u.getMainOrgId() != null ? orgService.getById(u.getMainOrgId()) : null;
    List<String> roleNames =
        roleService.listByIds(userService.listRoleIdsByUserId(u.getId())).stream()
            .map(Role::getRoleName)
            .collect(Collectors.toList());
    return new UserListItemDto(
        String.valueOf(u.getId()),
        u.getUsername(),
        u.getName(),
        u.getMobile(),
        u.getMainOrgId() != null ? String.valueOf(u.getMainOrgId()) : null,
        mainOrg != null ? mainOrg.getOrgName() : null,
        roleNames,
        u.getStatus(),
        formatDateTime(u.getLastLoginTime()));
  }

  private UserDetailDto toDetail(User u) {
    Org mainOrg = u.getMainOrgId() != null ? orgService.getById(u.getMainOrgId()) : null;
    return new UserDetailDto(
        String.valueOf(u.getId()),
        String.valueOf(u.getTenantId()),
        u.getUsername(),
        u.getName(),
        u.getMobile(),
        u.getEmail(),
        u.getUserType(),
        u.getMainOrgId() != null ? String.valueOf(u.getMainOrgId()) : null,
        mainOrg != null ? mainOrg.getOrgName() : null,
        toOrgOptions(userService.listOrgIdsByUserId(u.getId())),
        toRoleOptions(userService.listRoleIdsByUserId(u.getId())),
        u.getStatus(),
        u.getNeedResetPassword(),
        u.getLockStatus(),
        formatDateTime(u.getLastLoginTime()));
  }

  private void mapToEntity(UserCreateUpdateDto dto, User u) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      u.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    u.setUsername(dto.getUsername());
    u.setName(dto.getName());
    u.setMobile(dto.getMobile());
    u.setEmail(dto.getEmail());
    u.setUserType(dto.getUserType());
    if (dto.getMainOrgId() != null && !dto.getMainOrgId().isEmpty()) {
      u.setMainOrgId(Long.parseLong(dto.getMainOrgId()));
    }
    if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
      u.setPasswordEncrypted(dto.getPassword());
    }
  }

  private Long parseNullableLong(String value) {
    return value != null && !value.isEmpty() ? Long.parseLong(value) : null;
  }

  private List<OrgOptionDto> toOrgOptions(List<Long> orgIds) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Collections.emptyList();
    }
    return orgIds.stream()
        .map(orgService::getById)
        .filter(java.util.Objects::nonNull)
        .map(org -> new OrgOptionDto(String.valueOf(org.getId()), org.getOrgCode(), org.getOrgName()))
        .collect(Collectors.toList());
  }

  private List<RoleOptionDto> toRoleOptions(List<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      return Collections.emptyList();
    }
    return roleService.listByIds(roleIds).stream()
        .map(role -> new RoleOptionDto(String.valueOf(role.getId()), role.getRoleCode(), role.getRoleName()))
        .collect(Collectors.toList());
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(ISO) : null;
  }
}
