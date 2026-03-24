package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniEventCreateDto {

  private String eventType;
  private String title;
  private String content;
  private String attachmentUrls;
  private String priority;
  private Long projectId;
  private Long siteId;
  private String reportAddress;
  private String deadlineTime;
}
