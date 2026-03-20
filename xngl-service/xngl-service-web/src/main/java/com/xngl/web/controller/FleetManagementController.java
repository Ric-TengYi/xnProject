package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.fleet.FleetDispatchOrder;
import com.xngl.infrastructure.persistence.entity.fleet.FleetFinanceRecord;
import com.xngl.infrastructure.persistence.entity.fleet.FleetProfile;
import com.xngl.infrastructure.persistence.entity.fleet.FleetTransportPlan;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.FleetDispatchOrderMapper;
import com.xngl.infrastructure.persistence.mapper.FleetFinanceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.FleetProfileMapper;
import com.xngl.infrastructure.persistence.mapper.FleetTransportPlanMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.fleet.FleetDispatchAuditDto;
import com.xngl.web.dto.fleet.FleetDispatchOrderListItemDto;
import com.xngl.web.dto.fleet.FleetDispatchOrderUpsertDto;
import com.xngl.web.dto.fleet.FleetFinanceRecordListItemDto;
import com.xngl.web.dto.fleet.FleetFinanceRecordUpsertDto;
import com.xngl.web.dto.fleet.FleetProfileListItemDto;
import com.xngl.web.dto.fleet.FleetProfileUpsertDto;
import com.xngl.web.dto.fleet.FleetReportItemDto;
import com.xngl.web.dto.fleet.FleetSummaryDto;
import com.xngl.web.dto.fleet.FleetTransportPlanListItemDto;
import com.xngl.web.dto.fleet.FleetTransportPlanUpsertDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
@RequestMapping("/api/fleet-management")
public class FleetManagementController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final FleetProfileMapper fleetProfileMapper;
  private final FleetTransportPlanMapper fleetTransportPlanMapper;
  private final FleetDispatchOrderMapper fleetDispatchOrderMapper;
  private final FleetFinanceRecordMapper fleetFinanceRecordMapper;
  private final OrgMapper orgMapper;
  private final UserService userService;

  public FleetManagementController(
      FleetProfileMapper fleetProfileMapper,
      FleetTransportPlanMapper fleetTransportPlanMapper,
      FleetDispatchOrderMapper fleetDispatchOrderMapper,
      FleetFinanceRecordMapper fleetFinanceRecordMapper,
      OrgMapper orgMapper,
      UserService userService) {
    this.fleetProfileMapper = fleetProfileMapper;
    this.fleetTransportPlanMapper = fleetTransportPlanMapper;
    this.fleetDispatchOrderMapper = fleetDispatchOrderMapper;
    this.fleetFinanceRecordMapper = fleetFinanceRecordMapper;
    this.orgMapper = orgMapper;
    this.userService = userService;
  }

  @GetMapping("/summary")
  public ApiResult<FleetSummaryDto> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetProfile> profiles = listProfiles(currentUser.getTenantId());
    List<FleetTransportPlan> plans = listPlans(currentUser.getTenantId());
    List<FleetDispatchOrder> orders = listDispatchOrders(currentUser.getTenantId());
    List<FleetFinanceRecord> financeRecords = listFinanceRecords(currentUser.getTenantId());
    BigDecimal totalRevenue =
        financeRecords.stream()
            .map(FleetFinanceRecord::getRevenueAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal totalProfit =
        financeRecords.stream()
            .map(this::resolveProfitAmount)
            .reduce(ZERO, BigDecimal::add);
    return ApiResult.ok(
        new FleetSummaryDto(
            profiles.size(),
            (int) profiles.stream().filter(item -> "ENABLED".equalsIgnoreCase(item.getStatus())).count(),
            plans.size(),
            (int)
                orders.stream()
                    .filter(item -> "PENDING_APPROVAL".equalsIgnoreCase(item.getStatus()))
                    .count(),
            totalRevenue,
            totalProfit));
  }

  @GetMapping("/profiles")
  public ApiResult<PageResult<FleetProfileListItemDto>> profiles(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetProfileListItemDto> rows =
        new ArrayList<>(loadProfileRows(currentUser.getTenantId(), keyword, status, orgId));
    rows.sort(
        Comparator.comparing(
                FleetProfileListItemDto::getFleetName, Comparator.nullsLast(String::compareTo))
            .thenComparing(
                FleetProfileListItemDto::getCaptainName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @PostMapping("/profiles")
  public ApiResult<FleetProfileListItemDto> createProfile(
      @RequestBody FleetProfileUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateProfile(body, currentUser.getTenantId(), null);
    FleetProfile entity = new FleetProfile();
    entity.setTenantId(currentUser.getTenantId());
    applyProfile(entity, body);
    fleetProfileMapper.insert(entity);
    return ApiResult.ok(loadProfileDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/profiles/{id}")
  public ApiResult<FleetProfileListItemDto> updateProfile(
      @PathVariable Long id,
      @RequestBody FleetProfileUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    FleetProfile entity = requireProfile(id, currentUser.getTenantId());
    validateProfile(body, currentUser.getTenantId(), id);
    applyProfile(entity, body);
    fleetProfileMapper.updateById(entity);
    return ApiResult.ok(loadProfileDto(id, currentUser.getTenantId()));
  }

  @GetMapping("/transport-plans")
  public ApiResult<PageResult<FleetTransportPlanListItemDto>> transportPlans(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetTransportPlanListItemDto> rows =
        new ArrayList<>(loadTransportPlanRows(currentUser.getTenantId(), keyword, status, fleetId));
    rows.sort(
        Comparator.comparing(
                FleetTransportPlanListItemDto::getPlanDate, Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @PostMapping("/transport-plans")
  public ApiResult<FleetTransportPlanListItemDto> createTransportPlan(
      @RequestBody FleetTransportPlanUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateTransportPlan(body, currentUser.getTenantId());
    FleetTransportPlan entity = new FleetTransportPlan();
    entity.setTenantId(currentUser.getTenantId());
    entity.setPlanNo(StringUtils.hasText(body.getPlanNo()) ? body.getPlanNo().trim() : "FTP-" + System.currentTimeMillis());
    applyTransportPlan(entity, body, currentUser.getTenantId());
    fleetTransportPlanMapper.insert(entity);
    return ApiResult.ok(loadTransportPlanDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/transport-plans/{id}")
  public ApiResult<FleetTransportPlanListItemDto> updateTransportPlan(
      @PathVariable Long id,
      @RequestBody FleetTransportPlanUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateTransportPlan(body, currentUser.getTenantId());
    FleetTransportPlan entity = requireTransportPlan(id, currentUser.getTenantId());
    applyTransportPlan(entity, body, currentUser.getTenantId());
    fleetTransportPlanMapper.updateById(entity);
    return ApiResult.ok(loadTransportPlanDto(id, currentUser.getTenantId()));
  }

  @GetMapping("/dispatch-orders")
  public ApiResult<PageResult<FleetDispatchOrderListItemDto>> dispatchOrders(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetDispatchOrderListItemDto> rows =
        new ArrayList<>(loadDispatchRows(currentUser.getTenantId(), keyword, status, fleetId));
    rows.sort(
        Comparator.comparing(
                FleetDispatchOrderListItemDto::getApplyDate, Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @PostMapping("/dispatch-orders")
  public ApiResult<FleetDispatchOrderListItemDto> createDispatchOrder(
      @RequestBody FleetDispatchOrderUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateDispatchOrder(body, currentUser.getTenantId());
    FleetDispatchOrder entity = new FleetDispatchOrder();
    entity.setTenantId(currentUser.getTenantId());
    entity.setOrderNo("FDO-" + System.currentTimeMillis());
    applyDispatchOrder(entity, body, currentUser.getTenantId());
    fleetDispatchOrderMapper.insert(entity);
    return ApiResult.ok(loadDispatchDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/dispatch-orders/{id}")
  public ApiResult<FleetDispatchOrderListItemDto> updateDispatchOrder(
      @PathVariable Long id,
      @RequestBody FleetDispatchOrderUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateDispatchOrder(body, currentUser.getTenantId());
    FleetDispatchOrder entity = requireDispatchOrder(id, currentUser.getTenantId());
    if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
      throw new BizException(400, "已完成调度单不支持修改");
    }
    applyDispatchOrder(entity, body, currentUser.getTenantId());
    fleetDispatchOrderMapper.updateById(entity);
    return ApiResult.ok(loadDispatchDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/dispatch-orders/{id}/approve")
  public ApiResult<FleetDispatchOrderListItemDto> approveDispatchOrder(
      @PathVariable Long id,
      @RequestBody FleetDispatchAuditDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    FleetDispatchOrder entity = requireDispatchOrder(id, currentUser.getTenantId());
    entity.setStatus("APPROVED");
    entity.setApprovedBy(resolveUserName(currentUser));
    entity.setApprovedTime(LocalDateTime.now());
    entity.setAuditRemark(trimToNull(body != null ? body.getComment() : null));
    fleetDispatchOrderMapper.updateById(entity);
    return ApiResult.ok(loadDispatchDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/dispatch-orders/{id}/reject")
  public ApiResult<FleetDispatchOrderListItemDto> rejectDispatchOrder(
      @PathVariable Long id,
      @RequestBody FleetDispatchAuditDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    FleetDispatchOrder entity = requireDispatchOrder(id, currentUser.getTenantId());
    entity.setStatus("REJECTED");
    entity.setApprovedBy(resolveUserName(currentUser));
    entity.setApprovedTime(LocalDateTime.now());
    entity.setAuditRemark(trimToNull(body != null ? body.getComment() : null));
    fleetDispatchOrderMapper.updateById(entity);
    return ApiResult.ok(loadDispatchDto(id, currentUser.getTenantId()));
  }

  @GetMapping("/finance-records")
  public ApiResult<PageResult<FleetFinanceRecordListItemDto>> financeRecords(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetFinanceRecordListItemDto> rows =
        new ArrayList<>(loadFinanceRows(currentUser.getTenantId(), keyword, status, fleetId));
    rows.sort(
        Comparator.comparing(
                FleetFinanceRecordListItemDto::getStatementMonth,
                Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @PostMapping("/finance-records")
  public ApiResult<FleetFinanceRecordListItemDto> createFinanceRecord(
      @RequestBody FleetFinanceRecordUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateFinanceRecord(body, currentUser.getTenantId());
    FleetFinanceRecord entity = new FleetFinanceRecord();
    entity.setTenantId(currentUser.getTenantId());
    entity.setRecordNo("FFR-" + System.currentTimeMillis());
    applyFinanceRecord(entity, body, currentUser.getTenantId());
    fleetFinanceRecordMapper.insert(entity);
    return ApiResult.ok(loadFinanceDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/finance-records/{id}")
  public ApiResult<FleetFinanceRecordListItemDto> updateFinanceRecord(
      @PathVariable Long id,
      @RequestBody FleetFinanceRecordUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateFinanceRecord(body, currentUser.getTenantId());
    FleetFinanceRecord entity = requireFinanceRecord(id, currentUser.getTenantId());
    applyFinanceRecord(entity, body, currentUser.getTenantId());
    fleetFinanceRecordMapper.updateById(entity);
    return ApiResult.ok(loadFinanceDto(id, currentUser.getTenantId()));
  }

  @GetMapping("/report")
  public ApiResult<List<FleetReportItemDto>> report(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetProfile> profiles = listProfiles(currentUser.getTenantId());
    Map<Long, Org> orgMap = loadOrgMapFromProfiles(profiles);
    Map<Long, List<FleetTransportPlan>> planMap =
        listPlans(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .collect(Collectors.groupingBy(FleetTransportPlan::getFleetId));
    Map<Long, List<FleetDispatchOrder>> dispatchMap =
        listDispatchOrders(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .collect(Collectors.groupingBy(FleetDispatchOrder::getFleetId));
    Map<Long, List<FleetFinanceRecord>> financeMap =
        listFinanceRecords(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .collect(Collectors.groupingBy(FleetFinanceRecord::getFleetId));
    List<FleetReportItemDto> rows = new ArrayList<>();
    for (FleetProfile profile : profiles) {
      List<FleetTransportPlan> profilePlans = planMap.getOrDefault(profile.getId(), Collections.emptyList());
      List<FleetDispatchOrder> profileOrders =
          dispatchMap.getOrDefault(profile.getId(), Collections.emptyList());
      List<FleetFinanceRecord> profileFinance =
          financeMap.getOrDefault(profile.getId(), Collections.emptyList());
      FleetReportItemDto dto = new FleetReportItemDto();
      dto.setFleetId(profile.getId() != null ? String.valueOf(profile.getId()) : null);
      dto.setFleetName(profile.getFleetName());
      dto.setOrgName(resolveOrgName(orgMap.get(profile.getOrgId()), profile.getOrgId()));
      dto.setTotalPlans(profilePlans.size());
      dto.setTotalDispatchOrders(profileOrders.size());
      dto.setApprovedDispatchOrders(
          (int)
              profileOrders.stream()
                  .filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus()))
                  .count());
      dto.setPlannedVolume(
          profilePlans.stream()
              .map(FleetTransportPlan::getPlannedVolume)
              .filter(Objects::nonNull)
              .reduce(ZERO, BigDecimal::add));
      dto.setRevenueAmount(
          profileFinance.stream()
              .map(FleetFinanceRecord::getRevenueAmount)
              .filter(Objects::nonNull)
              .reduce(ZERO, BigDecimal::add));
      dto.setCostAmount(
          profileFinance.stream()
              .map(item -> defaultDecimal(item.getCostAmount()).add(defaultDecimal(item.getOtherAmount())))
              .reduce(ZERO, BigDecimal::add));
      dto.setProfitAmount(profileFinance.stream().map(this::resolveProfitAmount).reduce(ZERO, BigDecimal::add));
      rows.add(dto);
    }
    rows.sort(
        Comparator.comparing(FleetReportItemDto::getRevenueAmount, Comparator.nullsLast(BigDecimal::compareTo))
            .reversed()
            .thenComparing(FleetReportItemDto::getFleetName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  private List<FleetProfile> listProfiles(Long tenantId) {
    return fleetProfileMapper.selectList(
        new LambdaQueryWrapper<FleetProfile>()
            .eq(FleetProfile::getTenantId, tenantId)
            .orderByAsc(FleetProfile::getFleetName)
            .orderByDesc(FleetProfile::getId));
  }

  private List<FleetTransportPlan> listPlans(Long tenantId) {
    return fleetTransportPlanMapper.selectList(
        new LambdaQueryWrapper<FleetTransportPlan>()
            .eq(FleetTransportPlan::getTenantId, tenantId)
            .orderByDesc(FleetTransportPlan::getPlanDate)
            .orderByDesc(FleetTransportPlan::getId));
  }

  private List<FleetDispatchOrder> listDispatchOrders(Long tenantId) {
    return fleetDispatchOrderMapper.selectList(
        new LambdaQueryWrapper<FleetDispatchOrder>()
            .eq(FleetDispatchOrder::getTenantId, tenantId)
            .orderByDesc(FleetDispatchOrder::getApplyDate)
            .orderByDesc(FleetDispatchOrder::getId));
  }

  private List<FleetFinanceRecord> listFinanceRecords(Long tenantId) {
    return fleetFinanceRecordMapper.selectList(
        new LambdaQueryWrapper<FleetFinanceRecord>()
            .eq(FleetFinanceRecord::getTenantId, tenantId)
            .orderByDesc(FleetFinanceRecord::getStatementMonth)
            .orderByDesc(FleetFinanceRecord::getId));
  }

  private List<FleetProfileListItemDto> loadProfileRows(
      Long tenantId, String keyword, String status, Long orgId) {
    Map<Long, Org> orgMap = loadOrgMapFromProfiles(listProfiles(tenantId));
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    return listProfiles(tenantId).stream()
        .filter(item -> orgId == null || Objects.equals(item.getOrgId(), orgId))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .map(item -> toProfileDto(item, orgMap))
        .filter(
            item ->
                !StringUtils.hasText(keywordValue)
                    || contains(item.getFleetName(), keywordValue)
                    || contains(item.getCaptainName(), keywordValue)
                    || contains(item.getOrgName(), keywordValue))
        .toList();
  }

  private List<FleetTransportPlanListItemDto> loadTransportPlanRows(
      Long tenantId, String keyword, String status, Long fleetId) {
    List<FleetTransportPlan> rows = listPlans(tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(rows.stream().map(FleetTransportPlan::getFleetId).toList(), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    return rows.stream()
        .filter(item -> fleetId == null || Objects.equals(item.getFleetId(), fleetId))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .map(item -> toTransportPlanDto(item, fleetMap.get(item.getFleetId()), orgMap))
        .filter(
            item ->
                !StringUtils.hasText(keywordValue)
                    || contains(item.getFleetName(), keywordValue)
                    || contains(item.getPlanNo(), keywordValue)
                    || contains(item.getSourcePoint(), keywordValue)
                    || contains(item.getDestinationPoint(), keywordValue))
        .toList();
  }

  private List<FleetDispatchOrderListItemDto> loadDispatchRows(
      Long tenantId, String keyword, String status, Long fleetId) {
    List<FleetDispatchOrder> rows = listDispatchOrders(tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(rows.stream().map(FleetDispatchOrder::getFleetId).toList(), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    return rows.stream()
        .filter(item -> fleetId == null || Objects.equals(item.getFleetId(), fleetId))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .map(item -> toDispatchDto(item, fleetMap.get(item.getFleetId()), orgMap))
        .filter(
            item ->
                !StringUtils.hasText(keywordValue)
                    || contains(item.getFleetName(), keywordValue)
                    || contains(item.getOrderNo(), keywordValue)
                    || contains(item.getRelatedPlanNo(), keywordValue)
                    || contains(item.getApplicantName(), keywordValue))
        .toList();
  }

  private List<FleetFinanceRecordListItemDto> loadFinanceRows(
      Long tenantId, String keyword, String status, Long fleetId) {
    List<FleetFinanceRecord> rows = listFinanceRecords(tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(rows.stream().map(FleetFinanceRecord::getFleetId).toList(), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    return rows.stream()
        .filter(item -> fleetId == null || Objects.equals(item.getFleetId(), fleetId))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .map(item -> toFinanceDto(item, fleetMap.get(item.getFleetId()), orgMap))
        .filter(
            item ->
                !StringUtils.hasText(keywordValue)
                    || contains(item.getFleetName(), keywordValue)
                    || contains(item.getRecordNo(), keywordValue)
                    || contains(item.getContractNo(), keywordValue)
                    || contains(item.getStatementMonth(), keywordValue))
        .toList();
  }

  private FleetProfileListItemDto loadProfileDto(Long id, Long tenantId) {
    FleetProfile entity = requireProfile(id, tenantId);
    return toProfileDto(entity, loadOrgMapFromProfiles(List.of(entity)));
  }

  private FleetTransportPlanListItemDto loadTransportPlanDto(Long id, Long tenantId) {
    FleetTransportPlan entity = requireTransportPlan(id, tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(List.of(entity.getFleetId()), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    return toTransportPlanDto(entity, fleetMap.get(entity.getFleetId()), orgMap);
  }

  private FleetDispatchOrderListItemDto loadDispatchDto(Long id, Long tenantId) {
    FleetDispatchOrder entity = requireDispatchOrder(id, tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(List.of(entity.getFleetId()), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    return toDispatchDto(entity, fleetMap.get(entity.getFleetId()), orgMap);
  }

  private FleetFinanceRecordListItemDto loadFinanceDto(Long id, Long tenantId) {
    FleetFinanceRecord entity = requireFinanceRecord(id, tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(List.of(entity.getFleetId()), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    return toFinanceDto(entity, fleetMap.get(entity.getFleetId()), orgMap);
  }

  private Map<Long, FleetProfile> loadFleetMap(List<Long> ids, Long tenantId) {
    LinkedHashSet<Long> fleetIds =
        ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (fleetIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return fleetProfileMapper.selectList(
            new LambdaQueryWrapper<FleetProfile>()
                .eq(FleetProfile::getTenantId, tenantId)
                .in(FleetProfile::getId, fleetIds))
        .stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(FleetProfile::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadOrgMapFromProfiles(List<FleetProfile> rows) {
    LinkedHashSet<Long> orgIds =
        rows.stream()
            .map(FleetProfile::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadOrgMapFromFleetMap(Map<Long, FleetProfile> fleetMap) {
    return loadOrgMapFromProfiles(new ArrayList<>(fleetMap.values()));
  }

  private FleetProfileListItemDto toProfileDto(FleetProfile entity, Map<Long, Org> orgMap) {
    FleetProfileListItemDto dto = new FleetProfileListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setFleetName(entity.getFleetName());
    dto.setCaptainName(entity.getCaptainName());
    dto.setCaptainPhone(entity.getCaptainPhone());
    dto.setDriverCountPlan(entity.getDriverCountPlan());
    dto.setVehicleCountPlan(entity.getVehicleCountPlan());
    dto.setStatus(defaultValue(entity.getStatus(), "ENABLED"));
    dto.setStatusLabel(resolveProfileStatusLabel(dto.getStatus()));
    dto.setAttendanceMode(entity.getAttendanceMode());
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private FleetTransportPlanListItemDto toTransportPlanDto(
      FleetTransportPlan entity, FleetProfile profile, Map<Long, Org> orgMap) {
    FleetTransportPlanListItemDto dto = new FleetTransportPlanListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setFleetId(entity.getFleetId() != null ? String.valueOf(entity.getFleetId()) : null);
    dto.setFleetName(profile != null ? profile.getFleetName() : "未关联车队");
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setPlanNo(entity.getPlanNo());
    dto.setPlanDate(formatDate(entity.getPlanDate()));
    dto.setSourcePoint(entity.getSourcePoint());
    dto.setDestinationPoint(entity.getDestinationPoint());
    dto.setCargoType(entity.getCargoType());
    dto.setPlannedTrips(entity.getPlannedTrips());
    dto.setPlannedVolume(defaultDecimal(entity.getPlannedVolume()));
    dto.setStatus(defaultValue(entity.getStatus(), "ACTIVE"));
    dto.setStatusLabel(resolvePlanStatusLabel(dto.getStatus()));
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private FleetDispatchOrderListItemDto toDispatchDto(
      FleetDispatchOrder entity, FleetProfile profile, Map<Long, Org> orgMap) {
    FleetDispatchOrderListItemDto dto = new FleetDispatchOrderListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setFleetId(entity.getFleetId() != null ? String.valueOf(entity.getFleetId()) : null);
    dto.setFleetName(profile != null ? profile.getFleetName() : "未关联车队");
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setOrderNo(entity.getOrderNo());
    dto.setRelatedPlanNo(entity.getRelatedPlanNo());
    dto.setApplyDate(formatDate(entity.getApplyDate()));
    dto.setRequestedVehicleCount(entity.getRequestedVehicleCount());
    dto.setRequestedDriverCount(entity.getRequestedDriverCount());
    dto.setUrgencyLevel(defaultValue(entity.getUrgencyLevel(), "MEDIUM"));
    dto.setUrgencyLabel(resolveUrgencyLabel(dto.getUrgencyLevel()));
    dto.setStatus(defaultValue(entity.getStatus(), "PENDING_APPROVAL"));
    dto.setStatusLabel(resolveDispatchStatusLabel(dto.getStatus()));
    dto.setApplicantName(entity.getApplicantName());
    dto.setApprovedBy(entity.getApprovedBy());
    dto.setApprovedTime(formatDateTime(entity.getApprovedTime()));
    dto.setAuditRemark(entity.getAuditRemark());
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private FleetFinanceRecordListItemDto toFinanceDto(
      FleetFinanceRecord entity, FleetProfile profile, Map<Long, Org> orgMap) {
    FleetFinanceRecordListItemDto dto = new FleetFinanceRecordListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setFleetId(entity.getFleetId() != null ? String.valueOf(entity.getFleetId()) : null);
    dto.setFleetName(profile != null ? profile.getFleetName() : "未关联车队");
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setRecordNo(entity.getRecordNo());
    dto.setContractNo(entity.getContractNo());
    dto.setStatementMonth(entity.getStatementMonth());
    dto.setRevenueAmount(defaultDecimal(entity.getRevenueAmount()));
    dto.setCostAmount(defaultDecimal(entity.getCostAmount()));
    dto.setOtherAmount(defaultDecimal(entity.getOtherAmount()));
    dto.setSettledAmount(defaultDecimal(entity.getSettledAmount()));
    dto.setProfitAmount(resolveProfitAmount(entity));
    dto.setOutstandingAmount(resolveOutstandingAmount(entity));
    dto.setStatus(defaultValue(entity.getStatus(), "CONFIRMED"));
    dto.setStatusLabel(resolveFinanceStatusLabel(dto.getStatus()));
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private void validateProfile(FleetProfileUpsertDto body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getFleetName())) {
      throw new BizException(400, "车队名称不能为空");
    }
    Long existing =
        fleetProfileMapper.selectCount(
            new LambdaQueryWrapper<FleetProfile>()
                .eq(FleetProfile::getTenantId, tenantId)
                .eq(FleetProfile::getFleetName, body.getFleetName().trim())
                .ne(currentId != null, FleetProfile::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "车队名称已存在");
    }
  }

  private void applyProfile(FleetProfile entity, FleetProfileUpsertDto body) {
    entity.setOrgId(body.getOrgId());
    entity.setFleetName(body.getFleetName().trim());
    entity.setCaptainName(trimToNull(body.getCaptainName()));
    entity.setCaptainPhone(trimToNull(body.getCaptainPhone()));
    entity.setDriverCountPlan(body.getDriverCountPlan() != null ? body.getDriverCountPlan() : 0);
    entity.setVehicleCountPlan(body.getVehicleCountPlan() != null ? body.getVehicleCountPlan() : 0);
    entity.setStatus(defaultValue(body.getStatus(), "ENABLED"));
    entity.setAttendanceMode(defaultValue(body.getAttendanceMode(), "MANUAL"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private void validateTransportPlan(FleetTransportPlanUpsertDto body, Long tenantId) {
    if (body == null || body.getFleetId() == null) {
      throw new BizException(400, "请选择车队");
    }
    requireProfile(body.getFleetId(), tenantId);
  }

  private void applyTransportPlan(FleetTransportPlan entity, FleetTransportPlanUpsertDto body, Long tenantId) {
    FleetProfile profile = requireProfile(body.getFleetId(), tenantId);
    entity.setFleetId(profile.getId());
    entity.setOrgId(profile.getOrgId());
    entity.setPlanNo(
        StringUtils.hasText(body.getPlanNo()) ? body.getPlanNo().trim() : entity.getPlanNo());
    entity.setPlanDate(body.getPlanDate() != null ? body.getPlanDate() : LocalDate.now());
    entity.setSourcePoint(trimToNull(body.getSourcePoint()));
    entity.setDestinationPoint(trimToNull(body.getDestinationPoint()));
    entity.setCargoType(trimToNull(body.getCargoType()));
    entity.setPlannedTrips(body.getPlannedTrips() != null ? body.getPlannedTrips() : 0);
    entity.setPlannedVolume(defaultDecimal(body.getPlannedVolume()));
    entity.setStatus(defaultValue(body.getStatus(), "ACTIVE"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private void validateDispatchOrder(FleetDispatchOrderUpsertDto body, Long tenantId) {
    if (body == null || body.getFleetId() == null) {
      throw new BizException(400, "请选择车队");
    }
    requireProfile(body.getFleetId(), tenantId);
  }

  private void applyDispatchOrder(FleetDispatchOrder entity, FleetDispatchOrderUpsertDto body, Long tenantId) {
    FleetProfile profile = requireProfile(body.getFleetId(), tenantId);
    entity.setFleetId(profile.getId());
    entity.setOrgId(profile.getOrgId());
    entity.setRelatedPlanNo(trimToNull(body.getRelatedPlanNo()));
    entity.setApplyDate(body.getApplyDate() != null ? body.getApplyDate() : LocalDate.now());
    entity.setRequestedVehicleCount(
        body.getRequestedVehicleCount() != null ? body.getRequestedVehicleCount() : 0);
    entity.setRequestedDriverCount(
        body.getRequestedDriverCount() != null ? body.getRequestedDriverCount() : 0);
    entity.setUrgencyLevel(defaultValue(body.getUrgencyLevel(), "MEDIUM"));
    entity.setStatus(defaultValue(body.getStatus(), "PENDING_APPROVAL"));
    entity.setApplicantName(trimToNull(body.getApplicantName()));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private void validateFinanceRecord(FleetFinanceRecordUpsertDto body, Long tenantId) {
    if (body == null || body.getFleetId() == null) {
      throw new BizException(400, "请选择车队");
    }
    requireProfile(body.getFleetId(), tenantId);
  }

  private void applyFinanceRecord(FleetFinanceRecord entity, FleetFinanceRecordUpsertDto body, Long tenantId) {
    FleetProfile profile = requireProfile(body.getFleetId(), tenantId);
    entity.setFleetId(profile.getId());
    entity.setOrgId(profile.getOrgId());
    entity.setContractNo(trimToNull(body.getContractNo()));
    entity.setStatementMonth(trimToNull(body.getStatementMonth()));
    entity.setRevenueAmount(defaultDecimal(body.getRevenueAmount()));
    entity.setCostAmount(defaultDecimal(body.getCostAmount()));
    entity.setOtherAmount(defaultDecimal(body.getOtherAmount()));
    entity.setSettledAmount(defaultDecimal(body.getSettledAmount()));
    entity.setStatus(defaultValue(body.getStatus(), "CONFIRMED"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private FleetProfile requireProfile(Long id, Long tenantId) {
    FleetProfile entity = fleetProfileMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "车队不存在");
    }
    return entity;
  }

  private FleetTransportPlan requireTransportPlan(Long id, Long tenantId) {
    FleetTransportPlan entity = fleetTransportPlanMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "运输计划不存在");
    }
    return entity;
  }

  private FleetDispatchOrder requireDispatchOrder(Long id, Long tenantId) {
    FleetDispatchOrder entity = fleetDispatchOrderMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "调度单不存在");
    }
    return entity;
  }

  private FleetFinanceRecord requireFinanceRecord(Long id, Long tenantId) {
    FleetFinanceRecord entity = fleetFinanceRecordMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "财务记录不存在");
    }
    return entity;
  }

  private <T> PageResult<T> paginate(List<T> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(safePageNo, safePageSize, rows.size(), rows.subList(fromIndex, toIndex));
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

  private String resolveOrgName(Org org, Long orgId) {
    if (org != null && StringUtils.hasText(org.getOrgName())) {
      return org.getOrgName();
    }
    return orgId != null ? "单位#" + orgId : null;
  }

  private String resolveProfileStatusLabel(String status) {
    return "DISABLED".equalsIgnoreCase(status) ? "停用" : "启用";
  }

  private String resolvePlanStatusLabel(String status) {
    return switch (defaultValue(status, "ACTIVE")) {
      case "DRAFT" -> "草稿";
      case "COMPLETED" -> "已完成";
      default -> "执行中";
    };
  }

  private String resolveDispatchStatusLabel(String status) {
    return switch (defaultValue(status, "PENDING_APPROVAL")) {
      case "APPROVED" -> "已批准";
      case "REJECTED" -> "已驳回";
      case "IN_PROGRESS" -> "执行中";
      case "COMPLETED" -> "已完成";
      default -> "待审批";
    };
  }

  private String resolveFinanceStatusLabel(String status) {
    return switch (defaultValue(status, "CONFIRMED")) {
      case "DRAFT" -> "草稿";
      case "SETTLED" -> "已结清";
      default -> "已确认";
    };
  }

  private String resolveUrgencyLabel(String status) {
    return switch (defaultValue(status, "MEDIUM")) {
      case "HIGH" -> "高";
      case "LOW" -> "低";
      default -> "中";
    };
  }

  private String resolveUserName(User user) {
    return StringUtils.hasText(user.getName()) ? user.getName() : user.getUsername();
  }

  private BigDecimal resolveProfitAmount(FleetFinanceRecord entity) {
    return defaultDecimal(entity.getRevenueAmount())
        .subtract(defaultDecimal(entity.getCostAmount()))
        .subtract(defaultDecimal(entity.getOtherAmount()));
  }

  private BigDecimal resolveOutstandingAmount(FleetFinanceRecord entity) {
    return defaultDecimal(entity.getRevenueAmount()).subtract(defaultDecimal(entity.getSettledAmount()));
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
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

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }

  private BigDecimal percentage(int numerator, int denominator) {
    if (denominator <= 0) {
      return ZERO;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }
}
