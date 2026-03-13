package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.OperationLog;

import java.time.LocalDateTime;

public interface OperationLogService {

  void save(OperationLog log);

  IPage<OperationLog> page(
      Long tenantId,
      Long userId,
      String module,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize);
}
