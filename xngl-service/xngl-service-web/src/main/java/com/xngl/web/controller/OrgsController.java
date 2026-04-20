package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.dict.entity.DataDict;
import com.xngl.manager.dict.mapper.DataDictMapper;
import com.xngl.manager.org.CreateOrgResult;
import com.xngl.manager.org.OrgService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.unit.UnitContractItemDto;
import com.xngl.web.dto.unit.UnitProjectStatDto;
import com.xngl.web.dto.unit.UnitSiteContractGroupDto;
import com.xngl.web.dto.user.OrgCreateResponseDto;
import com.xngl.web.dto.user.OrgCreateUpdateDto;
import com.xngl.web.dto.user.OrgDetailDto;
import com.xngl.web.dto.user.OrgTreeNodeDto;
import com.xngl.web.dto.user.StatusUpdateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
@RequestMapping("/api/orgs")
public class OrgsController {

  private final OrgService orgService;
  private final OrgMapper orgMapper;
  private final UserService userService;
  private final UserContext userContext;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final ContractMapper contractMapper;
  private final VehicleMapper vehicleMapper;
  private final DataDictMapper dataDictMapper;

  public OrgsController(
      OrgService orgService,
      OrgMapper orgMapper,
      UserService userService,
      UserContext userContext,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      ContractMapper contractMapper,
      VehicleMapper vehicleMapper,
      DataDictMapper dataDictMapper) {
    this.orgService = orgService;
    this.orgMapper = orgMapper;
    this.userService = userService;
    this.userContext = userContext;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.contractMapper = contractMapper;
    this.vehicleMapper = vehicleMapper;
    this.dataDictMapper = dataDictMapper;
  }

  @GetMapping("/tree")
  public ApiResult<List<OrgTreeNodeDto>> tree(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String orgType,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (currentUser.getTenantId() == null) return ApiResult.ok(Collections.emptyList());
    List<Org> list = orgService.listTree(currentUser.getTenantId(), keyword, status);
    if (StringUtils.hasText(orgType) && !"ALL".equalsIgnoreCase(orgType.trim())) {
      list = list.stream()
          .filter(o -> orgType.trim().equalsIgnoreCase(o.getOrgType()))
          .toList();
    }
    Map<String, String> typeLabelMap = loadOrgTypeLabelMap(currentUser.getTenantId());
    List<OrgTreeNodeDto> tree = buildTree(list, 0L, typeLabelMap);
    return ApiResult.ok(tree);
  }

  @GetMapping("/{id}")
  public ApiResult<OrgDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = orgService.getById(id);
    if (o == null || !Objects.equals(o.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "组织不存在");
    }
    Map<String, String> typeLabelMap = loadOrgTypeLabelMap(currentUser.getTenantId());
    OrgMetrics metrics = loadMetrics(currentUser.getTenantId(), List.of(o));
    return ApiResult.ok(toDetail(o, typeLabelMap, metrics));
  }

  @PostMapping
  public ApiResult<OrgCreateResponseDto> create(
      @RequestBody OrgCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = new Org();
    mapToEntity(dto, o);
    o.setParentId(
        dto.getParentId() != null && !dto.getParentId().isEmpty()
            ? Long.parseLong(dto.getParentId()) : 0L);

    CreateOrgResult result =
        orgService.createWithAdmin(currentUser.getTenantId(), currentUser.getId(), o);
    return ApiResult.ok(
        new OrgCreateResponseDto(
            String.valueOf(result.orgId()),
            String.valueOf(result.adminUserId()),
            result.adminUsername(),
            result.plainPassword()));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(
      @PathVariable Long id, @RequestBody OrgCreateUpdateDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = orgService.getById(id);
    if (o == null || !Objects.equals(o.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "组织不存在");
    }
    mapToEntity(dto, o);
    o.setId(id);
    if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
      o.setParentId(Long.parseLong(dto.getParentId()));
    }
    orgService.update(o);
    return ApiResult.ok();
  }

  @PutMapping("/{id}/leader")
  public ApiResult<Void> updateLeader(
      @PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = orgService.getById(id);
    if (o == null || !Objects.equals(o.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "组织不存在");
    }
    String leaderUserId = body.get("leaderUserId");
    if (leaderUserId == null || leaderUserId.isEmpty()) return ApiResult.fail(400, "leaderUserId 必填");
    orgService.updateLeader(id, Long.parseLong(leaderUserId));
    return ApiResult.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResult<Void> updateStatus(
      @PathVariable Long id, @RequestBody StatusUpdateDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = orgService.getById(id);
    if (o == null || !Objects.equals(o.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "组织不存在");
    }
    orgService.updateStatus(id, dto.getStatus());
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org o = orgService.getById(id);
    if (o == null || !Objects.equals(o.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "组织不存在");
    }
    orgService.delete(id);
    return ApiResult.ok();
  }

  // ==================== 业务统计接口（从 UnitsController 迁移）====================

  @GetMapping("/{id}/projects")
  public ApiResult<List<UnitProjectStatDto>> listProjects(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureOrgBelongsToTenant(id, currentUser.getTenantId());
    List<Contract> contracts = listContractsByOrg(currentUser.getTenantId(), id);
    if (contracts.isEmpty()) {
      return ApiResult.ok(Collections.emptyList());
    }
    Map<Long, Project> projectMap =
        projectMapper.selectBatchIds(
                contracts.stream()
                    .map(Contract::getProjectId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(Project::getId, p -> p, (l, r) -> l));

    Map<Long, UnitProjectStatDto> result = new LinkedHashMap<>();
    for (Contract contract : contracts) {
      if (contract.getProjectId() == null) continue;
      Project project = projectMap.get(contract.getProjectId());
      UnitProjectStatDto dto =
          result.computeIfAbsent(
              contract.getProjectId(),
              key -> {
                UnitProjectStatDto item = new UnitProjectStatDto();
                item.setProjectId(String.valueOf(key));
                item.setProjectName(project != null ? project.getName() : "项目#" + key);
                item.setProjectCode(project != null ? project.getCode() : null);
                item.setContractCount(0L);
                item.setContractAmount(BigDecimal.ZERO);
                item.setAgreedVolume(BigDecimal.ZERO);
                return item;
              });
      dto.setContractCount(dto.getContractCount() + 1);
      dto.setContractAmount(sum(dto.getContractAmount(), contract.getContractAmount()));
      dto.setAgreedVolume(sum(dto.getAgreedVolume(), contract.getAgreedVolume()));
    }
    return ApiResult.ok(new ArrayList<>(result.values()));
  }

  @GetMapping("/{id}/contract-groups")
  public ApiResult<List<UnitSiteContractGroupDto>> listContractGroups(
      @PathVariable Long id,
      @RequestParam(required = false) Long projectId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureOrgBelongsToTenant(id, currentUser.getTenantId());
    List<Contract> contracts = listContractsByOrg(currentUser.getTenantId(), id);
    if (projectId != null) {
      contracts = contracts.stream()
          .filter(c -> Objects.equals(c.getProjectId(), projectId))
          .toList();
    }
    if (contracts.isEmpty()) return ApiResult.ok(Collections.emptyList());

    Map<Long, Site> siteMap =
        siteMapper.selectBatchIds(
                contracts.stream()
                    .map(Contract::getSiteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(Site::getId, s -> s, (l, r) -> l));

    Map<String, UnitSiteContractGroupDto> groups = new LinkedHashMap<>();
    for (Contract contract : contracts) {
      String groupKey = contract.getSiteId() != null ? String.valueOf(contract.getSiteId()) : "UNKNOWN";
      Site site = contract.getSiteId() != null ? siteMap.get(contract.getSiteId()) : null;
      UnitSiteContractGroupDto group =
          groups.computeIfAbsent(groupKey, key -> {
            UnitSiteContractGroupDto item = new UnitSiteContractGroupDto();
            item.setSiteId("UNKNOWN".equals(key) ? null : key);
            item.setSiteName(site != null ? site.getName() : "未配置场地");
            item.setContractCount(0L);
            item.setContractAmount(BigDecimal.ZERO);
            item.setAgreedVolume(BigDecimal.ZERO);
            item.setReceivedAmount(BigDecimal.ZERO);
            item.setContracts(new ArrayList<>());
            return item;
          });
      group.setContractCount(group.getContractCount() + 1);
      group.setContractAmount(sum(group.getContractAmount(), contract.getContractAmount()));
      group.setAgreedVolume(sum(group.getAgreedVolume(), contract.getAgreedVolume()));
      group.setReceivedAmount(sum(group.getReceivedAmount(), contract.getReceivedAmount()));

      UnitContractItemDto ci = new UnitContractItemDto();
      ci.setId(contract.getId() != null ? String.valueOf(contract.getId()) : null);
      ci.setContractNo(contract.getContractNo());
      ci.setName(contract.getName());
      ci.setContractType(contract.getContractType());
      ci.setContractStatus(contract.getContractStatus());
      ci.setSourceType(contract.getSourceType());
      ci.setSiteId(contract.getSiteId() != null ? String.valueOf(contract.getSiteId()) : null);
      ci.setSiteName(group.getSiteName());
      ci.setContractAmount(contract.getContractAmount());
      ci.setReceivedAmount(contract.getReceivedAmount());
      ci.setAgreedVolume(contract.getAgreedVolume());
      group.getContracts().add(ci);
    }
    return ApiResult.ok(new ArrayList<>(groups.values()));
  }

  @GetMapping("/{id}/users")
  public ApiResult<List<Map<String, Object>>> listUsers(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureOrgBelongsToTenant(id, currentUser.getTenantId());
    var page = userService.page(currentUser.getTenantId(), null, id, null, 1, 200);
    List<Map<String, Object>> users = page.getRecords().stream().map(u -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", String.valueOf(u.getId()));
      m.put("username", u.getUsername());
      m.put("name", u.getName());
      m.put("userType", u.getUserType());
      m.put("status", u.getStatus());
      return m;
    }).toList();
    return ApiResult.ok(users);
  }

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Org> all = orgService.listByTenantId(currentUser.getTenantId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("totalOrgs", all.size());
    result.put("departments", all.stream().filter(o -> "DEPARTMENT".equals(o.getOrgType())).count());
    result.put("constructionUnits", all.stream().filter(o -> "CONSTRUCTION_UNIT".equals(o.getOrgType())).count());
    result.put("builderUnits", all.stream().filter(o -> "BUILDER_UNIT".equals(o.getOrgType())).count());
    result.put("transportUnits", all.stream().filter(o -> "TRANSPORT_COMPANY".equals(o.getOrgType())).count());
    return ApiResult.ok(result);
  }

  // ==================== private helpers ====================

  private void ensureOrgBelongsToTenant(Long orgId, Long tenantId) {
    Org org = orgService.getById(orgId);
    if (org == null || !Objects.equals(org.getTenantId(), tenantId)) {
      throw new BizException(404, "组织不存在");
    }
  }

  private List<Contract> listContractsByOrg(Long tenantId, Long orgId) {
    return contractMapper.selectList(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(w -> w
                .eq(Contract::getConstructionOrgId, orgId)
                .or().eq(Contract::getTransportOrgId, orgId)
                .or().eq(Contract::getSiteOperatorOrgId, orgId)
                .or().eq(Contract::getPartyId, orgId))
            .orderByDesc(Contract::getUpdateTime)
            .orderByDesc(Contract::getId));
  }

  private List<OrgTreeNodeDto> buildTree(List<Org> list, Long parentId, Map<String, String> typeLabelMap) {
    return list.stream()
        .filter(o -> Objects.equals(o.getParentId(), parentId))
        .map(o -> {
          OrgTreeNodeDto node = new OrgTreeNodeDto();
          node.setId(String.valueOf(o.getId()));
          node.setOrgCode(o.getOrgCode());
          node.setOrgName(o.getOrgName());
          node.setParentId(String.valueOf(o.getParentId()));
          node.setOrgType(o.getOrgType());
          node.setOrgTypeLabel(typeLabelMap.getOrDefault(o.getOrgType(), o.getOrgType()));
          node.setLeaderUserId(o.getLeaderUserId() != null ? String.valueOf(o.getLeaderUserId()) : null);
          node.setLeaderName(o.getLeaderNameCache());
          node.setStatus(o.getStatus());
          List<OrgTreeNodeDto> children = buildTree(list, o.getId(), typeLabelMap);
          node.setChildrenCount(children.size());
          node.setChildren(children);
          return node;
        })
        .collect(Collectors.toList());
  }

  private OrgDetailDto toDetail(Org o, Map<String, String> typeLabelMap, OrgMetrics metrics) {
    OrgDetailDto dto = new OrgDetailDto();
    dto.setId(String.valueOf(o.getId()));
    dto.setOrgCode(o.getOrgCode());
    dto.setOrgName(o.getOrgName());
    dto.setParentId(String.valueOf(o.getParentId()));
    dto.setOrgType(o.getOrgType());
    dto.setOrgTypeLabel(typeLabelMap.getOrDefault(o.getOrgType(), o.getOrgType()));
    dto.setOrgPath(o.getOrgPath());
    dto.setLeaderUserId(o.getLeaderUserId() != null ? String.valueOf(o.getLeaderUserId()) : null);
    dto.setLeaderName(o.getLeaderNameCache());
    dto.setContactPerson(o.getContactPerson());
    dto.setContactPhone(o.getContactPhone());
    dto.setAddress(o.getAddress());
    dto.setUnifiedSocialCode(o.getUnifiedSocialCode());
    dto.setRemark(o.getRemark());
    dto.setSortOrder(o.getSortOrder());
    dto.setStatus(o.getStatus());
    Long orgId = o.getId();
    dto.setProjectCount(metrics.projectCountMap.getOrDefault(orgId, 0L));
    dto.setContractCount(metrics.contractCountMap.getOrDefault(orgId, 0L));
    dto.setVehicleCount(metrics.vehicleCountMap.getOrDefault(orgId, 0L));
    dto.setActiveVehicleCount(metrics.activeVehicleCountMap.getOrDefault(orgId, 0L));
    dto.setUserCount(metrics.userCountMap.getOrDefault(orgId, 0L));
    return dto;
  }

  private void mapToEntity(OrgCreateUpdateDto dto, Org o) {
    if (dto.getTenantId() != null && !dto.getTenantId().isEmpty()) {
      o.setTenantId(Long.parseLong(dto.getTenantId()));
    }
    o.setOrgCode(dto.getOrgCode());
    o.setOrgName(dto.getOrgName());
    o.setOrgType(dto.getOrgType() != null ? dto.getOrgType() : "DEPARTMENT");
    if (dto.getLeaderUserId() != null && !dto.getLeaderUserId().isEmpty()) {
      o.setLeaderUserId(Long.parseLong(dto.getLeaderUserId()));
    }
    o.setContactPerson(dto.getContactPerson());
    o.setContactPhone(dto.getContactPhone());
    o.setAddress(dto.getAddress());
    o.setUnifiedSocialCode(dto.getUnifiedSocialCode());
    o.setRemark(dto.getRemark());
    if (dto.getSortOrder() != null) o.setSortOrder(dto.getSortOrder());
    if (StringUtils.hasText(dto.getStatus())) o.setStatus(dto.getStatus());
  }

  private Map<String, String> loadOrgTypeLabelMap(Long tenantId) {
    List<DataDict> dicts = dataDictMapper.selectList(
        new LambdaQueryWrapper<DataDict>()
            .eq(DataDict::getTenantId, tenantId)
            .eq(DataDict::getDictType, "ORG_TYPE")
            .eq(DataDict::getStatus, "ENABLED"));
    Map<String, String> map = new HashMap<>();
    for (DataDict dict : dicts) {
      map.put(dict.getDictCode(), dict.getDictLabel());
    }
    return map;
  }

  private OrgMetrics loadMetrics(Long tenantId, List<Org> orgs) {
    OrgMetrics metrics = new OrgMetrics();
    LinkedHashSet<Long> orgIds = orgs.stream()
        .map(Org::getId).filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) return metrics;

    // Projects
    List<Project> projects =
        projectMapper.selectList(new LambdaQueryWrapper<Project>().in(Project::getOrgId, orgIds));
    for (Project p : projects) {
      if (p.getOrgId() != null) metrics.projectCountMap.merge(p.getOrgId(), 1L, Long::sum);
    }

    // Contracts
    List<Contract> contracts =
        contractMapper.selectList(new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    for (Contract c : contracts) {
      for (Long relOrgId : new Long[]{
          c.getConstructionOrgId(), c.getTransportOrgId(), c.getSiteOperatorOrgId()}) {
        if (relOrgId != null && orgIds.contains(relOrgId)) {
          metrics.contractCountMap.merge(relOrgId, 1L, Long::sum);
        }
      }
    }

    // Vehicles
    List<Vehicle> vehicles = vehicleMapper.selectList(
        new LambdaQueryWrapper<Vehicle>()
            .eq(Vehicle::getTenantId, tenantId)
            .in(Vehicle::getOrgId, orgIds));
    for (Vehicle v : vehicles) {
      if (v.getOrgId() == null) continue;
      metrics.vehicleCountMap.merge(v.getOrgId(), 1L, Long::sum);
      if (v.getStatus() != null && v.getStatus() == 1) {
        metrics.activeVehicleCountMap.merge(v.getOrgId(), 1L, Long::sum);
      }
    }

    // Users
    for (Long orgId : orgIds) {
      var page = userService.page(tenantId, null, orgId, null, 1, 1);
      metrics.userCountMap.put(orgId, page.getTotal());
    }

    return metrics;
  }

  private BigDecimal sum(BigDecimal left, BigDecimal right) {
    return (left != null ? left : BigDecimal.ZERO).add(right != null ? right : BigDecimal.ZERO);
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private static class OrgMetrics {
    final Map<Long, Long> projectCountMap = new HashMap<>();
    final Map<Long, Long> contractCountMap = new HashMap<>();
    final Map<Long, Long> vehicleCountMap = new HashMap<>();
    final Map<Long, Long> activeVehicleCountMap = new HashMap<>();
    final Map<Long, Long> userCountMap = new HashMap<>();
  }
}
