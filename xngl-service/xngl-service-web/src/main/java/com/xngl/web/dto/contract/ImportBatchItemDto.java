package com.xngl.web.dto.contract;

import lombok.Data;

@Data
public class ImportBatchItemDto {

  private String id;
  private String batchNo;
  private String fileName;
  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
  private String status;
  private String operatorId;
  private String createTime;
}
