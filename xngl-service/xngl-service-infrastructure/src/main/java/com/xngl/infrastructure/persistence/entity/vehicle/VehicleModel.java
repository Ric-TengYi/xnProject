package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_model")
public class VehicleModel extends BaseEntity {

  private Long tenantId;
  private String modelCode;
  private String brand;
  private String modelName;
  private String vehicleType;
  private Integer axleCount;
  private Integer seatCount;
  private BigDecimal deadWeight;
  private BigDecimal loadWeight;
  private String energyType;
  private String status;
  private String remark;
}
