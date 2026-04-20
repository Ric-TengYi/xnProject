package com.xngl.manager.contract;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractApprovalRecordVo {
  private Long id;
  private Long contractId;
  private String actionType;
  private String actionName;
  private Long operatorId;
  private String operatorName;
  private String fromStatus;
  private String toStatus;
  private String remark;
  private LocalDateTime operateTime;
}