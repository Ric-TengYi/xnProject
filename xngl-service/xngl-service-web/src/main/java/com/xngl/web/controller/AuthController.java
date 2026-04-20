package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;
import com.xngl.infrastructure.persistence.entity.system.SsoLoginTicket;
import com.xngl.infrastructure.persistence.mapper.SsoLoginTicketMapper;
import com.xngl.manager.auth.AuthService;
import com.xngl.manager.log.LoginLogService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.JwtUtils;
import com.xngl.web.auth.dto.LoginRequest;
import com.xngl.web.auth.dto.LoginResponse;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.platform.SsoTicketExchangeDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private static final long PERMISSION_VERSION = 1L;

  private final JwtUtils jwtUtils;
  private final AuthService authService;
  private final UserService userService;
  private final LoginLogService loginLogService;
  private final SsoLoginTicketMapper ssoLoginTicketMapper;

  public AuthController(
      JwtUtils jwtUtils,
      AuthService authService,
      UserService userService,
      LoginLogService loginLogService,
      SsoLoginTicketMapper ssoLoginTicketMapper) {
    this.jwtUtils = jwtUtils;
    this.authService = authService;
    this.userService = userService;
    this.loginLogService = loginLogService;
    this.ssoLoginTicketMapper = ssoLoginTicketMapper;
  }

  @PostMapping("/login")
  public ApiResult<LoginResponse> login(
      @Valid @RequestBody LoginRequest req, HttpServletRequest request) {
    try {
      Long tenantId = parseLong(req.getTenantId());
      User user = authService.getByTenantAndUsername(tenantId, req.getUsername());
      if (user == null || !authService.checkPassword(user, req.getPassword())) {
        saveLoginLogSafe(null, req.getUsername(), false, "用户名或密码错误", request);
        throw new BizException(401, "用户名或密码错误");
      }
      if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
        saveLoginLogSafe(user, req.getUsername(), false, "用户已被禁用", request);
        throw new BizException(403, "用户已被禁用");
      }

      String userId = String.valueOf(user.getId());
      String token = jwtUtils.createToken(user.getUsername(), userId);
      long expiresIn = jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
      userService.updateLastLoginTime(user.getId());

      saveLoginLogSafe(user, req.getUsername(), true, null, request);
      return ApiResult.ok(buildLoginResponse(user, token, expiresIn));
    } catch (BizException e) {
      throw e;
    } catch (DataAccessException e) {
      log.warn("Login failed due to data access error: {}", e.getMessage());
      throw new BizException(503, "服务暂时不可用，请稍后重试");
    } catch (Exception e) {
      log.warn("Login failed: {}", e.getMessage());
      throw new BizException(503, "服务暂时不可用，请稍后重试");
    }
  }

  private void saveLoginLogSafe(
      User user, String username, boolean success, String failReason, HttpServletRequest request) {
    try {
      saveLoginLog(user, username, success, failReason, request);
    } catch (Exception e) {
      log.warn("Failed to save login log: {}", e.getMessage());
    }
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

  @PostMapping("/sso/exchange")
  public ApiResult<LoginResponse> exchangeSsoTicket(@RequestBody SsoTicketExchangeDto req) {
    if (req == null || req.getTicket() == null || req.getTicket().isBlank()) {
      throw new BizException(400, "ticket 不能为空");
    }
    SsoLoginTicket ticket = authServiceExchangeTicket(req.getTicket().trim());
    User user = userService.getById(ticket.getUserId());
    if (user == null) {
      throw new BizException(404, "票据关联用户不存在");
    }
    if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
      throw new BizException(403, "用户已被禁用");
    }
    String token = jwtUtils.createToken(user.getUsername(), String.valueOf(user.getId()));
    long expiresIn =
        jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    User update = new User();
    update.setId(user.getId());
    update.setLastLoginTime(LocalDateTime.now());
    userService.update(update);
    ticket.setUsedFlag(1);
    ticket.setUsedTime(LocalDateTime.now());
    ssoLoginTicketMapper.updateById(ticket);
    return ApiResult.ok(buildLoginResponse(user, token, expiresIn));
  }

  private SsoLoginTicket authServiceExchangeTicket(String rawTicket) {
    SsoLoginTicket ticket =
        ssoLoginTicketMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SsoLoginTicket>()
                .eq(SsoLoginTicket::getTicket, rawTicket)
                .eq(SsoLoginTicket::getDeleted, 0)
                .last("limit 1"));
    if (ticket == null) {
      throw new BizException(404, "票据不存在");
    }
    if (ticket.getExpiresAt() == null || ticket.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BizException(401, "票据已过期");
    }
    if (ticket.getUsedFlag() != null && ticket.getUsedFlag() == 1) {
      throw new BizException(400, "票据已使用");
    }
    return ticket;
  }

  private LoginResponse buildLoginResponse(User user, String token, long expiresInMillis) {
    String userId = String.valueOf(user.getId());
    LoginResponse.UserInfo userInfo =
        new LoginResponse.UserInfo(
            userId,
            user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null,
            user.getMainOrgId() != null ? String.valueOf(user.getMainOrgId()) : null,
            user.getUsername(),
            user.getName(),
            user.getUserType());
    return new LoginResponse(token, "Bearer", expiresInMillis / 1000, PERMISSION_VERSION, userInfo);
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
