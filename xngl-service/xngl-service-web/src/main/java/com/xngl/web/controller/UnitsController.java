package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.unit.UnitDetailDto;
import com.xngl.web.dto.unit.UnitListItemDto;
import com.xngl.web.dto.unit.UnitSummaryDto;
import com.xngl.web.dto.unit.UnitUpsertDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
@RequestMapping("/api/units")
public class UnitsController {

  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final Set<String> SUPPORTED_TYPES =
      Set.of("CONSTRUCTION_UNIT", "BUILDER_UNIT", "TRANSPORT_COMPANY");

  private final OrgMapper orgMapper;
  private final ProjectMapper projectMapper;
  private final ContractMapper contractMapper;
  private final VehicleMapper vehicleMapper;
  private final UserService userService;

  public UnitsController(
      OrgMapper orgMapper,
      ProjectMapper projectMapper,
      ContractMapper contractMapper,
      VehicleMapper vehicleMapper,
      UserService userService) {
    this.orgMapper = orgMapper;
    this.projectMapper = projectMapper;
    this.contractMapper = contractMapper;
    this.vehicleMapper = vehicleMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<UnitListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String unitType,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    LambdaQueryWrapper<Org> query =
        new LambdaQueryWrapper<Org>().eq(Org::getTenantId, currentUser.getTenantId());
    query.in(Org::getOrgType, SUPPORTED_TYPES);
    if (StringUtils.hasText(unitType) && !"ALL".equalsIgnoreCase(unitType.trim())) {
      query.eq(Org::getOrgType, unitType.trim().toUpperCase());
    }
    if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status.trim())) {
      query.eq(Org::getStatus, status.trim().toUpperCase());
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(Org::getOrgName, effectiveKeyword)
                  .or()
                  .like(Org::getOrgCode, effectiveKeyword)
                  .or()
                  .like(Org::getContactPerson, effectiveKeyword)
                  .or()
                  .like(Org::getContactPhone, effectiveKeyword)
                  .or()
                  .like(Org::getUnifiedSocialCode, effectiveKeyword));
    }
    query.orderByDesc(Org::getUpdateTime).orderByDesc(Org::getId);

    IPage<Org> page = orgMapper.selectPage(new Page<>(pageNo, pageSize), query);
    UnitMetrics metrics = loadMetrics(currentUser.getTenantId(), page.getRecords());
    List<UnitListItemDto> records =
        page.getRecords().stream().map(org -> toListItem(org, metrics)).toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/summary")
  public ApiResult<UnitSummaryDto> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Org> units =
        orgMapper.selectList(
            new LambdaQueryWrapper<Org>()
                .eq(Org::getTenantId, currentUser.getTenantId())
                .in(Org::getOrgType, SUPPORTED_TYPES));
    UnitSummaryDto dto = new UnitSummaryDto();
    dto.setTotalUnits(units.size());
    dto.setConstructionUnits(units.stream().filter(org -> "CONSTRUCTION_UNIT".equals(org.getOrgType())).count());
    dto.setBuilderUnits(units.stream().filter(org -> "BUILDER_UNIT".equals(org.getOrgType())).count());
    dto.setTransportUnits(units.stream().filter(org -> "TRANSPORT_COMPANY".equals(org.getOrgType())).count());
    LinkedHashSet<Long> unitIds =
        units.stream().map(Org::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (!unitIds.isEmpty()) {
      dto.setTotalVehicles(
          vehicleMapper.selectCount(
              new LambdaQueryWrapper<Vehicle>()
                  .eq(Vehicle::getTenantId, currentUser.getTenantId())
                  .in(Vehicle::getOrgId, unitIds)));
    }
    return ApiResult.ok(dto);
  }

  @GetMapping("/{id}")
  public ApiResult<UnitDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org org = orgMapper.selectById(id);
    if (org == null
        || !Objects.equals(org.getTenantId(), currentUser.getTenantId())
        || !SUPPORTED_TYPES.contains(org.getOrgType())) {
      return ApiResult.fail(404, "单位不存在");
    }
    UnitMetrics metrics = loadMetrics(currentUser.getTenantId(), List.of(org));
    return ApiResult.ok(toDetail(org, metrics));
  }

  @PostMapping
  public ApiResult<UnitDetailDto> create(
      @Valid @RequestBody UnitUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId(), null);
    Org org = new Org();
    org.setTenantId(currentUser.getTenantId());
    org.setParentId(1L);
    org.setSortOrder(99);
    org.setOrgPath("/1");
    applyUpsert(org, body);
    orgMapper.insert(org);
    return ApiResult.ok(toDetail(orgMapper.selectById(org.getId()), loadMetrics(currentUser.getTenantId(), List.of(org))));
  }

  @PutMapping("/{id}")
  public ApiResult<UnitDetailDto> update(
      @PathVariable Long id, @Valid @RequestBody UnitUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org org = orgMapper.selectById(id);
    if (org == null
        || !Objects.equals(org.getTenantId(), currentUser.getTenantId())
        || !SUPPORTED_TYPES.contains(org.getOrgType())) {
      return ApiResult.fail(404, "单位不存在");
    }
    validateUpsert(body, currentUser.getTenantId(), id);
    applyUpsert(org, body);
    orgMapper.updateById(org);
    return ApiResult.ok(toDetail(orgMapper.selectById(id), loadMetrics(currentUser.getTenantId(), List.of(org))));
  }

  private void validateUpsert(UnitUpsertDto body, Long tenantId, Long currentId) {
    String orgType = normalizeType(body.getOrgType());
    if (!SUPPORTED_TYPES.contains(orgType)) {
      throw new BizException(400, "单位类型不支持");
    }
    if (StringUtils.hasText(body.getOrgCode())) {
      Long count =
          orgMapper.selectCount(
              new LambdaQueryWrapper<Org>()
                  .eq(Org::getTenantId, tenantId)
                  .eq(Org::getOrgCode, body.getOrgCode().trim())
                  .ne(currentId != null, Org::getId, currentId));
      if (count != null && count > 0) {
        throw new BizException(400, "单位编码已存在");
      }
    }
  }

  private void applyUpsert(Org org, UnitUpsertDto body) {
    String orgType = normalizeType(body.getOrgType());
    org.setOrgType(orgType);
    org.setOrgName(body.getOrgName().trim());
    String orgCode =
        StringUtils.hasText(body.getOrgCode())
            ? body.getOrgCode().trim()
            : buildDefaultCode(orgType, body.getOrgName());
    org.setOrgCode(orgCode);
    org.setContactPerson(trimToNull(body.getContactPerson()));
    org.setContactPhone(trimToNull(body.getContactPhone()));
    org.setAddress(trimToNull(body.getAddress()));
    org.setUnifiedSocialCode(trimToNull(body.getUnifiedSocialCode()));
    org.setRemark(trimToNull(body.getRemark()));
    org.setStatus(StringUtils.hasText(body.getStatus()) ? body.getStatus().trim().toUpperCase() : "ENABLED");
    if (!StringUtils.hasText(org.getOrgPath())) {
      org.setOrgPath("/1");
    }
    if (org.getParentId() == null) {
      org.setParentId(1L);
    }
  }

  private String buildDefaultCode(String orgType, String orgName) {
    String typePrefix =
        switch (orgType) {
          case "CONSTRUCTION_UNIT" -> "ORG-CONSTRUCT";
          case "BUILDER_UNIT" -> "ORG-BUILDER";
          case "TRANSPORT_COMPANY" -> "ORG-TRANS";
          default -> "ORG-UNIT";
        };
    return typePrefix + "-" + Math.abs(Objects.hash(orgName, LocalDateTime.now().getNano()));
  }

  private String normalizeType(String rawType) {
    return rawType == null ? "" : rawType.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private UnitListItemDto toListItem(Org org, UnitMetrics metrics) {
    UnitListItemDto dto = new UnitListItemDto();
    fillCommon(dto, org, metrics);
    return dto;
  }

  private UnitDetailDto toDetail(Org org, UnitMetrics metrics) {
    UnitDetailDto dto = new UnitDetailDto();
    fillCommon(dto, org, metrics);
    dto.setRemark(org.getRemark());
    return dto;
  }

  private void fillCommon(UnitListItemDto dto, Org org, UnitMetrics metrics) {
    Long id = org.getId();
    dto.setId(id != null ? String.valueOf(id) : null);
    dto.setOrgCode(org.getOrgCode());
    dto.setOrgName(org.getOrgName());
    dto.setOrgType(org.getOrgType());
    dto.setOrgTypeLabel(resolveTypeLabel(org.getOrgType()));
    dto.setContactPerson(org.getContactPerson());
    dto.setContactPhone(org.getContactPhone());
    dto.setAddress(org.getAddress());
    dto.setUnifiedSocialCode(org.getUnifiedSocialCode());
    dto.setStatus(org.getStatus());
    dto.setStatusLabel(resolveStatusLabel(org.getStatus()));
    dto.setProjectCount(metrics.projectCountMap.getOrDefault(id, 0L));
    dto.setContractCount(metrics.contractCountMap.getOrDefault(id, 0L));
    dto.setVehicleCount(metrics.vehicleCountMap.getOrDefault(id, 0L));
    dto.setActiveVehicleCount(metrics.activeVehicleCountMap.getOrDefault(id, 0L));
    dto.setCreateTime(formatDateTime(org.getCreateTime()));
    dto.setUpdateTime(formatDateTime(org.getUpdateTime()));
  }

  private String resolveTypeLabel(String orgType) {
    if (!StringUtils.hasText(orgType)) {
      return "未知";
    }
    return switch (orgType.trim().toUpperCase()) {
      case "CONSTRUCTION_UNIT" -> "建设单位";
      case "BUILDER_UNIT" -> "施工单位";
      case "TRANSPORT_COMPANY" -> "运输单位";
      default -> orgType;
    };
  }

  private String resolveStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未知";
    }
    return "ENABLED".equalsIgnoreCase(status) ? "正常" : "停用";
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private UnitMetrics loadMetrics(Long tenantId, List<Org> orgs) {
    UnitMetrics metrics = new UnitMetrics();
    LinkedHashSet<Long> orgIds =
        orgs.stream().map(Org::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return metrics;
    }

    List<Project> projects =
        projectMapper.selectList(new LambdaQueryWrapper<Project>().in(Project::getOrgId, orgIds));
    for (Project project : projects) {
      if (project.getOrgId() != null) {
        metrics.projectCountMap.merge(project.getOrgId(), 1L, Long::sum);
      }
    }

    List<Contract> contracts =
        contractMapper.selectList(new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    Map<Long, LinkedHashSet<Long>> projectSetMap = new HashMap<>();
    for (Contract contract : contracts) {
      List<Long> relatedOrgIds = new ArrayList<>();
      if (contract.getConstructionOrgId() != null) {
        relatedOrgIds.add(contract.getConstructionOrgId());
      }
      if (contract.getTransportOrgId() != null) {
        relatedOrgIds.add(contract.getTransportOrgId());
      }
      if (contract.getSiteOperatorOrgId() != null) {
        relatedOrgIds.add(contract.getSiteOperatorOrgId());
      }
      for (Long relatedOrgId : relatedOrgIds) {
        if (!orgIds.contains(relatedOrgId)) {
          continue;
        }
        metrics.contractCountMap.merge(relatedOrgId, 1L, Long::sum);
        if (contract.getProjectId() != null) {
          projectSetMap.computeIfAbsent(relatedOrgId, key -> new LinkedHashSet<>()).add(contract.getProjectId());
        }
      }
    }
    for (Map.Entry<Long, LinkedHashSet<Long>> entry : projectSetMap.entrySet()) {
      metrics.projectCountMap.merge(entry.getKey(), (long) entry.getValue().size(), Long::sum);
    }

    List<Vehicle> vehicles =
        vehicleMapper.selectList(
            new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getTenantId, tenantId)
                .in(Vehicle::getOrgId, orgIds));
    for (Vehicle vehicle : vehicles) {
      if (vehicle.getOrgId() == null) {
        continue;
      }
      metrics.vehicleCountMap.merge(vehicle.getOrgId(), 1L, Long::sum);
      if (vehicle.getStatus() != null && vehicle.getStatus() == 1) {
        metrics.activeVehicleCountMap.merge(vehicle.getOrgId(), 1L, Long::sum);
      }
    }

    return metrics;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (!StringUtils.hasText(userId)) {
      throw new BizException(401, "未登录或 token 无效");
    }
    try {
      User user = userService.getById(Long.parseLong(userId));
      if (user == null) {
        throw new BizException(401, "用户不存在");
      }
      if (user.getTenantId() == null) {
        throw new BizException(403, "当前用户未绑定租户");
      }
      return user;
    } catch (NumberFormatException ex) {
      throw new BizException(401, "token 中的用户信息无效");
    }
  }

  private static class UnitMetrics {
    private final Map<Long, Long> projectCountMap = new HashMap<>();
    private final Map<Long, Long> contractCountMap = new HashMap<>();
    private final Map<Long, Long> vehicleCountMap = new HashMap<>();
    private final Map<Long, Long> activeVehicleCountMap = new HashMap<>();
  }
}
