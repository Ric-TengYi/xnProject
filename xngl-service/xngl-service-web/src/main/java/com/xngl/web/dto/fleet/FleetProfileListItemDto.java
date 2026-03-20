package com.xngl.web.dto.fleet;

import lombok.Data;

@Data
public class FleetProfileListItemDto {

  private String id;
  private String orgId;
  private String orgName;
  private String fleetName;
  private String captainName;
  private String captainPhone;
  private Integer driverCountPlan;
  private Integer vehicleCountPlan;
  private String status;
  private String statusLabel;
  private String attendanceMode;
  private String remark;
}
