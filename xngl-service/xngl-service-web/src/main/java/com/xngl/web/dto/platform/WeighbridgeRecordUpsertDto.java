package com.xngl.web.dto.platform;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WeighbridgeRecordUpsertDto {

  private Long siteId;
  private Long deviceId;
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
