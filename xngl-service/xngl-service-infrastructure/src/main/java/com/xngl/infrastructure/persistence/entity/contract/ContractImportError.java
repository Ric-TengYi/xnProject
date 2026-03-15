package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_import_error")
public class ContractImportError extends BaseEntity {

  private Long tenantId;
  private Long batchId;
  private Integer rowNo;
  private String contractNo;
  private String errorCode;
  private String errorMessage;
  private String rawJson;
}
