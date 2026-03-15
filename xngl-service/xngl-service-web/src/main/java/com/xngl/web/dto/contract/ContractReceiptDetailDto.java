package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContractReceiptDetailDto extends ContractReceiptItemDto {

  private BigDecimal contractAmount;
  private BigDecimal receivedAmount;
  private String contractStatus;
}
