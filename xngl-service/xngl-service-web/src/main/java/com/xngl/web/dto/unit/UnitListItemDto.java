package com.xngl.web.dto.unit;

import lombok.Data;

@Data
public class UnitListItemDto {

  private String id;
  private String orgCode;
  private String orgName;
  private String orgType;
  private String orgTypeLabel;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String unifiedSocialCode;
  private String status;
  private String statusLabel;
  private long projectCount;
  private long contractCount;
  private long vehicleCount;
  private long activeVehicleCount;
  private String createTime;
  private String updateTime;
}
