package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsDto {

  private List<String> menuIds;
  private List<String> permissionIds;
  private List<String> buttonCodes;
  private List<String> apiCodes;
}
