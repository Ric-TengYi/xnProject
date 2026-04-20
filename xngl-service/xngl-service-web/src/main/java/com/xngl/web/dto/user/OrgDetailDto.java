package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgDetailDto {

  private String id;
  private String orgCode;
  private String orgName;
  private String parentId;
  private String orgType;
  private String orgTypeLabel;
  private String orgPath;
  private String leaderUserId;
  private String leaderName;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String unifiedSocialCode;
  private String remark;
  private Integer sortOrder;
  private String status;
  private Long projectCount;
  private Long contractCount;
  private Long vehicleCount;
  private Long activeVehicleCount;
  private Long userCount;
}
