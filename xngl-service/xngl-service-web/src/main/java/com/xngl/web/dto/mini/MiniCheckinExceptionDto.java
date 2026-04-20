package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniCheckinExceptionDto {

  private String id;
  private String checkinId;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String exceptionType;
  private String reason;
  private String attachmentUrls;
  private String status;
  private String linkedEventId;
  private String createTime;
}
