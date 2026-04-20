package com.xngl.infrastructure.persistence.entity.security;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_security_inspection_action")
public class SecurityInspectionAction extends BaseEntity {

  private Long tenantId;
  private Long inspectionId;
  private String actionType;
  private String actionLabel;
  private String beforeStatus;
  private String afterStatus;
  private String beforeResultLevel;
  private String afterResultLevel;
  private String actionRemark;
  private LocalDateTime nextCheckTime;
  private Long actorId;
  private String actorName;
}
