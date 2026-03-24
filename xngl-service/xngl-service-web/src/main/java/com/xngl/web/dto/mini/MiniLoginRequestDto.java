package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniLoginRequestDto {

  private String tenantId;
  private String username;
  private String password;
  private String mobile;
  private String smsCode;
  private String openId;
  private String unionId;
  private String deviceName;
}
