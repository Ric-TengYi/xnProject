package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniPasswordChangeDto {

  private String oldPassword;
  private String smsCode;
  private String newPassword;
}
