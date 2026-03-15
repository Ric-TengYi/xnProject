package com.xngl.manager.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ProjectPaymentCreateCommand {

  private String paymentNo;
  private String paymentType;
  private BigDecimal amount;
  private LocalDate paymentDate;
  private String voucherNo;
  private String sourceType;
  private Long sourceId;
  private String remark;
}
