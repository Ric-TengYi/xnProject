package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_feedback")
public class MiniFeedback extends BaseEntity {

  private Long tenantId;
  private String feedbackType;
  private Long projectId;
  private Long siteId;
  private Long userId;
  private String reporterName;
  private String title;
  private String content;
  private String attachmentUrls;
  private String reportAddress;
  private String status;
  private Long handlerId;
  private String handlerName;
  private LocalDateTime closeTime;
  private String closeRemark;
  private Long linkedEventId;
  private String sourceChannel;
}
