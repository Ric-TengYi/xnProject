package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleRepairOrderUpsertDto {

  private Long vehicleId;
  private String urgencyLevel;
  private String repairReason;
  private String repairContent;
  private BigDecimal budgetAmount;
  private LocalDate applyDate;
  private String applicantName;
  private String status;
  private String remark;
}
