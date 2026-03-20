package com.xngl.infrastructure.persistence.entity.event;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_manual_event")
public class ManualEvent extends BaseEntity {

  private Long tenantId;
  private String eventNo;
  private String eventType;
  private String title;
  private String content;
  private String sourceChannel;
  private String reportAddress;
  private Long projectId;
  private Long siteId;
  private Long vehicleId;
  private Long reporterId;
  private String reporterName;
  private String contactPhone;
  private String priority;
  private String status;
  private String currentAuditNode;
  private LocalDateTime occurTime;
  private LocalDateTime deadlineTime;
  private LocalDateTime reportTime;
  private LocalDateTime closeTime;
  private String closeRemark;
  private String attachmentUrls;
  private String assigneeName;
  private String assigneePhone;
  private String dispatchRemark;
}
