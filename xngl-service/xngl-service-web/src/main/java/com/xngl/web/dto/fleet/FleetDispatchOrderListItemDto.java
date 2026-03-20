package com.xngl.web.dto.fleet;

import lombok.Data;

@Data
public class FleetDispatchOrderListItemDto {

  private String id;
  private String fleetId;
  private String fleetName;
  private String orgId;
  private String orgName;
  private String orderNo;
  private String relatedPlanNo;
  private String applyDate;
  private Integer requestedVehicleCount;
  private Integer requestedDriverCount;
  private String urgencyLevel;
  private String urgencyLabel;
  private String status;
  private String statusLabel;
  private String applicantName;
  private String approvedBy;
  private String approvedTime;
  private String auditRemark;
  private String remark;
}
