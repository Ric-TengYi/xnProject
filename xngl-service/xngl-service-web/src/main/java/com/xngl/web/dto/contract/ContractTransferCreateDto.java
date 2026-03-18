package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTransferCreateDto {

  @NotNull(message = "源合同ID不能为空")
  private Long sourceContractId;

  @NotNull(message = "目标合同ID不能为空")
  private Long targetContractId;

  private BigDecimal transferAmount;

  private BigDecimal transferVolume;

  private String reason;
}
