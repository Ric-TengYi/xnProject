package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
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

  // Site change specific fields
  private Long originalSiteId;
  private Long newSiteId;

  // Volume change specific fields
  private BigDecimal originalVolume;
  private BigDecimal newVolume;
  private BigDecimal volumeDelta;

  // Amount/Price change specific fields
  private BigDecimal originalAmount;
  private BigDecimal newAmount;
  private BigDecimal originalUnitPrice;
  private BigDecimal newUnitPrice;

  // Date change specific fields
  private LocalDate originalExpireDate;
  private LocalDate newExpireDate;

  // Rejection reason
  private String rejectReason;
}
