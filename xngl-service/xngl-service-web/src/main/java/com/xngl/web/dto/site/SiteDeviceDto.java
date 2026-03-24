package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDeviceDto {

  private String id;
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
