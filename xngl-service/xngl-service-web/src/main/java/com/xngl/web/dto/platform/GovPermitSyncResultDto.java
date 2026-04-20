package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovPermitSyncResultDto {

  private String batchNo;
  private String syncMode;
  private Integer totalCount;
  private Integer createdCount;
  private Integer updatedCount;
  private Integer successCount;
  private Integer failCount;
  private String syncTime;
}
