package com.xngl.web.dto.vehicle;

import java.util.List;
import lombok.Data;

@Data
public class VehicleTrackHistoryDto {

  private String vehicleId;
  private String plateNo;
  private String startTime;
  private String endTime;
  private int pointCount;
  private List<VehicleTrackPointDto> points;
}
