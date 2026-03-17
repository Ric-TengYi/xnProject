package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.auth.AuthService;
import com.xngl.manager.role.RoleService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.JwtUtils;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序登录/绑定，复用现有用户体系。
 * 契约：POST /api/mini/auth/login 入参 username、password；出参 code、data.token、data.user.id/name/role。
 */
@RestController
@RequestMapping("/api/mini/auth")
public class MiniAuthController {

  private static final long DEFAULT_TENANT_ID = 1L;

  private final JwtUtils jwtUtils;
  private final AuthService authService;
  private final UserService userService;
  private final RoleService roleService;

  public MiniAuthController(
      JwtUtils jwtUtils,
      AuthService authService,
      UserService userService,
      RoleService roleService) {
    this.jwtUtils = jwtUtils;
    this.authService = authService;
    this.userService = userService;
    this.roleService = roleService;
  }

  @PostMapping("/login")
  public ApiResult<MiniLoginResponse> login(@Valid @RequestBody MiniLoginRequest req) {
    Long tenantId = req.getTenantId() != null ? req.getTenantId() : DEFAULT_TENANT_ID;
    User user = authService.getByTenantAndUsername(tenantId, req.getUsername());
    if (user == null || !authService.checkPassword(user, req.getPassword())) {
      throw new BizException(401, "用户名或密码错误");
    }
    if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
      throw new BizException(403, "用户已被禁用");
    }
    String userId = String.valueOf(user.getId());
    String token = jwtUtils.createToken(user.getUsername(), userId);
    String role = firstRoleCode(user.getId());
    String userType = user.getUserType() != null ? user.getUserType() : "TENANT_USER";
    MiniLoginResponse.UserInfo userInfo =
        new MiniLoginResponse.UserInfo(user.getId(), user.getName(), role, userType);
    return ApiResult.ok(new MiniLoginResponse(token, userInfo));
  }

  private String firstRoleCode(Long userId) {
    List<Long> roleIds = userService.listRoleIdsByUserId(userId);
    if (roleIds.isEmpty()) return "USER";
    var role = roleService.getById(roleIds.get(0));
    return role != null && role.getRoleCode() != null ? role.getRoleCode() : "USER";
  }

  @lombok.Data
  public static class MiniLoginRequest {
    private Long tenantId;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
  }

  @lombok.Data
  public static class MiniLoginResponse {
    private String token;
    private UserInfo user;

    public MiniLoginResponse(String token, UserInfo user) {
      this.token = token;
      this.user = user;
    }

    @lombok.Data
    public static class UserInfo {
      private Long id;
      private String name;
      private String role;
      private String userType;

      public UserInfo(Long id, String name, String role, String userType) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.userType = userType != null ? userType : "TENANT_USER";
      }
    }
  }
}
