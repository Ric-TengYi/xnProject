package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleCompanyCapacityDto;
import com.xngl.web.dto.vehicle.VehicleDetailDto;
import com.xngl.web.dto.vehicle.VehicleFleetSummaryDto;
import com.xngl.web.dto.vehicle.VehicleListItemDto;
import com.xngl.web.dto.vehicle.VehicleStatsDto;
import com.xngl.web.dto.vehicle.VehicleUpsertDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/vehicles")
public class VehiclesController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserService userService;

  public VehiclesController(
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserService userService) {
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String useStatus,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    LambdaQueryWrapper<Vehicle> query =
        new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getTenantId, currentUser.getTenantId());
    if (status != null) {
      query.eq(Vehicle::getStatus, status);
    }
    if (orgId != null) {
      query.eq(Vehicle::getOrgId, orgId);
    }
    if (StringUtils.hasText(useStatus)) {
      query.eq(Vehicle::getUseStatus, useStatus.trim().toUpperCase());
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      LinkedHashSet<Long> orgIds = findOrgIdsByKeyword(currentUser.getTenantId(), effectiveKeyword);
      query.and(
          wrapper -> {
            wrapper
                .like(Vehicle::getPlateNo, effectiveKeyword)
                .or()
                .like(Vehicle::getVin, effectiveKeyword)
                .or()
                .like(Vehicle::getDriverName, effectiveKeyword)
                .or()
                .like(Vehicle::getFleetName, effectiveKeyword)
                .or()
                .like(Vehicle::getBrand, effectiveKeyword)
                .or()
                .like(Vehicle::getModel, effectiveKeyword);
            if (!orgIds.isEmpty()) {
              wrapper.or().in(Vehicle::getOrgId, orgIds);
            }
          });
    }
    query.orderByDesc(Vehicle::getUpdateTime).orderByDesc(Vehicle::getId);
    IPage<Vehicle> page = vehicleMapper.selectPage(new Page<>(pageNo, pageSize), query);
    Map<Long, Org> orgMap = loadOrgMap(page.getRecords());
    List<VehicleListItemDto> records =
        page.getRecords().stream()
            .map(vehicle -> toListItem(vehicle, orgMap.get(vehicle.getOrgId())))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<VehicleDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "车辆不存在");
    }
    Org org = vehicle.getOrgId() != null ? orgMapper.selectById(vehicle.getOrgId()) : null;
    return ApiResult.ok(toDetail(vehicle, org));
  }

  @GetMapping("/stats")
  public ApiResult<VehicleStatsDto> stats(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(toStats(listTenantVehicles(currentUser.getTenantId())));
  }

  @GetMapping("/company-capacity")
  public ApiResult<List<VehicleCompanyCapacityDto>> companyCapacity(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Vehicle> vehicles = listTenantVehicles(currentUser.getTenantId());
    Map<Long, Org> orgMap = loadOrgMap(vehicles);
    Map<Long, List<Vehicle>> grouped =
        vehicles.stream()
            .collect(
                Collectors.groupingBy(
                    vehicle -> vehicle.getOrgId() != null ? vehicle.getOrgId() : -1L,
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<VehicleCompanyCapacityDto> records = new ArrayList<>();
    for (Map.Entry<Long, List<Vehicle>> entry : grouped.entrySet()) {
      Long currentOrgId = entry.getKey();
      List<Vehicle> currentVehicles = entry.getValue();
      Org org = currentOrgId != null && currentOrgId > 0 ? orgMap.get(currentOrgId) : null;
      VehicleCompanyCapacityDto dto = new VehicleCompanyCapacityDto();
      dto.setOrgId(currentOrgId != null && currentOrgId > 0 ? String.valueOf(currentOrgId) : null);
      dto.setOrgName(resolveOrgName(org, currentOrgId));
      dto.setTotalVehicles(currentVehicles.size());
      dto.setActiveVehicles(countByStatus(currentVehicles, 1));
      dto.setMovingVehicles(countByRunningStatus(currentVehicles, "MOVING"));
      dto.setDisabledVehicles(countByStatus(currentVehicles, 3));
      dto.setWarningVehicles(currentVehicles.stream().filter(this::hasWarning).count());
      dto.setTotalLoadTons(sumLoadWeight(currentVehicles));
      dto.setActiveRate(rate(dto.getActiveVehicles(), dto.getTotalVehicles()));
      dto.setAvgLoadTons(averageLoadWeight(currentVehicles));
      dto.setCaptainName(firstNonBlank(currentVehicles.stream().map(Vehicle::getCaptainName).toList()));
      dto.setCaptainPhone(firstNonBlank(currentVehicles.stream().map(Vehicle::getCaptainPhone).toList()));
      records.add(dto);
    }
    records.sort(
        Comparator.comparingLong(VehicleCompanyCapacityDto::getTotalVehicles)
            .reversed()
            .thenComparing(VehicleCompanyCapacityDto::getOrgName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(records);
  }

  @GetMapping("/fleets")
  public ApiResult<List<VehicleFleetSummaryDto>> fleets(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Vehicle> vehicles = listTenantVehicles(currentUser.getTenantId());
    Map<Long, Org> orgMap = loadOrgMap(vehicles);
    Map<String, List<Vehicle>> grouped =
        vehicles.stream()
            .collect(
                Collectors.groupingBy(
                    vehicle ->
                        (vehicle.getOrgId() != null ? vehicle.getOrgId() : -1L)
                            + "::"
                            + (StringUtils.hasText(vehicle.getFleetName())
                                ? vehicle.getFleetName().trim()
                                : "未编组车队"),
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<VehicleFleetSummaryDto> records = new ArrayList<>();
    for (Map.Entry<String, List<Vehicle>> entry : grouped.entrySet()) {
      List<Vehicle> currentVehicles = entry.getValue();
      Vehicle sample = currentVehicles.get(0);
      Long currentOrgId = sample.getOrgId();
      Org org = currentOrgId != null ? orgMap.get(currentOrgId) : null;
      VehicleFleetSummaryDto dto = new VehicleFleetSummaryDto();
      dto.setId(entry.getKey());
      dto.setFleetName(StringUtils.hasText(sample.getFleetName()) ? sample.getFleetName() : "未编组车队");
      dto.setOrgId(currentOrgId != null ? String.valueOf(currentOrgId) : null);
      dto.setOrgName(resolveOrgName(org, currentOrgId));
      dto.setCaptainName(firstNonBlank(currentVehicles.stream().map(Vehicle::getCaptainName).toList()));
      dto.setCaptainPhone(firstNonBlank(currentVehicles.stream().map(Vehicle::getCaptainPhone).toList()));
      dto.setDriverCount(
          currentVehicles.stream()
              .map(
                  vehicle ->
                      StringUtils.hasText(vehicle.getDriverPhone())
                          ? vehicle.getDriverPhone()
                          : vehicle.getDriverName())
              .filter(StringUtils::hasText)
              .distinct()
              .count());
      dto.setTotalVehicles(currentVehicles.size());
      dto.setActiveVehicles(countByStatus(currentVehicles, 1));
      dto.setMovingVehicles(countByRunningStatus(currentVehicles, "MOVING"));
      dto.setWarningVehicles(currentVehicles.stream().filter(this::hasWarning).count());
      dto.setTotalLoadTons(sumLoadWeight(currentVehicles));
      dto.setAvgLoadTons(averageLoadWeight(currentVehicles));
      dto.setStatusLabel(resolveFleetStatus(currentVehicles));
      records.add(dto);
    }
    records.sort(
        Comparator.comparingLong(VehicleFleetSummaryDto::getTotalVehicles)
            .reversed()
            .thenComparing(VehicleFleetSummaryDto::getFleetName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(records);
  }

  @PostMapping
  public ApiResult<VehicleDetailDto> create(
      @RequestBody VehicleUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId(), null);
    Vehicle vehicle = new Vehicle();
    vehicle.setTenantId(currentUser.getTenantId());
    applyUpsert(vehicle, body);
    vehicleMapper.insert(vehicle);
    Org org = vehicle.getOrgId() != null ? orgMapper.selectById(vehicle.getOrgId()) : null;
    return ApiResult.ok(toDetail(vehicleMapper.selectById(vehicle.getId()), org));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleDetailDto> update(
      @PathVariable Long id,
      @RequestBody VehicleUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "车辆不存在");
    }
    validateUpsert(body, currentUser.getTenantId(), id);
    applyUpsert(vehicle, body);
    vehicleMapper.updateById(vehicle);
    Org org = vehicle.getOrgId() != null ? orgMapper.selectById(vehicle.getOrgId()) : null;
    return ApiResult.ok(toDetail(vehicleMapper.selectById(vehicle.getId()), org));
  }

  private List<Vehicle> listTenantVehicles(Long tenantId) {
    return vehicleMapper.selectList(
        new LambdaQueryWrapper<Vehicle>()
            .eq(Vehicle::getTenantId, tenantId)
            .orderByDesc(Vehicle::getUpdateTime)
            .orderByDesc(Vehicle::getId));
  }

  private Map<Long, Org> loadOrgMap(List<Vehicle> vehicles) {
    LinkedHashSet<Long> orgIds =
        vehicles.stream()
            .map(Vehicle::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private LinkedHashSet<Long> findOrgIdsByKeyword(Long tenantId, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return new LinkedHashSet<>();
    }
    return orgMapper.selectList(
            new LambdaQueryWrapper<Org>()
                .eq(Org::getTenantId, tenantId)
                .and(
                    wrapper ->
                        wrapper
                            .like(Org::getOrgName, keyword)
                            .or()
                            .like(Org::getOrgCode, keyword)))
        .stream()
        .map(Org::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private VehicleListItemDto toListItem(Vehicle vehicle, Org org) {
    VehicleListItemDto dto = new VehicleListItemDto();
    fillCommon(dto, vehicle, org);
    return dto;
  }

  private VehicleDetailDto toDetail(Vehicle vehicle, Org org) {
    VehicleDetailDto dto = new VehicleDetailDto();
    fillCommon(dto, vehicle, org);
    dto.setDeadWeight(vehicle.getDeadWeight());
    dto.setLng(vehicle.getLng());
    dto.setLat(vehicle.getLat());
    dto.setGpsTime(formatDateTime(vehicle.getGpsTime()));
    dto.setRemark(vehicle.getRemark());
    return dto;
  }

  private void fillCommon(VehicleListItemDto dto, Vehicle vehicle, Org org) {
    dto.setId(vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setVin(vehicle.getVin());
    dto.setOrgId(vehicle.getOrgId() != null ? String.valueOf(vehicle.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(org, vehicle.getOrgId()));
    dto.setVehicleType(vehicle.getVehicleType());
    dto.setBrand(vehicle.getBrand());
    dto.setModel(vehicle.getModel());
    dto.setEnergyType(vehicle.getEnergyType());
    dto.setAxleCount(vehicle.getAxleCount());
    dto.setLoadWeight(vehicle.getLoadWeight());
    dto.setDriverName(vehicle.getDriverName());
    dto.setDriverPhone(vehicle.getDriverPhone());
    dto.setFleetName(vehicle.getFleetName());
    dto.setCaptainName(vehicle.getCaptainName());
    dto.setCaptainPhone(vehicle.getCaptainPhone());
    dto.setStatus(vehicle.getStatus());
    dto.setStatusLabel(resolveStatusLabel(vehicle.getStatus()));
    dto.setUseStatus(vehicle.getUseStatus());
    dto.setRunningStatus(vehicle.getRunningStatus());
    dto.setRunningStatusLabel(resolveRunningStatusLabel(vehicle.getRunningStatus()));
    dto.setCurrentSpeed(vehicle.getCurrentSpeed());
    dto.setCurrentMileage(vehicle.getCurrentMileage());
    dto.setNextMaintainDate(formatDate(vehicle.getNextMaintainDate()));
    dto.setAnnualInspectionExpireDate(formatDate(vehicle.getAnnualInspectionExpireDate()));
    dto.setInsuranceExpireDate(formatDate(vehicle.getInsuranceExpireDate()));
    dto.setWarningLabel(resolveWarningLabel(vehicle));
    dto.setCreateTime(formatDateTime(vehicle.getCreateTime()));
    dto.setUpdateTime(formatDateTime(vehicle.getUpdateTime()));
  }

  private VehicleStatsDto toStats(List<Vehicle> vehicles) {
    VehicleStatsDto dto = new VehicleStatsDto();
    dto.setTotalVehicles(vehicles.size());
    dto.setActiveVehicles(countByStatus(vehicles, 1));
    dto.setMaintenanceVehicles(countByStatus(vehicles, 2));
    dto.setDisabledVehicles(countByStatus(vehicles, 3));
    dto.setWarningVehicles(vehicles.stream().filter(this::hasWarning).count());
    dto.setActiveRate(rate(dto.getActiveVehicles(), dto.getTotalVehicles()));
    dto.setTotalLoadTons(sumLoadWeight(vehicles));
    return dto;
  }

  private long countByStatus(List<Vehicle> vehicles, int expectedStatus) {
    return vehicles.stream()
        .filter(vehicle -> vehicle.getStatus() != null && vehicle.getStatus() == expectedStatus)
        .count();
  }

  private long countByRunningStatus(List<Vehicle> vehicles, String expectedStatus) {
    return vehicles.stream()
        .filter(
            vehicle ->
                StringUtils.hasText(vehicle.getRunningStatus())
                    && expectedStatus.equalsIgnoreCase(vehicle.getRunningStatus()))
        .count();
  }

  private BigDecimal sumLoadWeight(List<Vehicle> vehicles) {
    return vehicles.stream()
        .map(Vehicle::getLoadWeight)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal averageLoadWeight(List<Vehicle> vehicles) {
    long count = vehicles.stream().map(Vehicle::getLoadWeight).filter(Objects::nonNull).count();
    if (count <= 0) {
      return BigDecimal.ZERO;
    }
    return sumLoadWeight(vehicles).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
  }

  private BigDecimal rate(long numerator, long denominator) {
    if (denominator <= 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
  }

  private void validateUpsert(VehicleUpsertDto body, Long tenantId, Long currentId) {
    if (!StringUtils.hasText(body.getPlateNo())) {
      throw new BizException(400, "车牌号不能为空");
    }
    Long existing =
        vehicleMapper.selectCount(
            new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getTenantId, tenantId)
                .eq(Vehicle::getPlateNo, body.getPlateNo().trim())
                .ne(currentId != null, Vehicle::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "车牌号已存在");
    }
  }

  private void applyUpsert(Vehicle vehicle, VehicleUpsertDto body) {
    vehicle.setPlateNo(body.getPlateNo() != null ? body.getPlateNo().trim() : null);
    vehicle.setVin(trimToNull(body.getVin()));
    vehicle.setOrgId(body.getOrgId());
    vehicle.setVehicleType(trimToNull(body.getVehicleType()));
    vehicle.setBrand(trimToNull(body.getBrand()));
    vehicle.setModel(trimToNull(body.getModel()));
    vehicle.setEnergyType(trimToNull(body.getEnergyType()));
    vehicle.setAxleCount(body.getAxleCount());
    vehicle.setDeadWeight(body.getDeadWeight());
    vehicle.setLoadWeight(body.getLoadWeight());
    vehicle.setDriverName(trimToNull(body.getDriverName()));
    vehicle.setDriverPhone(trimToNull(body.getDriverPhone()));
    vehicle.setFleetName(trimToNull(body.getFleetName()));
    vehicle.setCaptainName(trimToNull(body.getCaptainName()));
    vehicle.setCaptainPhone(trimToNull(body.getCaptainPhone()));
    vehicle.setStatus(body.getStatus() != null ? body.getStatus() : 1);
    vehicle.setUseStatus(
        StringUtils.hasText(body.getUseStatus()) ? body.getUseStatus().trim().toUpperCase() : "ACTIVE");
    vehicle.setRunningStatus(
        StringUtils.hasText(body.getRunningStatus())
            ? body.getRunningStatus().trim().toUpperCase()
            : "STOPPED");
    vehicle.setCurrentSpeed(body.getCurrentSpeed());
    vehicle.setCurrentMileage(body.getCurrentMileage());
    vehicle.setNextMaintainDate(body.getNextMaintainDate());
    vehicle.setAnnualInspectionExpireDate(body.getAnnualInspectionExpireDate());
    vehicle.setInsuranceExpireDate(body.getInsuranceExpireDate());
    vehicle.setLng(body.getLng());
    vehicle.setLat(body.getLat());
    vehicle.setGpsTime(body.getGpsTime());
    vehicle.setRemark(trimToNull(body.getRemark()));
  }

  private String resolveOrgName(Org org, Long orgId) {
    if (org != null && StringUtils.hasText(org.getOrgName())) {
      return org.getOrgName();
    }
    if (orgId == null || orgId <= 0) {
      return "未归属单位";
    }
    return "组织#" + orgId;
  }

  private String resolveStatusLabel(Integer status) {
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

  private String resolveRunningStatusLabel(String runningStatus) {
    if (!StringUtils.hasText(runningStatus)) {
      return "未上报";
    }
    return switch (runningStatus.trim().toUpperCase()) {
      case "MOVING" -> "行驶中";
      case "STOPPED" -> "静止";
      case "OFFLINE" -> "离线";
      default -> runningStatus;
    };
  }

  private String resolveFleetStatus(List<Vehicle> vehicles) {
    long active = countByStatus(vehicles, 1);
    long moving = countByRunningStatus(vehicles, "MOVING");
    if (moving > 0) {
      return "出车";
    }
    if (active > 0) {
      return "待命";
    }
    if (countByStatus(vehicles, 3) == vehicles.size()) {
      return "停运";
    }
    return "休整";
  }

  private boolean hasWarning(Vehicle vehicle) {
    return !Objects.equals(resolveWarningLabel(vehicle), "正常");
  }

  private String resolveWarningLabel(Vehicle vehicle) {
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
      return "已到期";
    }
    if (minDays <= 7) {
      return "7日内到期";
    }
    if (minDays <= 30) {
      return "30日内到期";
    }
    return "正常";
  }

  private String firstNonBlank(List<String> values) {
    return values.stream().filter(StringUtils::hasText).findFirst().orElse(null);
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }
}
