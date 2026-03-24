package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniUserProfileDto {

  private String userId;
  private String tenantId;
  private String username;
  private String name;
  private String mobile;
  private String userType;
  private String mainOrgId;
  private String openId;
  private String bindingStatus;
  private String lastLoginTime;
}
