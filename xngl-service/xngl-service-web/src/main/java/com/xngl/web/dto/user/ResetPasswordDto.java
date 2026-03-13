package com.xngl.web.dto.user;

import lombok.Data;

@Data
public class ResetPasswordDto {

  private String newPassword;
  private Boolean forceReset;
}
