package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleMaintenancePlanListItemDto {

  private String id;
  private String planNo;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String planType;
  private String cycleType;
  private Integer cycleValue;
  private String lastMaintainDate;
  private String nextMaintainDate;
  private BigDecimal lastOdometer;
  private BigDecimal nextOdometer;
  private String responsibleName;
  private String status;
  private String statusLabel;
  private Boolean overdue;
  private String remark;
}
