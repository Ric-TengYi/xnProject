package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_personnel_config")
public class SitePersonnelConfig extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private Long userId;
  private Long orgId;
  private String roleType;
  private String dutyScope;
  private String shiftGroup;
  private Integer accountEnabled;
  private String remark;
}
