package com.xngl.web.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPermitSummaryDto {

  private String permitId;
  private String permitNo;
  private String permitType;
  private String status;
  private String bindStatus;
  private String vehicleNo;
  private String sourcePlatform;
  private String syncBatchNo;
  private String issueDate;
  private String expireDate;
  private String contractId;
  private String contractNo;
  private String contractName;
  private String siteId;
  private String siteName;
}
