package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_dam_monitor_record")
public class DamMonitorRecord extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private String integrationCode;
  private String deviceName;
  private LocalDateTime monitorTime;
  private String onlineStatus;
  private String safetyLevel;
  private BigDecimal displacementValue;
  private BigDecimal waterLevel;
  private BigDecimal rainfall;
  private Integer alarmFlag;
  private String remark;
}
