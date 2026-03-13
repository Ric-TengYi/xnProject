package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;
import com.xngl.manager.auth.AuthService;
import com.xngl.manager.log.LoginLogService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.JwtUtils;
import com.xngl.web.auth.dto.LoginRequest;
import com.xngl.web.auth.dto.LoginResponse;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final long PERMISSION_VERSION = 1L;

  private final JwtUtils jwtUtils;
  private final AuthService authService;
  private final UserService userService;
  private final LoginLogService loginLogService;

  public AuthController(
      JwtUtils jwtUtils,
      AuthService authService,
      UserService userService,
      LoginLogService loginLogService) {
    this.jwtUtils = jwtUtils;
    this.authService = authService;
    this.userService = userService;
    this.loginLogService = loginLogService;
  }

  @PostMapping("/login")
  public ApiResult<LoginResponse> login(
      @Valid @RequestBody LoginRequest req, HttpServletRequest request) {
    Long tenantId = parseLong(req.getTenantId());
    User user = authService.getByTenantAndUsername(tenantId, req.getUsername());
    if (user == null || !authService.checkPassword(user, req.getPassword())) {
      saveLoginLog(null, req.getUsername(), false, "用户名或密码错误", request);
      throw new BizException(401, "用户名或密码错误");
    }
    if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
      saveLoginLog(user, req.getUsername(), false, "用户已被禁用", request);
      throw new BizException(403, "用户已被禁用");
    }

    String userId = String.valueOf(user.getId());
    String token = jwtUtils.createToken(user.getUsername(), userId);
    long expiresIn = jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    User update = new User();
    update.setId(user.getId());
    update.setLastLoginTime(java.time.LocalDateTime.now());
    userService.update(update);

    saveLoginLog(user, req.getUsername(), true, null, request);
    LoginResponse.UserInfo userInfo =
        new LoginResponse.UserInfo(
            userId,
            user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null,
            user.getMainOrgId() != null ? String.valueOf(user.getMainOrgId()) : null,
            user.getUsername(),
            user.getName(),
            user.getUserType());
    return ApiResult.ok(
        new LoginResponse(token, "Bearer", expiresIn / 1000, PERMISSION_VERSION, userInfo));
  }

  private void saveLoginLog(
      User user, String username, boolean success, String failReason, HttpServletRequest request) {
    LoginLog log = new LoginLog();
    if (user != null) {
      log.setTenantId(user.getTenantId());
      log.setUserId(user.getId());
    }
    log.setUsername(username);
    log.setLoginType("ACCOUNT");
    log.setSuccessFlag(success ? 1 : 0);
    log.setIp(request.getRemoteAddr());
    log.setUserAgent(request.getHeader("User-Agent"));
    log.setFailReason(failReason);
    loginLogService.save(log);
  }

  /** 刷新令牌。契约 4.3。当前 stub：返回 501，前端可暂不调用。 */
  @PostMapping("/refresh")
  public ApiResult<?> refresh(@RequestBody RefreshRequest req) {
    throw new BizException(501, "刷新令牌暂未实现");
  }

  /** 登出。契约 4.4。当前 stub：仅返回成功，不维护服务端黑名单。 */
  @PostMapping("/logout")
  public ApiResult<LogoutResponse> logout() {
    return ApiResult.ok(new LogoutResponse(true));
  }

  private Long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      throw new BizException(400, "tenantId 格式错误");
    }
  }

  @lombok.Data
  public static class RefreshRequest {
    private String refreshToken;
  }

  @lombok.Data
  public static class LogoutResponse {
    private final boolean success;
  }
}
