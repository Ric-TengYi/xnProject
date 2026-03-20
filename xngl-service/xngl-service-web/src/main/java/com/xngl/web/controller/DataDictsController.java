package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.dict.entity.DataDict;
import com.xngl.manager.dict.mapper.DataDictMapper;
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

  private final DataDictMapper mapper;
  private final UserService userService;

  public DataDictsController(DataDictMapper mapper, UserService userService) {
    this.mapper = mapper;
    this.userService = userService;
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

  @PostMapping
  public ApiResult<DataDict> create(@RequestBody DictUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
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
    if (body == null || !StringUtils.hasText(body.getDictType()) || !StringUtils.hasText(body.getDictCode())) {
      throw new BizException(400, "字典类型和编码不能为空");
    }
    if (!StringUtils.hasText(body.getDictLabel()) || !StringUtils.hasText(body.getDictValue())) {
      throw new BizException(400, "字典标签和值不能为空");
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
