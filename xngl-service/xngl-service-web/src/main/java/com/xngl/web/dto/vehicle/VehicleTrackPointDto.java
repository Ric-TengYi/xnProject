package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleTrackPointDto {

  private String id;
  private BigDecimal lng;
  private BigDecimal lat;
  private BigDecimal speed;
  private BigDecimal direction;
  private String locateTime;
  private String sourceType;
  private String remark;
}
