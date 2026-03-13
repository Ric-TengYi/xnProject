package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_scope_rule")
public class DataScopeRule extends BaseEntity {

  private Long tenantId;
  private Long roleId;
  private String bizModule;
  private String scopeType;
  private String scopeValue;
  
  // 兼容字段
  private String ruleType;
  private String ruleValue;
  private String resourceCode;
}
