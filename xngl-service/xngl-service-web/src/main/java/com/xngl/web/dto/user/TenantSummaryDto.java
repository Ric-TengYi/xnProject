package com.xngl.web.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSummaryDto {

  private String tenantId;
  private long orgCount;
  private long userCount;
  private long roleCount;
  private String status;
}
