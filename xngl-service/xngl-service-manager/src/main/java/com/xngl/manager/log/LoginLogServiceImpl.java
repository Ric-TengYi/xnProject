package com.xngl.manager.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;
import com.xngl.infrastructure.persistence.mapper.LoginLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int pageNo,
      int pageSize) {
    return loginLogMapper.selectPage(
        new Page<>(pageNo, pageSize),
        buildQuery(tenantId, userId, keyword, status, startTime, endTime));
  }

  @Override
  public List<LoginLog> list(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    return loginLogMapper.selectList(buildQuery(tenantId, userId, keyword, status, startTime, endTime));
  }

  private LambdaQueryWrapper<LoginLog> buildQuery(
      Long tenantId,
      Long userId,
      String keyword,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    LambdaQueryWrapper<LoginLog> q = new LambdaQueryWrapper<>();
    if (tenantId != null) q.eq(LoginLog::getTenantId, tenantId);
    if (userId != null) q.eq(LoginLog::getUserId, userId);
    if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
      q.eq(LoginLog::getSuccessFlag, "SUCCESS".equalsIgnoreCase(status) ? 1 : 0);
    }
    if (startTime != null) q.ge(LoginLog::getLoginTime, startTime);
    if (endTime != null) q.le(LoginLog::getLoginTime, endTime);
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      q.and(
          wrapper ->
              wrapper.like(LoginLog::getUsername, effectiveKeyword)
                  .or()
                  .like(LoginLog::getIp, effectiveKeyword)
                  .or()
                  .like(LoginLog::getFailReason, effectiveKeyword)
                  .or()
                  .like(LoginLog::getUserAgent, effectiveKeyword));
    }
    q.orderByDesc(LoginLog::getLoginTime).orderByDesc(LoginLog::getId);
    return q;
  }
}
