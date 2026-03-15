package com.xngl.manager.contract.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ContractReceipt {

  private Long id;
  private Long contractId;
  private BigDecimal amount;
  private LocalDate receiptDate;
  private String voucherNo;
  private String remark;
  private String status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
