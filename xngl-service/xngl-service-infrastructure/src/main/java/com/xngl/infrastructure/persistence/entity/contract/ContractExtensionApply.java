package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_extension_apply")
public class ContractExtensionApply extends BaseEntity {

  private Long tenantId;
  private String applyNo;
  private Long contractId;
  private LocalDate originalExpireDate;
  private LocalDate requestedExpireDate;
  private BigDecimal requestedVolumeDelta;
  private String reason;
  private String approvalStatus;
  private String processInstanceId;
  private Long applicantId;
}
