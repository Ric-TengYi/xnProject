package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgAnalysisItemDto {

  private String orgId;
  private String orgName;
  private Integer activeProjectCount;
  private Integer totalVehicles;
  private Integer movingVehicles;
  private BigDecimal volume;
  private Integer warningCount;
  private Integer rank;
}
