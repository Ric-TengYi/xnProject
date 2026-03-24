package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class MiniVehicleFenceDto {

  private String id;
  private String fenceCode;
  private String fenceName;
  private String vehicleId;
  private String vehiclePlateNo;
  private BigDecimal centerLng;
  private BigDecimal centerLat;
  private BigDecimal radiusMeters;
  private String warningTimeRange;
  private String directionRule;
  private List<String> permissionUserIds;
  private String status;
}
