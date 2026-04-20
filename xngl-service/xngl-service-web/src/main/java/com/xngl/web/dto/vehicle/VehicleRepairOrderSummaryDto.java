package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRepairOrderSummaryDto {

  private Integer totalOrders;
  private Integer pendingOrders;
  private Integer approvedOrders;
  private Integer inProgressOrders;
  private Integer completedOrders;
  private BigDecimal totalBudgetAmount;
  private BigDecimal totalActualAmount;
}
