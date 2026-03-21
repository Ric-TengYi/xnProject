package com.xngl.web.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.web.controller.AlertsController;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "app.alerts",
    name = "auto-generate-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AlertAutoGenerateScheduler {

  private final AlertsController alertsController;
  private final UserMapper userMapper;

  public AlertAutoGenerateScheduler(AlertsController alertsController, UserMapper userMapper) {
    this.alertsController = alertsController;
    this.userMapper = userMapper;
  }

  @Scheduled(cron = "${app.alerts.auto-generate-cron:0 0/30 * * * ?}")
  public void refreshTenantAlerts() {
    Set<Long> tenantIds = loadTenantIds();
    for (Long tenantId : tenantIds) {
      try {
        Map<String, Object> result = alertsController.generateForTenant(tenantId, Set.of("PROJECT", "CONTRACT", "USER"));
        log.info("auto-generate alerts success tenantId={} result={}", tenantId, result);
      } catch (Exception ex) {
        log.warn("auto-generate alerts failed tenantId={} reason={}", tenantId, ex.getMessage());
      }
    }
  }

  private Set<Long> loadTenantIds() {
    return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .isNotNull(User::getTenantId)
                .select(User::getTenantId))
        .stream()
        .map(User::getTenantId)
        .filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }
}
