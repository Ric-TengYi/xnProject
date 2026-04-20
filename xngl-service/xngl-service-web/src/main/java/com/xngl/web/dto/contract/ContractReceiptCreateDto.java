package com.xngl.web.dto.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ContractReceiptCreateDto {

  @NotNull(message = "入账金额不能为空")
  @DecimalMin(value = "0.01", message = "入账金额必须大于 0")
  private BigDecimal amount;

  @NotNull(message = "入账日期不能为空")
  private LocalDate receiptDate;

  private String receiptType;
  private String voucherNo;
  private String bankFlowNo;
  private Boolean isThreeParty;
  private String sourceType;
  private BigDecimal unitPriceInside;
  private BigDecimal unitPriceOutside;
  private String applicantId;
  private String rejectReason;
  private Integer changeVersion;

  @Size(max = 500, message = "备注长度不能超过 500")
  private String remark;
}
