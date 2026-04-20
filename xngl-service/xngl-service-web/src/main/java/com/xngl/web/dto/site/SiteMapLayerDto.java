package com.xngl.web.dto.site;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteMapLayerDto {

  private String id;
  private String name;
  private String code;
  private String siteType;
  private Integer status;
  private BigDecimal lng;
  private BigDecimal lat;
  private String boundaryGeoJson;
  private List<SiteDeviceDto> devices;
}
