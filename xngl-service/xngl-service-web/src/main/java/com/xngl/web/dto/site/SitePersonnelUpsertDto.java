package com.xngl.web.dto.site;

import lombok.Data;

@Data
public class SitePersonnelUpsertDto {

  private Long userId;
  private String roleType;
  private String dutyScope;
  private String shiftGroup;
  private Boolean accountEnabled;
  private String remark;
}
