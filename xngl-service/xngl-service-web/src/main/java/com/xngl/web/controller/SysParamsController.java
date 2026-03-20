package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.sysparam.entity.SysParam;
import com.xngl.manager.sysparam.mapper.SysParamMapper;
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
@RequestMapping("/api/sys-params")
public class SysParamsController {

  private final SysParamMapper mapper;
  private final UserService userService;

  public SysParamsController(SysParamMapper mapper, UserService userService) {
    this.mapper = mapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<SysParam>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String paramType,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    String typeValue = trimToNull(paramType);
    List<SysParam> rows =
        mapper.selectList(
            new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getTenantId, user.getTenantId())
                .eq(StringUtils.hasText(typeValue), SysParam::getParamType, typeValue)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(SysParam::getParamKey, keywordValue)
                            .or()
                            .like(SysParam::getParamName, keywordValue)
                            .or()
                            .like(SysParam::getRemark, keywordValue))
                .orderByAsc(SysParam::getParamType)
                .orderByAsc(SysParam::getParamKey));
    rows.sort(
        Comparator.comparing(SysParam::getParamType, Comparator.nullsLast(String::compareTo))
            .thenComparing(SysParam::getParamKey, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  @PostMapping
  public ApiResult<SysParam> create(@RequestBody SysParamUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    validate(body, user.getTenantId(), null);
    SysParam entity = new SysParam();
    entity.setTenantId(user.getTenantId());
    apply(entity, body);
    entity.setStatus(defaultStatus(body.getStatus()));
    mapper.insert(entity);
    return ApiResult.ok(mapper.selectById(entity.getId()));
  }

  @PutMapping("/{id}")
  public ApiResult<SysParam> update(
      @PathVariable Long id, @RequestBody SysParamUpsertRequest body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    SysParam entity = requireEntity(id, user.getTenantId());
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
    SysParam entity = requireEntity(id, user.getTenantId());
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

  private void validate(SysParamUpsertRequest body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getParamKey()) || !StringUtils.hasText(body.getParamName())) {
      throw new BizException(400, "参数键和值名称不能为空");
    }
    SysParam existing =
        mapper.selectOne(
            new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getTenantId, tenantId)
                .eq(SysParam::getParamKey, body.getParamKey().trim()));
    if (existing != null && !Objects.equals(existing.getId(), currentId)) {
      throw new BizException(400, "参数键已存在");
    }
  }

  private void apply(SysParam entity, SysParamUpsertRequest body) {
    entity.setParamKey(body.getParamKey().trim());
    entity.setParamName(body.getParamName().trim());
    entity.setParamValue(body.getParamValue());
    entity.setParamType(StringUtils.hasText(body.getParamType()) ? body.getParamType().trim().toUpperCase() : "STRING");
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private SysParam requireEntity(Long id, Long tenantId) {
    SysParam entity = mapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "系统参数不存在");
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
  public static class SysParamUpsertRequest {
    private String paramKey;
    private String paramName;
    private String paramValue;
    private String paramType;
    private String status;
    private String remark;
  }
}
