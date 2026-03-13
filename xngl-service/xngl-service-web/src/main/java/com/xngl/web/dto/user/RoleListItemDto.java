package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleListItemDto {

  private String id;
  private String roleCode;
  private String roleName;
  private String roleScope;
  private String roleCategory;
  private String status;
  private Boolean builtinFlag;
}
