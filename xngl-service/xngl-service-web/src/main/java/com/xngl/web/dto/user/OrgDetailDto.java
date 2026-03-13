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
  private String orgPath;
  private String leaderUserId;
  private String leaderName;
  private Integer sortOrder;
  private String status;
}
