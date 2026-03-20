package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.site.DisposalListItemDto;
import com.xngl.web.dto.site.SiteDetailDto;
import com.xngl.web.dto.site.SiteListItemDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final ProjectMapper projectMapper;

  public SitesController(
      SiteService siteService,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      ProjectMapper projectMapper) {
    this.siteService = siteService;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.projectMapper = projectMapper;
  }

  @GetMapping
  public ApiResult<List<SiteListItemDto>> list() {
    List<Site> sites = siteService.list();
    List<SiteListItemDto> records =
        sites.stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(records);
  }

  @GetMapping("/{id}")
  public ApiResult<SiteDetailDto> get(@PathVariable Long id) {
    Site site = siteService.getById(id);
    if (site == null) return ApiResult.fail(404, "场地不存在");
    return ApiResult.ok(toDetail(site));
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
  public ApiResult<?> create(@RequestBody Object body) {
    return ApiResult.ok(null);
  }

  private SiteListItemDto toListItem(Site s) {
    SiteListItemDto dto = new SiteListItemDto();
    dto.setId(s.getId() != null ? String.valueOf(s.getId()) : null);
    dto.setName(s.getName());
    dto.setCode(s.getCode());
    dto.setAddress(s.getAddress());
    dto.setStatus(s.getStatus());
    dto.setSiteType(s.getSiteType());
    dto.setCapacity(s.getCapacity());
    dto.setSettlementMode(s.getSettlementMode());
    return dto;
  }

  private SiteDetailDto toDetail(Site s) {
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
    dto.setCreateTime(s.getCreateTime() != null ? s.getCreateTime().format(ISO) : null);
    dto.setUpdateTime(s.getUpdateTime() != null ? s.getUpdateTime().format(ISO) : null);
    return dto;
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
