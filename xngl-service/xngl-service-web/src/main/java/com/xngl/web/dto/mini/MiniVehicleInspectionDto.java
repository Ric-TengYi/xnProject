package com.xngl.web.dto.mini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniVehicleInspectionDto {

  private String id;
  private String vehicleId;
  private String plateNo;
  private String orgId;
  private String orgName;
  private String dispatchNo;
  private String inspectionTime;
  private String vehiclePhotoUrls;
  private String certificatePhotoUrls;
  private String issueSummary;
  private String conclusion;
  private String status;
  private String inspectorName;
  private String createTime;
}
