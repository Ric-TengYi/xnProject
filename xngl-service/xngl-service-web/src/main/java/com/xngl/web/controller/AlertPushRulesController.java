package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertPushRule;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.AlertPushRuleMapper;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alert-push-rules")
public class AlertPushRulesController {

  private final AlertPushRuleMapper mapper;
  private final UserService userService;

  public AlertPushRulesController(AlertPushRuleMapper mapper, UserService userService) {
    this.mapper = mapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<AlertPushRule>> list(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<AlertPushRule> rows =
        mapper.selectList(
            new LambdaQueryWrapper<AlertPushRule>()
                .eq(AlertPushRule::getTenantId, user.getTenantId())
                .orderByAsc(AlertPushRule::getLevel)
                .orderByAsc(AlertPushRule::getRuleCode));
    rows.sort(
        Comparator.comparing(AlertPushRule::getLevel, Comparator.nullsLast(String::compareTo))
            .thenComparing(AlertPushRule::getRuleCode, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  @PostMapping
  public ApiResult<AlertPushRule> create(
      @RequestBody AlertPushRuleUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(body);
    AlertPushRule entity = new AlertPushRule();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultValue(body.getStatus(), "ENABLED"));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<AlertPushRule> update(
      @PathVariable Long id, @RequestBody AlertPushRuleUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    AlertPushRule entity = requireEntity(id, user.getTenantId());
    validate(body);
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
    AlertPushRule entity = requireEntity(id, user.getTenantId());
    entity.setStatus(defaultValue(body != null ? body.getStatus() : null, "ENABLED"));
    mapper.updateById(entity);
    return ApiResult.ok();
  }

  private void validate(AlertPushRuleUpsertRequest body) {
    if (body == null || !StringUtils.hasText(body.getRuleCode()) || !StringUtils.hasText(body.getLevel())) {
      throw new BizException(400, "推送规则的规则编码和等级不能为空");
    }
    if (!StringUtils.hasText(body.getChannelTypes()) || !StringUtils.hasText(body.getReceiverExpr())) {
      throw new BizException(400, "推送方式和对象不能为空");
    }
  }

  private void apply(AlertPushRule entity, AlertPushRuleUpsertRequest body) {
    entity.setRuleCode(body.getRuleCode().trim());
    entity.setLevel(defaultValue(body.getLevel(), "L2"));
    entity.setChannelTypes(body.getChannelTypes().trim().toUpperCase());
    entity.setReceiverType(defaultValue(body.getReceiverType(), "ROLE"));
    entity.setReceiverExpr(body.getReceiverExpr().trim());
    entity.setPushTimeRule(trimToNull(body.getPushTimeRule()));
    entity.setEscalationMinutes(body.getEscalationMinutes() != null ? body.getEscalationMinutes() : 0);
  }

  private AlertPushRule requireEntity(Long id, Long tenantId) {
    AlertPushRule entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "推送规则不存在");
    }
    return entity;
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
  public static class AlertPushRuleUpsertRequest {
    private String ruleCode;
    private String level;
    private String channelTypes;
    private String receiverType;
    private String receiverExpr;
    private String pushTimeRule;
    private Integer escalationMinutes;
    private String status;
  }
}
