package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniManualDisposalCreateDto {

  private Long siteId;
  private Long contractId;
  private Long vehicleId;
  private String plateNo;
  private String disposalTime;
  private BigDecimal volume;
  private BigDecimal amount;
  private BigDecimal weightTons;
  private String photoUrls;
  private String remark;
}
