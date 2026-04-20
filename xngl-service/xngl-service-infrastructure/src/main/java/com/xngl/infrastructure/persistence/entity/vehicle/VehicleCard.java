package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_card")
public class VehicleCard extends BaseEntity {

  private Long tenantId;
  private String cardNo;
  private String cardType;
  private String providerName;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long orgId;

  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long vehicleId;
  private BigDecimal balance;
  private BigDecimal totalRecharge;
  private BigDecimal totalConsume;
  private String status;
  private String remark;
}
