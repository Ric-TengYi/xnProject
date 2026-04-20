package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FleetTransportPlanListItemDto {

  private String id;
  private String fleetId;
  private String fleetName;
  private String orgId;
  private String orgName;
  private String planNo;
  private String planDate;
  private String sourcePoint;
  private String destinationPoint;
  private String cargoType;
  private Integer plannedTrips;
  private BigDecimal plannedVolume;
  private String status;
  private String statusLabel;
  private String remark;
}
