package com.xngl.manager.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentRecordVo {

  private Long id;
  private Long projectId;
  private String projectName;
  private String projectCode;
  private String paymentNo;
  private String paymentType;
  private BigDecimal amount;
  private LocalDate paymentDate;
  private String voucherNo;
  private String status;
  private String sourceType;
  private Long sourceId;
  private String remark;
  private Long operatorId;
  private Long cancelOperatorId;
  private LocalDateTime cancelTime;
  private String cancelReason;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
