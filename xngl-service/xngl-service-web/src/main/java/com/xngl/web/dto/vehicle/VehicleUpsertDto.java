package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleUpsertDto {

  private String plateNo;
  private String vin;
  private Long orgId;
  private String vehicleType;
  private String brand;
  private String model;
  private String energyType;
  private Integer axleCount;
  private BigDecimal deadWeight;
  private BigDecimal loadWeight;
  private String driverName;
  private String driverPhone;
  private String fleetName;
  private String captainName;
  private String captainPhone;
  private Integer status;
  private String useStatus;
  private String runningStatus;
  private BigDecimal currentSpeed;
  private BigDecimal currentMileage;
  private LocalDate nextMaintainDate;
  private LocalDate annualInspectionExpireDate;
  private LocalDate insuranceExpireDate;
  private BigDecimal lng;
  private BigDecimal lat;
  private LocalDateTime gpsTime;
  private String remark;
}
