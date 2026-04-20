package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {

  private String reportDate;
  private Integer totalSites;
  private Integer activeSites;
  private Integer totalProjects;
  private Integer activeProjects;
  private Integer totalOrgs;
  private Integer activeOrgs;
  private Integer totalVehicles;
  private Integer movingVehicles;
  private BigDecimal dailyVolume;
  private BigDecimal monthlyVolume;
  private Integer warningCount;
}
