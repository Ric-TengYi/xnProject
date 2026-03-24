package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniSafetyCourseDto {

  private String id;
  private String courseCode;
  private String title;
  private String courseType;
  private String coverUrl;
  private String fileUrl;
  private Integer durationMinutes;
  private Integer randomCheckMinutes;
  private Boolean faceCheckRequired;
  private String description;
  private String status;
}
