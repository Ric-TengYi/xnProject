package com.xngl.web.dto.contract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTransferItemDto {

  private String id;
  private String transferNo;
  private String sourceContractId;
  private String sourceContractNo;
  private String targetContractId;
  private String targetContractNo;
  private BigDecimal transferAmount;
  private BigDecimal transferVolume;
  private String reason;
  private String approvalStatus;
  private String applicantId;
  private String createTime;
}
