package com.xngl.web.controller;

import com.xngl.web.auth.dto.CurrentUserDto;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

  @GetMapping("/me")
  public ApiResult<CurrentUserDto> me(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    String username = (String) request.getAttribute("username");
    if (userId == null || username == null) {
      throw new BizException(401, "未登录或 token 无效");
    }
    return ApiResult.ok(new CurrentUserDto(userId, username, "管理员"));
  }
}
