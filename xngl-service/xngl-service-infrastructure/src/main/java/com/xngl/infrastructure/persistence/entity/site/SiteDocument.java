package com.xngl.infrastructure.persistence.entity.site;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xngl.infrastructure.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_site_document")
public class SiteDocument extends BaseEntity {

  private Long tenantId;
  private Long siteId;
  private String stageCode;
  private String approvalType;
  private String documentType;
  private String fileName;
  private String fileUrl;
  private Long fileSize;
  private String mimeType;
  private String formatRequirement;
  private Long uploaderId;
  private String uploaderName;
  private String remark;
}
