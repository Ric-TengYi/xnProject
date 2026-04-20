package com.xngl.web.dto.unit;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UnitProjectStatDto {

  private String projectId;
  private String projectName;
  private String projectCode;
  private Long contractCount;
  private BigDecimal contractAmount;
  private BigDecimal agreedVolume;
}
