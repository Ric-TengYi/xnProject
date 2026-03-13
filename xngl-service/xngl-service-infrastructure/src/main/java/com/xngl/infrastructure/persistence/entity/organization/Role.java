package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

  private Long tenantId;
  private String roleCode;
  private String roleName;
  private String roleScope;
  private String roleCategory;
  private String description;
  private String dataScopeTypeDefault;
  private String status;
  private Integer builtinFlag;
}
