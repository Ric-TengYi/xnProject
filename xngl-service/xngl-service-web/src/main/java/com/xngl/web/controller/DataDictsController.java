package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.dict.entity.DataDict;
import com.xngl.manager.dict.mapper.DataDictMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import com.xngl.web.support.CsvExportSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
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
@RequestMapping("/api/data-dicts")
public class DataDictsController {

  /** 平台预置的字典类型：typeCode → 中文名称。只有运维/超管可见此菜单。 */
  private static final Map<String, String> PREDEFINED_TYPES;
  static {
    PREDEFINED_TYPES = new LinkedHashMap<>();
    PREDEFINED_TYPES.put("ORG_TYPE", "组织类型");
    PREDEFINED_TYPES.put("alert_level", "预警级别");
    PREDEFINED_TYPES.put("event_type", "事件类型");
    PREDEFINED_TYPES.put("VEHICLE_MODEL", "车型分类");
    PREDEFINED_TYPES.put("contract_type", "合同类型");
    PREDEFINED_TYPES.put("settlement_type", "结算方式");
  }

  /** 自增序列用于生成 dictCode */
  private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

  private final DataDictMapper mapper;
  private final UserContext userContext;

  public DataDictsController(DataDictMapper mapper, UserContext userContext) {
    this.mapper = mapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<DataDict>> list(
      @RequestParam(required = false) String dictType,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String typeValue = trimToNull(dictType);
    String keywordValue = trimToNull(keyword);
    List<DataDict> rows =
        mapper.selectList(
            new LambdaQueryWrapper<DataDict>()
                .eq(DataDict::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(typeValue), DataDict::getDictType, typeValue)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(DataDict::getDictLabel, keywordValue)
                            .or()
                            .like(DataDict::getDictCode, keywordValue)
                            .or()
                            .like(DataDict::getDictValue, keywordValue))
                .orderByAsc(DataDict::getDictType)
                .orderByAsc(DataDict::getSort)
                .orderByDesc(DataDict::getId));
    rows.sort(
        Comparator.comparing(DataDict::getDictType, Comparator.nullsLast(String::compareTo))
            .thenComparing(DataDict::getSort, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(DataDict::getId, Comparator.nullsLast(Long::compareTo)));
    return ApiResult.ok(rows);
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String dictType,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<DataDict> rows =
        mapper.selectList(
            new LambdaQueryWrapper<DataDict>()
                .eq(DataDict::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(trimToNull(dictType)), DataDict::getDictType, trimToNull(dictType))
                .and(
                    StringUtils.hasText(trimToNull(keyword)),
                    wrapper ->
                        wrapper
                            .like(DataDict::getDictType, trimToNull(keyword))
                            .or()
                            .like(DataDict::getDictLabel, trimToNull(keyword))
                            .or()
                            .like(DataDict::getDictCode, trimToNull(keyword))
                            .or()
                            .like(DataDict::getDictValue, trimToNull(keyword)))
                .orderByAsc(DataDict::getDictType)
                .orderByAsc(DataDict::getSort)
                .orderByAsc(DataDict::getId));
    return CsvExportSupport.csvResponse(
        "data_dicts",
        List.of("字典类型", "字典编码", "字典标签", "字典值", "排序", "状态", "备注"),
        rows.stream()
            .map(
                row ->
                    List.of(
                        CsvExportSupport.value(row.getDictType()),
                        CsvExportSupport.value(row.getDictCode()),
                        CsvExportSupport.value(row.getDictLabel()),
                        CsvExportSupport.value(row.getDictValue()),
                        CsvExportSupport.value(row.getSort()),
                        CsvExportSupport.value(row.getStatus()),
                        CsvExportSupport.value(row.getRemark())))
            .toList());
  }

  @PostMapping
  public ApiResult<DataDict> create(@RequestBody DictUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    // dictCode 为空时自动生成
    if (!StringUtils.hasText(body.getDictCode())) {
      body.setDictCode(generateDictCode(body.getDictType()));
    }
    // dictValue 为空时自动生成（与 dictCode 相同）
    if (!StringUtils.hasText(body.getDictValue())) {
      body.setDictValue(body.getDictCode());
    }
    validate(body, user.getTenantId(), null);
    DataDict entity = new DataDict();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultStatus(body.getStatus()));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<DataDict> update(
      @PathVariable Long id, @RequestBody DictUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    DataDict entity = requireEntity(id, user.getTenantId());
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
    DataDict entity = requireEntity(id, user.getTenantId());
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

  private void validate(DictUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getDictType())) {
      throw new BizException(400, "字典类型不能为空");
    }
    if (!StringUtils.hasText(body.getDictLabel())) {
      throw new BizException(400, "字典标签不能为空");
    }
    // dictCode 在 create 阶段已自动生成，此处保证有值
    if (!StringUtils.hasText(body.getDictCode())) {
      throw new BizException(400, "字典编码不能为空");
    }
    DataDict existing =
        mapper.selectOne(
            new LambdaQueryWrapper<DataDict>()
                .eq(DataDict::getTenantId, tenantId)
                .eq(DataDict::getDictType, body.getDictType().trim())
                .eq(DataDict::getDictCode, body.getDictCode().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "同类型下字典编码已存在");
    }
  }

  /** 根据 dictType 前缀 + 自增序列生成唯一 dictCode */
  private String generateDictCode(String dictType) {
    String prefix = StringUtils.hasText(dictType) ? dictType.trim().toUpperCase() : "DICT";
    // 取前缀的前8个字符 + 5位序列号
    if (prefix.length() > 8) {
      prefix = prefix.substring(0, 8);
    }
    return prefix + "_" + String.format("%05d", CODE_SEQ.incrementAndGet() % 100000);
  }

  private void apply(DataDict entity, DictUpsertRequest body) {
    entity.setDictType(body.getDictType().trim());
    entity.setDictCode(body.getDictCode().trim());
    entity.setDictLabel(body.getDictLabel().trim());
    entity.setDictValue(body.getDictValue().trim());
    entity.setSort(body.getSort() != null ? body.getSort() : 0);
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private DataDict requireEntity(Long id, Long tenantId) {
    DataDict entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "字典项不存在");
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
  public static class DictUpsertRequest {
    private String dictType;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String status;
    private String remark;
  }
}
