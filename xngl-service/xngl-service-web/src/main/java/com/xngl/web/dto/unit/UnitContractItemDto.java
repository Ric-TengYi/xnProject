package com.xngl.web.dto.unit;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UnitContractItemDto {

  private String id;
  private String contractNo;
  private String name;
  private String contractType;
  private String contractStatus;
  private String sourceType;
  private String siteId;
  private String siteName;
  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private BigDecimal agreedVolume;
}
