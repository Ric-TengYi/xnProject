package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCapacityItemDto {

  private String vehicleId;
  private String plateNo;
  private String orgName;
  private String fleetName;
  private String energyType;
  private String statusLabel;
  private BigDecimal averageVolume;
  private BigDecimal loadedMileage;
  private BigDecimal emptyMileage;
  private BigDecimal energyConsumption;
  private BigDecimal loadWeight;
  private BigDecimal currentMileage;
}
