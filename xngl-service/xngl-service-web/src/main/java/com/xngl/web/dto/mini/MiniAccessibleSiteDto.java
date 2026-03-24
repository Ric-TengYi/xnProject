package com.xngl.web.dto.mini;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAccessibleSiteDto {

  private String siteId;
  private String siteName;
  private String siteCode;
  private String roleType;
  private String dutyScope;
  private Integer todayDisposalCount;
  private BigDecimal todayDisposalVolume;
  private Integer onlineDeviceCount;
}
