package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_maintenance_record")
public class VehicleMaintenanceRecord extends BaseEntity {

  private Long tenantId;
  private Long planId;
  private Long vehicleId;
  private Long orgId;
  private String recordNo;
  private String maintainType;
  private LocalDate serviceDate;
  private BigDecimal odometer;
  private String vendorName;
  private BigDecimal costAmount;
  private String items;
  private String operatorName;
  private String status;
  private String remark;
}
