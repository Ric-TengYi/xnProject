package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniAuthCodeRequestDto {

  private String tenantId;
  private String username;
  private String password;
  private String mobile;
  private String openId;
}
