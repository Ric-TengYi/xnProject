package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.fleet.FleetDispatchOrder;
import com.xngl.infrastructure.persistence.entity.fleet.FleetFinanceRecord;
import com.xngl.infrastructure.persistence.entity.fleet.FleetProfile;
import com.xngl.infrastructure.persistence.entity.fleet.FleetTransportPlan;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleTrackPoint;
import com.xngl.infrastructure.persistence.mapper.FleetDispatchOrderMapper;
import com.xngl.infrastructure.persistence.mapper.FleetFinanceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.FleetProfileMapper;
import com.xngl.infrastructure.persistence.mapper.FleetTransportPlanMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleTrackPointMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.fleet.FleetDispatchAuditDto;
import com.xngl.web.dto.fleet.FleetDispatchOrderListItemDto;
import com.xngl.web.dto.fleet.FleetDispatchOrderUpsertDto;
import com.xngl.web.dto.fleet.FleetFinanceRecordListItemDto;
import com.xngl.web.dto.fleet.FleetFinanceSummaryDto;
import com.xngl.web.dto.fleet.FleetFinanceRecordUpsertDto;
import com.xngl.web.dto.fleet.FleetProfileListItemDto;
import com.xngl.web.dto.fleet.FleetProfileUpsertDto;
import com.xngl.web.dto.fleet.FleetReportItemDto;
import com.xngl.web.dto.fleet.FleetSummaryDto;
import com.xngl.web.dto.fleet.FleetTrackingHistoryDto;
import com.xngl.web.dto.fleet.FleetTrackingItemDto;
import com.xngl.web.dto.fleet.FleetTrackingStopDto;
import com.xngl.web.dto.fleet.FleetTrackingSummaryDto;
import com.xngl.web.dto.fleet.FleetTransportPlanListItemDto;
import com.xngl.web.dto.fleet.FleetTransportPlanUpsertDto;
import com.xngl.web.dto.vehicle.VehicleTrackPointDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private static final DateTimeFormatter ISO_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final DateTimeFormatter ISO_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final FleetProfileMapper fleetProfileMapper;
  private final FleetTransportPlanMapper fleetTransportPlanMapper;
  private final FleetDispatchOrderMapper fleetDispatchOrderMapper;
  private final FleetFinanceRecordMapper fleetFinanceRecordMapper;
  private final OrgMapper orgMapper;
  private final VehicleMapper vehicleMapper;
  private final VehicleTrackPointMapper vehicleTrackPointMapper;
  private final UserContext userContext;

  public FleetManagementController(
      FleetProfileMapper fleetProfileMapper,
      FleetTransportPlanMapper fleetTransportPlanMapper,
      FleetDispatchOrderMapper fleetDispatchOrderMapper,
      FleetFinanceRecordMapper fleetFinanceRecordMapper,
      OrgMapper orgMapper,
      VehicleMapper vehicleMapper,
      VehicleTrackPointMapper vehicleTrackPointMapper,
      UserContext userContext) {
    this.fleetProfileMapper = fleetProfileMapper;
    this.fleetTransportPlanMapper = fleetTransportPlanMapper;
    this.fleetDispatchOrderMapper = fleetDispatchOrderMapper;
    this.fleetFinanceRecordMapper = fleetFinanceRecordMapper;
    this.orgMapper = orgMapper;
    this.vehicleMapper = vehicleMapper;
    this.vehicleTrackPointMapper = vehicleTrackPointMapper;
    this.userContext = userContext;
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
      @RequestParam(required = false) String contractNo,
      @RequestParam(required = false) String statementMonthFrom,
      @RequestParam(required = false) String statementMonthTo,
      @RequestParam(required = false) Boolean unsettledOnly,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetFinanceRecordListItemDto> rows =
        new ArrayList<>(
            loadFinanceRows(
                currentUser.getTenantId(),
                keyword,
                status,
                fleetId,
                contractNo,
                normalizeMonth(statementMonthFrom),
                normalizeMonth(statementMonthTo),
                Boolean.TRUE.equals(unsettledOnly)));
    rows.sort(
        Comparator.comparing(
                FleetFinanceRecordListItemDto::getStatementMonth,
                Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/finance-records/summary")
  public ApiResult<FleetFinanceSummaryDto> financeSummary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(required = false) String contractNo,
      @RequestParam(required = false) String statementMonthFrom,
      @RequestParam(required = false) String statementMonthTo,
      @RequestParam(required = false) Boolean unsettledOnly,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetFinanceRecordListItemDto> rows =
        loadFinanceRows(
            currentUser.getTenantId(),
            keyword,
            status,
            fleetId,
            contractNo,
            normalizeMonth(statementMonthFrom),
            normalizeMonth(statementMonthTo),
            Boolean.TRUE.equals(unsettledOnly));
    return ApiResult.ok(buildFinanceSummary(rows));
  }

  @GetMapping("/finance-records/export")
  public ResponseEntity<byte[]> exportFinanceRecords(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(required = false) String contractNo,
      @RequestParam(required = false) String statementMonthFrom,
      @RequestParam(required = false) String statementMonthTo,
      @RequestParam(required = false) Boolean unsettledOnly,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetFinanceRecordListItemDto> rows =
        loadFinanceRows(
            currentUser.getTenantId(),
            keyword,
            status,
            fleetId,
            contractNo,
            normalizeMonth(statementMonthFrom),
            normalizeMonth(statementMonthTo),
            Boolean.TRUE.equals(unsettledOnly));
    String csv = buildFinanceCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fleet_finance_records.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.getBytes(StandardCharsets.UTF_8));
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
  public ApiResult<List<FleetReportItemDto>> report(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String statementMonthFrom,
      @RequestParam(required = false) String statementMonthTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String monthFrom = normalizeMonth(statementMonthFrom);
    String monthTo = normalizeMonth(statementMonthTo);
    List<FleetProfile> profiles = listProfiles(currentUser.getTenantId());
    Map<Long, Org> orgMap = loadOrgMapFromProfiles(profiles);
    Map<Long, List<FleetTransportPlan>> planMap =
        listPlans(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .filter(item -> matchDateMonthRange(item.getPlanDate(), monthFrom, monthTo))
            .collect(Collectors.groupingBy(FleetTransportPlan::getFleetId));
    Map<Long, List<FleetDispatchOrder>> dispatchMap =
        listDispatchOrders(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .filter(item -> matchDateMonthRange(item.getApplyDate(), monthFrom, monthTo))
            .collect(Collectors.groupingBy(FleetDispatchOrder::getFleetId));
    Map<Long, List<FleetFinanceRecord>> financeMap =
        listFinanceRecords(currentUser.getTenantId()).stream()
            .filter(item -> item.getFleetId() != null)
            .filter(item -> matchMonthRange(item.getStatementMonth(), monthFrom, monthTo))
            .collect(Collectors.groupingBy(FleetFinanceRecord::getFleetId));
    List<FleetReportItemDto> rows = new ArrayList<>();
    for (FleetProfile profile : profiles) {
      if (orgId != null && !Objects.equals(profile.getOrgId(), orgId)) {
        continue;
      }
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
      if (matchesReportKeyword(dto, trimToNull(keyword))) {
        rows.add(dto);
      }
    }
    rows.sort(
        Comparator.comparing(FleetReportItemDto::getRevenueAmount, Comparator.nullsLast(BigDecimal::compareTo))
            .reversed()
            .thenComparing(FleetReportItemDto::getFleetName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(rows);
  }

  @GetMapping("/report/export")
  public ResponseEntity<byte[]> exportReport(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String statementMonthFrom,
      @RequestParam(required = false) String statementMonthTo,
      HttpServletRequest request) {
    List<FleetReportItemDto> rows =
        report(keyword, orgId, statementMonthFrom, statementMonthTo, request).getData();
    String csv = buildReportCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fleet_reports.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/tracking/summary")
  public ApiResult<FleetTrackingSummaryDto> trackingSummary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadTrackingSummary(currentUser.getTenantId(), keyword, fleetId, status));
  }

  @GetMapping("/tracking")
  public ApiResult<PageResult<FleetTrackingItemDto>> tracking(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<FleetTrackingItemDto> rows =
        loadTrackingRows(currentUser.getTenantId(), keyword, fleetId, status);
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/tracking/{vehicleId}/history")
  public ApiResult<FleetTrackingHistoryDto> trackingHistory(
      @PathVariable Long vehicleId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) Long fleetId,
      @RequestParam(defaultValue = "10") long minStopMinutes,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = requireVehicle(vehicleId, currentUser.getTenantId());
    FleetProfile requiredProfile = fleetId != null ? requireProfile(fleetId, currentUser.getTenantId()) : null;
    if (requiredProfile != null && !matchesFleetProfile(vehicle, requiredProfile)) {
      throw new BizException(400, "该车辆不属于当前车队");
    }
    List<FleetProfile> profiles = listProfiles(currentUser.getTenantId());
    Map<String, FleetProfile> profileMap = loadFleetProfileKeyMap(profiles);
    FleetProfile profile = resolveFleetProfile(profileMap, vehicle);
    LocalDateTime start = parseDateTime(startTime);
    LocalDateTime end = parseDateTime(endTime);
    if (start != null && end != null && start.isAfter(end)) {
      throw new BizException(400, "开始时间不能晚于结束时间");
    }
    List<VehicleTrackPoint> points = loadTrackPoints(currentUser.getTenantId(), vehicle, start, end);
    FleetDispatchOrder dispatch =
        resolveActiveDispatch(
            profile != null
                ? listDispatchOrders(currentUser.getTenantId()).stream()
                    .filter(item -> Objects.equals(item.getFleetId(), profile.getId()))
                    .toList()
                : Collections.emptyList());
    FleetTransportPlan plan =
        resolveTrackingPlan(
            dispatch,
            profile,
            profile != null
                ? listPlans(currentUser.getTenantId()).stream()
                    .filter(item -> Objects.equals(item.getFleetId(), profile.getId()))
                    .toList()
                : Collections.emptyList());
    return ApiResult.ok(
        toTrackingHistoryDto(vehicle, profile, dispatch, plan, points, Math.max(minStopMinutes, 1L)));
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

  private List<Vehicle> listTrackingVehicles(Long tenantId) {
    return vehicleMapper.selectList(
        new LambdaQueryWrapper<Vehicle>()
            .eq(Vehicle::getTenantId, tenantId)
            .isNotNull(Vehicle::getFleetName)
            .orderByDesc(Vehicle::getGpsTime)
            .orderByDesc(Vehicle::getUpdateTime)
            .orderByDesc(Vehicle::getId));
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
      Long tenantId,
      String keyword,
      String status,
      Long fleetId,
      String contractNo,
      String statementMonthFrom,
      String statementMonthTo,
      boolean unsettledOnly) {
    List<FleetFinanceRecord> rows = listFinanceRecords(tenantId);
    Map<Long, FleetProfile> fleetMap = loadFleetMap(rows.stream().map(FleetFinanceRecord::getFleetId).toList(), tenantId);
    Map<Long, Org> orgMap = loadOrgMapFromFleetMap(fleetMap);
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    String contractValue = trimToNull(contractNo);
    return rows.stream()
        .filter(item -> fleetId == null || Objects.equals(item.getFleetId(), fleetId))
        .filter(item -> !StringUtils.hasText(contractValue) || contains(item.getContractNo(), contractValue))
        .filter(item -> matchMonthRange(item.getStatementMonth(), statementMonthFrom, statementMonthTo))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .map(item -> toFinanceDto(item, fleetMap.get(item.getFleetId()), orgMap))
        .filter(item -> !unsettledOnly || defaultDecimal(item.getOutstandingAmount()).compareTo(ZERO) > 0)
        .filter(
            item ->
                !StringUtils.hasText(keywordValue)
                    || contains(item.getFleetName(), keywordValue)
                    || contains(item.getRecordNo(), keywordValue)
                    || contains(item.getContractNo(), keywordValue)
                    || contains(item.getStatementMonth(), keywordValue))
        .toList();
  }

  private FleetFinanceSummaryDto buildFinanceSummary(List<FleetFinanceRecordListItemDto> rows) {
    return new FleetFinanceSummaryDto(
        rows.size(),
        (int) rows.stream().filter(item -> "SETTLED".equalsIgnoreCase(item.getStatus())).count(),
        rows.stream()
            .map(FleetFinanceRecordListItemDto::getRevenueAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .map(item -> defaultDecimal(item.getCostAmount()).add(defaultDecimal(item.getOtherAmount())))
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .map(FleetFinanceRecordListItemDto::getProfitAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .map(FleetFinanceRecordListItemDto::getOutstandingAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add));
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

  private FleetTrackingSummaryDto loadTrackingSummary(
      Long tenantId, String keyword, Long fleetId, String status) {
    List<FleetTrackingItemDto> rows = loadTrackingRows(tenantId, keyword, fleetId, status);
    return new FleetTrackingSummaryDto(
        rows.size(),
        (int) rows.stream().filter(item -> "MOVING".equalsIgnoreCase(item.getTrackingStatus())).count(),
        (int) rows.stream().filter(item -> "STOPPED".equalsIgnoreCase(item.getTrackingStatus())).count(),
        (int) rows.stream().filter(item -> "OFFLINE".equalsIgnoreCase(item.getTrackingStatus())).count(),
        (int)
            rows.stream()
                .filter(
                    item ->
                        "APPROVED".equalsIgnoreCase(item.getDispatchStatus())
                            || "IN_PROGRESS".equalsIgnoreCase(item.getDispatchStatus()))
                .count(),
        (int) rows.stream().filter(item -> StringUtils.hasText(item.getWarningLabel())).count());
  }

  private List<FleetTrackingItemDto> loadTrackingRows(
      Long tenantId, String keyword, Long fleetId, String status) {
    List<FleetProfile> profiles = listProfiles(tenantId);
    FleetProfile requiredProfile = fleetId != null ? requireProfile(fleetId, tenantId) : null;
    Map<String, FleetProfile> profileMap = loadFleetProfileKeyMap(profiles);
    List<Vehicle> vehicles = listTrackingVehicles(tenantId);
    Map<Long, Org> vehicleOrgMap = loadVehicleOrgMap(vehicles);
    Map<Long, List<FleetDispatchOrder>> dispatchMap =
        listDispatchOrders(tenantId).stream()
            .filter(item -> item.getFleetId() != null)
            .collect(Collectors.groupingBy(FleetDispatchOrder::getFleetId));
    Map<Long, List<FleetTransportPlan>> planMap =
        listPlans(tenantId).stream()
            .filter(item -> item.getFleetId() != null)
            .collect(Collectors.groupingBy(FleetTransportPlan::getFleetId));
    String keywordValue = trimToNull(keyword);
    String statusValue = defaultValue(status, null);
    List<FleetTrackingItemDto> rows =
        vehicles.stream()
            .filter(item -> requiredProfile == null || matchesFleetProfile(item, requiredProfile))
            .map(
                item ->
                    toTrackingDto(
                        item,
                        resolveFleetProfile(profileMap, item),
                        vehicleOrgMap.get(item.getOrgId()),
                        dispatchMap,
                        planMap))
            .filter(item -> matchesTrackingKeyword(item, keywordValue))
            .filter(item -> matchesTrackingStatus(item, statusValue))
            .sorted(
                Comparator.<FleetTrackingItemDto>comparingInt(this::trackingSortScore)
                    .reversed()
                    .thenComparing(
                        FleetTrackingItemDto::getGpsTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        FleetTrackingItemDto::getPlateNo,
                        Comparator.nullsLast(String::compareTo)))
            .toList();
    return new ArrayList<>(rows);
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

  private Map<Long, Org> loadVehicleOrgMap(List<Vehicle> vehicles) {
    LinkedHashSet<Long> orgIds =
        vehicles.stream()
            .map(Vehicle::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private Map<String, FleetProfile> loadFleetProfileKeyMap(List<FleetProfile> profiles) {
    return profiles.stream()
        .filter(item -> item.getId() != null)
        .filter(item -> StringUtils.hasText(item.getFleetName()))
        .collect(
            Collectors.toMap(
                item -> buildFleetKey(item.getOrgId(), item.getFleetName()),
                Function.identity(),
                (left, right) -> left));
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

  private boolean matchesReportKeyword(FleetReportItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getFleetName(), keyword)
        || contains(item.getOrgName(), keyword);
  }

  private boolean matchMonthRange(String statementMonth, String from, String to) {
    if (!StringUtils.hasText(from) && !StringUtils.hasText(to)) {
      return true;
    }
    String normalized = normalizeMonth(statementMonth);
    if (!StringUtils.hasText(normalized)) {
      return false;
    }
    return (from == null || normalized.compareTo(from) >= 0)
        && (to == null || normalized.compareTo(to) <= 0);
  }

  private boolean matchDateMonthRange(LocalDate date, String from, String to) {
    if (!StringUtils.hasText(from) && !StringUtils.hasText(to)) {
      return true;
    }
    if (date == null) {
      return false;
    }
    return matchMonthRange(date.format(ISO_MONTH), from, to);
  }

  private FleetTrackingItemDto toTrackingDto(
      Vehicle vehicle,
      FleetProfile profile,
      Org org,
      Map<Long, List<FleetDispatchOrder>> dispatchMap,
      Map<Long, List<FleetTransportPlan>> planMap) {
    FleetDispatchOrder dispatch =
        profile != null
            ? resolveActiveDispatch(dispatchMap.getOrDefault(profile.getId(), Collections.emptyList()))
            : null;
    FleetTransportPlan plan =
        resolveTrackingPlan(
            dispatch,
            profile,
            profile != null ? planMap.getOrDefault(profile.getId(), Collections.emptyList()) : Collections.emptyList());
    String trackingStatus = resolveTrackingStatus(vehicle);
    FleetTrackingItemDto dto = new FleetTrackingItemDto();
    dto.setVehicleId(vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setOrgId(vehicle.getOrgId() != null ? String.valueOf(vehicle.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(org, vehicle.getOrgId()));
    dto.setFleetId(profile != null && profile.getId() != null ? String.valueOf(profile.getId()) : null);
    dto.setFleetName(
        StringUtils.hasText(vehicle.getFleetName())
            ? vehicle.getFleetName()
            : profile != null ? profile.getFleetName() : "未编组车队");
    dto.setDriverName(vehicle.getDriverName());
    dto.setDriverPhone(vehicle.getDriverPhone());
    dto.setTrackingStatus(trackingStatus);
    dto.setTrackingStatusLabel(resolveTrackingStatusLabel(trackingStatus));
    dto.setRunningStatus(defaultValue(vehicle.getRunningStatus(), "STOPPED"));
    dto.setRunningStatusLabel(resolveVehicleRunningStatusLabel(dto.getRunningStatus()));
    dto.setVehicleStatusLabel(resolveVehicleStatusLabel(vehicle.getStatus()));
    dto.setWarningLabel(hasVehicleWarning(vehicle) ? resolveVehicleWarningLabel(vehicle) : null);
    dto.setCurrentSpeed(defaultDecimal(vehicle.getCurrentSpeed()));
    dto.setCurrentMileage(defaultDecimal(vehicle.getCurrentMileage()));
    dto.setLng(vehicle.getLng());
    dto.setLat(vehicle.getLat());
    dto.setGpsTime(formatDateTime(vehicle.getGpsTime()));
    dto.setDispatchOrderNo(dispatch != null ? dispatch.getOrderNo() : null);
    dto.setDispatchStatus(dispatch != null ? defaultValue(dispatch.getStatus(), "PENDING_APPROVAL") : null);
    dto.setDispatchStatusLabel(
        dispatch != null ? resolveDispatchStatusLabel(dispatch.getStatus()) : null);
    dto.setRelatedPlanNo(plan != null ? plan.getPlanNo() : dispatch != null ? dispatch.getRelatedPlanNo() : null);
    dto.setSourcePoint(plan != null ? plan.getSourcePoint() : null);
    dto.setDestinationPoint(plan != null ? plan.getDestinationPoint() : null);
    dto.setCargoType(plan != null ? plan.getCargoType() : null);
    return dto;
  }

  private FleetDispatchOrder resolveActiveDispatch(List<FleetDispatchOrder> rows) {
    if (rows == null || rows.isEmpty()) {
      return null;
    }
    return rows.stream()
        .filter(item -> "IN_PROGRESS".equalsIgnoreCase(item.getStatus()))
        .findFirst()
        .or(() -> rows.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus())).findFirst())
        .or(() -> rows.stream().filter(item -> "PENDING_APPROVAL".equalsIgnoreCase(item.getStatus())).findFirst())
        .orElse(rows.get(0));
  }

  private FleetTransportPlan resolveTrackingPlan(
      FleetDispatchOrder dispatch, FleetProfile profile, List<FleetTransportPlan> rows) {
    if (rows == null || rows.isEmpty()) {
      return null;
    }
    if (dispatch != null && StringUtils.hasText(dispatch.getRelatedPlanNo())) {
      FleetTransportPlan matched =
          rows.stream()
              .filter(item -> dispatch.getRelatedPlanNo().equalsIgnoreCase(item.getPlanNo()))
              .findFirst()
              .orElse(null);
      if (matched != null) {
        return matched;
      }
    }
    return rows.stream()
        .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
        .findFirst()
        .or(() -> rows.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus())).findFirst())
        .orElse(rows.get(0));
  }

  private FleetTrackingHistoryDto toTrackingHistoryDto(
      Vehicle vehicle,
      FleetProfile profile,
      FleetDispatchOrder dispatch,
      FleetTransportPlan plan,
      List<VehicleTrackPoint> points,
      long minStopMinutes) {
    FleetTrackingHistoryDto dto = new FleetTrackingHistoryDto();
    dto.setVehicleId(vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setFleetId(profile != null && profile.getId() != null ? String.valueOf(profile.getId()) : null);
    dto.setFleetName(
        profile != null && StringUtils.hasText(profile.getFleetName())
            ? profile.getFleetName()
            : vehicle.getFleetName());
    dto.setStartTime(points.isEmpty() ? null : formatDateTime(points.get(0).getLocateTime()));
    dto.setEndTime(points.isEmpty() ? null : formatDateTime(points.get(points.size() - 1).getLocateTime()));
    dto.setDispatchOrderNo(dispatch != null ? dispatch.getOrderNo() : null);
    dto.setRelatedPlanNo(plan != null ? plan.getPlanNo() : dispatch != null ? dispatch.getRelatedPlanNo() : null);
    dto.setSourcePoint(plan != null ? plan.getSourcePoint() : null);
    dto.setDestinationPoint(plan != null ? plan.getDestinationPoint() : null);
    dto.setCargoType(plan != null ? plan.getCargoType() : null);
    dto.setPointCount(points.size());
    dto.setTotalDistanceKm(calculateTrackDistance(points));
    dto.setMaxSpeed(calculateMaxTrackSpeed(points));
    dto.setAverageSpeed(calculateAverageTrackSpeed(points));
    dto.setPoints(points.stream().map(this::toTrackPointDto).toList());
    dto.setStops(calculateStops(points, minStopMinutes));
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

  private Vehicle requireVehicle(Long id, Long tenantId) {
    Vehicle entity = vehicleMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "车辆不存在");
    }
    return entity;
  }

  private FleetProfile resolveFleetProfile(Map<String, FleetProfile> profileMap, Vehicle vehicle) {
    if (profileMap.isEmpty() || !StringUtils.hasText(vehicle.getFleetName())) {
      return null;
    }
    FleetProfile matched = profileMap.get(buildFleetKey(vehicle.getOrgId(), vehicle.getFleetName()));
    if (matched != null) {
      return matched;
    }
    return profileMap.get(buildFleetKey(null, vehicle.getFleetName()));
  }

  private boolean matchesFleetProfile(Vehicle vehicle, FleetProfile profile) {
    if (!StringUtils.hasText(vehicle.getFleetName()) || !StringUtils.hasText(profile.getFleetName())) {
      return false;
    }
    if (!vehicle.getFleetName().trim().equalsIgnoreCase(profile.getFleetName().trim())) {
      return false;
    }
    return profile.getOrgId() == null || Objects.equals(vehicle.getOrgId(), profile.getOrgId());
  }

  private String buildFleetKey(Long orgId, String fleetName) {
    return (orgId != null ? orgId : -1L) + "::" + fleetName.trim().toUpperCase();
  }

  private boolean matchesTrackingKeyword(FleetTrackingItemDto item, String keyword) {
    return !StringUtils.hasText(keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getFleetName(), keyword)
        || contains(item.getOrgName(), keyword)
        || contains(item.getDriverName(), keyword)
        || contains(item.getDispatchOrderNo(), keyword)
        || contains(item.getRelatedPlanNo(), keyword)
        || contains(item.getDestinationPoint(), keyword);
  }

  private boolean matchesTrackingStatus(FleetTrackingItemDto item, String status) {
    if (!StringUtils.hasText(status)) {
      return true;
    }
    return switch (status) {
      case "MOVING", "STOPPED", "OFFLINE" ->
          status.equalsIgnoreCase(item.getTrackingStatus());
      case "DELIVERING" ->
          "APPROVED".equalsIgnoreCase(item.getDispatchStatus())
              || "IN_PROGRESS".equalsIgnoreCase(item.getDispatchStatus());
      case "WARNING" -> StringUtils.hasText(item.getWarningLabel());
      default -> true;
    };
  }

  private int trackingSortScore(FleetTrackingItemDto item) {
    if ("IN_PROGRESS".equalsIgnoreCase(item.getDispatchStatus())) {
      return 400;
    }
    if ("APPROVED".equalsIgnoreCase(item.getDispatchStatus())) {
      return 300;
    }
    return switch (defaultValue(item.getTrackingStatus(), "STOPPED")) {
      case "MOVING" -> 200;
      case "STOPPED" -> 100;
      default -> 0;
    };
  }

  private <T> PageResult<T> paginate(List<T> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(safePageNo, safePageSize, rows.size(), rows.subList(fromIndex, toIndex));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
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

  private String resolveTrackingStatus(Vehicle vehicle) {
    if (vehicle.getStatus() != null && vehicle.getStatus() == 3) {
      return "OFFLINE";
    }
    if ("OFFLINE".equalsIgnoreCase(vehicle.getRunningStatus()) || isGpsExpired(vehicle)) {
      return "OFFLINE";
    }
    if ("MOVING".equalsIgnoreCase(vehicle.getRunningStatus())
        || (vehicle.getCurrentSpeed() != null && vehicle.getCurrentSpeed().compareTo(BigDecimal.ONE) > 0)) {
      return "MOVING";
    }
    return "STOPPED";
  }

  private String resolveTrackingStatusLabel(String status) {
    return switch (defaultValue(status, "STOPPED")) {
      case "MOVING" -> "行驶中";
      case "OFFLINE" -> "离线";
      default -> "停留中";
    };
  }

  private String resolveVehicleRunningStatusLabel(String status) {
    return switch (defaultValue(status, "STOPPED")) {
      case "MOVING" -> "行驶中";
      case "OFFLINE" -> "离线";
      default -> "静止";
    };
  }

  private String resolveVehicleStatusLabel(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 1 -> "在用";
      case 2 -> "维修";
      case 3 -> "禁用";
      case 4 -> "待命";
      case 5 -> "停用";
      default -> "状态" + status;
    };
  }

  private boolean hasVehicleWarning(Vehicle vehicle) {
    return !"正常".equals(resolveVehicleWarningLabel(vehicle));
  }

  private String resolveVehicleWarningLabel(Vehicle vehicle) {
    if (isGpsExpired(vehicle)) {
      return "定位超时";
    }
    List<LocalDate> dates = new ArrayList<>();
    if (vehicle.getNextMaintainDate() != null) {
      dates.add(vehicle.getNextMaintainDate());
    }
    if (vehicle.getAnnualInspectionExpireDate() != null) {
      dates.add(vehicle.getAnnualInspectionExpireDate());
    }
    if (vehicle.getInsuranceExpireDate() != null) {
      dates.add(vehicle.getInsuranceExpireDate());
    }
    if (dates.isEmpty()) {
      return "正常";
    }
    long minDays =
        dates.stream()
            .map(date -> ChronoUnit.DAYS.between(LocalDate.now(), date))
            .min(Long::compareTo)
            .orElse(999L);
    if (minDays < 0) {
      return "证照到期";
    }
    if (minDays <= 7) {
      return "7日内到期";
    }
    if (minDays <= 30) {
      return "30日内到期";
    }
    return "正常";
  }

  private boolean isGpsExpired(Vehicle vehicle) {
    if (vehicle.getGpsTime() == null) {
      return true;
    }
    return ChronoUnit.MINUTES.between(vehicle.getGpsTime(), LocalDateTime.now()) > 120;
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

  private String normalizeMonth(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim() + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          .format(ISO_MONTH);
    } catch (Exception ex) {
      throw new BizException(400, "月份格式错误，应为 yyyy-MM");
    }
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private String buildFinanceCsv(List<FleetFinanceRecordListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("结算单号,车队,所属单位,运输合同号,账期,收入,成本,其他费用,利润,已结算,未结金额,状态,备注\n");
    for (FleetFinanceRecordListItemDto row : rows) {
      builder
          .append(csv(row.getRecordNo())).append(',')
          .append(csv(row.getFleetName())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getContractNo())).append(',')
          .append(csv(row.getStatementMonth())).append(',')
          .append(defaultDecimal(row.getRevenueAmount())).append(',')
          .append(defaultDecimal(row.getCostAmount())).append(',')
          .append(defaultDecimal(row.getOtherAmount())).append(',')
          .append(defaultDecimal(row.getProfitAmount())).append(',')
          .append(defaultDecimal(row.getSettledAmount())).append(',')
          .append(defaultDecimal(row.getOutstandingAmount())).append(',')
          .append(csv(row.getStatusLabel())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
  }

  private String buildReportCsv(List<FleetReportItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("车队,所属单位,运输计划数,调度申请数,已批准调度,计划方量,收入,成本,利润\n");
    for (FleetReportItemDto row : rows) {
      builder
          .append(csv(row.getFleetName())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(row.getTotalPlans() != null ? row.getTotalPlans() : 0).append(',')
          .append(row.getTotalDispatchOrders() != null ? row.getTotalDispatchOrders() : 0).append(',')
          .append(row.getApprovedDispatchOrders() != null ? row.getApprovedDispatchOrders() : 0).append(',')
          .append(defaultDecimal(row.getPlannedVolume())).append(',')
          .append(defaultDecimal(row.getRevenueAmount())).append(',')
          .append(defaultDecimal(row.getCostAmount())).append(',')
          .append(defaultDecimal(row.getProfitAmount())).append('\n');
    }
    return builder.toString();
  }

  private String csv(String value) {
    if (value == null) {
      return "";
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      if (value.contains("T")) {
        return LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
      return LocalDateTime.parse(value.trim(), ISO_DATE_TIME);
    } catch (Exception ex) {
      throw new BizException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
    }
  }

  private List<VehicleTrackPoint> loadTrackPoints(
      Long tenantId, Vehicle vehicle, LocalDateTime start, LocalDateTime end) {
    List<VehicleTrackPoint> rows =
        vehicleTrackPointMapper.selectList(
            new LambdaQueryWrapper<VehicleTrackPoint>()
                .eq(VehicleTrackPoint::getTenantId, tenantId)
                .eq(VehicleTrackPoint::getVehicleId, vehicle.getId())
                .ge(start != null, VehicleTrackPoint::getLocateTime, start)
                .le(end != null, VehicleTrackPoint::getLocateTime, end)
                .orderByAsc(VehicleTrackPoint::getLocateTime)
                .orderByAsc(VehicleTrackPoint::getId));
    if (!rows.isEmpty() || vehicle.getLng() == null || vehicle.getLat() == null) {
      return rows;
    }
    LocalDateTime baseTime = vehicle.getGpsTime() != null ? vehicle.getGpsTime() : LocalDateTime.now();
    if (start != null && baseTime.isBefore(start)) {
      return Collections.emptyList();
    }
    if (end != null && baseTime.isAfter(end)) {
      return Collections.emptyList();
    }
    VehicleTrackPoint fallback = new VehicleTrackPoint();
    fallback.setId(-1L);
    fallback.setTenantId(tenantId);
    fallback.setVehicleId(vehicle.getId());
    fallback.setPlateNo(vehicle.getPlateNo());
    fallback.setLng(vehicle.getLng());
    fallback.setLat(vehicle.getLat());
    fallback.setSpeed(vehicle.getCurrentSpeed());
    fallback.setDirection(BigDecimal.ZERO);
    fallback.setLocateTime(baseTime);
    fallback.setSourceType("REALTIME");
    fallback.setRemark("当前定位");
    return List.of(fallback);
  }

  private VehicleTrackPointDto toTrackPointDto(VehicleTrackPoint point) {
    VehicleTrackPointDto dto = new VehicleTrackPointDto();
    dto.setId(point.getId() != null ? String.valueOf(point.getId()) : null);
    dto.setLng(point.getLng());
    dto.setLat(point.getLat());
    dto.setSpeed(point.getSpeed());
    dto.setDirection(point.getDirection());
    dto.setLocateTime(formatDateTime(point.getLocateTime()));
    dto.setSourceType(point.getSourceType());
    dto.setRemark(point.getRemark());
    return dto;
  }

  private List<FleetTrackingStopDto> calculateStops(List<VehicleTrackPoint> points, long minStopMinutes) {
    if (points.size() < 2) {
      return Collections.emptyList();
    }
    List<FleetTrackingStopDto> rows = new ArrayList<>();
    VehicleTrackPoint stopStart = null;
    for (VehicleTrackPoint point : points) {
      boolean stopped = point.getSpeed() == null || point.getSpeed().compareTo(BigDecimal.ONE) <= 0;
      if (stopped && stopStart == null) {
        stopStart = point;
      } else if (!stopped && stopStart != null) {
        long duration = ChronoUnit.MINUTES.between(stopStart.getLocateTime(), point.getLocateTime());
        if (duration >= minStopMinutes) {
          rows.add(
              new FleetTrackingStopDto(
                  formatDateTime(stopStart.getLocateTime()),
                  formatDateTime(point.getLocateTime()),
                  duration,
                  stopStart.getLng(),
                  stopStart.getLat(),
                  stopStart.getRemark()));
        }
        stopStart = null;
      }
    }
    if (stopStart != null) {
      VehicleTrackPoint last = points.get(points.size() - 1);
      long duration = ChronoUnit.MINUTES.between(stopStart.getLocateTime(), last.getLocateTime());
      if (duration >= minStopMinutes) {
        rows.add(
            new FleetTrackingStopDto(
                formatDateTime(stopStart.getLocateTime()),
                formatDateTime(last.getLocateTime()),
                duration,
                stopStart.getLng(),
                stopStart.getLat(),
                stopStart.getRemark()));
      }
    }
    rows.sort(
        Comparator.comparing(
                FleetTrackingStopDto::getStartTime, Comparator.nullsLast(String::compareTo))
            .reversed());
    return rows;
  }

  private BigDecimal calculateTrackDistance(List<VehicleTrackPoint> points) {
    if (points.size() < 2) {
      return ZERO;
    }
    double total = 0D;
    for (int index = 1; index < points.size(); index++) {
      total += haversineKm(points.get(index - 1), points.get(index));
    }
    return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateMaxTrackSpeed(List<VehicleTrackPoint> points) {
    return points.stream()
        .map(VehicleTrackPoint::getSpeed)
        .filter(Objects::nonNull)
        .max(BigDecimal::compareTo)
        .orElse(ZERO)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateAverageTrackSpeed(List<VehicleTrackPoint> points) {
    List<BigDecimal> speeds =
        points.stream().map(VehicleTrackPoint::getSpeed).filter(Objects::nonNull).toList();
    if (speeds.isEmpty()) {
      return ZERO;
    }
    BigDecimal total = speeds.stream().reduce(ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(speeds.size()), 2, RoundingMode.HALF_UP);
  }

  private double haversineKm(VehicleTrackPoint left, VehicleTrackPoint right) {
    if (left.getLng() == null
        || left.getLat() == null
        || right.getLng() == null
        || right.getLat() == null) {
      return 0D;
    }
    double earthRadiusKm = 6371D;
    double deltaLat = Math.toRadians(right.getLat().doubleValue() - left.getLat().doubleValue());
    double deltaLng = Math.toRadians(right.getLng().doubleValue() - left.getLng().doubleValue());
    double startLat = Math.toRadians(left.getLat().doubleValue());
    double endLat = Math.toRadians(right.getLat().doubleValue());
    double a =
        Math.sin(deltaLat / 2D) * Math.sin(deltaLat / 2D)
            + Math.cos(startLat)
                * Math.cos(endLat)
                * Math.sin(deltaLng / 2D)
                * Math.sin(deltaLng / 2D);
    return earthRadiusKm * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
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
