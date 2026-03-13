package com.xngl.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

  private Long tenantId;
  private String tenantScope;
  private String permissionCode;
  private String permissionName;
  @TableField(exist = false)
  private String permissionType;
  private String moduleCode;
  private Long menuId;
  private String resourceRef;
  @TableField("permission_type")
  private String resourceType;
  private String httpMethod;
  private String apiPath;
  private String status;
}
