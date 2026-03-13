package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActorRuleListItemDto {

  private String id;
  private String processKey;
  private String ruleName;
  private String ruleType;
  private Integer priority;
  private String status;
}
