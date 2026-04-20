package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementStatsDto {

  private BigDecimal pendingAmount;
  private BigDecimal settledAmount;
  private long totalOrders;
  private long draftOrders;
  private long pendingOrders;
  private long settledOrders;
}
