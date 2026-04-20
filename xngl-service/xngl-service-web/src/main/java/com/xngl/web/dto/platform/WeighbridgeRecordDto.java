package com.xngl.web.dto.platform;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeighbridgeRecordDto {

  private String id;
  private String siteId;
  private String siteName;
  private String deviceId;
  private String deviceName;
  private String vehicleNo;
  private String ticketNo;
  private BigDecimal grossWeight;
  private BigDecimal tareWeight;
  private BigDecimal netWeight;
  private BigDecimal estimatedVolume;
  private String weighTime;
  private String syncStatus;
  private String controlCommand;
  private String sourceType;
  private String remark;
}
