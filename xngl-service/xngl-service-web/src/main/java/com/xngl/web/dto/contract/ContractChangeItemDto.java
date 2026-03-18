package com.xngl.web.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractChangeItemDto {

  private String id;
  private String changeNo;
  private String contractId;
  private String contractNo;
  private String changeType;
  private String reason;
  private String approvalStatus;
  private String applicantId;
  private String createTime;
}
