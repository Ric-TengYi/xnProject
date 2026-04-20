package com.xngl.infrastructure.persistence.entity.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_alert_rule")
public class AlertRule extends BaseEntity {

  private Long tenantId;
  private String ruleCode;
  private String ruleName;
  private String ruleScene;
  private String metricCode;
  private String thresholdJson;
  private String level;
  private String status;
  private String scopeType;
  private String remark;
}
