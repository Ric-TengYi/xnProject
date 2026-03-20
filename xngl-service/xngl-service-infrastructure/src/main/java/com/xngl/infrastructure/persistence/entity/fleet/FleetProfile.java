package com.xngl.infrastructure.persistence.entity.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_fleet_profile")
public class FleetProfile extends BaseEntity {

  private Long tenantId;
  private Long orgId;
  private String fleetName;
  private String captainName;
  private String captainPhone;
  private Integer driverCountPlan;
  private Integer vehicleCountPlan;
  private String status;
  private String attendanceMode;
  private String remark;
}
