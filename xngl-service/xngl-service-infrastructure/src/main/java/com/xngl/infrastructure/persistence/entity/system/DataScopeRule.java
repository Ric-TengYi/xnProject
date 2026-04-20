package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
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

  // 兼容字段，仅用于接口 DTO 映射，不参与数据库读写
  @TableField(exist = false)
  private String ruleType;
  @TableField(exist = false)
  private String ruleValue;
  @TableField(exist = false)
  private String resourceCode;
}
