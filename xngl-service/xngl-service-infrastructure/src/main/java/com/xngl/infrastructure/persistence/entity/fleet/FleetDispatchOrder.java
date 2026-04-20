package com.xngl.infrastructure.persistence.entity.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_fleet_dispatch_order")
public class FleetDispatchOrder extends BaseEntity {

  private Long tenantId;
  private Long fleetId;
  private Long orgId;
  private String orderNo;
  private String relatedPlanNo;
  private LocalDate applyDate;
  private Integer requestedVehicleCount;
  private Integer requestedDriverCount;
  private String urgencyLevel;
  private String status;
  private String applicantName;
  private String approvedBy;
  private LocalDateTime approvedTime;
  private String auditRemark;
  private String remark;
}
