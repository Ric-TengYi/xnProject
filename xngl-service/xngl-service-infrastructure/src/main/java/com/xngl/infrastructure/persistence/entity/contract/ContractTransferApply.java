package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_transfer_apply")
public class ContractTransferApply extends BaseEntity {

  private Long tenantId;
  private String transferNo;
  private Long sourceContractId;
  private Long targetContractId;
  private BigDecimal transferAmount;
  private BigDecimal transferVolume;
  private String reason;
  private String approvalStatus;
  private String processInstanceId;
  private Long applicantId;
}
