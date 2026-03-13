package com.xngl.manager.approval;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.ApprovalActorRule;

public interface ApprovalActorRuleService {

  ApprovalActorRule getById(Long id);

  IPage<ApprovalActorRule> page(
      Long tenantId, String processKey, String status, int pageNo, int pageSize);

  long create(ApprovalActorRule rule);

  void update(ApprovalActorRule rule);

  void updateStatus(Long id, String status);

  void delete(Long id);
}
