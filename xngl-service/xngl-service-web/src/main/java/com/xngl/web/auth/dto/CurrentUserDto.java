package com.xngl.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserDto {

  private String userId;
  private String tenantId;
  private String orgId;
  private String username;
  private String name;
  private String userType;
  private List<String> roleCodes;
  private String tenantType;
}
