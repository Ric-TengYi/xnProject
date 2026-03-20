package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteReportSummaryDto {

  private String periodType;
  private String reportPeriod;
  private Integer siteCount;
  private Integer activeSiteCount;
  private Integer totalTrips;
  private BigDecimal periodVolume;
  private BigDecimal periodAmount;
  private BigDecimal totalCapacity;
  private BigDecimal accumulatedVolume;
  private Integer utilizationRate;
}
