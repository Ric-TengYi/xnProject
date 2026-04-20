package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SiteCreateDto {

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
