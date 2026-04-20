package com.xngl.web.dto.project;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfigDto {

  private boolean checkinEnabled;
  private String checkinAccount;
  private String checkinAuthScope;
  private boolean locationCheckRequired;
  private BigDecimal locationRadiusMeters;
  private BigDecimal preloadVolume;
  private String routeGeoJson;
  private boolean violationRuleEnabled;
  private String violationFenceCode;
  private String violationFenceName;
  private String violationFenceGeoJson;
  private String remark;
}
