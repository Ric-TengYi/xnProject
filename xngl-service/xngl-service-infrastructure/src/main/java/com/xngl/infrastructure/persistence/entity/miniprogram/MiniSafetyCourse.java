package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_safety_course")
public class MiniSafetyCourse extends BaseEntity {

  private Long tenantId;
  private String courseCode;
  private String title;
  private String courseType;
  private String coverUrl;
  private String fileUrl;
  private Integer durationMinutes;
  private Integer randomCheckMinutes;
  private Integer faceCheckRequired;
  private String description;
  private String status;
}
