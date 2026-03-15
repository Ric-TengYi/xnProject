package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractReceiptItemDto {

  private String id;
  private String contractId;
  private String contractNo;
  private String contractName;
  private String receiptNo;
  private String receiptDate;
  private BigDecimal amount;
  private String receiptType;
  private String voucherNo;
  private String bankFlowNo;
  private String status;
  private String operatorId;
  private String remark;
  private String createTime;
}
