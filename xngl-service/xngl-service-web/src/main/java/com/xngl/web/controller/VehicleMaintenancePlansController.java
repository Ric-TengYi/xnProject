package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleMaintenancePlan;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleMaintenanceRecord;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMaintenancePlanMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMaintenanceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleMaintenanceExecuteDto;
import com.xngl.web.dto.vehicle.VehicleMaintenancePlanListItemDto;
import com.xngl.web.dto.vehicle.VehicleMaintenancePlanSummaryDto;
import com.xngl.web.dto.vehicle.VehicleMaintenancePlanUpsertDto;
import com.xngl.web.dto.vehicle.VehicleMaintenanceRecordListItemDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@RequestMapping("/api/vehicle-maintenance-plans")
public class VehicleMaintenancePlansController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final VehicleMaintenancePlanMapper planMapper;
  private final VehicleMaintenanceRecordMapper recordMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserService userService;

  public VehicleMaintenancePlansController(
      VehicleMaintenancePlanMapper planMapper,
      VehicleMaintenanceRecordMapper recordMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserService userService) {
    this.planMapper = planMapper;
    this.recordMapper = recordMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleMaintenancePlanListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleMaintenancePlanListItemDto> rows =
        new ArrayList<>(loadPlans(currentUser.getTenantId(), keyword, status, orgId, vehicleId));
    rows.sort(
        Comparator.comparing(
                VehicleMaintenancePlanListItemDto::getNextMaintainDate,
                Comparator.nullsLast(String::compareTo))
            .thenComparing(
                VehicleMaintenancePlanListItemDto::getPlanNo, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleMaintenancePlanSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleMaintenancePlanListItemDto> plans =
        loadPlans(currentUser.getTenantId(), keyword, status, orgId, vehicleId);
    List<VehicleMaintenanceRecordListItemDto> records =
        loadRecords(currentUser.getTenantId(), keyword, orgId, vehicleId);
    return ApiResult.ok(
        new VehicleMaintenancePlanSummaryDto(
            plans.size(),
            (int) plans.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count(),
            (int) plans.stream().filter(item -> Boolean.TRUE.equals(item.getOverdue())).count(),
            (int) plans.stream().filter(item -> "PAUSED".equalsIgnoreCase(item.getStatus())).count(),
            records.size(),
            records.stream()
                .map(VehicleMaintenanceRecordListItemDto::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)));
  }

  @GetMapping("/records")
  public ApiResult<PageResult<VehicleMaintenanceRecordListItemDto>> records(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleMaintenanceRecordListItemDto> rows =
        new ArrayList<>(loadRecords(currentUser.getTenantId(), keyword, orgId, vehicleId));
    rows.sort(
        Comparator.comparing(
                VehicleMaintenanceRecordListItemDto::getServiceDate,
                Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginateRecords(rows, pageNo, pageSize));
  }

  @PostMapping
  public ApiResult<VehicleMaintenancePlanListItemDto> create(
      @RequestBody VehicleMaintenancePlanUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validatePlan(body, currentUser.getTenantId());
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    VehicleMaintenancePlan entity = new VehicleMaintenancePlan();
    entity.setTenantId(currentUser.getTenantId());
    entity.setVehicleId(vehicle.getId());
    entity.setOrgId(vehicle.getOrgId());
    entity.setPlanNo("MPLAN-" + System.currentTimeMillis());
    applyPlan(entity, body);
    planMapper.insert(entity);
    return ApiResult.ok(loadPlanDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleMaintenancePlanListItemDto> update(
      @PathVariable Long id,
      @RequestBody VehicleMaintenancePlanUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validatePlan(body, currentUser.getTenantId());
    VehicleMaintenancePlan entity = requirePlan(id, currentUser.getTenantId());
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    entity.setVehicleId(vehicle.getId());
    entity.setOrgId(vehicle.getOrgId());
    applyPlan(entity, body);
    planMapper.updateById(entity);
    return ApiResult.ok(loadPlanDto(entity.getId(), currentUser.getTenantId()));
  }

  @PostMapping("/{id}/execute")
  public ApiResult<VehicleMaintenanceRecordListItemDto> execute(
      @PathVariable Long id,
      @RequestBody VehicleMaintenanceExecuteDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleMaintenancePlan plan = requirePlan(id, currentUser.getTenantId());
    Vehicle vehicle = requireVehicle(plan.getVehicleId(), currentUser.getTenantId());
    VehicleMaintenanceRecord record = new VehicleMaintenanceRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setPlanId(plan.getId());
    record.setVehicleId(plan.getVehicleId());
    record.setOrgId(plan.getOrgId());
    record.setRecordNo("MREC-" + System.currentTimeMillis());
    record.setMaintainType(plan.getPlanType());
    record.setServiceDate(body != null && body.getServiceDate() != null ? body.getServiceDate() : LocalDate.now());
    record.setOdometer(body != null && body.getOdometer() != null ? body.getOdometer() : defaultDecimal(vehicle.getCurrentMileage()));
    record.setVendorName(body != null ? trimToNull(body.getVendorName()) : null);
    record.setCostAmount(body != null ? defaultDecimal(body.getCostAmount()) : ZERO);
    record.setItems(body != null ? trimToNull(body.getItems()) : null);
    record.setOperatorName(body != null ? trimToNull(body.getOperatorName()) : null);
    record.setStatus(defaultStatus(body != null ? body.getStatus() : null, "DONE"));
    record.setRemark(body != null ? trimToNull(body.getRemark()) : null);
    recordMapper.insert(record);

    plan.setLastMaintainDate(record.getServiceDate());
    plan.setLastOdometer(record.getOdometer());
    plan.setNextMaintainDate(resolveNextMaintainDate(plan, body, record.getServiceDate()));
    plan.setNextOdometer(resolveNextOdometer(plan, body, record.getOdometer()));
    plan.setStatus("ACTIVE");
    planMapper.updateById(plan);
    return ApiResult.ok(loadRecordDto(record.getId(), currentUser.getTenantId()));
  }

  private List<VehicleMaintenancePlanListItemDto> loadPlans(
      Long tenantId, String keyword, String status, Long orgId, Long vehicleId) {
    List<VehicleMaintenancePlan> rows =
        planMapper.selectList(
            new LambdaQueryWrapper<VehicleMaintenancePlan>()
                .eq(VehicleMaintenancePlan::getTenantId, tenantId)
                .eq(vehicleId != null, VehicleMaintenancePlan::getVehicleId, vehicleId)
                .orderByAsc(VehicleMaintenancePlan::getNextMaintainDate)
                .orderByDesc(VehicleMaintenancePlan::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(rows.stream().map(VehicleMaintenancePlan::getVehicleId).toList());
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    String keywordValue = trimToNull(keyword);
    String statusValue = trimToNull(status);
    return rows.stream()
        .map(item -> toPlanDto(item, vehicleMap.get(item.getVehicleId()), orgMap))
        .filter(item -> matchPlanKeyword(item, keywordValue))
        .filter(item -> !StringUtils.hasText(statusValue) || statusValue.equalsIgnoreCase(item.getStatus()))
        .filter(item -> orgId == null || Objects.equals(parseLong(item.getOrgId()), orgId))
        .toList();
  }

  private List<VehicleMaintenanceRecordListItemDto> loadRecords(
      Long tenantId, String keyword, Long orgId, Long vehicleId) {
    List<VehicleMaintenanceRecord> rows =
        recordMapper.selectList(
            new LambdaQueryWrapper<VehicleMaintenanceRecord>()
                .eq(VehicleMaintenanceRecord::getTenantId, tenantId)
                .eq(vehicleId != null, VehicleMaintenanceRecord::getVehicleId, vehicleId)
                .orderByDesc(VehicleMaintenanceRecord::getServiceDate)
                .orderByDesc(VehicleMaintenanceRecord::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(rows.stream().map(VehicleMaintenanceRecord::getVehicleId).toList());
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    Map<Long, VehicleMaintenancePlan> planMap = loadPlanMap(rows.stream().map(VehicleMaintenanceRecord::getPlanId).toList(), tenantId);
    String keywordValue = trimToNull(keyword);
    return rows.stream()
        .map(item -> toRecordDto(item, vehicleMap.get(item.getVehicleId()), orgMap, planMap.get(item.getPlanId())))
        .filter(item -> matchRecordKeyword(item, keywordValue))
        .filter(item -> orgId == null || Objects.equals(parseLong(item.getOrgId()), orgId))
        .toList();
  }

  private VehicleMaintenancePlanListItemDto loadPlanDto(Long id, Long tenantId) {
    VehicleMaintenancePlan entity = requirePlan(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity.getVehicleId()));
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    return toPlanDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap);
  }

  private VehicleMaintenanceRecordListItemDto loadRecordDto(Long id, Long tenantId) {
    VehicleMaintenanceRecord entity = requireRecord(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity.getVehicleId()));
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    Map<Long, VehicleMaintenancePlan> planMap = loadPlanMap(List.of(entity.getPlanId()), tenantId);
    return toRecordDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap, planMap.get(entity.getPlanId()));
  }

  private Map<Long, Vehicle> loadVehicleMap(List<Long> ids) {
    LinkedHashSet<Long> values = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (values.isEmpty()) {
      return Collections.emptyMap();
    }
    return vehicleMapper.selectBatchIds(values).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadOrgMap(List<Vehicle> vehicles) {
    LinkedHashSet<Long> values = vehicles.stream().map(Vehicle::getOrgId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (values.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(values).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, VehicleMaintenancePlan> loadPlanMap(List<Long> ids, Long tenantId) {
    LinkedHashSet<Long> values = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (values.isEmpty()) {
      return Collections.emptyMap();
    }
    return planMapper.selectList(
            new LambdaQueryWrapper<VehicleMaintenancePlan>()
                .eq(VehicleMaintenancePlan::getTenantId, tenantId)
                .in(VehicleMaintenancePlan::getId, values))
        .stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(VehicleMaintenancePlan::getId, Function.identity(), (left, right) -> left));
  }

  private VehicleMaintenancePlanListItemDto toPlanDto(
      VehicleMaintenancePlan entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehicleMaintenancePlanListItemDto dto = new VehicleMaintenancePlanListItemDto();
    dto.setId(String.valueOf(entity.getId()));
    dto.setPlanNo(entity.getPlanNo());
    dto.setVehicleId(entity.getVehicleId() != null ? String.valueOf(entity.getVehicleId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setPlanType(entity.getPlanType());
    dto.setCycleType(entity.getCycleType());
    dto.setCycleValue(entity.getCycleValue());
    dto.setLastMaintainDate(formatDate(entity.getLastMaintainDate()));
    dto.setNextMaintainDate(formatDate(entity.getNextMaintainDate()));
    dto.setLastOdometer(defaultDecimal(entity.getLastOdometer()));
    dto.setNextOdometer(entity.getNextOdometer());
    dto.setResponsibleName(entity.getResponsibleName());
    String status = defaultStatus(entity.getStatus(), "ACTIVE");
    dto.setStatus(status);
    dto.setStatusLabel(resolvePlanStatusLabel(status));
    dto.setOverdue(entity.getNextMaintainDate() != null && entity.getNextMaintainDate().isBefore(LocalDate.now()) && "ACTIVE".equalsIgnoreCase(status));
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private VehicleMaintenanceRecordListItemDto toRecordDto(
      VehicleMaintenanceRecord entity,
      Vehicle vehicle,
      Map<Long, Org> orgMap,
      VehicleMaintenancePlan plan) {
    VehicleMaintenanceRecordListItemDto dto = new VehicleMaintenanceRecordListItemDto();
    dto.setId(String.valueOf(entity.getId()));
    dto.setRecordNo(entity.getRecordNo());
    dto.setPlanId(entity.getPlanId() != null ? String.valueOf(entity.getPlanId()) : null);
    dto.setPlanNo(plan != null ? plan.getPlanNo() : null);
    dto.setVehicleId(entity.getVehicleId() != null ? String.valueOf(entity.getVehicleId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    dto.setMaintainType(entity.getMaintainType());
    dto.setServiceDate(formatDate(entity.getServiceDate()));
    dto.setOdometer(defaultDecimal(entity.getOdometer()));
    dto.setVendorName(entity.getVendorName());
    dto.setCostAmount(defaultDecimal(entity.getCostAmount()));
    dto.setItems(entity.getItems());
    dto.setOperatorName(entity.getOperatorName());
    String status = defaultStatus(entity.getStatus(), "DONE");
    dto.setStatus(status);
    dto.setStatusLabel(resolveRecordStatusLabel(status));
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private boolean matchPlanKeyword(VehicleMaintenancePlanListItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getPlanNo(), keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getOrgName(), keyword)
        || contains(item.getPlanType(), keyword)
        || contains(item.getResponsibleName(), keyword);
  }

  private boolean matchRecordKeyword(VehicleMaintenanceRecordListItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getRecordNo(), keyword)
        || contains(item.getPlanNo(), keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getMaintainType(), keyword)
        || contains(item.getVendorName(), keyword)
        || contains(item.getOperatorName(), keyword);
  }

  private void validatePlan(VehicleMaintenancePlanUpsertDto body, Long tenantId) {
    if (body == null || body.getVehicleId() == null) {
      throw new BizException(400, "请选择车辆");
    }
    requireVehicle(body.getVehicleId(), tenantId);
    if (!StringUtils.hasText(body.getPlanType())) {
      throw new BizException(400, "维保类型不能为空");
    }
    if (!StringUtils.hasText(body.getCycleType())) {
      throw new BizException(400, "维保周期类型不能为空");
    }
    if (body.getCycleValue() == null || body.getCycleValue() <= 0) {
      throw new BizException(400, "维保周期值必须大于 0");
    }
  }

  private void applyPlan(VehicleMaintenancePlan entity, VehicleMaintenancePlanUpsertDto body) {
    entity.setPlanType(body.getPlanType().trim());
    entity.setCycleType(defaultStatus(body.getCycleType(), "MONTH"));
    entity.setCycleValue(body.getCycleValue());
    entity.setLastMaintainDate(body.getLastMaintainDate());
    entity.setNextMaintainDate(body.getNextMaintainDate());
    entity.setLastOdometer(defaultDecimal(body.getLastOdometer()));
    entity.setNextOdometer(body.getNextOdometer());
    entity.setResponsibleName(trimToNull(body.getResponsibleName()));
    entity.setStatus(defaultStatus(body.getStatus(), "ACTIVE"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private LocalDate resolveNextMaintainDate(
      VehicleMaintenancePlan plan, VehicleMaintenanceExecuteDto body, LocalDate serviceDate) {
    if (body != null && body.getNextMaintainDate() != null) {
      return body.getNextMaintainDate();
    }
    String cycleType = defaultStatus(plan.getCycleType(), "MONTH");
    Integer cycleValue = plan.getCycleValue();
    if (serviceDate == null || cycleValue == null) {
      return plan.getNextMaintainDate();
    }
    return switch (cycleType) {
      case "DAY" -> serviceDate.plusDays(cycleValue);
      case "MONTH" -> serviceDate.plusMonths(cycleValue.longValue());
      default -> plan.getNextMaintainDate();
    };
  }

  private BigDecimal resolveNextOdometer(
      VehicleMaintenancePlan plan, VehicleMaintenanceExecuteDto body, BigDecimal odometer) {
    if (body != null && body.getNextOdometer() != null) {
      return body.getNextOdometer();
    }
    if ("KM".equalsIgnoreCase(plan.getCycleType()) && odometer != null && plan.getCycleValue() != null) {
      return odometer.add(BigDecimal.valueOf(plan.getCycleValue()));
    }
    return plan.getNextOdometer();
  }

  private VehicleMaintenancePlan requirePlan(Long id, Long tenantId) {
    VehicleMaintenancePlan entity = planMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "维保计划不存在");
    }
    return entity;
  }

  private VehicleMaintenanceRecord requireRecord(Long id, Long tenantId) {
    VehicleMaintenanceRecord entity = recordMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "维保记录不存在");
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

  private PageResult<VehicleMaintenancePlanListItemDto> paginate(
      List<VehicleMaintenancePlanListItemDto> rows, int pageNo, int pageSize) {
    return paginateRows(rows, pageNo, pageSize);
  }

  private PageResult<VehicleMaintenanceRecordListItemDto> paginateRecords(
      List<VehicleMaintenanceRecordListItemDto> rows, int pageNo, int pageSize) {
    return paginateRows(rows, pageNo, pageSize);
  }

  private <T> PageResult<T> paginateRows(List<T> rows, int pageNo, int pageSize) {
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

  private String resolvePlanStatusLabel(String status) {
    return switch (defaultStatus(status, "ACTIVE")) {
      case "PAUSED" -> "已暂停";
      case "COMPLETED" -> "已完成";
      default -> "执行中";
    };
  }

  private String resolveRecordStatusLabel(String status) {
    return switch (defaultStatus(status, "DONE")) {
      case "PENDING" -> "待确认";
      case "CANCELLED" -> "已取消";
      default -> "已执行";
    };
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }

  private String defaultStatus(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
