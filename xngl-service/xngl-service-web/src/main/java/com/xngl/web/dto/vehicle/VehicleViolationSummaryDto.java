package com.xngl.web.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleViolationSummaryDto {

  private Integer totalCount;
  private Integer pendingCount;
  private Integer processedCount;
  private Integer disabledCount;
  private Integer releasedCount;
  private Integer vehicleCount;
}
