package com.xngl.infrastructure.persistence.entity.contract;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_report_export_task")
public class ReportExportTask extends BaseEntity {

  private Long tenantId;
  private String bizType;
  private String exportType;
  private String queryJson;
  private String fileName;
  private String fileUrl;
  private String status;
  private String failReason;
  private Long creatorId;
  private LocalDateTime expireTime;
}
