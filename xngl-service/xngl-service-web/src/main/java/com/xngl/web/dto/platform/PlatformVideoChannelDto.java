package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformVideoChannelDto {

  private String siteId;
  private String siteName;
  private String deviceId;
  private String deviceCode;
  private String deviceName;
  private String deviceType;
  private String status;
  private String playUrl;
}
