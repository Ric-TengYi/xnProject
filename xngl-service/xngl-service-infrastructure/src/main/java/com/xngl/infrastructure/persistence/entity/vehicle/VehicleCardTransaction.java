package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_card_transaction")
public class VehicleCardTransaction extends BaseEntity {

  private Long tenantId;
  private Long cardId;
  private String cardNo;
  private String cardType;
  private String txnType;
  private Long orgId;
  private Long vehicleId;
  private BigDecimal amount;
  private BigDecimal balanceBefore;
  private BigDecimal balanceAfter;
  private LocalDateTime occurredAt;
  private String operatorName;
  private String remark;
}
