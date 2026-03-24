package com.xngl.infrastructure.persistence.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_config")
public class ProjectConfig extends BaseEntity {

  private Long tenantId;
  private Long projectId;
  private Integer checkinEnabled;
  private String checkinAccount;
  private String checkinAuthScope;
  private Integer locationCheckRequired;
  private BigDecimal locationRadiusMeters;
  private BigDecimal preloadVolume;
  private String routeGeoJson;
  private String violationFenceCode;
  private Integer violationRuleEnabled;
  private String remark;
}
