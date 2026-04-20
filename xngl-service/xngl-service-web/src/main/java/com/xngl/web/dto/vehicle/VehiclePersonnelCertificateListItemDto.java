package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehiclePersonnelCertificateListItemDto {

  private String id;
  private String personName;
  private String mobile;
  private String roleType;
  private String roleTypeLabel;
  private String orgId;
  private String orgName;
  private String vehicleId;
  private String plateNo;
  private String idCardNo;
  private String driverLicenseNo;
  private String driverLicenseExpireDate;
  private String transportLicenseNo;
  private String transportLicenseExpireDate;
  private String status;
  private String statusLabel;
  private Integer remainingDays;
  private BigDecimal feeAmount;
  private BigDecimal paidAmount;
  private BigDecimal unpaidAmount;
  private String feeDueDate;
  private String remark;
}
