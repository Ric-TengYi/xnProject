package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionListItemDto {

  private String id;
  private String permissionCode;
  private String permissionName;
  private String menuId;
  private String resourceType;
  private String status;
}
