package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.query.CheckinListItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
public class CheckinsController {

  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final String VOID_REASON_PREFIX = "[VOID_REASON]";

  private final ContractTicketMapper contractTicketMapper;
  private final ContractMapper contractMapper;
  private final ProjectMapper projectMapper;
  private final OrgMapper orgMapper;
  private final SiteService siteService;
  private final UserContext userContext;

  public CheckinsController(
      ContractTicketMapper contractTicketMapper,
      ContractMapper contractMapper,
      ProjectMapper projectMapper,
      OrgMapper orgMapper,
      SiteService siteService,
      UserContext userContext) {
    this.contractTicketMapper = contractTicketMapper;
    this.contractMapper = contractMapper;
    this.projectMapper = projectMapper;
    this.orgMapper = orgMapper;
    this.siteService = siteService;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<CheckinListItemDto>> list(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Contract> contracts = loadContracts(currentUser.getTenantId(), projectId, siteId);
    if (contracts.isEmpty()) {
      return ApiResult.ok(new PageResult<>(pageNo, pageSize, 0L, Collections.emptyList()));
    }

    Map<Long, Contract> contractMap =
        contracts.stream()
            .filter(contract -> contract.getId() != null)
            .collect(Collectors.toMap(Contract::getId, contract -> contract, (left, right) -> left, LinkedHashMap::new));

    List<ContractTicket> tickets = loadTickets(contractMap.keySet(), status, startDate, endDate);
    if (tickets.isEmpty()) {
      return ApiResult.ok(new PageResult<>(pageNo, pageSize, 0L, Collections.emptyList()));
    }

    Map<Long, Project> projectMap = loadProjects(contracts);
    Map<Long, Site> siteMap = loadSites(contracts);
    Map<Long, Org> orgMap = loadTransportOrgs(contracts);

    String keywordValue = trimToNull(keyword);
    List<CheckinListItemDto> rows =
        tickets.stream()
            .map(ticket -> toItem(ticket, contractMap.get(ticket.getContractId()), projectMap, siteMap, orgMap))
            .filter(item -> matchesKeyword(item, keywordValue))
            .sorted(Comparator.comparing(CheckinListItemDto::getPunchTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CheckinListItemDto::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    long total = rows.size();
    int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
    if (fromIndex >= rows.size()) {
      return ApiResult.ok(new PageResult<>(pageNo, pageSize, total, Collections.emptyList()));
    }
    int toIndex = Math.min(rows.size(), fromIndex + pageSize);
    return ApiResult.ok(new PageResult<>(pageNo, pageSize, total, rows.subList(fromIndex, toIndex)));
  }

  @PutMapping("/{id}/void")
  public ApiResult<CheckinListItemDto> voidCheckin(
      @PathVariable Long id,
      @RequestBody CheckinVoidRequest body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractTicket ticket = contractTicketMapper.selectById(id);
    if (ticket == null || !Objects.equals(ticket.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "打卡记录不存在");
    }
    if (!StringUtils.hasText(body.getReason())) {
      throw new BizException(400, "作废原因不能为空");
    }
    ticket.setStatus("CANCELLED");
    ticket.setRemark(VOID_REASON_PREFIX + body.getReason().trim());
    contractTicketMapper.updateById(ticket);

    Contract contract = contractMapper.selectById(ticket.getContractId());
    if (contract == null) {
      throw new BizException(404, "关联合同不存在");
    }
    Map<Long, Project> projectMap = loadProjects(List.of(contract));
    Map<Long, Site> siteMap = loadSites(List.of(contract));
    Map<Long, Org> orgMap = loadTransportOrgs(List.of(contract));
    return ApiResult.ok(toItem(ticket, contract, projectMap, siteMap, orgMap));
  }

  private List<Contract> loadContracts(Long tenantId, Long projectId, Long siteId) {
    return contractMapper.selectList(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .eq(projectId != null, Contract::getProjectId, projectId)
            .eq(siteId != null, Contract::getSiteId, siteId)
            .orderByDesc(Contract::getUpdateTime)
            .orderByDesc(Contract::getId));
  }

  private List<ContractTicket> loadTickets(
      Set<Long> contractIds, String status, LocalDate startDate, LocalDate endDate) {
    if (contractIds.isEmpty()) {
      return Collections.emptyList();
    }
    String statusValue = normalizeStatusParam(status);
    return contractTicketMapper.selectList(
        new LambdaQueryWrapper<ContractTicket>()
            .in(ContractTicket::getContractId, contractIds)
            .eq(StringUtils.hasText(statusValue), ContractTicket::getStatus, statusValue)
            .ge(startDate != null, ContractTicket::getTicketDate, startDate)
            .le(endDate != null, ContractTicket::getTicketDate, endDate)
            .orderByDesc(ContractTicket::getCreateTime)
            .orderByDesc(ContractTicket::getId));
  }

  private Map<Long, Project> loadProjects(List<Contract> contracts) {
    LinkedHashSet<Long> projectIds = contracts.stream()
        .map(Contract::getProjectId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (projectIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(projectIds).stream()
        .filter(project -> project.getId() != null)
        .collect(Collectors.toMap(Project::getId, project -> project, (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, Site> loadSites(List<Contract> contracts) {
    Map<Long, Site> allSites = siteService.list().stream()
        .filter(site -> site.getId() != null)
        .collect(Collectors.toMap(Site::getId, site -> site, (left, right) -> left, LinkedHashMap::new));
    LinkedHashSet<Long> siteIds = contracts.stream()
        .map(Contract::getSiteId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return allSites.entrySet().stream()
        .filter(entry -> siteIds.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, Org> loadTransportOrgs(List<Contract> contracts) {
    LinkedHashSet<Long> orgIds = contracts.stream()
        .map(Contract::getTransportOrgId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(org -> org.getId() != null)
        .collect(Collectors.toMap(Org::getId, org -> org, (left, right) -> left, LinkedHashMap::new));
  }

  private CheckinListItemDto toItem(
      ContractTicket ticket,
      Contract contract,
      Map<Long, Project> projectMap,
      Map<Long, Site> siteMap,
      Map<Long, Org> orgMap) {
    Project project = contract != null ? projectMap.get(contract.getProjectId()) : null;
    Site site = contract != null ? siteMap.get(contract.getSiteId()) : null;
    Org transportOrg = contract != null ? orgMap.get(contract.getTransportOrgId()) : null;
    CheckinListItemDto dto = new CheckinListItemDto();
    dto.setId(ticket.getId() != null ? String.valueOf(ticket.getId()) : null);
    dto.setTicketNo(ticket.getTicketNo());
    dto.setPunchTime(resolvePunchTime(ticket));
    dto.setStatus(ticket.getStatus());
    dto.setStatusLabel(resolveStatusLabel(ticket.getStatus()));
    dto.setExceptionType(resolveExceptionType(ticket.getStatus()));
    dto.setVoidReason(resolveVoidReason(ticket.getRemark()));
    dto.setVolume(ticket.getVolume() != null ? ticket.getVolume() : BigDecimal.ZERO);
    dto.setSourceType(StringUtils.hasText(ticket.getTicketType()) ? ticket.getTicketType() : "合同领票打卡");
    dto.setContractId(contract != null && contract.getId() != null ? String.valueOf(contract.getId()) : null);
    dto.setContractNo(contract != null ? firstNonBlank(contract.getContractNo(), contract.getCode()) : null);
    dto.setContractName(contract != null ? contract.getName() : null);
    dto.setProjectId(project != null && project.getId() != null ? String.valueOf(project.getId()) : null);
    dto.setProjectName(project != null ? project.getName() : null);
    dto.setSiteId(site != null && site.getId() != null ? String.valueOf(site.getId()) : null);
    dto.setSiteName(site != null ? site.getName() : null);
    dto.setTransportOrgName(transportOrg != null ? transportOrg.getOrgName() : null);
    return dto;
  }

  private boolean matchesKeyword(CheckinListItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getTicketNo(), keyword)
        || contains(item.getProjectName(), keyword)
        || contains(item.getSiteName(), keyword)
        || contains(item.getContractNo(), keyword)
        || contains(item.getContractName(), keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getDriverName(), keyword)
        || contains(item.getTransportOrgName(), keyword);
  }

  private boolean contains(String value, String keyword) {
    return StringUtils.hasText(value) && value.contains(keyword);
  }

  private String resolvePunchTime(ContractTicket ticket) {
    LocalDateTime createTime = ticket.getCreateTime();
    if (createTime != null) {
      return createTime.format(ISO_DATE_TIME);
    }
    if (ticket.getTicketDate() != null) {
      return ticket.getTicketDate().atStartOfDay().format(ISO_DATE_TIME);
    }
    return null;
  }

  private String resolveStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "正常";
    }
    String normalized = status.trim().toUpperCase();
    if ("NORMAL".equals(normalized) || "VALID".equals(normalized)) {
      return "正常";
    }
    if ("CANCELLED".equals(normalized) || "VOID".equals(normalized)) {
      return "已作废";
    }
    return status;
  }

  private String resolveExceptionType(String status) {
    String normalized = normalizeStatusParam(status);
    return "CANCELLED".equals(normalized) || "VOID".equals(normalized) ? "异常打卡" : "正常打卡";
  }

  private String resolveVoidReason(String remark) {
    if (!StringUtils.hasText(remark)) {
      return null;
    }
    if (remark.startsWith(VOID_REASON_PREFIX)) {
      return remark.substring(VOID_REASON_PREFIX.length()).trim();
    }
    return remark;
  }

  private String normalizeStatusParam(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }
    String normalized = status.trim().toUpperCase();
    if ("正常".equals(status.trim())) {
      return "NORMAL";
    }
    if ("异常".equals(status.trim()) || "已作废".equals(status.trim())) {
      return "CANCELLED";
    }
    return normalized;
  }

  private String firstNonBlank(String first, String second) {
    return StringUtils.hasText(first) ? first : second;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  @Data
  public static class CheckinVoidRequest {
    private String reason;
  }
}
