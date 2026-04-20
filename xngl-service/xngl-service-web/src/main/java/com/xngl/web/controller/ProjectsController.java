package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.ProjectConfig;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectConfigMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.manager.project.ProjectPaymentService;
import com.xngl.manager.project.ProjectPaymentSummaryVo;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.project.ProjectConfigDto;
import com.xngl.web.dto.project.ProjectConfigUpdateRequestDto;
import com.xngl.web.dto.project.ProjectContractSummaryDto;
import com.xngl.web.dto.project.ProjectDetailDto;
import com.xngl.web.dto.project.ProjectListItemDto;
import com.xngl.web.dto.project.ProjectPermitSummaryDto;
import com.xngl.web.dto.project.ProjectSiteSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.MasterDataAccessScope;
import com.xngl.web.support.MasterDataAccessScopeResolver;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
@RequestMapping("/api/projects")
public class ProjectsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ProjectMapper projectMapper;
  private final OrgMapper orgMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final SiteMapper siteMapper;
  private final DisposalPermitMapper disposalPermitMapper;
  private final ProjectConfigMapper projectConfigMapper;
  private final AlertFenceMapper alertFenceMapper;
  private final ProjectPaymentService projectPaymentService;
  private final UserContext userContext;
  private final MasterDataAccessScopeResolver accessScopeResolver;

  public ProjectsController(
      ProjectMapper projectMapper,
      OrgMapper orgMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      SiteMapper siteMapper,
      DisposalPermitMapper disposalPermitMapper,
      ProjectConfigMapper projectConfigMapper,
      AlertFenceMapper alertFenceMapper,
      ProjectPaymentService projectPaymentService,
      UserContext userContext,
      MasterDataAccessScopeResolver accessScopeResolver) {
    this.projectMapper = projectMapper;
    this.orgMapper = orgMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.siteMapper = siteMapper;
    this.disposalPermitMapper = disposalPermitMapper;
    this.projectConfigMapper = projectConfigMapper;
    this.alertFenceMapper = alertFenceMapper;
    this.projectPaymentService = projectPaymentService;
    this.userContext = userContext;
    this.accessScopeResolver = accessScopeResolver;
  }

  @GetMapping
  public ApiResult<PageResult<ProjectListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MasterDataAccessScope scope =
        accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    if (!scope.hasAnyAccess()) {
      return ApiResult.ok(new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList()));
    }

    LambdaQueryWrapper<Project> query = new LambdaQueryWrapper<>();
    if (!scope.isTenantWideAccess()) {
      query.and(
          wrapper -> {
            boolean hasCondition = false;
            if (!scope.getOrgIds().isEmpty()) {
              wrapper.in(Project::getOrgId, scope.getOrgIds());
              hasCondition = true;
            }
            if (!scope.getProjectIds().isEmpty()) {
              if (hasCondition) {
                wrapper.or().in(Project::getId, scope.getProjectIds());
              } else {
                wrapper.in(Project::getId, scope.getProjectIds());
              }
              hasCondition = true;
            }
            if (!hasCondition) {
              wrapper.apply("1 = 0");
            }
          });
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(Project::getName, effectiveKeyword)
                  .or()
                  .like(Project::getCode, effectiveKeyword)
                  .or()
                  .like(Project::getAddress, effectiveKeyword));
    }
    if (status != null) {
      query.eq(Project::getStatus, status);
    }
    query.orderByDesc(Project::getUpdateTime).orderByDesc(Project::getId);

    IPage<Project> page = projectMapper.selectPage(new Page<>(pageNo, pageSize), query);
    List<Project> visibleProjects =
        page.getRecords().stream().filter(project -> canAccessProject(scope, project)).toList();
    Map<Long, Org> orgMap = loadOrgMap(visibleProjects);
    List<ProjectListItemDto> records =
        visibleProjects.stream()
            .map(project -> toListItem(project, orgMap.get(project.getOrgId()), currentUser.getTenantId()))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<ProjectDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MasterDataAccessScope scope =
        accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    Project project = projectMapper.selectById(id);
    Org org = project != null && project.getOrgId() != null ? orgMapper.selectById(project.getOrgId()) : null;
    if (project == null || !canAccessProject(scope, project) || !belongsToTenant(project, org, currentUser.getTenantId())) {
      return ApiResult.fail(404, "项目不存在");
    }
    return ApiResult.ok(toDetail(project, org, currentUser.getTenantId()));
  }

  @PutMapping("/{id}/config")
  public ApiResult<ProjectConfigDto> updateConfig(
      @PathVariable Long id,
      @RequestBody ProjectConfigUpdateRequestDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MasterDataAccessScope scope =
        accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT);
    Project project = projectMapper.selectById(id);
    Org org = project != null && project.getOrgId() != null ? orgMapper.selectById(project.getOrgId()) : null;
    if (project == null || !canAccessProject(scope, project) || !belongsToTenant(project, org, currentUser.getTenantId())) {
      return ApiResult.fail(404, "项目不存在");
    }

    validateProjectConfig(body, currentUser.getTenantId());
    ProjectConfig entity = loadProjectConfigEntity(currentUser.getTenantId(), id);
    if (entity == null) {
      entity = new ProjectConfig();
      entity.setTenantId(currentUser.getTenantId());
      entity.setProjectId(id);
    }
    applyProjectConfig(entity, body);
    if (entity.getId() == null) {
      projectConfigMapper.insert(entity);
    } else {
      projectConfigMapper.updateById(entity);
    }
    return ApiResult.ok(loadProjectConfig(currentUser.getTenantId(), id));
  }

  @PostMapping
  public ApiResult<?> create(@RequestBody Object body) {
    return ApiResult.ok(null);
  }

  private Map<Long, Org> loadOrgMap(List<Project> projects) {
    LinkedHashSet<Long> orgIds =
        projects.stream()
            .map(Project::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private ProjectListItemDto toListItem(Project project, Org org, Long tenantId) {
    ProjectPaymentSummaryVo paymentSummary = projectPaymentService.getSummary(tenantId, project.getId());
    return new ProjectListItemDto(
        stringValue(project.getId()),
        project.getCode(),
        project.getName(),
        project.getAddress(),
        project.getStatus(),
        resolveProjectStatusLabel(project.getStatus()),
        stringValue(project.getOrgId()),
        org != null ? org.getOrgName() : null,
        countContracts(tenantId, project.getId()),
        countSites(project.getId()),
        paymentSummary.getTotalAmount(),
        paymentSummary.getPaidAmount(),
        paymentSummary.getDebtAmount(),
        formatDate(paymentSummary.getLastPaymentDate()),
        paymentSummary.getStatus(),
        formatDateTime(project.getCreateTime()),
        formatDateTime(project.getUpdateTime()));
  }

  private ProjectDetailDto toDetail(Project project, Org org, Long tenantId) {
    ProjectListItemDto item = toListItem(project, org, tenantId);
    List<ProjectContractSummaryDto> contractDetails = loadContractDetails(tenantId, project.getId());
    List<ProjectSiteSummaryDto> siteDetails = summarizeSites(contractDetails);
    ProjectDetailDto dto = new ProjectDetailDto();
    dto.setId(item.getId());
    dto.setCode(item.getCode());
    dto.setName(item.getName());
    dto.setAddress(item.getAddress());
    dto.setStatus(item.getStatus());
    dto.setStatusLabel(item.getStatusLabel());
    dto.setOrgId(item.getOrgId());
    dto.setOrgName(item.getOrgName());
    dto.setContractCount(item.getContractCount());
    dto.setSiteCount(item.getSiteCount());
    dto.setTotalAmount(item.getTotalAmount());
    dto.setPaidAmount(item.getPaidAmount());
    dto.setDebtAmount(item.getDebtAmount());
    dto.setLastPaymentDate(item.getLastPaymentDate());
    dto.setPaymentStatus(item.getPaymentStatus());
    dto.setCreateTime(item.getCreateTime());
    dto.setUpdateTime(item.getUpdateTime());
    dto.setPaymentStatusLabel(resolvePaymentStatusLabel(item.getPaymentStatus()));
    dto.setContractDetails(contractDetails);
    dto.setSiteDetails(siteDetails);
    dto.setPermits(loadProjectPermits(tenantId, project.getId()));
    dto.setConfig(loadProjectConfig(tenantId, project.getId()));
    return dto;
  }

  private List<ProjectPermitSummaryDto> loadProjectPermits(Long tenantId, Long projectId) {
    List<DisposalPermit> permits =
        disposalPermitMapper.selectList(
            new LambdaQueryWrapper<DisposalPermit>()
                .eq(DisposalPermit::getTenantId, tenantId)
                .eq(DisposalPermit::getProjectId, projectId)
                .orderByDesc(DisposalPermit::getLastSyncTime)
                .orderByDesc(DisposalPermit::getUpdateTime)
                .orderByDesc(DisposalPermit::getId));
    if (permits.isEmpty()) {
      return Collections.emptyList();
    }

    LinkedHashSet<Long> contractIds =
        permits.stream()
            .map(DisposalPermit::getContractId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Contract> contractMap =
        contractIds.isEmpty()
            ? Collections.emptyMap()
            : contractMapper.selectBatchIds(contractIds).stream()
                .filter(contract -> contract.getId() != null)
                .collect(Collectors.toMap(Contract::getId, Function.identity(), (left, right) -> left));

    LinkedHashSet<Long> siteIds = new LinkedHashSet<>();
    permits.stream().map(DisposalPermit::getSiteId).filter(Objects::nonNull).forEach(siteIds::add);
    contractMap.values().stream().map(Contract::getSiteId).filter(Objects::nonNull).forEach(siteIds::add);
    Map<Long, Site> siteMap =
        siteIds.isEmpty()
            ? Collections.emptyMap()
            : siteMapper.selectBatchIds(siteIds).stream()
                .filter(site -> site.getId() != null)
                .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));

    return permits.stream()
        .map(
            permit -> {
              Contract contract = permit.getContractId() != null ? contractMap.get(permit.getContractId()) : null;
              Site site =
                  permit.getSiteId() != null
                      ? siteMap.get(permit.getSiteId())
                      : (contract != null ? siteMap.get(contract.getSiteId()) : null);
              return new ProjectPermitSummaryDto(
                  stringValue(permit.getId()),
                  permit.getPermitNo(),
                  permit.getPermitType(),
                  permit.getStatus(),
                  permit.getBindStatus(),
                  permit.getVehicleNo(),
                  permit.getSourcePlatform(),
                  permit.getSyncBatchNo(),
                  formatDate(permit.getIssueDate()),
                  formatDate(permit.getExpireDate()),
                  stringValue(permit.getContractId()),
                  contract != null ? contract.getContractNo() : null,
                  contract != null ? contract.getName() : null,
                  stringValue(site != null ? site.getId() : permit.getSiteId()),
                  site != null ? site.getName() : null);
            })
        .toList();
  }

  private List<ProjectContractSummaryDto> loadContractDetails(Long tenantId, Long projectId) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .eq(Contract::getProjectId, projectId)
                .orderByDesc(Contract::getUpdateTime)
                .orderByDesc(Contract::getId));
    if (contracts.isEmpty()) {
      return Collections.emptyList();
    }

    LinkedHashSet<Long> siteIds =
        contracts.stream()
            .map(Contract::getSiteId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Site> siteMap =
        siteIds.isEmpty()
            ? Collections.emptyMap()
            : siteMapper.selectBatchIds(siteIds).stream()
                .filter(site -> site.getId() != null)
                .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));

    LinkedHashSet<Long> contractIds =
        contracts.stream()
            .map(Contract::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, BigDecimal> disposedVolumeMap = new LinkedHashMap<>();
    if (!contractIds.isEmpty()) {
      contractTicketMapper.selectList(
              new LambdaQueryWrapper<ContractTicket>()
                  .in(ContractTicket::getContractId, contractIds)
                  .notIn(ContractTicket::getStatus, List.of("VOID", "CANCELLED")))
          .forEach(
              ticket ->
                  disposedVolumeMap.merge(
                      ticket.getContractId(),
                      defaultDecimal(ticket.getVolume()),
                      BigDecimal::add));
    }

    return contracts.stream()
        .map(
            contract -> {
              Site site = siteMap.get(contract.getSiteId());
              BigDecimal agreedVolume = defaultDecimal(contract.getAgreedVolume());
              BigDecimal disposedVolume = defaultDecimal(disposedVolumeMap.get(contract.getId()));
              BigDecimal remainingVolume = agreedVolume.subtract(disposedVolume);
              if (remainingVolume.signum() < 0) {
                remainingVolume = BigDecimal.ZERO;
              }
              return new ProjectContractSummaryDto(
                  stringValue(contract.getId()),
                  contract.getContractNo(),
                  contract.getName(),
                  stringValue(contract.getSiteId()),
                  site != null ? site.getName() : null,
                  site != null ? site.getSiteType() : null,
                  agreedVolume,
                  disposedVolume,
                  remainingVolume,
                  contract.getUnitPrice(),
                  contract.getContractAmount(),
                  contract.getContractStatus(),
                  contract.getApprovalStatus(),
                  formatDate(contract.getExpireDate()));
            })
        .toList();
  }

  private List<ProjectSiteSummaryDto> summarizeSites(List<ProjectContractSummaryDto> contractDetails) {
    if (contractDetails.isEmpty()) {
      return Collections.emptyList();
    }

    LinkedHashSet<Long> siteIds =
        contractDetails.stream()
            .map(ProjectContractSummaryDto::getSiteId)
            .filter(StringUtils::hasText)
            .map(Long::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Site> siteMap =
        siteIds.isEmpty()
            ? Collections.emptyMap()
            : siteMapper.selectBatchIds(siteIds).stream()
                .filter(site -> site.getId() != null)
                .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));

    Map<String, List<ProjectContractSummaryDto>> grouped =
        contractDetails.stream()
            .collect(
                Collectors.groupingBy(
                    item -> StringUtils.hasText(item.getSiteId()) ? item.getSiteId() : "UNASSIGNED",
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<ProjectSiteSummaryDto> records = new ArrayList<>();
    grouped.forEach(
        (siteId, items) -> {
          Site site = "UNASSIGNED".equals(siteId) ? null : siteMap.get(Long.valueOf(siteId));
          BigDecimal contractVolume =
              items.stream()
                  .map(ProjectContractSummaryDto::getAgreedVolume)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal disposedVolume =
              items.stream()
                  .map(ProjectContractSummaryDto::getDisposedVolume)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal remainingVolume =
              items.stream()
                  .map(ProjectContractSummaryDto::getRemainingVolume)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          records.add(
              new ProjectSiteSummaryDto(
                  "UNASSIGNED".equals(siteId) ? null : siteId,
                  site != null ? site.getName() : "未关联场地",
                  site != null ? site.getSiteType() : null,
                  site != null ? site.getCapacity() : BigDecimal.ZERO,
                  site != null ? site.getLng() : null,
                  site != null ? site.getLat() : null,
                  (long) items.size(),
                  contractVolume,
                  disposedVolume,
                  remainingVolume));
        });
    return records;
  }

  private ProjectConfigDto loadProjectConfig(Long tenantId, Long projectId) {
    ProjectConfig config = loadProjectConfigEntity(tenantId, projectId);
    AlertFence fence = null;
    if (config != null && StringUtils.hasText(config.getViolationFenceCode())) {
      fence =
          alertFenceMapper.selectOne(
              new LambdaQueryWrapper<AlertFence>()
                  .eq(AlertFence::getTenantId, tenantId)
                  .eq(AlertFence::getFenceCode, config.getViolationFenceCode())
                  .last("limit 1"));
    }
    return new ProjectConfigDto(
        config != null && Objects.equals(config.getCheckinEnabled(), 1),
        config != null ? config.getCheckinAccount() : null,
        config != null ? config.getCheckinAuthScope() : null,
        config != null && Objects.equals(config.getLocationCheckRequired(), 1),
        config != null ? config.getLocationRadiusMeters() : BigDecimal.ZERO,
        config != null ? config.getPreloadVolume() : BigDecimal.ZERO,
        config != null ? config.getRouteGeoJson() : null,
        config != null && Objects.equals(config.getViolationRuleEnabled(), 1),
        config != null ? config.getViolationFenceCode() : null,
        fence != null ? fence.getFenceName() : null,
        fence != null ? fence.getGeoJson() : null,
        config != null ? config.getRemark() : null);
  }

  private ProjectConfig loadProjectConfigEntity(Long tenantId, Long projectId) {
    return projectConfigMapper.selectOne(
        new LambdaQueryWrapper<ProjectConfig>()
            .eq(ProjectConfig::getTenantId, tenantId)
            .eq(ProjectConfig::getProjectId, projectId)
            .last("limit 1"));
  }

  private void validateProjectConfig(ProjectConfigUpdateRequestDto body, Long tenantId) {
    if (body == null) {
      throw new BizException(400, "请求体不能为空");
    }
    if (Boolean.TRUE.equals(body.getCheckinEnabled()) && !StringUtils.hasText(body.getCheckinAccount())) {
      throw new BizException(400, "打卡账号不能为空");
    }
    if (body.getLocationRadiusMeters() != null && body.getLocationRadiusMeters().signum() < 0) {
      throw new BizException(400, "位置判断半径不能小于 0");
    }
    if (body.getPreloadVolume() != null && body.getPreloadVolume().signum() < 0) {
      throw new BizException(400, "出土预扣值不能小于 0");
    }
    String fenceCode = trimToNull(body.getViolationFenceCode());
    if (Boolean.TRUE.equals(body.getViolationRuleEnabled()) && !StringUtils.hasText(fenceCode)) {
      throw new BizException(400, "违规围栏不能为空");
    }
    if (StringUtils.hasText(fenceCode)) {
      AlertFence fence =
          alertFenceMapper.selectOne(
              new LambdaQueryWrapper<AlertFence>()
                  .eq(AlertFence::getTenantId, tenantId)
                  .eq(AlertFence::getFenceCode, fenceCode)
                  .last("limit 1"));
      if (fence == null) {
        throw new BizException(400, "违规围栏不存在");
      }
    }
  }

  private void applyProjectConfig(ProjectConfig entity, ProjectConfigUpdateRequestDto body) {
    entity.setCheckinEnabled(Boolean.TRUE.equals(body.getCheckinEnabled()) ? 1 : 0);
    entity.setCheckinAccount(trimToNull(body.getCheckinAccount()));
    entity.setCheckinAuthScope(trimToNull(body.getCheckinAuthScope()));
    entity.setLocationCheckRequired(Boolean.TRUE.equals(body.getLocationCheckRequired()) ? 1 : 0);
    entity.setLocationRadiusMeters(defaultDecimal(body.getLocationRadiusMeters()));
    entity.setPreloadVolume(defaultDecimal(body.getPreloadVolume()));
    entity.setRouteGeoJson(trimToNull(body.getRouteGeoJson()));
    entity.setViolationRuleEnabled(Boolean.TRUE.equals(body.getViolationRuleEnabled()) ? 1 : 0);
    entity.setViolationFenceCode(trimToNull(body.getViolationFenceCode()));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private long countContracts(Long tenantId, Long projectId) {
    return contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .eq(Contract::getProjectId, projectId));
  }

  private long countSites(Long projectId) {
    return siteMapper.selectCount(
        new LambdaQueryWrapper<Site>().eq(Site::getProjectId, projectId));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private boolean canAccessProject(MasterDataAccessScope scope, Project project) {
    if (project == null) {
      return false;
    }
    return scope.hasProjectAccess(project.getId()) || scope.hasOrgAccess(project.getOrgId());
  }

  private boolean belongsToTenant(Project project, Org org, Long tenantId) {
    if (project == null || project.getOrgId() == null || tenantId == null) {
      return true;
    }
    return org != null && Objects.equals(org.getTenantId(), tenantId);
  }

  private String resolveProjectStatusLabel(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 0 -> "立项";
      case 1 -> "在建";
      case 2 -> "预警";
      case 3 -> "完工";
      default -> "状态" + status;
    };
  }

  private String resolvePaymentStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未结算";
    }
    return switch (status.trim().toUpperCase()) {
      case "SETTLED" -> "已结清";
      case "ARREARS" -> "欠款中";
      default -> status;
    };
  }

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }
}
