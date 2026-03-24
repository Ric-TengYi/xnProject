package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_device")
public class SiteDevice extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private String deviceCode;
  private String deviceName;
  private String deviceType;
  private String provider;
  private String ipAddress;
  private String status;
  private BigDecimal lng;
  private BigDecimal lat;
  private String remark;
}
