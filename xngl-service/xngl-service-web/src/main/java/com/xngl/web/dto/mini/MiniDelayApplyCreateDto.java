package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniDelayApplyCreateDto {

  private String bizType;
  private Long bizId;
  private Long projectId;
  private Long siteId;
  private String requestedEndTime;
  private String reason;
  private String attachmentUrls;
}
