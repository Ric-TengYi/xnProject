package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UnitStatItemDto {

  private String orgId;
  private String orgName;
  private Integer contractCount;
  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private BigDecimal pendingAmount;
  private BigDecimal settledAmount;
  private BigDecimal agreedVolume;
}
