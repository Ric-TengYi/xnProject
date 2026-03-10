package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site")
public class Site extends BaseEntity {

  private String name;
  private String code;
  private String address;
  private Long projectId;
  private Integer status;
  private Long orgId;
}
