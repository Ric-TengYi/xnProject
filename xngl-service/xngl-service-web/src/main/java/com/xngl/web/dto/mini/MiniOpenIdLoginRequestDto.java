package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniOpenIdLoginRequestDto {

  private String openId;
  private String unionId;
  private String deviceName;
}
