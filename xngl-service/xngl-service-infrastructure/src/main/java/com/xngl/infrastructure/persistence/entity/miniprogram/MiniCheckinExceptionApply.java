package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_checkin_exception_apply")
public class MiniCheckinExceptionApply extends BaseEntity {

  private Long tenantId;
  private Long checkinId;
  private Long projectId;
  private Long siteId;
  private Long userId;
  private String reporterName;
  private String exceptionType;
  private String reason;
  private String attachmentUrls;
  private String status;
  private Long linkedEventId;
  private String sourceChannel;
}
