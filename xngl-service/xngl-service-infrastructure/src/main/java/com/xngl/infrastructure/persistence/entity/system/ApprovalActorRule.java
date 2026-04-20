package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
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
  private String nodeCode;
  private String processKey;
  private String ruleName;
  private String ruleType;
  private String ruleExpression;
  private String actorType;
  @TableField("actor_ref_id")
  private String actorRefId;
  private String matchMode;
  private Integer priority;
  @TableField("actor_snapshot_flag")
  private Integer actorSnapshotFlag;
  private String status;
}
