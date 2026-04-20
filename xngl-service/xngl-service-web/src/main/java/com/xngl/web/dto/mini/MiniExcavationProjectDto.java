package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniExcavationProjectDto {

  private String projectId;
  private String projectCode;
  private String projectName;
  private String address;
  private Integer status;
  private String statusLabel;
  private String orgId;
  private String orgName;
  private Long contractCount;
  private Long siteCount;
  private BigDecimal agreedVolume;
  private BigDecimal disposedVolume;
  private BigDecimal remainingVolume;
}
