package com.xngl.infrastructure.persistence.entity.security;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_security_inspection")
public class SecurityInspection extends BaseEntity {

  private Long tenantId;
  private String inspectionNo;
  private String objectType;
  private Long objectId;
  private String title;
  private String checkScene;
  private String checkType;
  private String hazardCategory;
  private String resultLevel;
  private String dangerLevel;
  private Integer issueCount;
  private String status;
  private Long projectId;
  private Long siteId;
  private Long vehicleId;
  private Long userId;
  private Long inspectorId;
  private String inspectorName;
  private String rectifyOwner;
  private String rectifyOwnerPhone;
  private String description;
  private String attachmentUrls;
  private BigDecimal estimatedCost;
  private LocalDateTime rectifyDeadline;
  private String rectifyRemark;
  private LocalDateTime rectifyTime;
  private LocalDateTime checkTime;
  private LocalDateTime nextCheckTime;
}
