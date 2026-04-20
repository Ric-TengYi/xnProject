package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniSiteOverviewDto {

  private String siteId;
  private String siteName;
  private String siteCode;
  private String address;
  private String siteType;
  private Integer status;
  private BigDecimal capacity;
  private String siteLevel;
  private String managementArea;
  private Integer todayDisposalCount;
  private BigDecimal todayDisposalVolume;
  private Integer totalDisposalCount;
  private BigDecimal totalDisposalVolume;
  private Integer onlineDeviceCount;
  private Integer personnelCount;
}
