package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActorRuleCreateUpdateDto {

  private String tenantId;
  private String processKey;
  private String ruleName;
  private String ruleType;
  private String ruleExpression;
  private Integer priority;
}
