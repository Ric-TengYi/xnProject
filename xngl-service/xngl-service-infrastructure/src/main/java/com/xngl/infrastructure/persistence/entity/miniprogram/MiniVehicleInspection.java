package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_vehicle_inspection")
public class MiniVehicleInspection extends BaseEntity {

  private Long tenantId;
  private Long vehicleId;
  private Long orgId;
  private Long userId;
  private String inspectorName;
  private String plateNo;
  private String dispatchNo;
  private LocalDateTime inspectionTime;
  private String vehiclePhotoUrls;
  private String certificatePhotoUrls;
  private String issueSummary;
  private String conclusion;
  private String status;
}
