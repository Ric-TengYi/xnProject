package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FleetFinanceRecordUpsertDto {

  private Long fleetId;
  private String contractNo;
  private String statementMonth;
  private BigDecimal revenueAmount;
  private BigDecimal costAmount;
  private BigDecimal otherAmount;
  private BigDecimal settledAmount;
  private String status;
  private String remark;
}
