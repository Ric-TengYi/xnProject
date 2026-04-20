package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_delay_apply")
public class MiniDelayApply extends BaseEntity {

  private Long tenantId;
  private String bizType;
  private Long bizId;
  private Long projectId;
  private Long siteId;
  private Long userId;
  private String reporterName;
  private LocalDateTime requestedEndTime;
  private String reason;
  private String attachmentUrls;
  private String status;
  private Long linkedEventId;
  private String sourceChannel;
}
