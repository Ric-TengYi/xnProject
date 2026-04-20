package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehiclePersonnelCertificateUpsertDto {

  private Long orgId;
  private Long vehicleId;
  private String personName;
  private String mobile;
  private String roleType;
  private String idCardNo;
  private String driverLicenseNo;
  private LocalDate driverLicenseExpireDate;
  private String transportLicenseNo;
  private LocalDate transportLicenseExpireDate;
  private BigDecimal feeAmount;
  private BigDecimal paidAmount;
  private LocalDate feeDueDate;
  private String status;
  private String remark;
}
