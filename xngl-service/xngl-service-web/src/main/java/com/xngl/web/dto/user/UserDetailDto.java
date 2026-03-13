package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {

  private String id;
  private String tenantId;
  private String username;
  private String name;
  private String mobile;
  private String email;
  private String userType;
  private String mainOrgId;
  private String mainOrgName;
  private List<OrgOptionDto> orgs;
  private List<RoleOptionDto> roles;
  private String status;
  private Integer needResetPassword;
  private Integer lockStatus;
  private String lastLoginTime;
}
