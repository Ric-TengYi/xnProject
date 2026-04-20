package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.system.ApprovalMaterialConfig;
import com.xngl.infrastructure.persistence.mapper.ApprovalMaterialConfigMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.ApprovalMaterialConfigCreateUpdateDto;
import com.xngl.web.dto.user.ApprovalMaterialConfigDetailDto;
import com.xngl.web.dto.user.ApprovalMaterialConfigListItemDto;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import com.xngl.web.support.CsvExportSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
@RequestMapping("/api/approval-material-configs")
public class ApprovalMaterialConfigsController {

  private final ApprovalMaterialConfigMapper approvalMaterialConfigMapper;
  private final UserContext userContext;

  public ApprovalMaterialConfigsController(
      ApprovalMaterialConfigMapper approvalMaterialConfigMapper, UserContext userContext) {
    this.approvalMaterialConfigMapper = approvalMaterialConfigMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<ApprovalMaterialConfigListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<ApprovalMaterialConfig> rows =
        approvalMaterialConfigMapper.selectList(
            new LambdaQueryWrapper<ApprovalMaterialConfig>()
                .eq(ApprovalMaterialConfig::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(processKey), ApprovalMaterialConfig::getProcessKey, processKey)
                .eq(StringUtils.hasText(status), ApprovalMaterialConfig::getStatus, status)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(ApprovalMaterialConfig::getMaterialCode, keywordValue)
                            .or()
                            .like(ApprovalMaterialConfig::getMaterialName, keywordValue)
                            .or()
                            .like(ApprovalMaterialConfig::getMaterialType, keywordValue))
                .orderByAsc(ApprovalMaterialConfig::getProcessKey)
                .orderByAsc(ApprovalMaterialConfig::getSortOrder)
                .orderByAsc(ApprovalMaterialConfig::getId));
    return ApiResult.ok(rows.stream().map(this::toListItem).collect(Collectors.toList()));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String processKey,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<ApprovalMaterialConfig> rows =
        approvalMaterialConfigMapper.selectList(
            new LambdaQueryWrapper<ApprovalMaterialConfig>()
                .eq(ApprovalMaterialConfig::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(processKey), ApprovalMaterialConfig::getProcessKey, processKey)
                .eq(StringUtils.hasText(status), ApprovalMaterialConfig::getStatus, status)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(ApprovalMaterialConfig::getMaterialCode, keywordValue)
                            .or()
                            .like(ApprovalMaterialConfig::getMaterialName, keywordValue)
                            .or()
                            .like(ApprovalMaterialConfig::getMaterialType, keywordValue))
                .orderByAsc(ApprovalMaterialConfig::getProcessKey)
                .orderByAsc(ApprovalMaterialConfig::getSortOrder)
                .orderByAsc(ApprovalMaterialConfig::getId));
    return CsvExportSupport.csvResponse(
        "approval_material_configs",
        List.of("流程编码", "材料编码", "材料名称", "材料类型", "是否必填", "排序", "状态", "备注"),
        rows.stream()
            .map(
                row ->
                    List.of(
                        CsvExportSupport.value(row.getProcessKey()),
                        CsvExportSupport.value(row.getMaterialCode()),
                        CsvExportSupport.value(row.getMaterialName()),
                        CsvExportSupport.value(row.getMaterialType()),
                        row.getRequiredFlag() != null && row.getRequiredFlag() == 1 ? "是" : "否",
                        CsvExportSupport.value(row.getSortOrder()),
                        CsvExportSupport.value(row.getStatus()),
                        CsvExportSupport.value(row.getRemark())))
            .toList());
  }

  @GetMapping("/{id}")
  public ApiResult<ApprovalMaterialConfigDetailDto> get(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ApprovalMaterialConfig entity = requireEntity(id, user.getTenantId());
    return ApiResult.ok(toDetail(entity));
  }

  @PostMapping
  public ApiResult<String> create(
      @RequestBody ApprovalMaterialConfigCreateUpdateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(dto, user.getTenantId(), null);
    ApprovalMaterialConfig entity = new ApprovalMaterialConfig();
    entity.setTenantId(user.getTenantId());
    apply(dto, entity);
    entity.setStatus(defaultStatus(dto.getStatus()));
    approvalMaterialConfigMapper.insert(entity);
    return ApiResult.ok(String.valueOf(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id,
      @RequestBody ApprovalMaterialConfigCreateUpdateDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ApprovalMaterialConfig entity = requireEntity(id, user.getTenantId());
    validate(dto, user.getTenantId(), id);
    apply(dto, entity);
    if (StringUtils.hasText(dto.getStatus())) {
      entity.setStatus(defaultStatus(dto.getStatus()));
    }
    approvalMaterialConfigMapper.updateById(entity);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(
      @PathVariable Long id, @RequestBody StatusUpdateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ApprovalMaterialConfig entity = requireEntity(id, user.getTenantId());
    entity.setStatus(defaultStatus(dto != null ? dto.getStatus() : null));
    approvalMaterialConfigMapper.updateById(entity);
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    requireEntity(id, user.getTenantId());
    approvalMaterialConfigMapper.deleteById(id);
    return ApiResult.ok();
  }

  private void validate(
      ApprovalMaterialConfigCreateUpdateDto dto, Long tenantId, Long currentId) {
    if (dto == null
        || !StringUtils.hasText(dto.getProcessKey())
        || !StringUtils.hasText(dto.getMaterialCode())
        || !StringUtils.hasText(dto.getMaterialName())) {
      throw new BizException(400, "流程、材料编码、材料名称不能为空");
    }
    ApprovalMaterialConfig existing =
        approvalMaterialConfigMapper.selectOne(
            new LambdaQueryWrapper<ApprovalMaterialConfig>()
                .eq(ApprovalMaterialConfig::getTenantId, tenantId)
                .eq(ApprovalMaterialConfig::getProcessKey, dto.getProcessKey().trim())
                .eq(ApprovalMaterialConfig::getMaterialCode, dto.getMaterialCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "同流程下材料编码已存在");
    }
  }

  private void apply(
      ApprovalMaterialConfigCreateUpdateDto dto, ApprovalMaterialConfig entity) {
    entity.setProcessKey(dto.getProcessKey().trim());
    entity.setMaterialCode(dto.getMaterialCode().trim());
    entity.setMaterialName(dto.getMaterialName().trim());
    entity.setMaterialType(trimToNull(dto.getMaterialType()));
    entity.setRequiredFlag(Boolean.TRUE.equals(dto.getRequired()) ? 1 : 0);
    entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
    entity.setRemark(trimToNull(dto.getRemark()));
  }

  private ApprovalMaterialConfig requireEntity(Long id, Long tenantId) {
    ApprovalMaterialConfig entity = approvalMaterialConfigMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "审批材料配置不存在");
    }
    return entity;
  }

  private ApprovalMaterialConfigListItemDto toListItem(ApprovalMaterialConfig entity) {
    return new ApprovalMaterialConfigListItemDto(
        String.valueOf(entity.getId()),
        entity.getProcessKey(),
        entity.getMaterialCode(),
        entity.getMaterialName(),
        entity.getMaterialType(),
        entity.getRequiredFlag() != null && entity.getRequiredFlag() == 1,
        entity.getSortOrder(),
        defaultStatus(entity.getStatus()));
  }

  private ApprovalMaterialConfigDetailDto toDetail(ApprovalMaterialConfig entity) {
    return new ApprovalMaterialConfigDetailDto(
        String.valueOf(entity.getId()),
        entity.getProcessKey(),
        entity.getMaterialCode(),
        entity.getMaterialName(),
        entity.getMaterialType(),
        entity.getRequiredFlag() != null && entity.getRequiredFlag() == 1,
        entity.getSortOrder(),
        defaultStatus(entity.getStatus()),
        entity.getRemark());
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String defaultStatus(String status) {
    return StringUtils.hasText(status) ? status.trim().toUpperCase() : "ENABLED";
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
