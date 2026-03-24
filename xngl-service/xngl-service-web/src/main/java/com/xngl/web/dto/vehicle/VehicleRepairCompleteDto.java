package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleRepairCompleteDto {

  private LocalDate completedDate;
  private String vendorName;
  private String repairManager;
  private String technicianName;
  private String acceptanceResult;
  private String signoffStatus;
  private String attachmentUrls;
  private BigDecimal actualAmount;
  private BigDecimal partsCost;
  private BigDecimal laborCost;
  private BigDecimal otherCost;
  private String remark;
}
