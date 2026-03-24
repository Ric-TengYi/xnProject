package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgCreateResponseDto {

  private String orgId;
  private String adminUserId;
  private String adminUsername;
  private String adminPassword;
}
