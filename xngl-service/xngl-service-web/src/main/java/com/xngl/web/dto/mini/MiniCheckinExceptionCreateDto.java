package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniCheckinExceptionCreateDto {

  private Long checkinId;
  private String exceptionType;
  private String reason;
  private String attachmentUrls;
}
