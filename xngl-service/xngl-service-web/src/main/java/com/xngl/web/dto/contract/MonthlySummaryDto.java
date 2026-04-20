package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MonthlySummaryDto {

  private String month;
  private Integer contractCount;
  private Integer newContractCount;
  private BigDecimal contractAmount;
  private BigDecimal receiptAmount;
  private BigDecimal settlementAmount;
  private BigDecimal agreedVolume;
  private BigDecimal actualVolume;
}
