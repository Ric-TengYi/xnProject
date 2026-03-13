package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseRelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_org_rel")
public class UserOrgRel extends BaseRelEntity {

  private Long tenantId;
  private Long userId;
  private Long orgId;
  private Integer isMain;
}
