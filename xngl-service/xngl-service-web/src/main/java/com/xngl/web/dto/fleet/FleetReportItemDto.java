package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FleetReportItemDto {

  private String fleetId;
  private String fleetName;
  private String orgName;
  private Integer totalPlans;
  private Integer totalDispatchOrders;
  private Integer approvedDispatchOrders;
  private BigDecimal plannedVolume;
  private BigDecimal revenueAmount;
  private BigDecimal costAmount;
  private BigDecimal profitAmount;
}
