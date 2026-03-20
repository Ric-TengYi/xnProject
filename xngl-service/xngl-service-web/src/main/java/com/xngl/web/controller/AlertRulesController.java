package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertRule;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.AlertRuleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRulesController {

  private final AlertRuleMapper mapper;
  private final UserService userService;

  public AlertRulesController(AlertRuleMapper mapper, UserService userService) {
    this.mapper = mapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<AlertRule>> list(
      @RequestParam(required = false) String ruleScene,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<AlertRule> rows =
        mapper.selectList(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(ruleScene), AlertRule::getRuleScene, ruleScene)
                .eq(StringUtils.hasText(status), AlertRule::getStatus, status)
                .orderByAsc(AlertRule::getRuleScene)
                .orderByAsc(AlertRule::getRuleCode));
    rows.sort(
        Comparator.comparing(AlertRule::getRuleScene, Comparator.nullsLast(String::compareTo))
            .thenComparing(AlertRule::getRuleCode, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  @PostMapping
  public ApiResult<AlertRule> create(@RequestBody AlertRuleUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(body, user.getTenantId(), null);
    AlertRule entity = new AlertRule();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultStatus(body.getStatus()));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<AlertRule> update(
      @PathVariable Long id, @RequestBody AlertRuleUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    AlertRule entity = requireEntity(id, user.getTenantId());
    validate(body, user.getTenantId(), id);
    apply(entity, body);
    if (StringUtils.hasText(body.getStatus())) {
      entity.setStatus(body.getStatus().trim().toUpperCase());
    }
    mapper.updateById(entity);
    return ApiResult.ok(mapper.selectById(id));
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(
      @PathVariable Long id, @RequestBody StatusUpdateDto body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    AlertRule entity = requireEntity(id, user.getTenantId());
    entity.setStatus(defaultStatus(body != null ? body.getStatus() : null));
    mapper.updateById(entity);
    return ApiResult.ok();
  }

  private void validate(AlertRuleUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getRuleCode()) || !StringUtils.hasText(body.getRuleName())) {
      throw new BizException(400, "规则编码和名称不能为空");
    }
    AlertRule existing =
        mapper.selectOne(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getTenantId, tenantId)
                .eq(AlertRule::getRuleCode, body.getRuleCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "规则编码已存在");
    }
  }

  private void apply(AlertRule entity, AlertRuleUpsertRequest body) {
    entity.setRuleCode(body.getRuleCode().trim());
    entity.setRuleName(body.getRuleName().trim());
    entity.setRuleScene(defaultValue(body.getRuleScene(), "VEHICLE"));
    entity.setMetricCode(defaultValue(body.getMetricCode(), entity.getRuleCode()));
    entity.setThresholdJson(trimToNull(body.getThresholdJson()));
    entity.setLevel(defaultValue(body.getLevel(), "L2"));
    entity.setScopeType(defaultValue(body.getScopeType(), "GLOBAL"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private AlertRule requireEntity(Long id, Long tenantId) {
    AlertRule entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "预警规则不存在");
    }
    return entity;
  }

  private String defaultStatus(String status) {
    return defaultValue(status, "ENABLED");
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (!StringUtils.hasText(userId)) {
      throw new BizException(401, "未登录或 token 无效");
    }
    try {
      User user = userService.getById(Long.parseLong(userId));
      if (user == null || user.getTenantId() == null) {
        throw new BizException(401, "用户不存在");
      }
      return user;
    } catch (NumberFormatException ex) {
      throw new BizException(401, "token 中的用户信息无效");
    }
  }

  @Data
  public static class AlertRuleUpsertRequest {
    private String ruleCode;
    private String ruleName;
    private String ruleScene;
    private String metricCode;
    private String thresholdJson;
    private String level;
    private String scopeType;
    private String status;
    private String remark;
  }
}
