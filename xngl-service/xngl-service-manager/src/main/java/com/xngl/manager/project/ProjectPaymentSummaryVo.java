package com.xngl.manager.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentSummaryVo {

  private Long projectId;
  private String projectName;
  private String projectCode;
  private BigDecimal totalAmount;
  private BigDecimal paidAmount;
  private BigDecimal debtAmount;
  private LocalDate lastPaymentDate;
  private String status;
}
