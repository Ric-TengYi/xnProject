package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectContractSummaryDto {

  private String contractId;
  private String contractNo;
  private String contractName;
  private String siteId;
  private String siteName;
  private String siteType;
  private BigDecimal agreedVolume;
  private BigDecimal disposedVolume;
  private BigDecimal remainingVolume;
  private BigDecimal unitPrice;
  private BigDecimal contractAmount;
  private String contractStatus;
  private String approvalStatus;
  private String expireDate;
}
