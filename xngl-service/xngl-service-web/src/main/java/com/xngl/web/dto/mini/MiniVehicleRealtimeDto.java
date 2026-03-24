package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniVehicleRealtimeDto {

  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String driverName;
  private String fleetName;
  private String runningStatus;
  private String useStatus;
  private BigDecimal currentSpeed;
  private BigDecimal lng;
  private BigDecimal lat;
  private String gpsTime;
  private BigDecimal currentMileage;
}
