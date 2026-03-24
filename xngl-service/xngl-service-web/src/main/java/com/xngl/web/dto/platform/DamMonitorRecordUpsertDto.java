package com.xngl.web.dto.platform;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DamMonitorRecordUpsertDto {

  private Long siteId;
  private String deviceName;
  private String monitorTime;
  private String onlineStatus;
  private String safetyLevel;
  private BigDecimal displacementValue;
  private BigDecimal waterLevel;
  private BigDecimal rainfall;
  private Boolean alarmFlag;
  private String remark;
}
