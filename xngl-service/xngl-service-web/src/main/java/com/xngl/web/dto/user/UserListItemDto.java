package com.xngl.web.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListItemDto {

  private String id;
  private String username;
  private String name;
  private String mobile;
  private String mainOrgId;
  private String mainOrgName;
  private List<String> roleNames;
  private String status;
  private String lastLoginTime;
}
