package com.xngl.infrastructure.persistence.entity.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_alert_push_rule")
public class AlertPushRule extends BaseEntity {

  private Long tenantId;
  private String ruleCode;
  private String level;
  private String channelTypes;
  private String receiverType;
  private String receiverExpr;
  private String pushTimeRule;
  private Integer escalationMinutes;
  private String status;
}
