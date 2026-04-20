package com.xngl.infrastructure.persistence.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project")
public class Project extends BaseEntity {

  private Long tenantId;
  private String name;
  private String code;
  private String address;
  private Integer status;
  private Long orgId;
}
