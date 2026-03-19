package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_approval_record")
public class ContractApprovalRecord extends BaseEntity {

  private Long tenantId;
  private Long contractId;
  private String actionType;
  private Long operatorId;
  private String fromStatus;
  private String toStatus;
  private String remark;
  private LocalDateTime operateTime;
}