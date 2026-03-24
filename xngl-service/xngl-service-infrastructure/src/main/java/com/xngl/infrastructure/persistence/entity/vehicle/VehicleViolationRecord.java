package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_violation_record")
public class VehicleViolationRecord extends BaseEntity {

  private Long tenantId;
  private Long vehicleId;
  private String plateNo;
  private Long orgId;
  private String violationType;
  private LocalDateTime triggerTime;
  private String triggerLocation;
  private String actionStatus;
  private String penaltyResult;
  private LocalDateTime banStartTime;
  private LocalDateTime banEndTime;
  private LocalDateTime releaseTime;
  private String releaseReason;
  private String operatorName;
  private String remark;
}
