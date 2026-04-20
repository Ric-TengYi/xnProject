package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractStatsDto {

  private long totalContracts;
  private long effectiveContracts;
  private BigDecimal monthlyReceiptAmount;
  private long monthlyReceiptCount;
  private BigDecimal pendingReceiptAmount;
  private long totalSettlementOrders;
  private long pendingSettlementOrders;
}
