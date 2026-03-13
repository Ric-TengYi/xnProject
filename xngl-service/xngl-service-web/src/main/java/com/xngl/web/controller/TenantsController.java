package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.Tenant;
import com.xngl.manager.tenant.TenantService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final TenantService tenantService;

  public TenantsController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping
  public ApiResult<PageResult<TenantListItemDto>> list(
      @RequestParam(required = false) String tenantName,
      @RequestParam(required = false) String tenantType,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    IPage<Tenant> page = tenantService.page(tenantName, tenantType, status, pageNo, pageSize);
    List<TenantListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<TenantDetailDto> get(@PathVariable Long id) {
    Tenant t = tenantService.getById(id);
    if (t == null) return ApiResult.fail(404, "租户不存在");
    return ApiResult.ok(toDetail(t));
  }

  @GetMapping("/{id}/summary")
  public ApiResult<TenantSummaryDto> summary(@PathVariable Long id) {
    com.xngl.manager.tenant.TenantSummaryVo vo = tenantService.getSummary(id);
    if (vo == null) return ApiResult.fail(404, "租户不存在");
    TenantSummaryDto dto =
        new TenantSummaryDto(
            String.valueOf(vo.getTenantId()),
            vo.getOrgCount(),
            vo.getUserCount(),
            vo.getRoleCount(),
            vo.getStatus());
    return ApiResult.ok(dto);
  }

  @PostMapping
  public ApiResult<String> create(@RequestBody TenantCreateUpdateDto dto) {
    Tenant t = new Tenant();
    mapToEntity(dto, t);
    t.setStatus("ENABLED");
    long id = tenantService.create(t);
    return ApiResult.ok(String.valueOf(id));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id, @RequestBody TenantCreateUpdateDto dto) {
    Tenant t = tenantService.getById(id);
    if (t == null) return ApiResult.fail(404, "租户不存在");
    mapToEntity(dto, t);
    t.setId(id);
    tenantService.update(t);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDto dto) {
    Tenant t = tenantService.getById(id);
    if (t == null) return ApiResult.fail(404, "租户不存在");
    tenantService.updateStatus(id, dto.getStatus());
    return ApiResult.ok();
  }

  private TenantListItemDto toListItem(Tenant t) {
    return new TenantListItemDto(
        String.valueOf(t.getId()),
        t.getTenantCode(),
        t.getTenantName(),
        t.getTenantType(),
        t.getStatus(),
        t.getContactName(),
        t.getContactMobile(),
        t.getExpireTime() != null ? t.getExpireTime().format(ISO) : null);
  }

  private TenantDetailDto toDetail(Tenant t) {
    return new TenantDetailDto(
        String.valueOf(t.getId()),
        t.getTenantCode(),
        t.getTenantName(),
        t.getTenantType(),
        t.getStatus(),
        t.getContactName(),
        t.getContactMobile(),
        t.getBusinessLicenseNo(),
        t.getAddress(),
        t.getRemark(),
        t.getExpireTime() != null ? t.getExpireTime().format(ISO) : null,
        t.getCreateTime() != null ? t.getCreateTime().format(ISO) : null,
        t.getUpdateTime() != null ? t.getUpdateTime().format(ISO) : null);
  }

  private void mapToEntity(TenantCreateUpdateDto dto, Tenant t) {
    t.setTenantCode(dto.getTenantCode());
    t.setTenantName(dto.getTenantName());
    t.setTenantType(dto.getTenantType());
    t.setContactName(dto.getContactName());
    t.setContactMobile(dto.getContactMobile());
    t.setBusinessLicenseNo(dto.getBusinessLicenseNo());
    t.setAddress(dto.getAddress());
    t.setRemark(dto.getRemark());
    if (dto.getExpireTime() != null && !dto.getExpireTime().isEmpty()) {
      t.setExpireTime(java.time.LocalDateTime.parse(dto.getExpireTime(), ISO));
    }
  }
}
