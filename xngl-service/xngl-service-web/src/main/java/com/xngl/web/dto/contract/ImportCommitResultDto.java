package com.xngl.web.dto.contract;

import lombok.Data;

@Data
public class ImportCommitResultDto {

  private String batchId;
  private Integer successCount;
  private Integer failCount;
  private String status;
}
