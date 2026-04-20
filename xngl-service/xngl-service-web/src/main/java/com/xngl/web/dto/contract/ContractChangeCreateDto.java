package com.xngl.web.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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

  // Site change specific fields
  private Long newSiteId;
  private String newSiteName;

  // Volume change specific fields
  private BigDecimal newAgreedVolume;
  private BigDecimal volumeDelta;

  // Amount change specific fields
  private BigDecimal newContractAmount;
  private BigDecimal newUnitPrice;

  // Date change specific fields
  private String newExpireDate;
}
