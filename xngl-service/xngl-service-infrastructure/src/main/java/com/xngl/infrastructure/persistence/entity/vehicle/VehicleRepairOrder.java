package com.xngl.infrastructure.persistence.entity.vehicle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle_repair_order")
public class VehicleRepairOrder extends BaseEntity {

  private Long tenantId;
  private Long vehicleId;
  private Long orgId;
  private String orderNo;
  private String urgencyLevel;
  private String repairReason;
  private String repairContent;
  private BigDecimal budgetAmount;
  private LocalDate applyDate;
  private String applicantName;
  private String status;
  private String auditRemark;
  private String approvedBy;
  private LocalDateTime approvedTime;
  private LocalDate completedDate;
  private String vendorName;
  private BigDecimal actualAmount;
  private String remark;
}
