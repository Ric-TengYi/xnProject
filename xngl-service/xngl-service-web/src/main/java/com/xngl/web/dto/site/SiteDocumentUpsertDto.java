package com.xngl.web.dto.site;

import lombok.Data;

@Data
public class SiteDocumentUpsertDto {

  private String stageCode;
  private String approvalType;
  private String documentType;
  private String fileName;
  private String fileUrl;
  private Long fileSize;
  private String mimeType;
  private String remark;
}
