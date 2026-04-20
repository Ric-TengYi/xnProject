package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleInsuranceListItemDto {

  private String id;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String policyNo;
  private String insuranceType;
  private String insurerName;
  private BigDecimal coverageAmount;
  private BigDecimal premiumAmount;
  private BigDecimal claimAmount;
  private String startDate;
  private String endDate;
  private String status;
  private String statusLabel;
  private Integer remainingDays;
  private String remark;
}
