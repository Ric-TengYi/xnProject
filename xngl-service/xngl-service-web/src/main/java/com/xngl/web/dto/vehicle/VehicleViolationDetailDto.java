package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleViolationDetailDto extends VehicleViolationRecordDto {

  private String vehicleType;
  private String brand;
  private String model;
  private String driverName;
  private String driverPhone;
  private String fleetName;
  private String useStatus;
  private Integer status;
  private BigDecimal currentSpeed;
  private BigDecimal currentMileage;
}
