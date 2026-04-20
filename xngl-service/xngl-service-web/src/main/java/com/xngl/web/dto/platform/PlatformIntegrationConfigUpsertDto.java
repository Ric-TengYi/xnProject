package com.xngl.web.dto.platform;

import lombok.Data;

@Data
public class PlatformIntegrationConfigUpsertDto {

  private Boolean enabled;
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
}
