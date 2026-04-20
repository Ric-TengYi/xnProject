package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleCardListItemDto {

  private String id;
  private String cardNo;
  private String cardType;
  private String cardTypeLabel;
  private String providerName;
  private String orgId;
  private String orgName;
  private String vehicleId;
  private String plateNo;
  private BigDecimal balance;
  private BigDecimal totalRecharge;
  private BigDecimal totalConsume;
  private String status;
  private String statusLabel;
  private String remark;
  private String updateTime;
}
