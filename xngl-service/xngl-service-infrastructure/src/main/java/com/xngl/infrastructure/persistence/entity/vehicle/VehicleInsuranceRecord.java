package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_insurance")
public class VehicleInsuranceRecord extends BaseEntity {

  private Long tenantId;
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
