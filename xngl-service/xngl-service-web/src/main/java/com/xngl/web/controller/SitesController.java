package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import com.xngl.infrastructure.persistence.entity.site.SiteOperationConfig;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SiteOperationConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.site.DisposalListItemDto;
import com.xngl.web.dto.site.SiteCreateDto;
import com.xngl.web.dto.site.SiteDetailDto;
import com.xngl.web.dto.site.SiteDeviceDto;
import com.xngl.web.dto.site.SiteListItemDto;
import com.xngl.web.dto.site.SiteMapLayerDto;
import com.xngl.web.dto.site.SiteOperationConfigDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
public class SitesController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SiteService siteService;
  private final SiteMapper siteMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final ProjectMapper projectMapper;
  private final SiteDeviceMapper siteDeviceMapper;
  private final SiteOperationConfigMapper siteOperationConfigMapper;
  private final UserContext userContext;

  public SitesController(
      SiteService siteService,
      SiteMapper siteMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      ProjectMapper projectMapper,
      SiteDeviceMapper siteDeviceMapper,
      SiteOperationConfigMapper siteOperationConfigMapper,
      UserContext userContext) {
    this.siteService = siteService;
    this.siteMapper = siteMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.projectMapper = projectMapper;
    this.siteDeviceMapper = siteDeviceMapper;
    this.siteOperationConfigMapper = siteOperationConfigMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<List<SiteListItemDto>> list() {
    List<Site> sites = siteService.list();
    Map<Long, Site> siteMap =
        sites.stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    List<SiteListItemDto> records =
        sites.stream().map(site -> toListItem(site, siteMap)).collect(Collectors.toList());
    return ApiResult.ok(records);
  }

  @GetMapping("/{id}")
  public ApiResult<SiteDetailDto> get(@PathVariable Long id) {
    Site site = siteService.getById(id);
    if (site == null) return ApiResult.fail(404, "场地不存在");
    Map<Long, Site> siteMap =
        siteService.list().stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    return ApiResult.ok(toDetail(site, siteMap));
  }

  @GetMapping("/map-layers")
  public ApiResult<List<SiteMapLayerDto>> mapLayers() {
    List<Site> sites = siteService.list();
    Map<Long, List<SiteDevice>> deviceMap = loadDeviceMap(sites);
    List<SiteMapLayerDto> records =
        sites.stream()
            .map(
                site ->
                    new SiteMapLayerDto(
                        site.getId() != null ? String.valueOf(site.getId()) : null,
                        site.getName(),
                        site.getCode(),
                        site.getSiteType(),
                        site.getStatus(),
                        resolveLng(site),
                        resolveLat(site),
                        resolveBoundaryGeoJson(site),
                        toDeviceDtos(deviceMap.get(site.getId()), site)))
            .toList();
    return ApiResult.ok(records);
  }

  @GetMapping("/disposals")
  public ApiResult<PageResult<DisposalListItemDto>> listDisposals(
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    LinkedHashMap<Long, Site> siteMap =
        siteService.list().stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, site -> site, (left, right) -> left, LinkedHashMap::new));

    if (siteId != null && !siteMap.containsKey(siteId)) {
      return ApiResult.ok(new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList()));
    }

    LinkedHashSet<Long> availableSiteIds = new LinkedHashSet<>(siteMap.keySet());
    if (siteId != null) {
      availableSiteIds.clear();
      availableSiteIds.add(siteId);
    }
    if (availableSiteIds.isEmpty()) {
      return ApiResult.ok(new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList()));
    }

    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().in(Contract::getSiteId, availableSiteIds));
    if (contracts.isEmpty()) {
      return ApiResult.ok(new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList()));
    }

    Map<Long, Contract> contractMap =
        contracts.stream()
            .filter(contract -> contract.getId() != null)
            .collect(Collectors.toMap(Contract::getId, contract -> contract, (left, right) -> left));

    LinkedHashSet<Long> projectIds =
        contracts.stream()
            .map(Contract::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? Collections.emptyMap()
            : projectMapper.selectBatchIds(projectIds).stream()
                .filter(project -> project.getId() != null)
                .collect(Collectors.toMap(Project::getId, project -> project, (left, right) -> left));

    LambdaQueryWrapper<ContractTicket> query =
        new LambdaQueryWrapper<ContractTicket>()
            .in(ContractTicket::getContractId, contractMap.keySet())
            .orderByDesc(ContractTicket::getCreateTime)
            .orderByDesc(ContractTicket::getId);
    if (StringUtils.hasText(status)) {
      query.eq(ContractTicket::getStatus, normalizeStatusParam(status));
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      LinkedHashSet<Long> matchedContractIds =
          contracts.stream()
              .filter(
                  contract ->
                      contains(contract.getName(), effectiveKeyword)
                          || contains(contract.getContractNo(), effectiveKeyword)
                          || contains(contract.getCode(), effectiveKeyword)
                          || contains(siteMap.get(contract.getSiteId()) != null
                                  ? siteMap.get(contract.getSiteId()).getName()
                                  : null,
                              effectiveKeyword)
                          || contains(projectMap.get(contract.getProjectId()) != null
                                  ? projectMap.get(contract.getProjectId()).getName()
                                  : null,
                              effectiveKeyword))
              .map(Contract::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      query.and(
          wrapper -> {
            wrapper.like(ContractTicket::getTicketNo, effectiveKeyword);
            if (!matchedContractIds.isEmpty()) {
              wrapper.or().in(ContractTicket::getContractId, matchedContractIds);
            }
          });
    }

    IPage<ContractTicket> page =
        contractTicketMapper.selectPage(new Page<>(pageNo, pageSize), query);
    List<DisposalListItemDto> records =
        page.getRecords().stream()
            .map(ticket -> toDisposalItem(ticket, contractMap.get(ticket.getContractId()), siteMap, projectMap))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @PostMapping
  public ApiResult<SiteDetailDto> create(
      @RequestBody SiteCreateDto body, HttpServletRequest request) {
    requireCurrentUser(request);
    validateSiteCreate(body);

    Site site = new Site();
    site.setName(body.getName().trim());
    site.setCode(StringUtils.hasText(body.getCode()) ? body.getCode().trim() : generateSiteCode(body));
    site.setAddress(StringUtils.hasText(body.getAddress()) ? body.getAddress().trim() : null);
    site.setProjectId(body.getProjectId());
    site.setStatus(body.getStatus() != null ? body.getStatus() : 1);
    site.setOrgId(body.getOrgId());
    site.setSiteType(StringUtils.hasText(body.getSiteType()) ? body.getSiteType().trim() : "ENGINEERING");
    site.setCapacity(body.getCapacity() != null ? body.getCapacity() : BigDecimal.ZERO);
    site.setSettlementMode(
        StringUtils.hasText(body.getSettlementMode()) ? body.getSettlementMode().trim() : "UNIT_PRICE");
    site.setDisposalUnitPrice(
        body.getDisposalUnitPrice() != null ? body.getDisposalUnitPrice() : BigDecimal.ZERO);
    site.setDisposalFeeRate(
        body.getDisposalFeeRate() != null ? body.getDisposalFeeRate() : BigDecimal.ZERO);
    site.setServiceFeeUnitPrice(
        body.getServiceFeeUnitPrice() != null ? body.getServiceFeeUnitPrice() : BigDecimal.ZERO);
    site.setSiteLevel(normalizeSiteLevel(body.getSiteLevel()));
    site.setParentSiteId(body.getParentSiteId());
    site.setManagementArea(StringUtils.hasText(body.getManagementArea()) ? body.getManagementArea().trim() : null);
    site.setWeighbridgeSiteId(body.getWeighbridgeSiteId());
    site.setLng(body.getLng());
    site.setLat(body.getLat());
    site.setBoundaryGeoJson(StringUtils.hasText(body.getBoundaryGeoJson()) ? body.getBoundaryGeoJson().trim() : null);
    siteMapper.insert(site);

    Map<Long, Site> siteMap =
        siteService.list().stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    Site created = siteMapper.selectById(site.getId());
    if (created != null && created.getId() != null) {
      siteMap.put(created.getId(), created);
    }
    return ApiResult.ok(toDetail(created, siteMap));
  }

  private SiteListItemDto toListItem(Site s, Map<Long, Site> siteMap) {
    Site parentSite = s.getParentSiteId() != null ? siteMap.get(s.getParentSiteId()) : null;
    Site weighbridgeSite =
        s.getWeighbridgeSiteId() != null ? siteMap.get(s.getWeighbridgeSiteId()) : null;
    SiteListItemDto dto = new SiteListItemDto();
    dto.setId(s.getId() != null ? String.valueOf(s.getId()) : null);
    dto.setName(s.getName());
    dto.setCode(s.getCode());
    dto.setAddress(s.getAddress());
    dto.setStatus(s.getStatus());
    dto.setSiteType(s.getSiteType());
    dto.setCapacity(s.getCapacity());
    dto.setSettlementMode(s.getSettlementMode());
    dto.setSiteLevel(normalizeSiteLevel(s.getSiteLevel()));
    dto.setParentSiteId(parentSite != null && parentSite.getId() != null ? String.valueOf(parentSite.getId()) : null);
    dto.setParentSiteName(parentSite != null ? parentSite.getName() : null);
    dto.setManagementArea(s.getManagementArea());
    dto.setWeighbridgeSiteId(
        weighbridgeSite != null && weighbridgeSite.getId() != null ? String.valueOf(weighbridgeSite.getId()) : null);
    dto.setWeighbridgeSiteName(weighbridgeSite != null ? weighbridgeSite.getName() : null);
    dto.setLng(resolveLng(s));
    dto.setLat(resolveLat(s));
    return dto;
  }

  private SiteDetailDto toDetail(Site s, Map<Long, Site> siteMap) {
    Site parentSite = s.getParentSiteId() != null ? siteMap.get(s.getParentSiteId()) : null;
    Site weighbridgeSite =
        s.getWeighbridgeSiteId() != null ? siteMap.get(s.getWeighbridgeSiteId()) : null;
    SiteDetailDto dto = new SiteDetailDto();
    dto.setId(s.getId() != null ? String.valueOf(s.getId()) : null);
    dto.setName(s.getName());
    dto.setCode(s.getCode());
    dto.setAddress(s.getAddress());
    dto.setProjectId(s.getProjectId());
    dto.setStatus(s.getStatus());
    dto.setOrgId(s.getOrgId());
    dto.setSiteType(s.getSiteType());
    dto.setCapacity(s.getCapacity());
    dto.setSettlementMode(s.getSettlementMode());
    dto.setDisposalUnitPrice(s.getDisposalUnitPrice());
    dto.setDisposalFeeRate(s.getDisposalFeeRate());
    dto.setServiceFeeUnitPrice(s.getServiceFeeUnitPrice());
    dto.setSiteLevel(normalizeSiteLevel(s.getSiteLevel()));
    dto.setParentSiteId(parentSite != null && parentSite.getId() != null ? String.valueOf(parentSite.getId()) : null);
    dto.setParentSiteName(parentSite != null ? parentSite.getName() : null);
    dto.setManagementArea(s.getManagementArea());
    dto.setWeighbridgeSiteId(
        weighbridgeSite != null && weighbridgeSite.getId() != null ? String.valueOf(weighbridgeSite.getId()) : null);
    dto.setWeighbridgeSiteName(weighbridgeSite != null ? weighbridgeSite.getName() : null);
    dto.setLng(resolveLng(s));
    dto.setLat(resolveLat(s));
    dto.setBoundaryGeoJson(resolveBoundaryGeoJson(s));
    dto.setDevices(
        toDeviceDtos(
            siteDeviceMapper.selectList(
                new LambdaQueryWrapper<SiteDevice>()
                    .eq(SiteDevice::getSiteId, s.getId())
                    .orderByAsc(SiteDevice::getId)),
            s));
    dto.setOperationConfig(loadOperationConfig(s));
    dto.setCreateTime(s.getCreateTime() != null ? s.getCreateTime().format(ISO) : null);
    dto.setUpdateTime(s.getUpdateTime() != null ? s.getUpdateTime().format(ISO) : null);
    return dto;
  }

  private SiteOperationConfigDto loadOperationConfig(Site site) {
    SiteOperationConfig config =
        siteOperationConfigMapper.selectOne(
            new LambdaQueryWrapper<SiteOperationConfig>()
                .eq(SiteOperationConfig::getSiteId, site.getId())
                .last("limit 1"));
    if (config == null) {
      return new SiteOperationConfigDto(false, 0, false, BigDecimal.ZERO, 0, null);
    }
    return new SiteOperationConfigDto(
        Objects.equals(config.getQueueEnabled(), 1),
        config.getMaxQueueCount(),
        Objects.equals(config.getManualDisposalEnabled(), 1),
        config.getRangeCheckRadius(),
        config.getDurationLimitMinutes(),
        config.getRemark());
  }

  private Map<Long, List<SiteDevice>> loadDeviceMap(List<Site> sites) {
    LinkedHashSet<Long> siteIds =
        sites.stream()
            .map(Site::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return siteDeviceMapper.selectList(
            new LambdaQueryWrapper<SiteDevice>()
                .in(SiteDevice::getSiteId, siteIds)
                .orderByAsc(SiteDevice::getSiteId)
                .orderByAsc(SiteDevice::getId))
        .stream()
        .filter(device -> device.getSiteId() != null)
        .collect(Collectors.groupingBy(SiteDevice::getSiteId, LinkedHashMap::new, Collectors.toList()));
  }

  private List<SiteDeviceDto> toDeviceDtos(List<SiteDevice> devices, Site site) {
    if (devices == null || devices.isEmpty()) {
      return Collections.emptyList();
    }
    BigDecimal siteLng = resolveLng(site);
    BigDecimal siteLat = resolveLat(site);
    List<SiteDeviceDto> records = new ArrayList<>();
    for (int index = 0; index < devices.size(); index++) {
      SiteDevice device = devices.get(index);
      records.add(
          new SiteDeviceDto(
              device.getId() != null ? String.valueOf(device.getId()) : null,
              device.getDeviceCode(),
              device.getDeviceName(),
              device.getDeviceType(),
              device.getProvider(),
              device.getIpAddress(),
              device.getStatus(),
              resolveDeviceLng(device, siteLng, index),
              resolveDeviceLat(device, siteLat, index),
              device.getRemark()));
    }
    return records;
  }

  private BigDecimal resolveLng(Site site) {
    if (site.getLng() != null) {
      return site.getLng().setScale(6, RoundingMode.HALF_UP);
    }
    return buildFallbackPoint(site)[0];
  }

  private BigDecimal resolveLat(Site site) {
    if (site.getLat() != null) {
      return site.getLat().setScale(6, RoundingMode.HALF_UP);
    }
    return buildFallbackPoint(site)[1];
  }

  private BigDecimal resolveDeviceLng(SiteDevice device, BigDecimal siteLng, int index) {
    if (device.getLng() != null) {
      return device.getLng().setScale(6, RoundingMode.HALF_UP);
    }
    return siteLng.add(BigDecimal.valueOf((index + 1) * 0.00045D)).setScale(6, RoundingMode.HALF_UP);
  }

  private BigDecimal resolveDeviceLat(SiteDevice device, BigDecimal siteLat, int index) {
    if (device.getLat() != null) {
      return device.getLat().setScale(6, RoundingMode.HALF_UP);
    }
    return siteLat.add(BigDecimal.valueOf((index % 2 == 0 ? 1 : -1) * (index + 1) * 0.00035D))
        .setScale(6, RoundingMode.HALF_UP);
  }

  private String resolveBoundaryGeoJson(Site site) {
    if (StringUtils.hasText(site.getBoundaryGeoJson())) {
      return site.getBoundaryGeoJson();
    }
    BigDecimal lng = resolveLng(site);
    BigDecimal lat = resolveLat(site);
    BigDecimal deltaLng = BigDecimal.valueOf(0.0036D);
    BigDecimal deltaLat = BigDecimal.valueOf(0.0024D);
    return String.format(
        "{\"type\":\"Polygon\",\"coordinates\":[[[%s,%s],[%s,%s],[%s,%s],[%s,%s],[%s,%s]]]}",
        formatDecimal(lng.subtract(deltaLng)),
        formatDecimal(lat.subtract(deltaLat)),
        formatDecimal(lng.add(deltaLng)),
        formatDecimal(lat.subtract(deltaLat)),
        formatDecimal(lng.add(deltaLng)),
        formatDecimal(lat.add(deltaLat)),
        formatDecimal(lng.subtract(deltaLng)),
        formatDecimal(lat.add(deltaLat)),
        formatDecimal(lng.subtract(deltaLng)),
        formatDecimal(lat.subtract(deltaLat)));
  }

  private BigDecimal[] buildFallbackPoint(Site site) {
    String seedText =
        (site.getId() != null ? site.getId() : 0L) + "-" + (site.getName() != null ? site.getName() : "SITE");
    int seed = seedText.chars().reduce(0, Integer::sum);
    double angle = Math.toRadians((seed * 47) % 360);
    double radius = 0.055D + (seed % 7) * 0.006D;
    double lng = 120.1551D + Math.cos(angle) * radius;
    double lat = 30.2741D + Math.sin(angle) * radius * 0.72D;
    return new BigDecimal[] {
      BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP),
      BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP)
    };
  }

  private String formatDecimal(BigDecimal value) {
    return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }

  private void validateSiteCreate(SiteCreateDto body) {
    if (body == null) {
      throw new BizException(400, "请求体不能为空");
    }
    if (!StringUtils.hasText(body.getName())) {
      throw new BizException(400, "场地名称不能为空");
    }
    String siteLevel = normalizeSiteLevel(body.getSiteLevel());
    if ("SECONDARY".equals(siteLevel) && body.getParentSiteId() == null) {
      throw new BizException(400, "二级场地必须关联上级场地");
    }
    if (body.getParentSiteId() != null) {
      Site parentSite = siteMapper.selectById(body.getParentSiteId());
      if (parentSite == null || Objects.equals(parentSite.getDeleted(), 1)) {
        throw new BizException(404, "上级场地不存在");
      }
    }
    if (body.getWeighbridgeSiteId() != null) {
      Site weighbridgeSite = siteMapper.selectById(body.getWeighbridgeSiteId());
      if (weighbridgeSite == null || Objects.equals(weighbridgeSite.getDeleted(), 1)) {
        throw new BizException(404, "借用地磅场地不存在");
      }
      Long deviceCount =
          siteDeviceMapper.selectCount(
              new LambdaQueryWrapper<SiteDevice>()
                  .eq(SiteDevice::getSiteId, body.getWeighbridgeSiteId())
                  .eq(SiteDevice::getDeviceType, "WEIGHBRIDGE"));
      if (deviceCount == null || deviceCount <= 0) {
        throw new BizException(400, "所选借用场地未配置地磅设备");
      }
    }
  }

  private String normalizeSiteLevel(String siteLevel) {
    return "SECONDARY".equalsIgnoreCase(siteLevel) ? "SECONDARY" : "PRIMARY";
  }

  private String generateSiteCode(SiteCreateDto body) {
    String typePrefix =
        switch (body.getSiteType() != null ? body.getSiteType() : "") {
          case "STATE_OWNED" -> "GY";
          case "COLLECTIVE" -> "JT";
          case "SHORT_BARGE" -> "DB";
          default -> "GC";
        };
    long sequence = System.currentTimeMillis() % 100000;
    return typePrefix + String.format("%05d", sequence);
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private DisposalListItemDto toDisposalItem(
      ContractTicket ticket,
      Contract contract,
      Map<Long, Site> siteMap,
      Map<Long, Project> projectMap) {
    Site site = contract != null ? siteMap.get(contract.getSiteId()) : null;
    Project project = contract != null ? projectMap.get(contract.getProjectId()) : null;
    return new DisposalListItemDto(
        ticket.getId() != null ? String.valueOf(ticket.getId()) : null,
        site != null && site.getId() != null ? String.valueOf(site.getId()) : null,
        site != null ? site.getName() : null,
        resolveDisposalTime(ticket),
        "-",
        project != null ? project.getName() : (contract != null ? contract.getName() : null),
        StringUtils.hasText(ticket.getTicketType()) ? ticket.getTicketType() : "合同领票",
        ticket.getVolume() != null ? ticket.getVolume().intValue() : 0,
        normalizeStatusText(ticket.getStatus()));
  }

  private boolean contains(String value, String keyword) {
    return value != null && value.contains(keyword);
  }

  private String resolveDisposalTime(ContractTicket ticket) {
    if (ticket.getCreateTime() != null) {
      return ticket.getCreateTime().format(ISO);
    }
    LocalDate ticketDate = ticket.getTicketDate();
    return ticketDate != null ? ticketDate.atStartOfDay().format(ISO) : null;
  }

  private String normalizeStatusText(String rawStatus) {
    if (!StringUtils.hasText(rawStatus)) {
      return "正常";
    }
    String normalized = rawStatus.trim().toUpperCase();
    if ("NORMAL".equals(normalized) || "VALID".equals(normalized)) {
      return "正常";
    }
    if ("CANCELLED".equals(normalized) || "VOID".equals(normalized)) {
      return "异常";
    }
    return rawStatus;
  }

  private String normalizeStatusParam(String rawStatus) {
    if (!StringUtils.hasText(rawStatus)) {
      return rawStatus;
    }
    String normalized = rawStatus.trim();
    if ("正常".equals(normalized)) {
      return "NORMAL";
    }
    if ("异常".equals(normalized)) {
      return "CANCELLED";
    }
    return normalized;
  }
}
