package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteRankingItemDto {

  private String siteId;
  private String siteName;
  private String siteType;
  private BigDecimal capacity;
  private BigDecimal used;
  private BigDecimal today;
  private Integer rank;
  private String status;
}
