package com.xngl.web.dto.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDocumentDto {

  private String id;
  private String siteId;
  private String siteName;
  private String stageCode;
  private String approvalType;
  private String documentType;
  private String fileName;
  private String fileUrl;
  private Long fileSize;
  private String mimeType;
  private String formatRequirement;
  private String uploaderId;
  private String uploaderName;
  private String remark;
  private String createTime;
  private String updateTime;
}
