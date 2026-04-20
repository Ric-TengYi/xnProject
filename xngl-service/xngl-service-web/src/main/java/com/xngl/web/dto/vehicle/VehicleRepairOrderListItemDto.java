package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleRepairOrderListItemDto {

  private String id;
  private String orderNo;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String urgencyLevel;
  private String urgencyLabel;
  private String repairReason;
  private String repairContent;
  private String diagnosisResult;
  private String safetyImpact;
  private BigDecimal budgetAmount;
  private String applyDate;
  private String applicantName;
  private String status;
  private String statusLabel;
  private String approvedBy;
  private String approvedTime;
  private String completedDate;
  private String vendorName;
  private String repairManager;
  private String technicianName;
  private String acceptanceResult;
  private String signoffStatus;
  private String signoffStatusLabel;
  private String attachmentUrls;
  private BigDecimal actualAmount;
  private BigDecimal partsCost;
  private BigDecimal laborCost;
  private BigDecimal otherCost;
  private BigDecimal costVariance;
  private String auditRemark;
  private String remark;
}
