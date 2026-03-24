package com.xngl.web.dto.vehicle;

import lombok.Data;

@Data
public class VehicleDisableRequestDto {

  private Long vehicleId;
  private String violationType;
  private String triggerTime;
  private String triggerLocation;
  private String penaltyResult;
  private Integer banDays;
  private String banStartTime;
  private String banEndTime;
  private String remark;
}
