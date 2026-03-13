package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCreateUpdateDto {

  private String tenantId;
  private String menuId;
  private String permissionCode;
  private String permissionName;
  private String resourceType;
}
