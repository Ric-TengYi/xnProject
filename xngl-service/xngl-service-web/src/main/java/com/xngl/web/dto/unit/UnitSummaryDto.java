package com.xngl.web.dto.unit;

import lombok.Data;

@Data
public class UnitSummaryDto {

  private long totalUnits;
  private long constructionUnits;
  private long builderUnits;
  private long transportUnits;
  private long totalVehicles;
}
