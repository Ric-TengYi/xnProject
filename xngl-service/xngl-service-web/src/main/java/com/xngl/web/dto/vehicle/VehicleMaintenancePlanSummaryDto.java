package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMaintenancePlanSummaryDto {

  private Integer totalPlans;
  private Integer activePlans;
  private Integer overduePlans;
  private Integer pausedPlans;
  private Integer recordCount;
  private BigDecimal totalCostAmount;
}
