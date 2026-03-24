package com.xngl.web.dto.platform;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamMonitorRecordDto {

  private String id;
  private String siteId;
  private String siteName;
  private String deviceName;
  private String monitorTime;
  private String onlineStatus;
  private String safetyLevel;
  private BigDecimal displacementValue;
  private BigDecimal waterLevel;
  private BigDecimal rainfall;
  private boolean alarmFlag;
  private String remark;
}
