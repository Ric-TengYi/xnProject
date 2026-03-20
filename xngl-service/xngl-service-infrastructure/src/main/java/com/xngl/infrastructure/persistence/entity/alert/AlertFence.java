package com.xngl.infrastructure.persistence.entity.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_alert_fence")
public class AlertFence extends BaseEntity {

  private Long tenantId;
  private String ruleCode;
  private String fenceCode;
  private String fenceName;
  private String fenceType;
  private String geoJson;
  private BigDecimal bufferMeters;
  private String bizScope;
  private String activeTimeRange;
  private String directionRule;
  private String status;
}
