package com.xngl.web.dto.site;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SiteSurveyUpsertDto {

  private String surveyNo;
  private String surveyDate;
  private BigDecimal measuredVolume;
  private BigDecimal deductionVolume;
  private String surveyCompany;
  private String surveyorName;
  private String status;
  private String reportUrl;
  private String remark;
}
