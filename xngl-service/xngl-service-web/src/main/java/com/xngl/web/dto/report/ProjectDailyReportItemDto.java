package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDailyReportItemDto {

  private String projectId;
  private String projectCode;
  private String projectName;
  private String reportDate;
  private String orgName;
  private Integer vehicles;
  private Integer trips;
  private BigDecimal todayVolume;
  private BigDecimal totalVolume;
  private BigDecimal projectTotal;
  private Integer progressPercent;
  private String statusLabel;
}
