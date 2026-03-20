package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FleetFinanceRecordListItemDto {

  private String id;
  private String fleetId;
  private String fleetName;
  private String orgId;
  private String orgName;
  private String recordNo;
  private String contractNo;
  private String statementMonth;
  private BigDecimal revenueAmount;
  private BigDecimal costAmount;
  private BigDecimal otherAmount;
  private BigDecimal settledAmount;
  private BigDecimal profitAmount;
  private BigDecimal outstandingAmount;
  private String status;
  private String statusLabel;
  private String remark;
}
