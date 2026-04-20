package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.OperationLog;
import com.xngl.infrastructure.persistence.mapper.OperationLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
      String keyword,
      String module,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    return operationLogMapper.selectPage(
        new Page<>(pageNo, pageSize),
        buildQuery(tenantId, userId, keyword, module, startTime, endTime));
  }

  @Override
  public List<OperationLog> list(
      Long tenantId,
      Long userId,
      String keyword,
      String module,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    return operationLogMapper.selectList(
        buildQuery(tenantId, userId, keyword, module, startTime, endTime));
  }

  private LambdaQueryWrapper<OperationLog> buildQuery(
      Long tenantId,
      Long userId,
      String keyword,
      String module,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    LambdaQueryWrapper<OperationLog> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(OperationLog::getTenantId, tenantId);
    if (userId != null) q.eq(OperationLog::getUserId, userId);
    if (StringUtils.hasText(module) && !"all".equalsIgnoreCase(module)) {
      q.eq(OperationLog::getModule, module.trim());
    }
    if (startTime != null) q.ge(OperationLog::getCreateTime, startTime);
    if (endTime != null) q.le(OperationLog::getCreateTime, endTime);
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      q.and(
          wrapper ->
              wrapper.like(OperationLog::getUsername, effectiveKeyword)
                  .or()
                  .like(OperationLog::getModule, effectiveKeyword)
                  .or()
                  .like(OperationLog::getOperation, effectiveKeyword)
                  .or()
                  .like(OperationLog::getContent, effectiveKeyword)
                  .or()
                  .like(OperationLog::getRequestUri, effectiveKeyword)
                  .or()
                  .like(OperationLog::getIp, effectiveKeyword));
    }
    q.orderByDesc(OperationLog::getCreateTime).orderByDesc(OperationLog::getId);
    return q;
  }
}
