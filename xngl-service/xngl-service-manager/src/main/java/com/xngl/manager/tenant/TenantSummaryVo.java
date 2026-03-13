package com.xngl.manager.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSummaryVo {

  private Long tenantId;
  private long orgCount;
  private long userCount;
  private long roleCount;
  private String status;
}
