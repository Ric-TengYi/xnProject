package com.xngl.manager.project;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentChangeResultVo {

  private Long paymentId;
  private String paymentNo;
  private ProjectPaymentSummaryVo summary;
}
