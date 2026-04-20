package com.xngl.web.dto.fleet;

import com.xngl.web.dto.vehicle.VehicleTrackPointDto;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class FleetTrackingHistoryDto {

  private String vehicleId;
  private String plateNo;
  private String fleetId;
  private String fleetName;
  private String startTime;
  private String endTime;
  private String dispatchOrderNo;
  private String relatedPlanNo;
  private String sourcePoint;
  private String destinationPoint;
  private String cargoType;
  private Integer pointCount;
  private BigDecimal totalDistanceKm;
  private BigDecimal maxSpeed;
  private BigDecimal averageSpeed;
  private List<VehicleTrackPointDto> points;
  private List<FleetTrackingStopDto> stops;
}
