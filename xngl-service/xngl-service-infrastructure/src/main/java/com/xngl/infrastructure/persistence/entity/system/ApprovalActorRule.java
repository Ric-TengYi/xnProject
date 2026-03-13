package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_actor_rule")
public class ApprovalActorRule extends BaseEntity {

  private Long tenantId;
  private String bizType;
  private String processKey;
  private String ruleName;
  private String ruleType;
  private String ruleExpression;
  private String actorType;
  private String actorRef;
  private Integer priority;
  private String status;
}
