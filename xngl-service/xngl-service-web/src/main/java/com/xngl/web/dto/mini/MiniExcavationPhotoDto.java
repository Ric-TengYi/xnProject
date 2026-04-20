package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniExcavationPhotoDto {

  private String id;
  private String projectId;
  private String projectName;
  private String siteId;
  private String siteName;
  private String plateNo;
  private String recognitionSource;
  private String fileUrl;
  private String photoType;
  private BigDecimal longitude;
  private BigDecimal latitude;
  private String shootTime;
  private String remark;
  private String auditStatus;
}
