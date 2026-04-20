package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class VehicleCardConsumeDto {

  private BigDecimal amount;
  private String remark;
}
