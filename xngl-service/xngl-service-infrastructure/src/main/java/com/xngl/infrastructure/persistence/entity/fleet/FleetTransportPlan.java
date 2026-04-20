package com.xngl.infrastructure.persistence.entity.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_fleet_transport_plan")
public class FleetTransportPlan extends BaseEntity {

  private Long tenantId;
  private Long fleetId;
  private Long orgId;
  private String planNo;
  private LocalDate planDate;
  private String sourcePoint;
  private String destinationPoint;
  private String cargoType;
  private Integer plannedTrips;
  private BigDecimal plannedVolume;
  private String status;
  private String remark;
}
