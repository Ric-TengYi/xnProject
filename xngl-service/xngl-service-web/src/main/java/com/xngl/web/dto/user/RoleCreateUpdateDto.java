package com.xngl.web.dto.user;

import lombok.Data;

@Data
public class RoleCreateUpdateDto {

  private String tenantId;
  private String roleCode;
  private String roleName;
  private String roleScope;
  private String roleCategory;
  private String description;
  private String dataScopeTypeDefault;
}
