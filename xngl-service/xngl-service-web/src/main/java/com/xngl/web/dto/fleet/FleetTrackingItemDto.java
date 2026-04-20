package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FleetTrackingItemDto {

  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String fleetId;
  private String fleetName;
  private String driverName;
  private String driverPhone;
  private String trackingStatus;
  private String trackingStatusLabel;
  private String runningStatus;
  private String runningStatusLabel;
  private String vehicleStatusLabel;
  private String warningLabel;
  private BigDecimal currentSpeed;
  private BigDecimal currentMileage;
  private BigDecimal lng;
  private BigDecimal lat;
  private String gpsTime;
  private String dispatchOrderNo;
  private String dispatchStatus;
  private String dispatchStatusLabel;
  private String relatedPlanNo;
  private String sourcePoint;
  private String destinationPoint;
  private String cargoType;
}
