package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleMaintenanceExecuteDto {

  private LocalDate serviceDate;
  private BigDecimal odometer;
  private String vendorName;
  private BigDecimal costAmount;
  private BigDecimal laborCost;
  private BigDecimal materialCost;
  private BigDecimal externalCost;
  private String items;
  private String issueDescription;
  private String resultSummary;
  private String operatorName;
  private String technicianName;
  private String checkerName;
  private String signoffStatus;
  private String attachmentUrls;
  private String status;
  private String remark;
  private LocalDate nextMaintainDate;
  private BigDecimal nextOdometer;
}
