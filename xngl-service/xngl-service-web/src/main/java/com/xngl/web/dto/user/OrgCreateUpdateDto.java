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
  private Integer sortOrder;
}
