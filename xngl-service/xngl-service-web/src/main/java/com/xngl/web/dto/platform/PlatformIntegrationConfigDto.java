package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformIntegrationConfigDto {

  private String integrationCode;
  private String integrationName;
  private boolean enabled;
  private String vendorName;
  private String baseUrl;
  private String apiVersion;
  private String clientId;
  private String clientSecret;
  private String accessKey;
  private String accessSecret;
  private String callbackPath;
  private String extJson;
  private String remark;
  private String updateTime;
}
