package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniSafetyLearningRecordDto {

  private String id;
  private String courseId;
  private String courseTitle;
  private String courseType;
  private String learnerName;
  private String status;
  private Integer studiedMinutes;
  private Integer progressPercent;
  private Integer faceCheckCount;
  private String lastFaceCheckTime;
  private String nextFaceCheckTime;
  private String startTime;
  private String completeTime;
  private String lastStudyTime;
  private String remark;
}
