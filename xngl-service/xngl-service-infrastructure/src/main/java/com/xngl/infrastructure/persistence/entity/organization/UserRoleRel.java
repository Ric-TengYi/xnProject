package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseRelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role_rel")
public class UserRoleRel extends BaseRelEntity {

  private Long tenantId;
  private Long userId;
  private Long roleId;
}
