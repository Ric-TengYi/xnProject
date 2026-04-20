package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_maintenance_plan")
public class VehicleMaintenancePlan extends BaseEntity {

  private Long tenantId;
  private Long vehicleId;
  private Long orgId;
  private String planNo;
  private String planType;
  private String cycleType;
  private Integer cycleValue;
  private LocalDate lastMaintainDate;
  private LocalDate nextMaintainDate;
  private BigDecimal lastOdometer;
  private BigDecimal nextOdometer;
  private String responsibleName;
  private String status;
  private String remark;
}
