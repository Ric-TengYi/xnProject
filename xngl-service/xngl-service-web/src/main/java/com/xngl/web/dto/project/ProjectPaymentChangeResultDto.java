package com.xngl.web.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectPaymentChangeResultDto {

  private String paymentId;
  private String paymentNo;
  private ProjectPaymentSummaryDto summary;
}
