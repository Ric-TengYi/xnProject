package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_weighbridge_record")
public class WeighbridgeRecord extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private Long deviceId;
  private String vehicleNo;
  private String ticketNo;
  private BigDecimal grossWeight;
  private BigDecimal tareWeight;
  private BigDecimal netWeight;
  private BigDecimal estimatedVolume;
  private LocalDateTime weighTime;
  private String syncStatus;
  private String controlCommand;
  private String integrationCode;
  private String sourceType;
  private String remark;
}
