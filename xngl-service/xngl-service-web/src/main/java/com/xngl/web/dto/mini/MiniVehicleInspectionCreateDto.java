package com.xngl.web.dto.mini;

import lombok.Data;

@Data
public class MiniVehicleInspectionCreateDto {

  private Long vehicleId;
  private String dispatchNo;
  private String inspectionTime;
  private String vehiclePhotoUrls;
  private String certificatePhotoUrls;
  private String issueSummary;
  private String conclusion;
}
