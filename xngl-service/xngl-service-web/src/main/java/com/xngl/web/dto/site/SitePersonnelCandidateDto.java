package com.xngl.web.dto.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SitePersonnelCandidateDto {

  private String userId;
  private String username;
  private String userName;
  private String mobile;
  private String userType;
  private String orgId;
  private String orgName;
  private String status;
}
