package com.xngl.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectViolationStatItemDto {

  private String name;
  private int violationCount;
  private int handledCount;
  private int pendingCount;
  private String latestTriggerTime;
}
