package com.xngl.web.controller;

import com.xngl.web.auth.JwtUtils;
import com.xngl.web.auth.dto.LoginRequest;
import com.xngl.web.auth.dto.LoginResponse;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final JwtUtils jwtUtils;

  public AuthController(JwtUtils jwtUtils) {
    this.jwtUtils = jwtUtils;
  }

  @PostMapping("/login")
  public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    // TODO: 调用 manager 校验用户名密码
    if (!"admin".equals(req.getUsername()) || !"admin".equals(req.getPassword())) {
      throw new BizException(401, "用户名或密码错误");
    }
    String userId = "1";
    String token = jwtUtils.createToken(req.getUsername(), userId);
    long expiresIn = jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    LoginResponse.UserInfo user = new LoginResponse.UserInfo(userId, req.getUsername(), "管理员");
    return ApiResult.ok(new LoginResponse(token, "Bearer", expiresIn / 1000, user));
  }

}
