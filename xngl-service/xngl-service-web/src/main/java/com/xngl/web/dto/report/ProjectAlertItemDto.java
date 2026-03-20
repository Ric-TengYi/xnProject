package com.xngl.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAlertItemDto {

  private String projectId;
  private String projectName;
  private String siteName;
  private Integer progressPercent;
  private String status;
  private Integer warningLevel;
}
