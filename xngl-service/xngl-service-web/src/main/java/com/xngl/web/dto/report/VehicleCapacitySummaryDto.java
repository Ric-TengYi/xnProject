package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCapacitySummaryDto {

  private String periodType;
  private String reportPeriod;
  private Integer totalVehicles;
  private Integer activeVehicles;
  private BigDecimal averageVolume;
  private BigDecimal loadedMileage;
  private BigDecimal emptyMileage;
  private BigDecimal energyConsumption;
}
