package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoTicketDto {

  private String ticket;
  private String loginUrl;
  private String targetPlatform;
  private String expiresAt;
}
