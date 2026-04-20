package com.xngl.manager.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractInvoiceVo {
  private Long id;
  private Long contractId;
  private String invoiceNo;
  private String invoiceType;
  private LocalDate invoiceDate;
  private BigDecimal amount;
  private BigDecimal taxRate;
  private BigDecimal taxAmount;
  private String status;
  private String remark;
  private LocalDateTime createTime;
}