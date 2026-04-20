package com.xngl.infrastructure.persistence.entity.organization;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class Org extends BaseEntity {

  private Long tenantId;
  private String orgCode;
  private String orgName;
  private Long parentId;
  private String orgType;
  private String orgPath;
  private Long leaderUserId;
  private String leaderNameCache;
  private String contactPerson;
  private String contactPhone;
  private String address;
  private String unifiedSocialCode;
  private String remark;
  private Integer sortOrder;
  private String status;
}
