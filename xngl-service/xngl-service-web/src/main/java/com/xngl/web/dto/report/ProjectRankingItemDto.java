package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRankingItemDto {

  private String projectId;
  private String projectCode;
  private String projectName;
  private String orgName;
  private BigDecimal total;
  private BigDecimal used;
  private BigDecimal today;
  private Integer rank;
  private String status;
  private Integer progressPercent;
}
