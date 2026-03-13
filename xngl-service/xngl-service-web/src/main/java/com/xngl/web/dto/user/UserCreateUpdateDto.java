package com.xngl.web.dto.user;

import java.util.List;
import lombok.Data;

@Data
public class UserCreateUpdateDto {

  private String tenantId;
  private String username;
  private String name;
  private String mobile;
  private String email;
  private String userType;
  private String mainOrgId;
  private List<String> orgIds;
  private List<String> roleIds;
  private String password;
}
