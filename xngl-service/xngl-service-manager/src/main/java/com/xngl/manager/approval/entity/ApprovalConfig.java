package com.xngl.manager.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_approval_config")
public class ApprovalConfig {

  private Long id;
  private Long tenantId;
  private String processKey;
  private String configName;
  private String approvalType;
  private String nodeCode;
  private String nodeName;
  private String approvers;
  @TableField("condition_expr")
  private String conditions;
  private String formTemplateCode;
  private Integer timeoutHours;
  private Integer sortOrder;
  private String status;
  private String remark;
  private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
