package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantListItemDto {

  private String id;
  private String tenantCode;
  private String tenantName;
  private String tenantType;
  private String status;
  private String contactName;
  private String contactMobile;
  private String expireTime;
}
