package com.xngl.infrastructure.persistence.entity.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_alert_event")
public class AlertEvent extends BaseEntity {

  private Long tenantId;
  private String alertNo;
  private String title;
  private String alertType;
  private String ruleCode;
  private String targetType;
  private Long targetId;
  private Long projectId;
  private Long siteId;
  private Long vehicleId;
  private Long userId;
  private Long contractId;
  private String level;
  private String alertLevel;
  private Long relatedId;
  private String relatedType;
  private String sourceChannel;
  private String content;
  private String latestPositionJson;
  private String snapshotJson;
  private String handleRemark;
  private String alertStatus;
  private Integer status;
  private java.time.LocalDateTime occurTime;
  private java.time.LocalDateTime resolveTime;
}
