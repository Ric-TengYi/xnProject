package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractExtensionItemDto {

  private String id;
  private String applyNo;
  private String contractId;
  private String contractNo;
  private String originalExpireDate;
  private String requestedExpireDate;
  private BigDecimal requestedVolumeDelta;
  private String reason;
  private String approvalStatus;
  private String applicantId;
  private String createTime;
}
