package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniDelayApplyDto {

  private String id;
  private String bizType;
  private String bizId;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String requestedEndTime;
  private String reason;
  private String attachmentUrls;
  private String status;
  private String linkedEventId;
  private String createTime;
}
