package com.xngl.manager.project.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProjectPaymentRecord {

  private Long id;
  private Long projectId;
  private String paymentNo;
  private String paymentType;
  private BigDecimal amount;
  private LocalDate paymentDate;
  private String voucherNo;
  private String remark;
  private String status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
