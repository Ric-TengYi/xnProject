package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_integration_config")
public class PlatformIntegrationConfig extends BaseEntity {

  private Long tenantId;
  private String integrationCode;
  private String integrationName;
  private Integer enabled;
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
