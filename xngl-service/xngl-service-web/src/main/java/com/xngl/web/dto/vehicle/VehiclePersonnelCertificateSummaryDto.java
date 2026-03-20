package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePersonnelCertificateSummaryDto {

  private Integer totalPersons;
  private Integer activeCertificates;
  private Integer expiringCertificates;
  private Integer expiredCertificates;
  private BigDecimal totalFeeAmount;
  private BigDecimal paidAmount;
  private BigDecimal unpaidAmount;
}
