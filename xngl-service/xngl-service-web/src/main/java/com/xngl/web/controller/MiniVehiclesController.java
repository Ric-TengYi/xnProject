package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniVehicleInspection;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleTrackPoint;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.infrastructure.persistence.mapper.MiniVehicleInspectionMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleTrackPointMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.mini.MiniVehicleFenceDto;
import com.xngl.web.dto.mini.MiniVehicleFenceUpsertDto;
import com.xngl.web.dto.mini.MiniVehicleInspectionCreateDto;
import com.xngl.web.dto.mini.MiniVehicleInspectionDto;
import com.xngl.web.dto.mini.MiniVehicleRealtimeDto;
import com.xngl.web.dto.mini.MiniVehicleStopDto;
import com.xngl.web.dto.vehicle.VehicleTrackHistoryDto;
import com.xngl.web.dto.vehicle.VehicleTrackPointDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
@RequestMapping("/api/mini")
public class MiniVehiclesController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final VehicleMapper vehicleMapper;
  private final VehicleTrackPointMapper vehicleTrackPointMapper;
  private final MiniVehicleInspectionMapper miniVehicleInspectionMapper;
  private final AlertFenceMapper alertFenceMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  public MiniVehiclesController(
      VehicleMapper vehicleMapper,
      VehicleTrackPointMapper vehicleTrackPointMapper,
      MiniVehicleInspectionMapper miniVehicleInspectionMapper,
      AlertFenceMapper alertFenceMapper,
      OrgMapper orgMapper,
      UserContext userContext,
      ObjectMapper objectMapper) {
    this.vehicleMapper = vehicleMapper;
    this.vehicleTrackPointMapper = vehicleTrackPointMapper;
    this.miniVehicleInspectionMapper = miniVehicleInspectionMapper;
    this.alertFenceMapper = alertFenceMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/vehicles/realtime")
  public ApiResult<PageResult<MiniVehicleRealtimeDto>> realtimeVehicles(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String runningStatus,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    LambdaQueryWrapper<Vehicle> query =
        new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getTenantId, currentUser.getTenantId());
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(Vehicle::getPlateNo, effectiveKeyword)
                  .or()
                  .like(Vehicle::getDriverName, effectiveKeyword)
                  .or()
                  .like(Vehicle::getFleetName, effectiveKeyword));
    }
    if (StringUtils.hasText(runningStatus)) {
      query.eq(Vehicle::getRunningStatus, runningStatus.trim().toUpperCase());
    }
    query.orderByDesc(Vehicle::getGpsTime).orderByDesc(Vehicle::getId);
    IPage<Vehicle> page = vehicleMapper.selectPage(new Page<>(pageNo, pageSize), query);
    Map<Long, Org> orgMap = loadOrgMap(page.getRecords());
    List<MiniVehicleRealtimeDto> rows =
        page.getRecords().stream().map(item -> toRealtimeDto(item, orgMap.get(item.getOrgId()))).toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), rows));
  }

  @GetMapping("/vehicles/{id}/track-history")
  public ApiResult<VehicleTrackHistoryDto> trackHistory(
      @PathVariable Long id,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = requireVehicle(id, currentUser.getTenantId());
    LocalDateTime start = parseDateTime(startTime, null);
    LocalDateTime end = parseDateTime(endTime, null);
    List<VehicleTrackPoint> points = loadTrackPoints(currentUser.getTenantId(), vehicle, start, end);
    VehicleTrackHistoryDto dto = new VehicleTrackHistoryDto();
    dto.setVehicleId(String.valueOf(vehicle.getId()));
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setStartTime(start != null ? start.format(ISO) : null);
    dto.setEndTime(end != null ? end.format(ISO) : null);
    dto.setPoints(points.stream().map(this::toTrackPointDto).toList());
    dto.setPointCount(dto.getPoints() != null ? dto.getPoints().size() : 0);
    return ApiResult.ok(dto);
  }

  @GetMapping("/vehicles/{id}/stops")
  public ApiResult<List<MiniVehicleStopDto>> vehicleStops(
      @PathVariable Long id,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "5") long minStopMinutes,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Vehicle vehicle = requireVehicle(id, currentUser.getTenantId());
    List<VehicleTrackPoint> points =
        loadTrackPoints(
            currentUser.getTenantId(), vehicle, parseDateTime(startTime, null), parseDateTime(endTime, null));
    return ApiResult.ok(calculateStops(points, minStopMinutes));
  }

  @PostMapping("/vehicle-inspections")
  public ApiResult<MiniVehicleInspectionDto> createInspection(
      @RequestBody MiniVehicleInspectionCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getVehicleId() == null) {
      throw new BizException(400, "车辆不能为空");
    }
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    MiniVehicleInspection inspection = new MiniVehicleInspection();
    inspection.setTenantId(currentUser.getTenantId());
    inspection.setVehicleId(vehicle.getId());
    inspection.setOrgId(vehicle.getOrgId());
    inspection.setUserId(currentUser.getId());
    inspection.setInspectorName(resolveUserDisplayName(currentUser));
    inspection.setPlateNo(vehicle.getPlateNo());
    inspection.setDispatchNo(trimToNull(body.getDispatchNo()));
    inspection.setInspectionTime(parseDateTime(body.getInspectionTime(), LocalDateTime.now()));
    inspection.setVehiclePhotoUrls(trimToNull(body.getVehiclePhotoUrls()));
    inspection.setCertificatePhotoUrls(trimToNull(body.getCertificatePhotoUrls()));
    inspection.setIssueSummary(trimToNull(body.getIssueSummary()));
    inspection.setConclusion(normalizeText(body.getConclusion(), "PASS"));
    inspection.setStatus("SUBMITTED");
    miniVehicleInspectionMapper.insert(inspection);
    Org org = vehicle.getOrgId() != null ? orgMapper.selectById(vehicle.getOrgId()) : null;
    return ApiResult.ok(toInspectionDto(inspection, org));
  }

  @GetMapping("/vehicle-inspections")
  public ApiResult<List<MiniVehicleInspectionDto>> listInspections(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniVehicleInspection> rows =
        miniVehicleInspectionMapper.selectList(
            new LambdaQueryWrapper<MiniVehicleInspection>()
                .eq(MiniVehicleInspection::getTenantId, currentUser.getTenantId())
                .eq(MiniVehicleInspection::getUserId, currentUser.getId())
                .orderByDesc(MiniVehicleInspection::getInspectionTime)
                .orderByDesc(MiniVehicleInspection::getId));
    Map<Long, Org> orgMap =
        rows.stream()
            .map(MiniVehicleInspection::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), this::loadOrgMapByIds));
    return ApiResult.ok(rows.stream().map(item -> toInspectionDto(item, orgMap.get(item.getOrgId()))).toList());
  }

  @GetMapping("/vehicle-inspections/{id}")
  public ApiResult<MiniVehicleInspectionDto> getInspection(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniVehicleInspection inspection = miniVehicleInspectionMapper.selectById(id);
    if (inspection == null || !Objects.equals(inspection.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "车辆检查记录不存在");
    }
    Org org = inspection.getOrgId() != null ? orgMapper.selectById(inspection.getOrgId()) : null;
    return ApiResult.ok(toInspectionDto(inspection, org));
  }

  @GetMapping("/vehicle-fences")
  public ApiResult<List<MiniVehicleFenceDto>> listVehicleFences(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<AlertFence> rows =
        alertFenceMapper.selectList(
            new LambdaQueryWrapper<AlertFence>()
                .eq(AlertFence::getTenantId, currentUser.getTenantId())
                .like(AlertFence::getRuleCode, "MINI_VEHICLE")
                .orderByDesc(AlertFence::getId));
    Map<Long, Vehicle> vehicleMap = loadFenceVehicles(rows);
    return ApiResult.ok(
        rows.stream()
            .filter(item -> canAccessFence(currentUser, item))
            .map(item -> toFenceDto(item, vehicleMap.get(parseFenceVehicleId(item))))
            .toList());
  }

  @PostMapping("/vehicle-fences")
  public ApiResult<MiniVehicleFenceDto> createVehicleFence(
      @RequestBody MiniVehicleFenceUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    AlertFence fence = new AlertFence();
    fence.setTenantId(currentUser.getTenantId());
    applyFenceBody(fence, body, currentUser);
    fence.setFenceCode(
        StringUtils.hasText(body != null ? body.getFenceCode() : null)
            ? body.getFenceCode().trim()
            : "MINI-FENCE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
    alertFenceMapper.insert(fence);
    Vehicle vehicle = parseFenceVehicleId(fence) != null ? vehicleMapper.selectById(parseFenceVehicleId(fence)) : null;
    return ApiResult.ok(toFenceDto(fence, vehicle));
  }

  @PutMapping("/vehicle-fences/{id}")
  public ApiResult<MiniVehicleFenceDto> updateVehicleFence(
      @PathVariable Long id, @RequestBody MiniVehicleFenceUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    AlertFence fence = alertFenceMapper.selectById(id);
    if (fence == null || !Objects.equals(fence.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "电子围栏不存在");
    }
    if (!canAccessFence(currentUser, fence)) {
      throw new BizException(403, "无权维护该电子围栏");
    }
    applyFenceBody(fence, body, currentUser);
    if (StringUtils.hasText(body.getFenceCode())) {
      fence.setFenceCode(body.getFenceCode().trim());
    }
    alertFenceMapper.updateById(fence);
    Vehicle vehicle = parseFenceVehicleId(fence) != null ? vehicleMapper.selectById(parseFenceVehicleId(fence)) : null;
    return ApiResult.ok(toFenceDto(fence, vehicle));
  }

  private void applyFenceBody(AlertFence fence, MiniVehicleFenceUpsertDto body, User currentUser) {
    if (body == null
        || !StringUtils.hasText(body.getFenceName())
        || body.getVehicleId() == null
        || body.getCenterLng() == null
        || body.getCenterLat() == null) {
      throw new BizException(400, "围栏名称、车辆和中心坐标不能为空");
    }
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    fence.setRuleCode("MINI_VEHICLE_FENCE");
    fence.setFenceName(body.getFenceName().trim());
    fence.setFenceType("VEHICLE");
    fence.setBufferMeters(body.getRadiusMeters() != null ? body.getRadiusMeters() : BigDecimal.valueOf(100));
    fence.setGeoJson(buildFenceGeoJson(body));
    fence.setDirectionRule(normalizeText(body.getDirectionRule(), "BOTH"));
    fence.setActiveTimeRange(trimToNull(body.getWarningTimeRange()));
    fence.setStatus(normalizeText(body.getStatus(), "ENABLED"));
    fence.setBizScope(buildFenceScopeJson(vehicle.getId(), currentUser.getId(), body.getPermissionUserIds()));
  }

  private String buildFenceGeoJson(MiniVehicleFenceUpsertDto body) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("type", "Circle");
    payload.put("centerLng", body.getCenterLng());
    payload.put("centerLat", body.getCenterLat());
    payload.put("radiusMeters", body.getRadiusMeters() != null ? body.getRadiusMeters() : BigDecimal.valueOf(100));
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new BizException(500, "围栏坐标序列化失败");
    }
  }

  private String buildFenceScopeJson(Long vehicleId, Long creatorId, List<Long> permissionUserIds) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("vehicleId", vehicleId);
    payload.put("creatorId", creatorId);
    payload.put("permissionUserIds", permissionUserIds != null ? permissionUserIds : Collections.emptyList());
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new BizException(500, "围栏权限序列化失败");
    }
  }

  private boolean canAccessFence(User currentUser, AlertFence fence) {
    if (isAdminUser(currentUser)) {
      return true;
    }
    Map<String, Object> scope = parseFenceScope(fence);
    Object creatorId = scope.get("creatorId");
    if (creatorId != null && Objects.equals(String.valueOf(currentUser.getId()), String.valueOf(creatorId))) {
      return true;
    }
    Object permission = scope.get("permissionUserIds");
    if (permission instanceof List<?> list) {
      return list.stream().map(String::valueOf).anyMatch(String.valueOf(currentUser.getId())::equals);
    }
    return false;
  }

  private Map<Long, Vehicle> loadFenceVehicles(List<AlertFence> rows) {
    LinkedHashSet<Long> vehicleIds =
        rows.stream().map(this::parseFenceVehicleId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    if (vehicleIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return vehicleMapper.selectBatchIds(vehicleIds).stream()
        .collect(Collectors.toMap(Vehicle::getId, item -> item, (left, right) -> left));
  }

  private Long parseFenceVehicleId(AlertFence fence) {
    Object value = parseFenceScope(fence).get("vehicleId");
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private Map<String, Object> parseFenceScope(AlertFence fence) {
    if (fence == null || !StringUtils.hasText(fence.getBizScope())) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(fence.getBizScope(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return Collections.emptyMap();
    }
  }

  private MiniVehicleFenceDto toFenceDto(AlertFence fence, Vehicle vehicle) {
    Map<String, Object> scope = parseFenceScope(fence);
    Map<String, Object> geo = parseGeo(fence.getGeoJson());
    MiniVehicleFenceDto dto = new MiniVehicleFenceDto();
    dto.setId(fence.getId() != null ? String.valueOf(fence.getId()) : null);
    dto.setFenceCode(fence.getFenceCode());
    dto.setFenceName(fence.getFenceName());
    dto.setVehicleId(scope.get("vehicleId") != null ? String.valueOf(scope.get("vehicleId")) : null);
    dto.setVehiclePlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setCenterLng(toBigDecimal(geo.get("centerLng")));
    dto.setCenterLat(toBigDecimal(geo.get("centerLat")));
    dto.setRadiusMeters(fence.getBufferMeters());
    dto.setWarningTimeRange(fence.getActiveTimeRange());
    dto.setDirectionRule(fence.getDirectionRule());
    dto.setPermissionUserIds(
        scope.get("permissionUserIds") instanceof List<?> list
            ? list.stream().map(String::valueOf).toList()
            : Collections.emptyList());
    dto.setStatus(fence.getStatus());
    return dto;
  }

  private Map<String, Object> parseGeo(String geoJson) {
    if (!StringUtils.hasText(geoJson)) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(geoJson, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return Collections.emptyMap();
    }
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private MiniVehicleInspectionDto toInspectionDto(MiniVehicleInspection inspection, Org org) {
    return new MiniVehicleInspectionDto(
        inspection.getId() != null ? String.valueOf(inspection.getId()) : null,
        inspection.getVehicleId() != null ? String.valueOf(inspection.getVehicleId()) : null,
        inspection.getPlateNo(),
        org != null && org.getId() != null ? String.valueOf(org.getId()) : (inspection.getOrgId() != null ? String.valueOf(inspection.getOrgId()) : null),
        org != null ? org.getOrgName() : null,
        inspection.getDispatchNo(),
        inspection.getInspectionTime() != null ? inspection.getInspectionTime().format(ISO) : null,
        inspection.getVehiclePhotoUrls(),
        inspection.getCertificatePhotoUrls(),
        inspection.getIssueSummary(),
        inspection.getConclusion(),
        inspection.getStatus(),
        inspection.getInspectorName(),
        inspection.getCreateTime() != null ? inspection.getCreateTime().format(ISO) : null);
  }

  private MiniVehicleRealtimeDto toRealtimeDto(Vehicle vehicle, Org org) {
    MiniVehicleRealtimeDto dto = new MiniVehicleRealtimeDto();
    dto.setVehicleId(vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle.getPlateNo());
    dto.setOrgId(vehicle.getOrgId() != null ? String.valueOf(vehicle.getOrgId()) : null);
    dto.setOrgName(org != null ? org.getOrgName() : null);
    dto.setDriverName(vehicle.getDriverName());
    dto.setFleetName(vehicle.getFleetName());
    dto.setRunningStatus(vehicle.getRunningStatus());
    dto.setUseStatus(vehicle.getUseStatus());
    dto.setCurrentSpeed(vehicle.getCurrentSpeed());
    dto.setLng(vehicle.getLng());
    dto.setLat(vehicle.getLat());
    dto.setGpsTime(vehicle.getGpsTime() != null ? vehicle.getGpsTime().format(ISO) : null);
    dto.setCurrentMileage(vehicle.getCurrentMileage());
    return dto;
  }

  private List<MiniVehicleStopDto> calculateStops(List<VehicleTrackPoint> points, long minStopMinutes) {
    if (points.size() < 2) {
      return Collections.emptyList();
    }
    List<MiniVehicleStopDto> rows = new ArrayList<>();
    VehicleTrackPoint stopStart = null;
    for (VehicleTrackPoint point : points) {
      boolean stopped = point.getSpeed() == null || point.getSpeed().compareTo(BigDecimal.ONE) <= 0;
      if (stopped && stopStart == null) {
        stopStart = point;
      } else if (!stopped && stopStart != null) {
        long duration = ChronoUnit.MINUTES.between(stopStart.getLocateTime(), point.getLocateTime());
        if (duration >= minStopMinutes) {
          rows.add(
              new MiniVehicleStopDto(
                  stopStart.getLocateTime().format(ISO),
                  point.getLocateTime().format(ISO),
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
            new MiniVehicleStopDto(
                stopStart.getLocateTime().format(ISO),
                last.getLocateTime().format(ISO),
                duration,
                stopStart.getLng(),
                stopStart.getLat(),
                stopStart.getRemark()));
      }
    }
    rows.sort(Comparator.comparing(MiniVehicleStopDto::getStartTime, Comparator.nullsLast(String::compareTo)).reversed());
    return rows;
  }

  private List<VehicleTrackPoint> loadTrackPoints(
      Long tenantId, Vehicle vehicle, LocalDateTime start, LocalDateTime end) {
    List<VehicleTrackPoint> points =
        vehicleTrackPointMapper.selectList(
            new LambdaQueryWrapper<VehicleTrackPoint>()
                .eq(VehicleTrackPoint::getTenantId, tenantId)
                .eq(VehicleTrackPoint::getVehicleId, vehicle.getId())
                .ge(start != null, VehicleTrackPoint::getLocateTime, start)
                .le(end != null, VehicleTrackPoint::getLocateTime, end)
                .orderByAsc(VehicleTrackPoint::getLocateTime)
                .orderByAsc(VehicleTrackPoint::getId));
    if (!points.isEmpty() || vehicle.getLng() == null || vehicle.getLat() == null) {
      return points;
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
    fallback.setLocateTime(vehicle.getGpsTime() != null ? vehicle.getGpsTime() : LocalDateTime.now());
    fallback.setSourceType("GPS_REALTIME");
    fallback.setRemark("fallback realtime point");
    return List.of(fallback);
  }

  private VehicleTrackPointDto toTrackPointDto(VehicleTrackPoint point) {
    VehicleTrackPointDto dto = new VehicleTrackPointDto();
    dto.setId(point.getId() != null ? String.valueOf(point.getId()) : null);
    dto.setLng(point.getLng());
    dto.setLat(point.getLat());
    dto.setSpeed(point.getSpeed());
    dto.setDirection(point.getDirection());
    dto.setLocateTime(point.getLocateTime() != null ? point.getLocateTime().format(ISO) : null);
    dto.setSourceType(point.getSourceType());
    dto.setRemark(point.getRemark());
    return dto;
  }

  private Vehicle requireVehicle(Long id, Long tenantId) {
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId)) {
      throw new BizException(404, "车辆不存在");
    }
    return vehicle;
  }

  private Map<Long, Org> loadOrgMap(List<Vehicle> vehicles) {
    LinkedHashSet<Long> orgIds =
        vehicles.stream().map(Vehicle::getOrgId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    return loadOrgMapByIds(orgIds);
  }

  private Map<Long, Org> loadOrgMapByIds(LinkedHashSet<Long> orgIds) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(Org::getId, item -> item, (left, right) -> left));
  }

  private LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
    if (!StringUtils.hasText(value)) {
      return defaultValue;
    }
    try {
      return LocalDateTime.parse(value.trim(), ISO);
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  private String resolveUserDisplayName(User user) {
    if (user == null) {
      return null;
    }
    return StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getUsername();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String normalizeText(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private boolean isAdminUser(User user) {
    return user != null
        && StringUtils.hasText(user.getUserType())
        && Arrays.asList("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN")
            .contains(user.getUserType().trim().toUpperCase());
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
