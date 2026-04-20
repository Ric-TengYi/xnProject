package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_operation_config")
public class SiteOperationConfig extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private Integer queueEnabled;
  private Integer maxQueueCount;
  private Integer manualDisposalEnabled;
  private BigDecimal rangeCheckRadius;
  private Integer durationLimitMinutes;
  private String remark;
}
