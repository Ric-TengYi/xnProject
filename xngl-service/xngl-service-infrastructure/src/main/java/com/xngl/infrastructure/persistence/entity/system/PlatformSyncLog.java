package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_sync_log")
public class PlatformSyncLog extends BaseEntity {

  private Long tenantId;
  private String integrationCode;
  private String syncMode;
  private String bizType;
  private String batchNo;
  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
  private String status;
  private Long operatorId;
  private String operatorName;
  private String requestPayload;
  private String responsePayload;
  private String remark;
  private LocalDateTime syncTime;
}
