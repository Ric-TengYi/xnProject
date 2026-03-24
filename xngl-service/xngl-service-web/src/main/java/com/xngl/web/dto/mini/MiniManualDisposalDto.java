package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniManualDisposalDto {

  private String id;
  private String siteId;
  private String siteName;
  private String contractId;
  private String contractNo;
  private String projectId;
  private String projectName;
  private String vehicleId;
  private String plateNo;
  private String disposalTime;
  private String volume;
  private String amount;
  private String weightTons;
  private String photoUrls;
  private String remark;
  private String status;
  private String ticketId;
  private String ticketNo;
}
