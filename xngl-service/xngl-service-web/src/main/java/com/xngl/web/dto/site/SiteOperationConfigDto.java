package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteOperationConfigDto {

  private boolean queueEnabled;
  private Integer maxQueueCount;
  private boolean manualDisposalEnabled;
  private BigDecimal rangeCheckRadius;
  private Integer durationLimitMinutes;
  private String remark;
}
