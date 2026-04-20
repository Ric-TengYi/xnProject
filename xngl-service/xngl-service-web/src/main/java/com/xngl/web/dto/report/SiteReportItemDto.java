package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteReportItemDto {

  private String siteId;
  private String siteName;
  private String siteCode;
  private String siteType;
  private String reportPeriod;
  private BigDecimal periodVolume;
  private BigDecimal periodAmount;
  private Integer periodTrips;
  private BigDecimal accumulatedVolume;
  private BigDecimal capacity;
  private BigDecimal remainingCapacity;
  private Integer utilizationRate;
  private String status;
}
