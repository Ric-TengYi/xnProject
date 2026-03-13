package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;

import java.time.LocalDateTime;

public interface LoginLogService {

  void save(LoginLog log);

  IPage<LoginLog> page(
      Long tenantId,
      Long userId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize);
}
