package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleMaintenanceRecordListItemDto {

  private String id;
  private String recordNo;
  private String planId;
  private String planNo;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String maintainType;
  private String serviceDate;
  private BigDecimal odometer;
  private String vendorName;
  private BigDecimal costAmount;
  private String items;
  private String operatorName;
  private String status;
  private String statusLabel;
  private String remark;
}
