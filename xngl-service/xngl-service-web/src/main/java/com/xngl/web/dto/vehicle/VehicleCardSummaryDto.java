package com.xngl.web.dto.vehicle;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCardSummaryDto {

  private Integer totalCards;
  private Integer fuelCards;
  private Integer electricCards;
  private Integer boundCards;
  private Integer lowBalanceCards;
  private BigDecimal totalBalance;
  private BigDecimal fuelBalance;
  private BigDecimal electricBalance;
}
