package com.xngl.manager.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Tenant;

public interface TenantService {

  Tenant getById(Long id);

  IPage<Tenant> page(String tenantName, String tenantType, String status, int pageNo, int pageSize);

  TenantSummaryVo getSummary(Long tenantId);

  long create(Tenant tenant);

  void update(Tenant tenant);

  void updateStatus(Long id, String status);
}
