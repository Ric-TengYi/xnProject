package com.xngl.web.dto.user;

import lombok.Data;

@Data
public class OrgCreateUpdateDto {

  private String tenantId;
  private String orgCode;
  private String orgName;
  private String parentId;
  private String orgType;
  private String leaderUserId;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String unifiedSocialCode;
  private String remark;
  private Integer sortOrder;
  private String status;
}
