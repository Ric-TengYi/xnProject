package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSyncLogDto {

  private String id;
  private String integrationCode;
  private String syncMode;
  private String bizType;
  private String batchNo;
  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
  private String status;
  private String operatorName;
  private String syncTime;
  private String requestPayload;
  private String responsePayload;
  private String remark;
}
