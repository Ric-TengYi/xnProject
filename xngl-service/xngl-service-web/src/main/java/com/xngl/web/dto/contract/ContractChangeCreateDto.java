package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractChangeCreateDto {

  @NotNull(message = "合同ID不能为空")
  private Long contractId;

  @NotBlank(message = "变更类型不能为空")
  private String changeType;

  private String afterSnapshotJson;

  private String reason;
}
