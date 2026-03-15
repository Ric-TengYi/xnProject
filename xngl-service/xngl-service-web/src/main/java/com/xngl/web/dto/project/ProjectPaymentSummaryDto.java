package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentSummaryDto {

  private String projectId;
  private String projectName;
  private String projectCode;
  private BigDecimal totalAmount;
  private BigDecimal paidAmount;
  private BigDecimal debtAmount;
  private String lastPaymentDate;
  private String status;
}
