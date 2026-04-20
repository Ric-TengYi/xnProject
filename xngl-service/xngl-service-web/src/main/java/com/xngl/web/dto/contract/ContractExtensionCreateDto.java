package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractExtensionCreateDto {

  @NotNull(message = "合同ID不能为空")
  private Long contractId;

  @NotNull(message = "申请到期日期不能为空")
  private LocalDate requestedExpireDate;

  private BigDecimal requestedVolumeDelta;

  private String reason;
}
