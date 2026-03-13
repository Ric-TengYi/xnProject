package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailDto {

  private String id;
  private String tenantCode;
  private String tenantName;
  private String tenantType;
  private String status;
  private String contactName;
  private String contactMobile;
  private String businessLicenseNo;
  private String address;
  private String remark;
  private String expireTime;
  private String createTime;
  private String updateTime;
}
