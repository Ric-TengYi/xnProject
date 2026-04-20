package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleCardUpsertDto {

  private String cardNo;
  private String cardType;
  private String providerName;
  private Long orgId;
  private Long vehicleId;
  private BigDecimal balance;
  private BigDecimal totalRecharge;
  private BigDecimal totalConsume;
  private String status;
  private String remark;
}
