package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniFeedbackCreateDto {

  private String feedbackType;
  private Long projectId;
  private Long siteId;
  private String title;
  private String content;
  private String attachmentUrls;
  private String reportAddress;
}
