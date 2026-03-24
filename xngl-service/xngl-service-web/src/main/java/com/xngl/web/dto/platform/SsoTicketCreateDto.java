package com.xngl.web.dto.platform;

import lombok.Data;

@Data
public class SsoTicketCreateDto {

  private String targetPlatform;
  private String redirectUri;
}
