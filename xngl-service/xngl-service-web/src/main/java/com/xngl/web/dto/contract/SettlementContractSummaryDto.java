package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementContractSummaryDto {

  private String contractId;
  private String contractNo;
  private Integer itemCount;
  private BigDecimal totalVolume;
  private BigDecimal totalAmount;
  private BigDecimal averageUnitPrice;
}
