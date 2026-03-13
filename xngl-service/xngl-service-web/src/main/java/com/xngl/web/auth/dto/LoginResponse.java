package com.xngl.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private String token;
  private String tokenType = "Bearer";
  private Long expiresIn;
  private Long permissionVersion;
  private UserInfo user;

  public LoginResponse(
      String accessToken, String tokenType, Long expiresIn, Long permissionVersion, UserInfo user) {
    this.accessToken = accessToken;
    this.token = accessToken;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
    this.permissionVersion = permissionVersion;
    this.user = user;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private String userId;
    private String tenantId;
    private String orgId;
    private String username;
    private String name;
    private String userType;
  }
}
