package com.xngl.web.dto.platform;

import lombok.Data;

@Data
public class GovPermitSyncRequestDto {

  private String syncMode;
  private Boolean includeTransportPermits;
  private Long contractId;
  private Long projectId;
  private Long siteId;
  private String vehicleNo;
  private String remark;
}
