package com.xngl.web.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportTrendItemDto {

  private String periodLabel;
  private BigDecimal volume;
  private BigDecimal amount;
  private Integer trips;
  private Integer activeCount;
}
