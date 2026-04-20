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

  public TenantSummaryVo(Long tenantId, Long orgCount, Long userCount, Long roleCount, String status) {
    this.tenantId = tenantId;
    this.orgCount = orgCount == null ? 0L : orgCount;
    this.userCount = userCount == null ? 0L : userCount;
    this.roleCount = roleCount == null ? 0L : roleCount;
    this.status = status;
  }
}
