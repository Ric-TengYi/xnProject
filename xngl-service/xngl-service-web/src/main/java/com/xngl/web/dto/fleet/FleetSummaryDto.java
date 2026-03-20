package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FleetSummaryDto {

  private Integer totalFleets;
  private Integer activeFleets;
  private Integer totalPlans;
  private Integer pendingDispatchOrders;
  private BigDecimal totalRevenueAmount;
  private BigDecimal totalProfitAmount;
}
