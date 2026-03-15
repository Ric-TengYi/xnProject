package com.xngl.manager.site.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SiteSettlement {

  private Long id;
  private Long siteId;
  private String settlementNo;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private BigDecimal totalAmount;
  private BigDecimal settledAmount;
  private String status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
