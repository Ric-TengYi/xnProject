package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteSurveyRecordDto {

  private String id;
  private String siteId;
  private String siteName;
  private String surveyNo;
  private String surveyDate;
  private BigDecimal measuredVolume;
  private BigDecimal deductionVolume;
  private BigDecimal settlementVolume;
  private String surveyCompany;
  private String surveyorName;
  private String status;
  private String reportUrl;
  private String remark;
  private String createTime;
  private String updateTime;
}
