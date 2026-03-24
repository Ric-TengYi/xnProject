package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleViolationRecord;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleViolationRecordMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleViolationDetailDto;
import com.xngl.web.dto.vehicle.VehicleDisableRequestDto;
import com.xngl.web.dto.vehicle.VehicleReleaseRequestDto;
import com.xngl.web.dto.vehicle.VehicleViolationRecordDto;
import com.xngl.web.dto.vehicle.VehicleViolationSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
@RequestMapping("/api/vehicles/violations")
public class VehicleViolationsController {

  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final VehicleViolationRecordMapper vehicleViolationRecordMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public VehicleViolationsController(
      VehicleViolationRecordMapper vehicleViolationRecordMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.vehicleViolationRecordMapper = vehicleViolationRecordMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleViolationRecordDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String violationType,
      @RequestParam(required = false) String actionStatus,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    LocalDateTime start = parseDateTime(startTime);
    LocalDateTime end = parseDateTime(endTime);
    LambdaQueryWrapper<VehicleViolationRecord> query =
        buildQuery(currentUser.getTenantId(), keyword, violationType, actionStatus, start, end);
    query
        .orderByDesc(VehicleViolationRecord::getTriggerTime)
        .orderByDesc(VehicleViolationRecord::getId);

    IPage<VehicleViolationRecord> page =
        vehicleViolationRecordMapper.selectPage(new Page<>(pageNo, pageSize), query);
    Map<Long, Org> orgMap = loadOrgMap(page.getRecords());
    List<VehicleViolationRecordDto> records =
        page.getRecords().stream()
            .map(item -> toDto(item, orgMap.get(item.getOrgId())))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleViolationSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String violationType,
      @RequestParam(required = false) String actionStatus,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    LocalDateTime start = parseDateTime(startTime);
    LocalDateTime end = parseDateTime(endTime);
    List<VehicleViolationRecord> rows =
        vehicleViolationRecordMapper.selectList(
            buildQuery(currentUser.getTenantId(), keyword, violationType, actionStatus, start, end));
    Set<Long> vehicleIds =
        rows.stream()
            .map(VehicleViolationRecord::getVehicleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return ApiResult.ok(
        new VehicleViolationSummaryDto(
            rows.size(),
            (int) rows.stream().filter(item -> "PENDING".equalsIgnoreCase(item.getActionStatus())).count(),
            (int) rows.stream().filter(item -> "PROCESSED".equalsIgnoreCase(item.getActionStatus())).count(),
            (int) rows.stream().filter(item -> "DISABLED".equalsIgnoreCase(item.getActionStatus())).count(),
            (int) rows.stream().filter(item -> "RELEASED".equalsIgnoreCase(item.getActionStatus())).count(),
            vehicleIds.size()));
  }

  @GetMapping("/{id}")
  public ApiResult<VehicleViolationDetailDto> detail(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleViolationRecord record = vehicleViolationRecordMapper.selectById(id);
    if (record == null || !Objects.equals(record.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "违法记录不存在");
    }
    Org org = record.getOrgId() != null ? orgMapper.selectById(record.getOrgId()) : null;
    Vehicle vehicle = record.getVehicleId() != null ? vehicleMapper.selectById(record.getVehicleId()) : null;
    VehicleViolationRecordDto base = toDto(record, org);
    VehicleViolationDetailDto detail = new VehicleViolationDetailDto();
    detail.setId(base.getId());
    detail.setVehicleId(base.getVehicleId());
    detail.setPlateNo(base.getPlateNo());
    detail.setOrgId(base.getOrgId());
    detail.setOrgName(base.getOrgName());
    detail.setViolationType(base.getViolationType());
    detail.setTriggerTime(base.getTriggerTime());
    detail.setTriggerLocation(base.getTriggerLocation());
    detail.setActionStatus(base.getActionStatus());
    detail.setActionStatusLabel(base.getActionStatusLabel());
    detail.setPenaltyResult(base.getPenaltyResult());
    detail.setBanStartTime(base.getBanStartTime());
    detail.setBanEndTime(base.getBanEndTime());
    detail.setReleaseTime(base.getReleaseTime());
    detail.setReleaseReason(base.getReleaseReason());
    detail.setOperatorName(base.getOperatorName());
    detail.setRemark(base.getRemark());
    detail.setVehicleType(vehicle != null ? vehicle.getVehicleType() : null);
    detail.setBrand(vehicle != null ? vehicle.getBrand() : null);
    detail.setModel(vehicle != null ? vehicle.getModel() : null);
    detail.setDriverName(vehicle != null ? vehicle.getDriverName() : null);
    detail.setDriverPhone(vehicle != null ? vehicle.getDriverPhone() : null);
    detail.setFleetName(vehicle != null ? vehicle.getFleetName() : null);
    detail.setUseStatus(vehicle != null ? vehicle.getUseStatus() : null);
    detail.setStatus(vehicle != null ? vehicle.getStatus() : null);
    detail.setCurrentSpeed(vehicle != null ? vehicle.getCurrentSpeed() : null);
    detail.setCurrentMileage(vehicle != null ? vehicle.getCurrentMileage() : null);
    return ApiResult.ok(detail);
  }

  private LambdaQueryWrapper<VehicleViolationRecord> buildQuery(
      Long tenantId,
      String keyword,
      String violationType,
      String actionStatus,
      LocalDateTime start,
      LocalDateTime end) {
    LambdaQueryWrapper<VehicleViolationRecord> query =
        new LambdaQueryWrapper<VehicleViolationRecord>()
            .eq(VehicleViolationRecord::getTenantId, tenantId);
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(VehicleViolationRecord::getPlateNo, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getTriggerLocation, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getPenaltyResult, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getReleaseReason, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getOperatorName, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getRemark, effectiveKeyword));
    }
    if (StringUtils.hasText(violationType)) {
      query.eq(VehicleViolationRecord::getViolationType, violationType.trim());
    }
    if (StringUtils.hasText(actionStatus)) {
      query.eq(VehicleViolationRecord::getActionStatus, actionStatus.trim().toUpperCase());
    }
    query.ge(start != null, VehicleViolationRecord::getTriggerTime, start)
        .le(end != null, VehicleViolationRecord::getTriggerTime, end);
    return query;
  }

  @PostMapping("/disable")
  public ApiResult<VehicleViolationRecordDto> disable(
      @RequestBody VehicleDisableRequestDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body.getVehicleId() == null) {
      throw new BizException(400, "请选择车辆");
    }
    if (!StringUtils.hasText(body.getViolationType())) {
      throw new BizException(400, "请选择违规类型");
    }
    Vehicle vehicle = vehicleMapper.selectById(body.getVehicleId());
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "车辆不存在");
    }
    boolean alreadyDisabled =
        vehicleViolationRecordMapper.selectCount(
                new LambdaQueryWrapper<VehicleViolationRecord>()
                    .eq(VehicleViolationRecord::getTenantId, currentUser.getTenantId())
                    .eq(VehicleViolationRecord::getVehicleId, body.getVehicleId())
                    .eq(VehicleViolationRecord::getActionStatus, "DISABLED"))
            > 0;
    if (alreadyDisabled) {
      throw new BizException(400, "该车辆已处于禁用中");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime triggerTime = parseDateTime(body.getTriggerTime());
    LocalDateTime banStartTime = parseDateTime(body.getBanStartTime());
    LocalDateTime banEndTime = parseDateTime(body.getBanEndTime());
    if (triggerTime == null) {
      triggerTime = now;
    }
    if (banStartTime == null) {
      banStartTime = triggerTime;
    }
    if (banEndTime == null) {
      banEndTime = banStartTime.plusDays(body.getBanDays() != null && body.getBanDays() > 0 ? body.getBanDays() : 3);
    }
    if (!banEndTime.isAfter(banStartTime)) {
      throw new BizException(400, "禁用结束时间必须晚于开始时间");
    }

    VehicleViolationRecord record = new VehicleViolationRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setVehicleId(vehicle.getId());
    record.setPlateNo(vehicle.getPlateNo());
    record.setOrgId(vehicle.getOrgId());
    record.setViolationType(body.getViolationType().trim());
    record.setTriggerTime(triggerTime);
    record.setTriggerLocation(trimToNull(body.getTriggerLocation()));
    record.setActionStatus("DISABLED");
    record.setPenaltyResult(
        StringUtils.hasText(body.getPenaltyResult())
            ? body.getPenaltyResult().trim()
            : "禁用至 " + banEndTime.format(ISO_DATE_TIME));
    record.setBanStartTime(banStartTime);
    record.setBanEndTime(banEndTime);
    record.setOperatorName(resolveOperatorName(currentUser));
    record.setRemark(trimToNull(body.getRemark()));
    vehicleViolationRecordMapper.insert(record);

    vehicle.setStatus(3);
    vehicle.setUseStatus("DISABLED");
    vehicleMapper.updateById(vehicle);

    Org org = vehicle.getOrgId() != null ? orgMapper.selectById(vehicle.getOrgId()) : null;
    return ApiResult.ok(toDto(vehicleViolationRecordMapper.selectById(record.getId()), org));
  }

  @PutMapping("/{id}/release")
  public ApiResult<VehicleViolationRecordDto> release(
      @PathVariable Long id,
      @RequestBody VehicleReleaseRequestDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleViolationRecord record = vehicleViolationRecordMapper.selectById(id);
    if (record == null || !Objects.equals(record.getTenantId(), currentUser.getTenantId())) {
      throw new BizException(404, "禁用记录不存在");
    }
    if (!"DISABLED".equalsIgnoreCase(record.getActionStatus())) {
      throw new BizException(400, "当前记录不是禁用中状态");
    }
    record.setActionStatus("RELEASED");
    record.setReleaseTime(LocalDateTime.now());
    record.setReleaseReason(
        StringUtils.hasText(body.getReleaseReason()) ? body.getReleaseReason().trim() : "人工提前解禁");
    record.setOperatorName(resolveOperatorName(currentUser));
    vehicleViolationRecordMapper.updateById(record);

    long activeCount =
        vehicleViolationRecordMapper.selectCount(
            new LambdaQueryWrapper<VehicleViolationRecord>()
                .eq(VehicleViolationRecord::getTenantId, currentUser.getTenantId())
                .eq(VehicleViolationRecord::getVehicleId, record.getVehicleId())
                .eq(VehicleViolationRecord::getActionStatus, "DISABLED"));
    if (activeCount == 0 && record.getVehicleId() != null) {
      Vehicle vehicle = vehicleMapper.selectById(record.getVehicleId());
      if (vehicle != null && Objects.equals(vehicle.getTenantId(), currentUser.getTenantId())) {
        vehicle.setStatus(1);
        vehicle.setUseStatus("ACTIVE");
        vehicleMapper.updateById(vehicle);
      }
    }

    Org org = record.getOrgId() != null ? orgMapper.selectById(record.getOrgId()) : null;
    return ApiResult.ok(toDto(vehicleViolationRecordMapper.selectById(id), org));
  }

  private VehicleViolationRecordDto toDto(VehicleViolationRecord record, Org org) {
    return new VehicleViolationRecordDto(
        record.getId() != null ? String.valueOf(record.getId()) : null,
        record.getVehicleId() != null ? String.valueOf(record.getVehicleId()) : null,
        record.getPlateNo(),
        record.getOrgId() != null ? String.valueOf(record.getOrgId()) : null,
        org != null ? org.getOrgName() : null,
        record.getViolationType(),
        formatDateTime(record.getTriggerTime()),
        record.getTriggerLocation(),
        record.getActionStatus(),
        resolveActionStatusLabel(record.getActionStatus()),
        record.getPenaltyResult(),
        formatDateTime(record.getBanStartTime()),
        formatDateTime(record.getBanEndTime()),
        formatDateTime(record.getReleaseTime()),
        record.getReleaseReason(),
        record.getOperatorName(),
        record.getRemark());
  }

  private Map<Long, Org> loadOrgMap(List<VehicleViolationRecord> records) {
    LinkedHashSet<Long> orgIds =
        records.stream()
            .map(VehicleViolationRecord::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private String resolveActionStatusLabel(String actionStatus) {
    if (!StringUtils.hasText(actionStatus)) {
      return "待处理";
    }
    return switch (actionStatus.trim().toUpperCase()) {
      case "PENDING" -> "待处理";
      case "PROCESSED" -> "已处理";
      case "DISABLED" -> "禁用中";
      case "RELEASED" -> "已解禁";
      default -> actionStatus;
    };
  }

  private String resolveOperatorName(User currentUser) {
    if (StringUtils.hasText(currentUser.getName())) {
      return currentUser.getName();
    }
    return currentUser.getUsername();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
