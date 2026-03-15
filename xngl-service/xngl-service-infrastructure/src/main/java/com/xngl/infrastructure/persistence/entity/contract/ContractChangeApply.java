package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_change_apply")
public class ContractChangeApply extends BaseEntity {

  private Long tenantId;
  private String changeNo;
  private Long contractId;
  private String changeType;
  private String beforeSnapshotJson;
  private String afterSnapshotJson;
  private String reason;
  private String approvalStatus;
  private String processInstanceId;
  private String currentNodeCode;
  private Long applicantId;
}
