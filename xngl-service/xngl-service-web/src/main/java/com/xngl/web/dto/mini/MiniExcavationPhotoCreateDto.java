package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniExcavationPhotoCreateDto {

  private Long projectId;
  private Long siteId;
  private String plateNo;
  private String fileUrl;
  private String photoType;
  private BigDecimal longitude;
  private BigDecimal latitude;
  private String shootTime;
  private String remark;
}
