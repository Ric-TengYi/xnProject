package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniExportShareDto {

  private String taskId;
  private String status;
  private String fileName;
  private String downloadUrl;
  private String expireTime;
}
