package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogListItemDto {

  private String id;
  private String userId;
  private String username;
  private String loginTime;
  private String ip;
  private Boolean success;
  private String failReason;
}
