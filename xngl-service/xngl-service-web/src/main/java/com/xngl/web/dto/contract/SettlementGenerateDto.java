package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SettlementGenerateDto {

  @NotNull private Long targetId;
  @NotNull private LocalDate periodStart;
  @NotNull private LocalDate periodEnd;
  private String pricingMode;
  private String remark;
}
