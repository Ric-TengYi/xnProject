package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsersController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final UserService userService;

  public UsersController(UserService userService) {
    this.userService = userService;
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

  @PostMapping
  public ApiResult<String> create(@RequestBody UserCreateUpdateDto dto) {
    User u = new User();
    mapToEntity(dto, u);
    u.setStatus("ENABLED");
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
    return new UserListItemDto(
        String.valueOf(u.getId()),
        u.getUsername(),
        u.getName(),
        u.getMobile(),
        u.getMainOrgId() != null ? String.valueOf(u.getMainOrgId()) : null,
        null,
        Collections.emptyList(),
        u.getStatus(),
        null);
  }

  private UserDetailDto toDetail(User u) {
    List<Long> roleIds = userService.listRoleIdsByUserId(u.getId());
    List<Long> orgIds = userService.listOrgIdsByUserId(u.getId());
    return new UserDetailDto(
        String.valueOf(u.getId()),
        String.valueOf(u.getTenantId()),
        u.getUsername(),
        u.getName(),
        u.getMobile(),
        u.getEmail(),
        u.getUserType(),
        u.getMainOrgId() != null ? String.valueOf(u.getMainOrgId()) : null,
        null,
        orgIds.stream()
            .map(id -> new OrgOptionDto(String.valueOf(id), null, null))
            .collect(Collectors.toList()),
        roleIds.stream()
            .map(id -> new RoleOptionDto(String.valueOf(id), null, null))
            .collect(Collectors.toList()),
        u.getStatus(),
        null,
        null,
        null);
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
}
