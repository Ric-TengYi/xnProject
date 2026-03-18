package com.xngl.web.dto.contract;

import lombok.Data;

@Data
public class ExportTaskDto {

  private String id;
  private String bizType;
  private String exportType;
  private String fileName;
  private String fileUrl;
  private String status;
  private String failReason;
  private String creatorId;
  private String createTime;
  private String expireTime;
}
