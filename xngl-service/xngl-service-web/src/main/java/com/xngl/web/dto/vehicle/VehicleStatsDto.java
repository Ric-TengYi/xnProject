package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleStatsDto {

  private long totalVehicles;
  private long activeVehicles;
  private long maintenanceVehicles;
  private long disabledVehicles;
  private long warningVehicles;
  private BigDecimal activeRate;
  private BigDecimal totalLoadTons;
}
