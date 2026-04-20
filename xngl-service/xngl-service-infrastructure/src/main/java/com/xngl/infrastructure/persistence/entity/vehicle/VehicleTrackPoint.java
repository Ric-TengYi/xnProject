package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_track_point")
public class VehicleTrackPoint extends BaseEntity {

  private Long tenantId;
  private Long vehicleId;
  private String plateNo;
  private BigDecimal lng;
  private BigDecimal lat;
  private BigDecimal speed;
  private BigDecimal direction;
  private LocalDateTime locateTime;
  private String sourceType;
  private String remark;
}
