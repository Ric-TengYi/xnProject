package com.xngl.manager.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ContractDetailVo {
  private Long id;
  private Long tenantId;
  private String contractNo;
  private String code;
  private String name;
  private String contractType;
  private Long projectId;
  private String projectName;
  private Long siteId;
  private String siteName;
  private Long partyId;
  private String partyName;
  private Long constructionOrgId;
  private String constructionOrgName;
  private Long transportOrgId;
  private String transportOrgName;
  private Long siteOperatorOrgId;
  private String siteOperatorOrgName;
  private LocalDate signDate;
  private LocalDate effectiveDate;
  private LocalDate expireDate;
  private BigDecimal agreedVolume;
  private BigDecimal unitPrice;
  private BigDecimal unitPriceInside;
  private BigDecimal unitPriceOutside;
  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private BigDecimal settledAmount;
  private BigDecimal pendingAmount;
  private Integer changeVersion;
  private String contractStatus;
  private String approvalStatus;
  private String rejectReason;
  private Boolean isThreeParty;
  private String sourceType;
  private Long applicantId;
  private String applicantName;
  private String remark;
}