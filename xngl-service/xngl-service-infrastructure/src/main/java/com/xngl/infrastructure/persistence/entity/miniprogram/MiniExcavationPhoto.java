package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_excavation_photo")
public class MiniExcavationPhoto extends BaseEntity {

  private Long tenantId;
  private Long projectId;
  private Long siteId;
  private Long userId;
  private String reporterName;
  private String plateNo;
  private String recognitionSource;
  private String photoType;
  private String fileUrl;
  private BigDecimal longitude;
  private BigDecimal latitude;
  private LocalDateTime shootTime;
  private String remark;
  private String auditStatus;
}
