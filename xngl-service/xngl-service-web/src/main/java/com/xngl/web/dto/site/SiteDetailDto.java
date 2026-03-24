package com.xngl.web.dto.site;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDetailDto {

  private String id;
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
  private String parentSiteId;
  private String parentSiteName;
  private String managementArea;
  private String weighbridgeSiteId;
  private String weighbridgeSiteName;
  private BigDecimal lng;
  private BigDecimal lat;
  private String boundaryGeoJson;
  private List<SiteDeviceDto> devices;
  private SiteOperationConfigDto operationConfig;
  private String createTime;
  private String updateTime;
}
