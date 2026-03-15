package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentRecordDto {

  private String id;
  private String projectId;
  private String projectName;
  private String projectCode;
  private String paymentNo;
  private String paymentType;
  private BigDecimal amount;
  private String paymentDate;
  private String voucherNo;
  private String status;
  private String sourceType;
  private String sourceId;
  private String remark;
  private String operatorId;
  private String cancelOperatorId;
  private String cancelTime;
  private String cancelReason;
  private String createTime;
  private String updateTime;
}
