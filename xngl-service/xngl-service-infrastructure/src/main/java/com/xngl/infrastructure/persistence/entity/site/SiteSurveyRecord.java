package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_survey_record")
public class SiteSurveyRecord extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private String surveyNo;
  private LocalDate surveyDate;
  private BigDecimal measuredVolume;
  private BigDecimal deductionVolume;
  private BigDecimal settlementVolume;
  private String surveyCompany;
  private String surveyorName;
  private String status;
  private String reportUrl;
  private String remark;
}
