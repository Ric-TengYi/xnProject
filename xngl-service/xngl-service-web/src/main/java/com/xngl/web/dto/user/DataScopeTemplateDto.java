package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeTemplateDto {

  private String scopeType;
  private String scopeName;
  private Boolean supportsOrgSelection;
  private Boolean supportsProjectSelection;
}
