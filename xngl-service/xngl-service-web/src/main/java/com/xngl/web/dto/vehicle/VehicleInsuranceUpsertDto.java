package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleInsuranceUpsertDto {

  private Long vehicleId;
  private String policyNo;
  private String insuranceType;
  private String insurerName;
  private BigDecimal coverageAmount;
  private BigDecimal premiumAmount;
  private BigDecimal claimAmount;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
  private String remark;
}
