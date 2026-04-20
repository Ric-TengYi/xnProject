package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReportSummaryDto {

  private String periodType;
  private String reportPeriod;
  private Integer projectCount;
  private Integer activeProjectCount;
  private Integer totalTrips;
  private BigDecimal periodVolume;
  private BigDecimal periodAmount;
  private BigDecimal projectTotal;
  private BigDecimal accumulatedVolume;
  private Integer progressPercent;
}
