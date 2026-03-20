package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_personnel_certificate")
public class VehiclePersonnelCertificate extends BaseEntity {

  private Long tenantId;

  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long orgId;

  @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
