package com.xngl.web.auth.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserPermissionsDto {

  private List<String> buttonCodes;
  private List<String> apiCodes;
  private List<String> dataScopes;
  private Long permissionVersion;
}
