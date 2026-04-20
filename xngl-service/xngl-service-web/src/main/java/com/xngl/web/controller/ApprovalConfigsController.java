package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.approval.entity.ApprovalConfig;
import com.xngl.manager.approval.mapper.ApprovalConfigMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import com.xngl.web.support.CsvExportSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approval-configs")
public class ApprovalConfigsController {

  private final ApprovalConfigMapper mapper;
  private final UserContext userContext;

  public ApprovalConfigsController(ApprovalConfigMapper mapper, UserContext userContext) {
    this.mapper = mapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<ApprovalConfig>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<ApprovalConfig> rows =
        mapper.selectList(
            new LambdaQueryWrapper<ApprovalConfig>()
                .eq(ApprovalConfig::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(processKey), ApprovalConfig::getProcessKey, trimToNull(processKey))
                .eq(StringUtils.hasText(status), ApprovalConfig::getStatus, trimToNull(status))
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(ApprovalConfig::getConfigName, keywordValue)
                            .or()
                            .like(ApprovalConfig::getNodeName, keywordValue)
                            .or()
                            .like(ApprovalConfig::getApprovers, keywordValue)
                            .or()
                            .like(ApprovalConfig::getConditions, keywordValue))
                .orderByAsc(ApprovalConfig::getProcessKey)
                .orderByAsc(ApprovalConfig::getSortOrder)
                .orderByAsc(ApprovalConfig::getId));
    return ApiResult.ok(rows);
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<ApprovalConfig> rows =
        mapper.selectList(
            new LambdaQueryWrapper<ApprovalConfig>()
                .eq(ApprovalConfig::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(processKey), ApprovalConfig::getProcessKey, trimToNull(processKey))
                .eq(StringUtils.hasText(status), ApprovalConfig::getStatus, trimToNull(status))
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(ApprovalConfig::getConfigName, keywordValue)
                            .or()
                            .like(ApprovalConfig::getNodeName, keywordValue)
                            .or()
                            .like(ApprovalConfig::getApprovers, keywordValue)
                            .or()
                            .like(ApprovalConfig::getConditions, keywordValue))
                .orderByAsc(ApprovalConfig::getProcessKey)
                .orderByAsc(ApprovalConfig::getSortOrder)
                .orderByAsc(ApprovalConfig::getId));
    return CsvExportSupport.csvResponse(
        "approval_configs",
        List.of("流程编码", "流程名称", "节点编码", "节点名称", "审批方式", "审批人表达式", "流转条件", "超时小时", "排序", "状态"),
        rows.stream()
            .map(
                row ->
                    List.of(
                        CsvExportSupport.value(row.getProcessKey()),
                        CsvExportSupport.value(row.getConfigName()),
                        CsvExportSupport.value(row.getNodeCode()),
                        CsvExportSupport.value(row.getNodeName()),
                        CsvExportSupport.value(row.getApprovalType()),
                        CsvExportSupport.value(row.getApprovers()),
                        CsvExportSupport.value(row.getConditions()),
                        CsvExportSupport.value(row.getTimeoutHours()),
                        CsvExportSupport.value(row.getSortOrder()),
                        CsvExportSupport.value(row.getStatus())))
            .toList());
  }

  @GetMapping("/{id}")
  public ApiResult<ApprovalConfig> get(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    return ApiResult.ok(requireEntity(id, user.getTenantId()));
  }

  @PostMapping
  public ApiResult<ApprovalConfig> create(
      @RequestBody ApprovalConfigUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(body, user.getTenantId(), null);
    ApprovalConfig entity = new ApprovalConfig();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultStatus(body.getStatus()));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<ApprovalConfig> update(
      @PathVariable Long id, @RequestBody ApprovalConfigUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ApprovalConfig entity = requireEntity(id, user.getTenantId());
    validate(body, user.getTenantId(), id);
    apply(entity, body);
    if (StringUtils.hasText(body.getStatus())) {
      entity.setStatus(defaultStatus(body.getStatus()));
    }
    mapper.updateById(entity);
    return ApiResult.ok(mapper.selectById(id));
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(
      @PathVariable Long id, @RequestBody StatusUpdateDto body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ApprovalConfig entity = requireEntity(id, user.getTenantId());
    entity.setStatus(defaultStatus(body != null ? body.getStatus() : null));
    mapper.updateById(entity);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    requireEntity(id, user.getTenantId());
    mapper.deleteById(id);
    return ApiResult.ok();
  }

  private void validate(ApprovalConfigUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null
        || !StringUtils.hasText(body.getProcessKey())
        || !StringUtils.hasText(body.getConfigName())
        || !StringUtils.hasText(body.getNodeCode())
        || !StringUtils.hasText(body.getNodeName())) {
      throw new BizException(400, "流程、配置名称、节点编码、节点名称不能为空");
    }
    ApprovalConfig existing =
        mapper.selectOne(
            new LambdaQueryWrapper<ApprovalConfig>()
                .eq(ApprovalConfig::getTenantId, tenantId)
                .eq(ApprovalConfig::getProcessKey, body.getProcessKey().trim())
                .eq(ApprovalConfig::getNodeCode, body.getNodeCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "同流程下节点编码已存在");
    }
  }

  private void apply(ApprovalConfig entity, ApprovalConfigUpsertRequest body) {
    entity.setProcessKey(body.getProcessKey().trim());
    entity.setConfigName(body.getConfigName().trim());
    entity.setApprovalType(
        StringUtils.hasText(body.getApprovalType()) ? body.getApprovalType().trim().toUpperCase() : "SERIAL");
    entity.setNodeCode(body.getNodeCode().trim());
    entity.setNodeName(body.getNodeName().trim());
    entity.setApprovers(trimToNull(body.getApprovers()));
    entity.setConditions(trimToNull(body.getConditions()));
    entity.setFormTemplateCode(trimToNull(body.getFormTemplateCode()));
    entity.setTimeoutHours(body.getTimeoutHours() != null ? body.getTimeoutHours() : 24);
    entity.setSortOrder(body.getSortOrder() != null ? body.getSortOrder() : 0);
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private ApprovalConfig requireEntity(Long id, Long tenantId) {
    ApprovalConfig entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "审批流程配置不存在");
    }
    return entity;
  }

  private String defaultStatus(String status) {
    return StringUtils.hasText(status) ? status.trim().toUpperCase() : "ENABLED";
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  @Data
  public static class ApprovalConfigUpsertRequest {
    private String processKey;
    private String configName;
    private String approvalType;
    private String nodeCode;
    private String nodeName;
    private String approvers;
    private String conditions;
    private String formTemplateCode;
    private Integer timeoutHours;
    private Integer sortOrder;
    private String status;
    private String remark;
  }
}
