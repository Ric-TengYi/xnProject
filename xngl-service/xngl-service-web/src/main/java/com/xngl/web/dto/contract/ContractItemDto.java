package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractItemDto {

  private String id;
  private String contractNo;
  private String contractType;
  private String name;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String constructionOrgId;
  private String constructionOrgName;
  private String transportOrgId;
  private String transportOrgName;
  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private BigDecimal settledAmount;
  private BigDecimal agreedVolume;
  private BigDecimal unitPrice;
  private BigDecimal unitPriceInside;
  private BigDecimal unitPriceOutside;
  private String contractStatus;
  private String approvalStatus;
  private String signDate;
  private String effectiveDate;
  private String expireDate;
  private Boolean isThreeParty;
  private String sourceType;
  private String applicantId;
  private String rejectReason;
  private Integer changeVersion;
  private String createTime;
}
