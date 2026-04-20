package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleFleetSummaryDto {

  private String id;
  private String fleetName;
  private String orgId;
  private String orgName;
  private String captainName;
  private String captainPhone;
  private long driverCount;
  private long totalVehicles;
  private long activeVehicles;
  private long movingVehicles;
  private long warningVehicles;
  private BigDecimal totalLoadTons;
  private BigDecimal avgLoadTons;
  private String statusLabel;
}
