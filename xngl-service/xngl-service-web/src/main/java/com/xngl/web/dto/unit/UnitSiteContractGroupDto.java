package com.xngl.web.dto.unit;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class UnitSiteContractGroupDto {

  private String siteId;
  private String siteName;
  private Long contractCount;
  private BigDecimal contractAmount;
  private BigDecimal agreedVolume;
  private BigDecimal receivedAmount;
  private List<UnitContractItemDto> contracts;
}
