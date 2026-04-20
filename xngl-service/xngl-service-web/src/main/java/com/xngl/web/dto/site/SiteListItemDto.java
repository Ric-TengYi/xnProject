package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteListItemDto {

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
  private String siteLevel;
  private String parentSiteId;
  private String parentSiteName;
  private String managementArea;
  private String weighbridgeSiteId;
  private String weighbridgeSiteName;
  private BigDecimal lng;
  private BigDecimal lat;
}
