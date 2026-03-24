package com.xngl.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectViolationSummaryDto {

  private String reportPeriod;
  private int totalViolations;
  private int handledCount;
  private int pendingCount;
  private int vehicleCount;
  private int fleetCount;
  private int teamCount;
}
