package com.xngl.web.dto.fleet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FleetTrackingSummaryDto {

  private Integer totalVehicles;
  private Integer movingVehicles;
  private Integer stoppedVehicles;
  private Integer offlineVehicles;
  private Integer deliveringVehicles;
  private Integer warningVehicles;
}
