package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleCompanyCapacityDto {

  private String orgId;
  private String orgName;
  private long totalVehicles;
  private long activeVehicles;
  private long movingVehicles;
  private long warningVehicles;
  private long disabledVehicles;
  private BigDecimal totalLoadTons;
  private BigDecimal activeRate;
  private BigDecimal avgLoadTons;
  private String captainName;
  private String captainPhone;
}
