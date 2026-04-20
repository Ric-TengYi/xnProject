package com.xngl.web.dto.fleet;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FleetTrackingStopDto {

  private String startTime;
  private String endTime;
  private Long durationMinutes;
  private BigDecimal lng;
  private BigDecimal lat;
  private String remark;
}
