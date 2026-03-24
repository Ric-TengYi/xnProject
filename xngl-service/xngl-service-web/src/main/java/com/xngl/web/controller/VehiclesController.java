package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleTrackPoint;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleTrackPointMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleCompanyCapacityDto;
import com.xngl.web.dto.vehicle.VehicleDetailDto;
import com.xngl.web.dto.vehicle.VehicleFleetSummaryDto;
import com.xngl.web.dto.vehicle.VehicleTrackHistoryDto;
import com.xngl.web.dto.vehicle.VehicleListItemDto;
import com.xngl.web.dto.vehicle.VehicleStatsDto;
import com.xngl.web.dto.vehicle.VehicleTrackPointDto;
import com.xngl.web.dto.vehicle.VehicleUpsertDto;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final VehicleTrackPointMapper vehicleTrackPointMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public VehiclesController(
      VehicleMapper vehicleMapper,
      VehicleTrackPointMapper vehicleTrackPointMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.vehicleMapper = vehicleMapper;
    this.vehicleTrackPointMapper = vehicleTrackPointMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String vehicleType,
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
    if (StringUtils.hasText(vehicleType)) {
      query.eq(Vehicle::getVehicleType, vehicleType.trim());
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

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) String vehicleType,
      @RequestParam(required = false) String useStatus,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleListItemDto> rows =
        loadVehicleRows(currentUser.getTenantId(), keyword, status, orgId, vehicleType, useStatus);
    String csv = buildVehicleCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicles.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.getBytes(StandardCharsets.UTF_8));
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

  @GetMapping("/{id}/track-history")
  public ApiResult<VehicleTrackHistoryDto> trackHistory(
      @PathVariable Long id,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "车辆不存在");
    }
    LocalDateTime start = parseDateTime(startTime);
    LocalDateTime end = parseDateTime(endTime);
    LambdaQueryWrapper<VehicleTrackPoint> query =
        new LambdaQueryWrapper<VehicleTrackPoint>()
            .eq(VehicleTrackPoint::getTenantId, currentUser.getTenantId())
            .eq(VehicleTrackPoint::getVehicleId, id)
            .ge(start != null, VehicleTrackPoint::getLocateTime, start)
            .le(end != null, VehicleTrackPoint::getLocateTime, end)
            .orderByAsc(VehicleTrackPoint::getLocateTime)
            .orderByAsc(VehicleTrackPoint::getId);
    List<VehicleTrackPoint> points = vehicleTrackPointMapper.selectList(query);
    if (points.isEmpty() && vehicle.getLng() != null && vehicle.getLat() != null) {
      points = buildFallbackTrack(vehicle, start, end);
    }
    VehicleTrackHistoryDto dto = new VehicleTrackHistoryDto();
    dto.setVehicleId(String.valueOf(vehicle.getId()));
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setStartTime(formatDateTime(start));
    dto.setEndTime(formatDateTime(end));
    dto.setPoints(points.stream().map(this::toTrackPointDto).toList());
    dto.setPointCount(dto.getPoints() != null ? dto.getPoints().size() : 0);
    return ApiResult.ok(dto);
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

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
      return ApiResult.fail(404, "车辆不存在");
    }
    vehicleMapper.deleteById(id);
    return ApiResult.ok();
  }

  @PutMapping("/batch-status")
  public ApiResult<Map<String, Object>> batchUpdateStatus(
      @RequestBody VehicleBatchStatusDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Vehicle> vehicles = requireBatchVehicles(body != null ? body.getIds() : null, currentUser.getTenantId());
    Integer nextStatus = body != null ? body.getStatus() : null;
    if (nextStatus == null) {
      throw new BizException(400, "目标状态不能为空");
    }
    for (Vehicle vehicle : vehicles) {
      vehicle.setStatus(nextStatus);
      vehicle.setUseStatus(resolveUseStatus(nextStatus));
      vehicleMapper.updateById(vehicle);
    }
    return ApiResult.ok(Map.of("updated", vehicles.size(), "status", nextStatus));
  }

  @PostMapping("/batch-delete")
  public ApiResult<Map<String, Object>> batchDelete(
      @RequestBody VehicleBatchIdsDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Vehicle> vehicles = requireBatchVehicles(body != null ? body.getIds() : null, currentUser.getTenantId());
    for (Vehicle vehicle : vehicles) {
      vehicleMapper.deleteById(vehicle.getId());
    }
    return ApiResult.ok(Map.of("deleted", vehicles.size()));
  }

  private List<Vehicle> listTenantVehicles(Long tenantId) {
    return vehicleMapper.selectList(
        new LambdaQueryWrapper<Vehicle>()
            .eq(Vehicle::getTenantId, tenantId)
            .orderByDesc(Vehicle::getUpdateTime)
            .orderByDesc(Vehicle::getId));
  }

  private List<VehicleListItemDto> loadVehicleRows(
      Long tenantId, String keyword, Integer status, Long orgId, String vehicleType, String useStatus) {
    LambdaQueryWrapper<Vehicle> query =
        new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getTenantId, tenantId);
    if (status != null) {
      query.eq(Vehicle::getStatus, status);
    }
    if (orgId != null) {
      query.eq(Vehicle::getOrgId, orgId);
    }
    if (StringUtils.hasText(vehicleType)) {
      query.eq(Vehicle::getVehicleType, vehicleType.trim());
    }
    if (StringUtils.hasText(useStatus)) {
      query.eq(Vehicle::getUseStatus, useStatus.trim().toUpperCase());
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      LinkedHashSet<Long> orgIds = findOrgIdsByKeyword(tenantId, effectiveKeyword);
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
    List<Vehicle> vehicles = vehicleMapper.selectList(query);
    Map<Long, Org> orgMap = loadOrgMap(vehicles);
    return vehicles.stream()
        .map(vehicle -> toListItem(vehicle, orgMap.get(vehicle.getOrgId())))
        .toList();
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

  private List<VehicleTrackPoint> buildFallbackTrack(
      Vehicle vehicle, LocalDateTime start, LocalDateTime end) {
    LocalDateTime baseTime = vehicle.getGpsTime() != null ? vehicle.getGpsTime() : LocalDateTime.now();
    if (start != null && end != null && (baseTime.isBefore(start) || baseTime.isAfter(end))) {
      return Collections.emptyList();
    }
    VehicleTrackPoint point = new VehicleTrackPoint();
    point.setId(vehicle.getId());
    point.setVehicleId(vehicle.getId());
    point.setPlateNo(vehicle.getPlateNo());
    point.setLng(vehicle.getLng());
    point.setLat(vehicle.getLat());
    point.setSpeed(vehicle.getCurrentSpeed());
    point.setLocateTime(baseTime);
    point.setSourceType("REALTIME");
    point.setRemark("当前定位");
    return List.of(point);
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

  private String buildVehicleCsv(List<VehicleListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("车牌号,所属单位,车辆类型,品牌,型号,能源类型,司机,司机电话,车队,负责人,负责人电话,载重(吨),车辆状态,运行状态,当前速度(km/h),当前里程(km),保养到期日,年检到期日,保险到期日,预警状态,更新时间\n");
    for (VehicleListItemDto row : rows) {
      builder
          .append(csv(row.getPlateNo())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getVehicleType())).append(',')
          .append(csv(row.getBrand())).append(',')
          .append(csv(row.getModel())).append(',')
          .append(csv(row.getEnergyType())).append(',')
          .append(csv(row.getDriverName())).append(',')
          .append(csv(row.getDriverPhone())).append(',')
          .append(csv(row.getFleetName())).append(',')
          .append(csv(row.getCaptainName())).append(',')
          .append(csv(row.getCaptainPhone())).append(',')
          .append(defaultDecimal(row.getLoadWeight())).append(',')
          .append(csv(row.getStatusLabel())).append(',')
          .append(csv(row.getRunningStatusLabel())).append(',')
          .append(defaultDecimal(row.getCurrentSpeed())).append(',')
          .append(defaultDecimal(row.getCurrentMileage())).append(',')
          .append(csv(row.getNextMaintainDate())).append(',')
          .append(csv(row.getAnnualInspectionExpireDate())).append(',')
          .append(csv(row.getInsuranceExpireDate())).append(',')
          .append(csv(row.getWarningLabel())).append(',')
          .append(csv(row.getUpdateTime())).append('\n');
    }
    return builder.toString();
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

  private String resolveUseStatus(Integer status) {
    if (status == null) {
      return "ACTIVE";
    }
    return switch (status) {
      case 2 -> "MAINTENANCE";
      case 3, 5 -> "DISABLED";
      case 4 -> "STANDBY";
      default -> "ACTIVE";
    };
  }

  private List<Vehicle> requireBatchVehicles(List<Long> ids, Long tenantId) {
    LinkedHashSet<Long> validIds =
        ids == null
            ? new LinkedHashSet<>()
            : ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (validIds.isEmpty()) {
      throw new BizException(400, "请选择至少一条车辆记录");
    }
    List<Vehicle> vehicles = vehicleMapper.selectBatchIds(validIds);
    if (vehicles.size() != validIds.size()
        || vehicles.stream().anyMatch(item -> !Objects.equals(item.getTenantId(), tenantId))) {
      throw new BizException(400, "存在无效车辆记录");
    }
    return vehicles;
  }

  private String defaultDecimal(BigDecimal value) {
    return value != null ? value.stripTrailingZeros().toPlainString() : "";
  }

  private String csv(String value) {
    if (value == null) {
      return "";
    }
    String escaped = value.replace("\"", "\"\"");
    if (escaped.contains(",") || escaped.contains("\n")) {
      return "\"" + escaped + "\"";
    }
    return escaped;
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.trim(), ISO_DATE_TIME);
    } catch (Exception ex) {
      throw new BizException(400, "时间格式错误，应为 yyyy-MM-ddTHH:mm:ss");
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  @Data
  public static class VehicleBatchIdsDto {
    private List<Long> ids;
  }

  @Data
  public static class VehicleBatchStatusDto {
    private List<Long> ids;
    private Integer status;
  }
}
