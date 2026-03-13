package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;
import com.xngl.infrastructure.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService {

  private final LoginLogMapper loginLogMapper;

  public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
    this.loginLogMapper = loginLogMapper;
  }

  @Override
  public void save(LoginLog log) {
    if (log.getLoginTime() == null) log.setLoginTime(LocalDateTime.now());
    loginLogMapper.insert(log);
  }

  @Override
  public IPage<LoginLog> page(
      Long tenantId,
      Long userId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    LambdaQueryWrapper<LoginLog> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(LoginLog::getTenantId, tenantId);
    if (userId != null) q.eq(LoginLog::getUserId, userId);
    if (startTime != null) q.ge(LoginLog::getLoginTime, startTime);
    if (endTime != null) q.le(LoginLog::getLoginTime, endTime);
    q.orderByDesc(LoginLog::getLoginTime);
    return loginLogMapper.selectPage(new Page<>(pageNo, pageSize), q);
  }
}
