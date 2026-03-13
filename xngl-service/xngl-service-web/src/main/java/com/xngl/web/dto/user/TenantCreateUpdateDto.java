package com.xngl.web.dto.user;

import lombok.Data;

@Data
public class TenantCreateUpdateDto {

  private String tenantCode;
  private String tenantName;
  private String tenantType;
  private String contactName;
  private String contactMobile;
  private String expireTime;
  private String businessLicenseNo;
  private String address;
  private String remark;
}
