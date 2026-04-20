package com.xngl.infrastructure.persistence.entity.event;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_manual_event_audit_log")
public class ManualEventAuditLog extends BaseEntity {

  private Long tenantId;
  private Long eventId;
  private String nodeCode;
  private String action;
  private String resultStatus;
  private Long auditorId;
  private String auditorName;
  private String comment;
  private LocalDateTime auditTime;
}
