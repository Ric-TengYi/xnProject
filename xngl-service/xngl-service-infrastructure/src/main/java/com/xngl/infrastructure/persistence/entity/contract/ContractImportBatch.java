package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_import_batch")
public class ContractImportBatch extends BaseEntity {

  private Long tenantId;
  private String batchNo;
  private String fileName;
  private String fileUrl;
  private String fileHash;
  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
  private String status;
  private Long operatorId;
  private String remark;
}
