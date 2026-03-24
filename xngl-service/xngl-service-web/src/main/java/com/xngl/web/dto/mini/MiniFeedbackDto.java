package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniFeedbackDto {

  private String id;
  private String feedbackType;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String title;
  private String content;
  private String attachmentUrls;
  private String reportAddress;
  private String status;
  private String handlerName;
  private String closeTime;
  private String closeRemark;
  private String linkedEventId;
  private String createTime;
}
