package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FleetTransportPlanUpsertDto {

  private Long fleetId;
  private String planNo;
  private LocalDate planDate;
  private String sourcePoint;
  private String destinationPoint;
  private String cargoType;
  private Integer plannedTrips;
  private BigDecimal plannedVolume;
  private String status;
  private String remark;
}
