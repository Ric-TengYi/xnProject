package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
@RequestMapping("/api/alert-fences")
public class AlertFencesController {

  private final AlertFenceMapper mapper;
  private final UserContext userContext;

  public AlertFencesController(AlertFenceMapper mapper, UserContext userContext) {
    this.mapper = mapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<AlertFence>> list(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<AlertFence> rows =
        mapper.selectList(
            new LambdaQueryWrapper<AlertFence>()
                .eq(AlertFence::getTenantId, user.getTenantId())
                .orderByAsc(AlertFence::getFenceCode));
    rows.sort(Comparator.comparing(AlertFence::getFenceCode, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  @PostMapping
  public ApiResult<AlertFence> create(@RequestBody AlertFenceUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(body, user.getTenantId(), null);
    AlertFence entity = new AlertFence();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultValue(body.getStatus(), "ENABLED"));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<AlertFence> update(
      @PathVariable Long id, @RequestBody AlertFenceUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    AlertFence entity = requireEntity(id, user.getTenantId());
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
    AlertFence entity = requireEntity(id, user.getTenantId());
    entity.setStatus(defaultValue(body != null ? body.getStatus() : null, "ENABLED"));
    mapper.updateById(entity);
    return ApiResult.ok();
  }

  private void validate(AlertFenceUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getFenceCode()) || !StringUtils.hasText(body.getFenceName())) {
      throw new BizException(400, "围栏编码和名称不能为空");
    }
    AlertFence existing =
        mapper.selectOne(
            new LambdaQueryWrapper<AlertFence>()
                .eq(AlertFence::getTenantId, tenantId)
                .eq(AlertFence::getFenceCode, body.getFenceCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "围栏编码已存在");
    }
  }

  private void apply(AlertFence entity, AlertFenceUpsertRequest body) {
    entity.setRuleCode(trimToNull(body.getRuleCode()));
    entity.setFenceCode(body.getFenceCode().trim());
    entity.setFenceName(body.getFenceName().trim());
    entity.setFenceType(defaultValue(body.getFenceType(), "ENTRY"));
    entity.setGeoJson(trimToNull(body.getGeoJson()));
    entity.setBufferMeters(body.getBufferMeters() != null ? body.getBufferMeters() : BigDecimal.ZERO);
    entity.setBizScope(trimToNull(body.getBizScope()));
    entity.setActiveTimeRange(trimToNull(body.getActiveTimeRange()));
    entity.setDirectionRule(trimToNull(body.getDirectionRule()));
  }

  private AlertFence requireEntity(Long id, Long tenantId) {
    AlertFence entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "预警围栏不存在");
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
    return userContext.requireCurrentUser(request);
  }

  @Data
  public static class AlertFenceUpsertRequest {
    private String ruleCode;
    private String fenceCode;
    private String fenceName;
    private String fenceType;
    private String geoJson;
    private BigDecimal bufferMeters;
    private String bizScope;
    private String activeTimeRange;
    private String directionRule;
    private String status;
  }
}
