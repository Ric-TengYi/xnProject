package com.xngl.web.dto.fleet;

import java.time.LocalDate;
import lombok.Data;

@Data
public class FleetDispatchOrderUpsertDto {

  private Long fleetId;
  private String relatedPlanNo;
  private LocalDate applyDate;
  private Integer requestedVehicleCount;
  private Integer requestedDriverCount;
  private String urgencyLevel;
  private String status;
  private String applicantName;
  private String remark;
}
