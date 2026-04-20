package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.ErrorLog;
import com.xngl.infrastructure.persistence.mapper.ErrorLogMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {

  private final ErrorLogMapper errorLogMapper;

  public ErrorLogServiceImpl(ErrorLogMapper errorLogMapper) {
    this.errorLogMapper = errorLogMapper;
  }

  @Override
  public void save(ErrorLog log) {
    if (log.getCreateTime() == null) {
      log.setCreateTime(LocalDateTime.now());
    }
    if (log.getUpdateTime() == null) {
      log.setUpdateTime(log.getCreateTime());
    }
    if (!StringUtils.hasText(log.getLevel())) {
      log.setLevel("ERROR");
    }
    if (log.getDeleted() == null) {
      log.setDeleted(0);
    }
    errorLogMapper.insert(log);
  }

  @Override
  public IPage<ErrorLog> page(
      Long tenantId,
      Long userId,
      String keyword,
      String level,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    LambdaQueryWrapper<ErrorLog> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(ErrorLog::getTenantId, tenantId);
    }
    if (userId != null) {
      query.eq(ErrorLog::getUserId, userId);
    }
    if (StringUtils.hasText(level)) {
      query.eq(ErrorLog::getLevel, level.trim().toUpperCase());
    }
    if (startTime != null) {
      query.ge(ErrorLog::getCreateTime, startTime);
    }
    if (endTime != null) {
      query.le(ErrorLog::getCreateTime, endTime);
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(ErrorLog::getUsername, effectiveKeyword)
                  .or()
                  .like(ErrorLog::getExceptionType, effectiveKeyword)
                  .or()
                  .like(ErrorLog::getErrorMessage, effectiveKeyword)
                  .or()
                  .like(ErrorLog::getRequestUri, effectiveKeyword)
                  .or()
                  .like(ErrorLog::getIp, effectiveKeyword));
    }
    query.orderByDesc(ErrorLog::getCreateTime).orderByDesc(ErrorLog::getId);
    return errorLogMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }
}
