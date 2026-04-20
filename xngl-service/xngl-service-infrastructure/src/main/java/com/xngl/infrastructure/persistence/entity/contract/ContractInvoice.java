package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract_invoice")
public class ContractInvoice extends BaseEntity {

  private Long tenantId;
  private Long contractId;
  private String invoiceNo;
  private String invoiceType;
  private LocalDate invoiceDate;
  private BigDecimal amount;
  private BigDecimal taxRate;
  private BigDecimal taxAmount;
  private String status;
  private String remark;
}