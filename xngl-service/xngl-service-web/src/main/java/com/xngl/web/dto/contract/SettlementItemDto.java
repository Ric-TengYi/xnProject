package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementItemDto {

  private String id;
  private String settlementNo;
  private String settlementType;
  private String targetProjectId;
  private String targetProjectName;
  private String targetSiteId;
  private String targetSiteName;
  private String periodStart;
  private String periodEnd;
  private BigDecimal totalVolume;
  private BigDecimal totalAmount;
  private BigDecimal adjustAmount;
  private BigDecimal payableAmount;
  private String approvalStatus;
  private String settlementStatus;
  private String creatorId;
  private String createTime;
}
