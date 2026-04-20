package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.SettlementItem;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.contract.ContractAccessScope;
import com.xngl.manager.contract.SettlementService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.SettlementContractSummaryDto;
import com.xngl.web.dto.contract.SettlementDetailDto;
import com.xngl.web.dto.contract.SettlementItemDto;
import com.xngl.web.dto.contract.SettlementLineDto;
import com.xngl.web.dto.contract.SettlementStatsDto;
import com.xngl.web.dto.contract.SettlementGenerateDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.ContractAccessScopeResolver;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SettlementService settlementService;
  private final ContractService contractService;
  private final UserContext userContext;
  private final ContractAccessScopeResolver contractAccessScopeResolver;
  private final ContractTicketMapper contractTicketMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;

  public SettlementController(
      SettlementService settlementService,
      ContractService contractService,
      UserContext userContext,
      ContractAccessScopeResolver contractAccessScopeResolver,
      ContractTicketMapper contractTicketMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper) {
    this.settlementService = settlementService;
    this.contractService = contractService;
    this.userContext = userContext;
    this.contractAccessScopeResolver = contractAccessScopeResolver;
    this.contractTicketMapper = contractTicketMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
  }

  @PostMapping("/project/generate")
  public ApiResult<String> generateProject(
      @Valid @RequestBody SettlementGenerateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractAccessScope accessScope = resolveScope(user);
    if (!accessScope.hasProjectAccess(dto.getTargetId())) {
      throw new BizException(403, "当前账号无权发起该项目结算");
    }
    long id =
        settlementService.generateProjectSettlement(
            user.getTenantId(),
            user.getId(),
            dto.getTargetId(),
            dto.getPeriodStart(),
            dto.getPeriodEnd(),
            dto.getRemark());
    return ApiResult.ok(String.valueOf(id));
  }

  @PostMapping("/site/generate")
  public ApiResult<String> generateSite(
      @Valid @RequestBody SettlementGenerateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractAccessScope accessScope = resolveScope(user);
    if (!accessScope.hasSiteAccess(dto.getTargetId())) {
      throw new BizException(403, "当前账号无权发起该场地结算");
    }
    long id =
        settlementService.generateSiteSettlement(
            user.getTenantId(),
            user.getId(),
            dto.getTargetId(),
            dto.getPeriodStart(),
            dto.getPeriodEnd(),
            dto.getRemark());
    return ApiResult.ok(String.valueOf(id));
  }

  @GetMapping
  public ApiResult<PageResult<SettlementItemDto>> list(
      @RequestParam(required = false) String settlementType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractAccessScope accessScope = resolveScope(user);
    IPage<SettlementOrder> page =
        settlementService.pageSettlements(
            user.getTenantId(), settlementType, status, projectId, siteId, pageNo, pageSize, accessScope);
    Map<Long, Project> projectMap = loadProjectMap(page.getRecords());
    Map<Long, Site> siteMap = loadSiteMap(page.getRecords());
    List<SettlementItemDto> records =
        page.getRecords().stream().map(order -> toItemDto(order, projectMap, siteMap)).toList();
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<SettlementDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    SettlementOrder order = requireAccessibleSettlement(id, user);
    List<SettlementItem> items =
        settlementService.listSettlementItems(id, user.getTenantId());
    Project project =
        order.getTargetProjectId() != null ? projectMapper.selectById(order.getTargetProjectId()) : null;
    Site site = order.getTargetSiteId() != null ? siteMapper.selectById(order.getTargetSiteId()) : null;
    return ApiResult.ok(toDetailDto(order, items, project, site));
  }

  @PostMapping("/{id}/submit")
  public ApiResult<Void> submit(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    requireAccessibleSettlement(id, user);
    settlementService.submitSettlement(id, user.getTenantId());
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/approve")
  public ApiResult<Void> approve(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    userContext.requireApprovalPermission(user);
    requireAccessibleSettlement(id, user);
    settlementService.approveSettlement(id, user.getTenantId());
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long id,
      @RequestBody(required = false) ApprovalActionDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    userContext.requireApprovalPermission(user);
    requireAccessibleSettlement(id, user);
    settlementService.rejectSettlement(
        id, user.getTenantId(), dto != null ? dto.getReason() : null);
    return ApiResult.ok(null);
  }

  @GetMapping("/stats")
  public ApiResult<SettlementStatsDto> stats(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    var raw = settlementService.getSettlementStats(user.getTenantId(), resolveScope(user));
    SettlementStatsDto stats = new SettlementStatsDto();
    stats.setPendingAmount(raw.getPendingAmount());
    stats.setSettledAmount(raw.getSettledAmount());
    stats.setTotalOrders(raw.getTotalOrders());
    stats.setDraftOrders(raw.getDraftOrders());
    stats.setPendingOrders(raw.getPendingOrders());
    stats.setSettledOrders(raw.getSettledOrders());
    return ApiResult.ok(stats);
  }

  private SettlementItemDto toItemDto(
      SettlementOrder order, Map<Long, Project> projectMap, Map<Long, Site> siteMap) {
    SettlementItemDto dto = new SettlementItemDto();
    dto.setId(stringValue(order.getId()));
    dto.setSettlementNo(order.getSettlementNo());
    dto.setSettlementType(order.getSettlementType());
    dto.setTargetProjectId(stringValue(order.getTargetProjectId()));
    dto.setTargetProjectName(
        order.getTargetProjectId() != null && projectMap.containsKey(order.getTargetProjectId())
            ? projectMap.get(order.getTargetProjectId()).getName()
            : null);
    dto.setTargetSiteId(stringValue(order.getTargetSiteId()));
    dto.setTargetSiteName(
        order.getTargetSiteId() != null && siteMap.containsKey(order.getTargetSiteId())
            ? siteMap.get(order.getTargetSiteId()).getName()
            : null);
    dto.setPeriodStart(formatDate(order.getPeriodStart()));
    dto.setPeriodEnd(formatDate(order.getPeriodEnd()));
    dto.setTotalVolume(order.getTotalVolume());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setAdjustAmount(order.getAdjustAmount());
    dto.setPayableAmount(order.getPayableAmount());
    dto.setApprovalStatus(order.getApprovalStatus());
    dto.setSettlementStatus(order.getSettlementStatus());
    dto.setCreatorId(stringValue(order.getCreatorId()));
    dto.setCreateTime(formatDateTime(order.getCreateTime()));
    return dto;
  }

  private SettlementDetailDto toDetailDto(
      SettlementOrder order, List<SettlementItem> items, Project project, Site site) {
    Map<Long, ContractTicket> ticketMap = loadTicketMap(items);
    Map<Long, Contract> contractMap = loadContractMapFromTickets(ticketMap, order.getTenantId());
    SettlementDetailDto dto = new SettlementDetailDto();
    dto.setId(stringValue(order.getId()));
    dto.setSettlementNo(order.getSettlementNo());
    dto.setSettlementType(order.getSettlementType());
    dto.setTargetProjectId(stringValue(order.getTargetProjectId()));
    dto.setTargetProjectName(project != null ? project.getName() : null);
    dto.setTargetSiteId(stringValue(order.getTargetSiteId()));
    dto.setTargetSiteName(site != null ? site.getName() : null);
    dto.setPeriodStart(formatDate(order.getPeriodStart()));
    dto.setPeriodEnd(formatDate(order.getPeriodEnd()));
    dto.setTotalVolume(order.getTotalVolume());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setAdjustAmount(order.getAdjustAmount());
    dto.setPayableAmount(order.getPayableAmount());
    dto.setApprovalStatus(order.getApprovalStatus());
    dto.setSettlementStatus(order.getSettlementStatus());
    dto.setCreatorId(stringValue(order.getCreatorId()));
    dto.setCreateTime(formatDateTime(order.getCreateTime()));
    dto.setUnitPrice(order.getUnitPrice());
    dto.setSettlementDate(formatDate(order.getSettlementDate()));
    dto.setProcessInstanceId(order.getProcessInstanceId());
    dto.setRemark(order.getRemark());
    dto.setContractSummaries(buildContractSummaries(items, ticketMap, contractMap));
    dto.setItems(items.stream().map(item -> toLineDto(item, ticketMap, contractMap)).toList());
    return dto;
  }

  private SettlementLineDto toLineDto(
      SettlementItem item, Map<Long, ContractTicket> ticketMap, Map<Long, Contract> contractMap) {
    ContractTicket ticket = resolveTicket(item, ticketMap);
    Contract contract = ticket != null ? contractMap.get(ticket.getContractId()) : null;
    SettlementLineDto dto = new SettlementLineDto();
    dto.setId(stringValue(item.getId()));
    dto.setContractId(ticket != null ? stringValue(ticket.getContractId()) : null);
    dto.setContractNo(contract != null ? contract.getContractNo() : null);
    dto.setSourceRecordType(item.getSourceRecordType());
    dto.setSourceRecordId(stringValue(item.getSourceRecordId()));
    dto.setProjectId(stringValue(item.getProjectId()));
    dto.setSiteId(stringValue(item.getSiteId()));
    dto.setVehicleId(stringValue(item.getVehicleId()));
    dto.setBizDate(formatDate(item.getBizDate()));
    dto.setVolume(item.getVolume());
    dto.setUnitPrice(item.getUnitPrice());
    dto.setAmount(item.getAmount());
    dto.setRemark(item.getRemark());
    return dto;
  }

  private Map<Long, ContractTicket> loadTicketMap(List<SettlementItem> items) {
    List<Long> ticketIds =
        items.stream()
            .filter(item -> "CONTRACT_TICKET".equals(item.getSourceRecordType()))
            .map(SettlementItem::getSourceRecordId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (ticketIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return contractTicketMapper.selectBatchIds(ticketIds).stream()
        .filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toMap(ContractTicket::getId, ticket -> ticket));
  }

  private Map<Long, Contract> loadContractMapFromTickets(
      Map<Long, ContractTicket> ticketMap, Long tenantId) {
    if (ticketMap.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> contractIds =
        ticketMap.values().stream()
            .map(ContractTicket::getContractId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return contractService.listContractsByIds(contractIds, tenantId).stream()
        .collect(java.util.stream.Collectors.toMap(Contract::getId, contract -> contract));
  }

  private ContractTicket resolveTicket(SettlementItem item, Map<Long, ContractTicket> ticketMap) {
    if (item == null || !"CONTRACT_TICKET".equals(item.getSourceRecordType())) {
      return null;
    }
    return ticketMap.get(item.getSourceRecordId());
  }

  private List<SettlementContractSummaryDto> buildContractSummaries(
      List<SettlementItem> items,
      Map<Long, ContractTicket> ticketMap,
      Map<Long, Contract> contractMap) {
    Map<Long, ContractAggregation> aggregations = new HashMap<>();
    for (SettlementItem item : items) {
      ContractTicket ticket = resolveTicket(item, ticketMap);
      if (ticket == null || ticket.getContractId() == null) {
        continue;
      }
      ContractAggregation aggregation =
          aggregations.computeIfAbsent(ticket.getContractId(), key -> new ContractAggregation());
      aggregation.itemCount += 1;
      aggregation.totalVolume = aggregation.totalVolume.add(zeroIfNull(item.getVolume()));
      aggregation.totalAmount = aggregation.totalAmount.add(zeroIfNull(item.getAmount()));
    }
    return aggregations.entrySet().stream()
        .map(
            entry -> {
              ContractAggregation aggregation = entry.getValue();
              BigDecimal averageUnitPrice =
                  aggregation.totalVolume.compareTo(BigDecimal.ZERO) > 0
                      ? aggregation.totalAmount.divide(aggregation.totalVolume, 2, RoundingMode.HALF_UP)
                      : BigDecimal.ZERO;
              Contract contract = contractMap.get(entry.getKey());
              return new SettlementContractSummaryDto(
                  stringValue(entry.getKey()),
                  contract != null ? contract.getContractNo() : null,
                  aggregation.itemCount,
                  aggregation.totalVolume,
                  aggregation.totalAmount,
                  averageUnitPrice);
            })
        .sorted(Comparator.comparing(SettlementContractSummaryDto::getContractNo, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static final class ContractAggregation {
    private int itemCount;
    private BigDecimal totalVolume = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private ContractAccessScope resolveScope(User currentUser) {
    return contractAccessScopeResolver.resolve(currentUser);
  }

  private SettlementOrder requireAccessibleSettlement(Long settlementId, User currentUser) {
    SettlementOrder order = settlementService.getSettlement(settlementId, currentUser.getTenantId());
    if (!resolveScope(currentUser).matchesSettlement(order)) {
      throw new BizException(403, "当前账号无权查看该结算单");
    }
    return order;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private Map<Long, Project> loadProjectMap(List<SettlementOrder> orders) {
    LinkedHashSet<Long> ids =
        orders.stream()
            .map(SettlementOrder::getTargetProjectId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(ids).stream()
        .filter(project -> project.getId() != null)
        .collect(java.util.stream.Collectors.toMap(Project::getId, project -> project, (left, right) -> left));
  }

  private Map<Long, Site> loadSiteMap(List<SettlementOrder> orders) {
    LinkedHashSet<Long> ids =
        orders.stream()
            .map(SettlementOrder::getTargetSiteId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    return siteMapper.selectBatchIds(ids).stream()
        .filter(site -> site.getId() != null)
        .collect(java.util.stream.Collectors.toMap(Site::getId, site -> site, (left, right) -> left));
  }
}
