package com.xngl.web.dto.report;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCapacityAnalysisDto {

  private VehicleCapacitySummaryDto summary;
  private List<ReportTrendItemDto> trend;
  private List<VehicleCapacityItemDto> records;
}
