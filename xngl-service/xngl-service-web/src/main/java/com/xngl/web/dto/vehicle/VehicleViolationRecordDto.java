package com.xngl.web.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleViolationRecordDto {

  private String id;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String violationType;
  private String triggerTime;
  private String triggerLocation;
  private String actionStatus;
  private String actionStatusLabel;
  private String penaltyResult;
  private String banStartTime;
  private String banEndTime;
  private String releaseTime;
  private String releaseReason;
  private String operatorName;
  private String remark;
}
