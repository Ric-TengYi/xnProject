package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleCardTransactionListItemDto {

  private String id;
  private String cardId;
  private String cardNo;
  private String cardType;
  private String cardTypeLabel;
  private String txnType;
  private String txnTypeLabel;
  private String orgId;
  private String orgName;
  private String vehicleId;
  private String plateNo;
  private BigDecimal amount;
  private BigDecimal balanceBefore;
  private BigDecimal balanceAfter;
  private String occurredAt;
  private String operatorName;
  private String remark;
}
