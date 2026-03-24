package com.xngl.manager.contract;

import lombok.Data;

@Data
public class ContractImportCommitResult {

  private Long batchId;
  private Integer successCount;
  private Integer failCount;
  private String status;
}
