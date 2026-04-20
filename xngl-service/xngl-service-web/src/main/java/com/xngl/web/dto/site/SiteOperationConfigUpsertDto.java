package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SiteOperationConfigUpsertDto {

  private Boolean queueEnabled;
  private Integer maxQueueCount;
  private Boolean manualDisposalEnabled;
  private BigDecimal rangeCheckRadius;
  private Integer durationLimitMinutes;
  private String remark;
}
