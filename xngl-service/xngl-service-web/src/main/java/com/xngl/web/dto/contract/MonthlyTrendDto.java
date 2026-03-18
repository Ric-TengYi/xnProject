package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MonthlyTrendDto {

  private String month;
  private BigDecimal volume;
  private BigDecimal amount;
  private BigDecimal receiptAmount;
}
