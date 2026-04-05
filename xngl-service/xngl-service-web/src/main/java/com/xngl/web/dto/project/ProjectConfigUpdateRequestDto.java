package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProjectConfigUpdateRequestDto {

  private Boolean checkinEnabled;
  private String checkinAccount;
  private String checkinAuthScope;
  private Boolean locationCheckRequired;
  private BigDecimal locationRadiusMeters;
  private BigDecimal preloadVolume;
  private String routeGeoJson;
  private Boolean violationRuleEnabled;
  private String violationFenceCode;
  private String remark;
}
