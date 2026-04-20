package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReportItemDto {

  private String projectId;
  private String projectCode;
  private String projectName;
  private String orgName;
  private String reportPeriod;
  private BigDecimal periodVolume;
  private BigDecimal periodAmount;
  private Integer periodTrips;
  private BigDecimal accumulatedVolume;
  private BigDecimal projectTotal;
  private BigDecimal remainingVolume;
  private Integer progressPercent;
  private String status;
}
