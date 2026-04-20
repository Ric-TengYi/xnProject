package com.xngl.web.dto.contract;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ContractExportRequestDto {

  private String contractType;
  private String contractStatus;
  private String approvalStatus;
  private String keyword;
  private Long projectId;
  private Long siteId;
  private Long constructionOrgId;
  private Long transportOrgId;
  private Boolean isThreeParty;
  private String sourceType;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate effectiveStartDate;
  private LocalDate effectiveEndDate;
  private LocalDate expireStartDate;
  private LocalDate expireEndDate;
  private String exportType;
}
