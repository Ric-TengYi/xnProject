package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInsuranceSummaryDto {

  private Integer totalPolicies;
  private Integer activePolicies;
  private Integer expiringPolicies;
  private Integer expiredPolicies;
  private BigDecimal totalCoverageAmount;
  private BigDecimal totalPremiumAmount;
  private BigDecimal totalClaimAmount;
}
