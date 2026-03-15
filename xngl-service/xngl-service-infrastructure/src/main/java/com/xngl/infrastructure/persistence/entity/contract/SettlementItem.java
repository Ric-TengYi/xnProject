package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settlement_item")
public class SettlementItem extends BaseEntity {

  private Long tenantId;
  private Long settlementOrderId;
  private String sourceRecordType;
  private Long sourceRecordId;
  private Long projectId;
  private Long siteId;
  private Long vehicleId;
  private LocalDate bizDate;
  private BigDecimal volume;
  private BigDecimal unitPrice;
  private BigDecimal amount;
  private String remark;
}
