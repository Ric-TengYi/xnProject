package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSiteSummaryDto {

  private String siteId;
  private String siteName;
  private String siteType;
  private BigDecimal capacity;
  private BigDecimal lng;
  private BigDecimal lat;
  private Long contractCount;
  private BigDecimal contractVolume;
  private BigDecimal disposedVolume;
  private BigDecimal remainingVolume;
}
