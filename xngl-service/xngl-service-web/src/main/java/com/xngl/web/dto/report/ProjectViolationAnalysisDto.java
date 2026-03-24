package com.xngl.web.dto.report;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectViolationAnalysisDto {

  private ProjectViolationSummaryDto summary;
  private List<ProjectViolationStatItemDto> byFleet;
  private List<ProjectViolationStatItemDto> byPlate;
  private List<ProjectViolationStatItemDto> byTeam;
}
