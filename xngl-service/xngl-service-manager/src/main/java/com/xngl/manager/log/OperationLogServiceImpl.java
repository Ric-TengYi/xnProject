package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.OperationLog;
import com.xngl.infrastructure.persistence.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl implements OperationLogService {

  private final OperationLogMapper operationLogMapper;

  public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  public void save(OperationLog log) {
    if (log.getCreateTime() == null) log.setCreateTime(LocalDateTime.now());
    operationLogMapper.insert(log);
  }

  @Override
  public IPage<OperationLog> page(
      Long tenantId,
      Long userId,
      String module,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    LambdaQueryWrapper<OperationLog> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(OperationLog::getTenantId, tenantId);
    if (userId != null) q.eq(OperationLog::getUserId, userId);
    if (module != null && !module.isEmpty()) q.eq(OperationLog::getModule, module);
    if (startTime != null) q.ge(OperationLog::getCreateTime, startTime);
    if (endTime != null) q.le(OperationLog::getCreateTime, endTime);
    q.orderByDesc(OperationLog::getCreateTime);
    return operationLogMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }
}
