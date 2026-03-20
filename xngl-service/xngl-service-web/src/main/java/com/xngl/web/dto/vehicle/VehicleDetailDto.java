package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleDetailDto extends VehicleListItemDto {

  private BigDecimal deadWeight;
  private BigDecimal lng;
  private BigDecimal lat;
  private String gpsTime;
  private String remark;
}
