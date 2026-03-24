package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniExcavationOrgDto {

  private String orgId;
  private String orgCode;
  private String orgName;
  private String orgType;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String status;
  private Boolean isMain;
  private Long projectCount;
}
