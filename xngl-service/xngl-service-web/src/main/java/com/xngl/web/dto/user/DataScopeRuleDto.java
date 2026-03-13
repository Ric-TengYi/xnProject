package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeRuleDto {

  private String ruleType;
  private String ruleValue;
  private String resourceCode;
}
