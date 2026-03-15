package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settlement_order")
public class SettlementOrder extends BaseEntity {

  private Long tenantId;
  private String settlementNo;
  private String settlementType;
  private Long targetProjectId;
  private Long targetSiteId;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private LocalDate settlementDate;
  private BigDecimal totalVolume;
  private BigDecimal unitPrice;
  private BigDecimal totalAmount;
  private BigDecimal adjustAmount;
  private BigDecimal payableAmount;
  private String approvalStatus;
  private String settlementStatus;
  private String processInstanceId;
  private Long creatorId;
  private String remark;
}
