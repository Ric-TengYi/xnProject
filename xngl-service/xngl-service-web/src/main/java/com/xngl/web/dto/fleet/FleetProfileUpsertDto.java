package com.xngl.web.dto.fleet;

import lombok.Data;

@Data
public class FleetProfileUpsertDto {

  private Long orgId;
  private String fleetName;
  private String captainName;
  private String captainPhone;
  private Integer driverCountPlan;
  private Integer vehicleCountPlan;
  private String status;
  private String attendanceMode;
  private String remark;
}
