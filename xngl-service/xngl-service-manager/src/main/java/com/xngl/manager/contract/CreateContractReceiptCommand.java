package com.xngl.manager.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateContractReceiptCommand {

  private BigDecimal amount;
  private LocalDate receiptDate;
  private String receiptType;
  private String voucherNo;
  private String bankFlowNo;
  private String remark;
}
