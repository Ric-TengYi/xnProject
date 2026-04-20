package com.xngl.web.dto.query;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CheckinListItemDto {

  private String id;
  private String ticketNo;
  private String punchTime;
  private String status;
  private String statusLabel;
  private String exceptionType;
  private String voidReason;
  private BigDecimal volume;
  private String sourceType;
  private String contractId;
  private String contractNo;
  private String contractName;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String plateNo;
  private String driverName;
  private String transportOrgName;
}
