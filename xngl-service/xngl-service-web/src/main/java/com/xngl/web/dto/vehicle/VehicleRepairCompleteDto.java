package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleRepairCompleteDto {

  private LocalDate completedDate;
  private String vendorName;
  private BigDecimal actualAmount;
  private String remark;
}
