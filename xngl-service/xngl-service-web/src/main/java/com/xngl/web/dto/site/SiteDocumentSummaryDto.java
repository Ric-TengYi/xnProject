package com.xngl.web.dto.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDocumentSummaryDto {

  private String siteId;
  private String siteName;
  private String stageCode;
  private String approvalType;
  private String documentType;
  private Integer documentCount;
  private String lastUpdateTime;
  private String uploaderName;
}
