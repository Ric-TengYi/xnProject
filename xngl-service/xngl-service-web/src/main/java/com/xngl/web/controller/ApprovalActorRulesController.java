package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.ApprovalActorRule;
import com.xngl.manager.approval.ApprovalActorRuleService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.support.CsvExportSupport;
import com.xngl.web.dto.user.*;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/approval-actor-rules")
public class ApprovalActorRulesController {

  private final ApprovalActorRuleService approvalActorRuleService;

  public ApprovalActorRulesController(ApprovalActorRuleService approvalActorRuleService) {
    this.approvalActorRuleService = approvalActorRuleService;
  }

  @GetMapping
  public ApiResult<PageResult<ApprovalActorRuleListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    IPage<ApprovalActorRule> page =
        approvalActorRuleService.page(tenantId, keyword, processKey, status, pageNo, pageSize);
    List<ApprovalActorRuleListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status) {
    List<ApprovalActorRule> rows = approvalActorRuleService.list(tenantId, keyword, processKey, status);
    List<List<?>> exportRows = new ArrayList<>();
    for (ApprovalActorRule row : rows) {
      exportRows.add(
          List.of(
              CsvExportSupport.value(StringUtils.hasText(row.getProcessKey()) ? row.getProcessKey() : row.getBizType()),
              CsvExportSupport.value(StringUtils.hasText(row.getRuleName()) ? row.getRuleName() : row.getBizType()),
              CsvExportSupport.value(StringUtils.hasText(row.getRuleType()) ? row.getRuleType() : row.getActorType()),
              CsvExportSupport.value(StringUtils.hasText(row.getRuleExpression()) ? row.getRuleExpression() : row.getActorRefId()),
              CsvExportSupport.value(row.getPriority()),
              CsvExportSupport.value(StringUtils.hasText(row.getStatus()) ? row.getStatus() : "ENABLED")));
    }
    return CsvExportSupport.csvResponse(
        "approval_actor_rules",
        List.of("流程编码", "规则名称", "规则类型", "规则表达式", "优先级", "状态"),
        exportRows);
  }

  @GetMapping("/{id}")
  public ApiResult<ApprovalActorRuleDetailDto> get(@PathVariable Long id) {
    ApprovalActorRule r = approvalActorRuleService.getById(id);
    if (r == null) return ApiResult.fail(404, "审批人规则不存在");
    return ApiResult.ok(toDetail(r));
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody ApprovalActorRuleCreateUpdateDto dto) {
    ApprovalActorRule r = new ApprovalActorRule();
    mapToEntity(dto, r);
    r.setStatus("ENABLED");
    long id = approvalActorRuleService.create(r);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id, @RequestBody ApprovalActorRuleCreateUpdateDto dto) {
    ApprovalActorRule r = approvalActorRuleService.getById(id);
    if (r == null) return ApiResult.fail(404, "审批人规则不存在");
    mapToEntity(dto, r);
    r.setId(id);
    approvalActorRuleService.update(r);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDto dto) {
    ApprovalActorRule r = approvalActorRuleService.getById(id);
    if (r == null) return ApiResult.fail(404, "审批人规则不存在");
    approvalActorRuleService.updateStatus(id, dto.getStatus());
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id) {
    ApprovalActorRule r = approvalActorRuleService.getById(id);
    if (r == null) return ApiResult.fail(404, "审批人规则不存在");
    approvalActorRuleService.delete(id);
    return ApiResult.ok();
  }

  private ApprovalActorRuleListItemDto toListItem(ApprovalActorRule r) {
    return new ApprovalActorRuleListItemDto(
        String.valueOf(r.getId()),
        StringUtils.hasText(r.getProcessKey()) ? r.getProcessKey() : r.getBizType(),
        StringUtils.hasText(r.getRuleName()) ? r.getRuleName() : r.getBizType(),
        StringUtils.hasText(r.getRuleType()) ? r.getRuleType() : r.getActorType(),
        r.getPriority(),
        StringUtils.hasText(r.getStatus()) ? r.getStatus() : "ENABLED");
  }

  private ApprovalActorRuleDetailDto toDetail(ApprovalActorRule r) {
    return new ApprovalActorRuleDetailDto(
        String.valueOf(r.getId()),
        String.valueOf(r.getTenantId()),
        StringUtils.hasText(r.getProcessKey()) ? r.getProcessKey() : r.getBizType(),
        StringUtils.hasText(r.getRuleName()) ? r.getRuleName() : r.getBizType(),
        StringUtils.hasText(r.getRuleType()) ? r.getRuleType() : r.getActorType(),
        StringUtils.hasText(r.getRuleExpression()) ? r.getRuleExpression() : r.getActorRefId(),
        r.getPriority(),
        StringUtils.hasText(r.getStatus()) ? r.getStatus() : "ENABLED");
  }

  private void mapToEntity(ApprovalActorRuleCreateUpdateDto dto, ApprovalActorRule r) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      r.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    r.setBizType(dto.getProcessKey());
    r.setNodeCode("DEFAULT");
    r.setProcessKey(dto.getProcessKey());
    r.setRuleName(dto.getRuleName());
    r.setRuleType(dto.getRuleType());
    r.setRuleExpression(dto.getRuleExpression());
    r.setActorType(StringUtils.hasText(dto.getRuleType()) ? dto.getRuleType() : "ROLE");
    r.setActorRefId(dto.getRuleExpression());
    if (!StringUtils.hasText(r.getMatchMode())) {
      r.setMatchMode("OR");
    }
    if (r.getActorSnapshotFlag() == null) {
      r.setActorSnapshotFlag(0);
    }
    r.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
  }
}
