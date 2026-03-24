package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.ErrorLog;
import java.time.LocalDateTime;

public interface ErrorLogService {

  void save(ErrorLog log);

  IPage<ErrorLog> page(
      Long tenantId,
      Long userId,
      String keyword,
      String level,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize);
}
