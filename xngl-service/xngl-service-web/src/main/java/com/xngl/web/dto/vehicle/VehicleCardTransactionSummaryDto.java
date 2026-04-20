package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleCardTransactionSummaryDto {

  private Integer totalTransactions;
  private Integer rechargeTransactions;
  private Integer consumeTransactions;
  private BigDecimal totalRechargeAmount;
  private BigDecimal totalConsumeAmount;
}
