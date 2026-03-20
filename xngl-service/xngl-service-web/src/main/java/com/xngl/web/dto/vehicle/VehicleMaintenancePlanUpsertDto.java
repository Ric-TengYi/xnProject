package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleMaintenancePlanUpsertDto {

  private Long vehicleId;
  private String planType;
  private String cycleType;
  private Integer cycleValue;
  private LocalDate lastMaintainDate;
  private LocalDate nextMaintainDate;
  private BigDecimal lastOdometer;
  private BigDecimal nextOdometer;
  private String responsibleName;
  private String status;
  private String remark;
}
