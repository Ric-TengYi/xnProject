package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class MiniVehicleFenceUpsertDto {

  private String fenceCode;
  private String fenceName;
  private Long vehicleId;
  private BigDecimal centerLng;
  private BigDecimal centerLat;
  private BigDecimal radiusMeters;
  private String warningTimeRange;
  private String directionRule;
  private List<Long> permissionUserIds;
  private String status;
}
