package com.xngl.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  private String token;
  private String tokenType = "Bearer";
  private Long expiresIn;
  private UserInfo user;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private String userId;
    private String username;
    private String name;
  }
}
