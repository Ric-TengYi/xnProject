package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleListItemDto {

  private String id;
  private String plateNo;
  private String vin;
  private String orgId;
  private String orgName;
  private String vehicleType;
  private String brand;
  private String model;
  private String energyType;
  private Integer axleCount;
  private BigDecimal loadWeight;
  private String driverName;
  private String driverPhone;
  private String fleetName;
  private String captainName;
  private String captainPhone;
  private Integer status;
  private String statusLabel;
  private String useStatus;
  private String runningStatus;
  private String runningStatusLabel;
  private BigDecimal currentSpeed;
  private BigDecimal currentMileage;
  private String nextMaintainDate;
  private String annualInspectionExpireDate;
  private String insuranceExpireDate;
  private String warningLabel;
  private String createTime;
  private String updateTime;
}
