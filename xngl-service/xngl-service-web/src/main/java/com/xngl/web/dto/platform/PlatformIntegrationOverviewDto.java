package com.xngl.web.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformIntegrationOverviewDto {

  private int enabledCount;
  private int totalCount;
  private int videoChannelCount;
  private int onlineDamSiteCount;
  private int activeSsoTicketCount;
  private int govSyncCount;
  private int weighbridgeRecordCount;
}
