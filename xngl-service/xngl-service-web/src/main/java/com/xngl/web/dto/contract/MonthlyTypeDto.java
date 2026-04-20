package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MonthlyTypeDto {

  private String contractType;
  private Integer count;
  private BigDecimal amount;
  private BigDecimal volume;
}
