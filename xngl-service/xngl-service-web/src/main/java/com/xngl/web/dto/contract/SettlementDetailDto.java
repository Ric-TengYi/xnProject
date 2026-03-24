package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SettlementDetailDto extends SettlementItemDto {

  private BigDecimal unitPrice;
  private String settlementDate;
  private String processInstanceId;
  private String remark;
  private List<SettlementContractSummaryDto> contractSummaries;
  private List<SettlementLineDto> items;
}
