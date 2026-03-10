package com.xngl.infrastructure.persistence.entity.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_alert_event")
public class AlertEvent extends BaseEntity {

  private String title;
  private String level;
  private Long relatedId;
  private String relatedType;
  private Integer status;
}
