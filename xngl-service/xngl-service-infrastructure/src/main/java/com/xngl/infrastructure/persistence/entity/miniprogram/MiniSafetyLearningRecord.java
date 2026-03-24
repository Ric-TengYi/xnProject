package com.xngl.infrastructure.persistence.entity.miniprogram;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mini_safety_learning_record")
public class MiniSafetyLearningRecord extends BaseEntity {

  private Long tenantId;
  private Long courseId;
  private Long userId;
  private String learnerName;
  private String status;
  private Integer studiedMinutes;
  private Integer progressPercent;
  private Integer faceCheckCount;
  private LocalDateTime lastFaceCheckTime;
  private LocalDateTime nextFaceCheckTime;
  private LocalDateTime startTime;
  private LocalDateTime completeTime;
  private LocalDateTime lastStudyTime;
  private String remark;
}
