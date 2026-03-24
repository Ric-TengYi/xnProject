package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniEventDto {

  private String id;
  private String eventNo;
  private String eventType;
  private String title;
  private String content;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String priority;
  private String status;
  private String reportAddress;
  private String attachmentUrls;
  private String deadlineTime;
  private String reportTime;
  private String closeTime;
  private String closeRemark;
}
