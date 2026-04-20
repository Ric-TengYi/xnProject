package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site")
public class Site extends BaseEntity {

  private Long tenantId;
  private String name;
  private String code;
  private String address;
  private Long projectId;
  private Integer status;
  private Long orgId;
  private String siteType;
  private BigDecimal capacity;
  private String settlementMode;
  private BigDecimal disposalUnitPrice;
  private BigDecimal disposalFeeRate;
  private BigDecimal serviceFeeUnitPrice;
  private String siteLevel;
  private Long parentSiteId;
  private String managementArea;
  private Long weighbridgeSiteId;
  private BigDecimal lng;
  private BigDecimal lat;
  private String boundaryGeoJson;
}
