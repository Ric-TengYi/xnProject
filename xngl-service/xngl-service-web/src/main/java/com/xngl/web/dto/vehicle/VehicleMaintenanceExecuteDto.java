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
  private String items;
  private String operatorName;
  private String status;
  private String remark;
  private LocalDate nextMaintainDate;
  private BigDecimal nextOdometer;
}
