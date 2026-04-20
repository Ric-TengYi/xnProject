package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FleetFinanceSummaryDto {

  private Integer totalRecords;
  private Integer settledRecords;
  private BigDecimal totalRevenueAmount;
  private BigDecimal totalCostAmount;
  private BigDecimal totalProfitAmount;
  private BigDecimal totalOutstandingAmount;
}
