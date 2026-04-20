package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleInsuranceRecord;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleInsuranceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleInsuranceListItemDto;
import com.xngl.web.dto.vehicle.VehicleInsuranceSummaryDto;
import com.xngl.web.dto.vehicle.VehicleInsuranceUpsertDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle-insurances")
public class VehicleInsurancesController {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private final VehicleInsuranceRecordMapper insuranceMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public VehicleInsurancesController(
      VehicleInsuranceRecordMapper insuranceMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.insuranceMapper = insuranceMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleInsuranceListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String startDateFrom,
      @RequestParam(required = false) String startDateTo,
      @RequestParam(required = false) String endDateFrom,
      @RequestParam(required = false) String endDateTo,
      @RequestParam(required = false) Integer expiringWithinDays,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleInsuranceListItemDto> rows =
        new ArrayList<>(
            loadRows(
                currentUser.getTenantId(),
                keyword,
                status,
                orgId,
                vehicleId,
                parseDate(startDateFrom),
                parseDate(startDateTo),
                parseDate(endDateFrom),
                parseDate(endDateTo),
                expiringWithinDays));
    rows.sort(
        Comparator.comparing(
                VehicleInsuranceListItemDto::getEndDate, Comparator.nullsLast(String::compareTo))
            .thenComparing(
                VehicleInsuranceListItemDto::getPolicyNo, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleInsuranceSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String startDateFrom,
      @RequestParam(required = false) String startDateTo,
      @RequestParam(required = false) String endDateFrom,
      @RequestParam(required = false) String endDateTo,
      @RequestParam(required = false) Integer expiringWithinDays,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        toSummary(
            loadRows(
                currentUser.getTenantId(),
                keyword,
                status,
                orgId,
                vehicleId,
                parseDate(startDateFrom),
                parseDate(startDateTo),
                parseDate(endDateFrom),
                parseDate(endDateTo),
                expiringWithinDays)));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String startDateFrom,
      @RequestParam(required = false) String startDateTo,
      @RequestParam(required = false) String endDateFrom,
      @RequestParam(required = false) String endDateTo,
      @RequestParam(required = false) Integer expiringWithinDays,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleInsuranceListItemDto> rows =
        loadRows(
            currentUser.getTenantId(),
            keyword,
            status,
            orgId,
            vehicleId,
            parseDate(startDateFrom),
            parseDate(startDateTo),
            parseDate(endDateFrom),
            parseDate(endDateTo),
            expiringWithinDays);
    String csv = buildInsuranceCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicle_insurances.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.getBytes(StandardCharsets.UTF_8));
  }

  @PostMapping
  public ApiResult<VehicleInsuranceListItemDto> create(
      @RequestBody VehicleInsuranceUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId(), null);
    VehicleInsuranceRecord entity = new VehicleInsuranceRecord();
    entity.setTenantId(currentUser.getTenantId());
    applyUpsert(entity, body, currentUser.getTenantId());
    insuranceMapper.insert(entity);
    syncVehicleInsuranceExpireDate(entity.getVehicleId());
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleInsuranceListItemDto> update(
      @PathVariable Long id,
      @RequestBody VehicleInsuranceUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleInsuranceRecord entity = requireInsurance(id, currentUser.getTenantId());
    Long previousVehicleId = entity.getVehicleId();
    validateUpsert(body, currentUser.getTenantId(), id);
    applyUpsert(entity, body, currentUser.getTenantId());
    insuranceMapper.updateById(entity);
    syncVehicleInsuranceExpireDate(previousVehicleId);
    syncVehicleInsuranceExpireDate(entity.getVehicleId());
    return ApiResult.ok(loadDto(id, currentUser.getTenantId()));
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleInsuranceRecord entity = requireInsurance(id, currentUser.getTenantId());
    Long vehicleId = entity.getVehicleId();
    insuranceMapper.deleteById(id);
    syncVehicleInsuranceExpireDate(vehicleId);
    return ApiResult.ok();
  }

  private List<VehicleInsuranceListItemDto> loadRows(
      Long tenantId,
      String keyword,
      String status,
      Long orgId,
      Long vehicleId,
      LocalDate startDateFrom,
      LocalDate startDateTo,
      LocalDate endDateFrom,
      LocalDate endDateTo,
      Integer expiringWithinDays) {
    List<VehicleInsuranceRecord> rows =
        insuranceMapper.selectList(
            new LambdaQueryWrapper<VehicleInsuranceRecord>()
                .eq(VehicleInsuranceRecord::getTenantId, tenantId)
                .eq(vehicleId != null, VehicleInsuranceRecord::getVehicleId, vehicleId)
                .orderByAsc(VehicleInsuranceRecord::getEndDate)
                .orderByDesc(VehicleInsuranceRecord::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(rows);
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    String keywordValue = StringUtils.hasText(keyword) ? keyword.trim() : null;
    String statusValue = StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    return rows.stream()
        .map(item -> toDto(item, vehicleMap.get(item.getVehicleId()), orgMap))
        .filter(dto -> matchKeyword(dto, keywordValue))
        .filter(dto -> !StringUtils.hasText(statusValue) || statusValue.equalsIgnoreCase(dto.getStatus()))
        .filter(dto -> orgId == null || Objects.equals(parseLong(dto.getOrgId()), orgId))
        .filter(dto -> matchDateRange(dto.getStartDate(), startDateFrom, startDateTo))
        .filter(dto -> matchDateRange(dto.getEndDate(), endDateFrom, endDateTo))
        .filter(dto -> matchExpiringWithinDays(dto.getRemainingDays(), dto.getStatus(), expiringWithinDays))
        .toList();
  }

  private boolean matchDateRange(String value, LocalDate from, LocalDate to) {
    if (value == null && (from != null || to != null)) {
      return false;
    }
    if (!StringUtils.hasText(value)) {
      return true;
    }
    LocalDate date = parseDate(value);
    if (date == null) {
      return false;
    }
    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
  }

  private boolean matchExpiringWithinDays(Integer remainingDays, String status, Integer expiringWithinDays) {
    if (expiringWithinDays == null) {
      return true;
    }
    if (!"ACTIVE".equalsIgnoreCase(status) && !"EXPIRING".equalsIgnoreCase(status)) {
      return false;
    }
    return remainingDays != null && remainingDays >= 0 && remainingDays <= expiringWithinDays;
  }

  private String buildInsuranceCsv(List<VehicleInsuranceListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("车牌号,所属单位,保单号,险种,承保公司,保额,保费,赔付,开始日期,结束日期,剩余天数,状态,备注\n");
    for (VehicleInsuranceListItemDto row : rows) {
      builder
          .append(csv(row.getPlateNo())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getPolicyNo())).append(',')
          .append(csv(row.getInsuranceType())).append(',')
          .append(csv(row.getInsurerName())).append(',')
          .append(defaultDecimal(row.getCoverageAmount())).append(',')
          .append(defaultDecimal(row.getPremiumAmount())).append(',')
          .append(defaultDecimal(row.getClaimAmount())).append(',')
          .append(csv(row.getStartDate())).append(',')
          .append(csv(row.getEndDate())).append(',')
          .append(row.getRemainingDays() != null ? row.getRemainingDays() : "").append(',')
          .append(csv(row.getStatusLabel())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
  }

  private VehicleInsuranceSummaryDto toSummary(List<VehicleInsuranceListItemDto> rows) {
    return new VehicleInsuranceSummaryDto(
        rows.size(),
        (int) rows.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count(),
        (int) rows.stream().filter(item -> "EXPIRING".equalsIgnoreCase(item.getStatus())).count(),
        (int) rows.stream().filter(item -> "EXPIRED".equalsIgnoreCase(item.getStatus())).count(),
        rows.stream()
            .map(VehicleInsuranceListItemDto::getCoverageAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .map(VehicleInsuranceListItemDto::getPremiumAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .map(VehicleInsuranceListItemDto::getClaimAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add));
  }

  private VehicleInsuranceListItemDto loadDto(Long id, Long tenantId) {
    VehicleInsuranceRecord entity = requireInsurance(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity));
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    return toDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap);
  }

  private Map<Long, Vehicle> loadVehicleMap(List<VehicleInsuranceRecord> rows) {
    LinkedHashSet<Long> vehicleIds =
        rows.stream()
            .map(VehicleInsuranceRecord::getVehicleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (vehicleIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return vehicleMapper.selectBatchIds(vehicleIds).stream()
        .filter(vehicle -> vehicle.getId() != null)
        .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));
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
        .filter(org -> org.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private VehicleInsuranceListItemDto toDto(
      VehicleInsuranceRecord entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehicleInsuranceListItemDto dto = new VehicleInsuranceListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setVehicleId(vehicle != null && vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    Long orgId = vehicle != null ? vehicle.getOrgId() : null;
    dto.setOrgId(orgId != null ? String.valueOf(orgId) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(orgId), orgId));
    dto.setPolicyNo(entity.getPolicyNo());
    dto.setInsuranceType(entity.getInsuranceType());
    dto.setInsurerName(entity.getInsurerName());
    dto.setCoverageAmount(defaultDecimal(entity.getCoverageAmount()));
    dto.setPremiumAmount(defaultDecimal(entity.getPremiumAmount()));
    dto.setClaimAmount(defaultDecimal(entity.getClaimAmount()));
    dto.setStartDate(formatDate(entity.getStartDate()));
    dto.setEndDate(formatDate(entity.getEndDate()));
    String resolvedStatus = resolveInsuranceStatus(entity.getStatus(), entity.getEndDate());
    dto.setStatus(resolvedStatus);
    dto.setStatusLabel(resolveStatusLabel(resolvedStatus));
    dto.setRemainingDays(
        entity.getEndDate() != null
            ? (int) ChronoUnit.DAYS.between(LocalDate.now(), entity.getEndDate())
            : null);
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private boolean matchKeyword(VehicleInsuranceListItemDto dto, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(dto.getPlateNo(), keyword)
        || contains(dto.getOrgName(), keyword)
        || contains(dto.getPolicyNo(), keyword)
        || contains(dto.getInsuranceType(), keyword)
        || contains(dto.getInsurerName(), keyword);
  }

  private void validateUpsert(VehicleInsuranceUpsertDto body, Long tenantId, Long currentId) {
    if (body == null || body.getVehicleId() == null) {
      throw new BizException(400, "请选择车辆");
    }
    requireVehicle(body.getVehicleId(), tenantId);
    if (!StringUtils.hasText(body.getPolicyNo())) {
      throw new BizException(400, "保单号不能为空");
    }
    if (body.getStartDate() == null || body.getEndDate() == null) {
      throw new BizException(400, "保险起止日期不能为空");
    }
    if (body.getEndDate().isBefore(body.getStartDate())) {
      throw new BizException(400, "保险结束日期不能早于开始日期");
    }
    Long existing =
        insuranceMapper.selectCount(
            new LambdaQueryWrapper<VehicleInsuranceRecord>()
                .eq(VehicleInsuranceRecord::getTenantId, tenantId)
                .eq(VehicleInsuranceRecord::getPolicyNo, body.getPolicyNo().trim())
                .ne(currentId != null, VehicleInsuranceRecord::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "保单号已存在");
    }
  }

  private void applyUpsert(VehicleInsuranceRecord entity, VehicleInsuranceUpsertDto body, Long tenantId) {
    Vehicle vehicle = requireVehicle(body.getVehicleId(), tenantId);
    entity.setVehicleId(vehicle.getId());
    entity.setPolicyNo(body.getPolicyNo() != null ? body.getPolicyNo().trim() : null);
    entity.setInsuranceType(trimToNull(body.getInsuranceType()));
    entity.setInsurerName(trimToNull(body.getInsurerName()));
    entity.setCoverageAmount(defaultDecimal(body.getCoverageAmount()));
    entity.setPremiumAmount(defaultDecimal(body.getPremiumAmount()));
    entity.setClaimAmount(defaultDecimal(body.getClaimAmount()));
    entity.setStartDate(body.getStartDate());
    entity.setEndDate(body.getEndDate());
    entity.setRemark(trimToNull(body.getRemark()));
    entity.setStatus(resolveInsuranceStatus(body.getStatus(), body.getEndDate()));
  }

  private String resolveInsuranceStatus(String rawStatus, LocalDate endDate) {
    if (StringUtils.hasText(rawStatus) && "CANCELLED".equalsIgnoreCase(rawStatus.trim())) {
      return "CANCELLED";
    }
    if (endDate == null) {
      return "ACTIVE";
    }
    long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    if (remainingDays < 0) {
      return "EXPIRED";
    }
    if (remainingDays <= 30) {
      return "EXPIRING";
    }
    return "ACTIVE";
  }

  private String resolveStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未知";
    }
    return switch (status.trim().toUpperCase()) {
      case "ACTIVE" -> "有效";
      case "EXPIRING" -> "即将到期";
      case "EXPIRED" -> "已过期";
      case "CANCELLED" -> "已失效";
      default -> status;
    };
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

  private void syncVehicleInsuranceExpireDate(Long vehicleId) {
    if (vehicleId == null) {
      return;
    }
    Vehicle vehicle = vehicleMapper.selectById(vehicleId);
    if (vehicle == null) {
      return;
    }
    LocalDate latestEndDate =
        insuranceMapper.selectList(
                new LambdaQueryWrapper<VehicleInsuranceRecord>()
                    .eq(VehicleInsuranceRecord::getVehicleId, vehicleId)
                    .orderByDesc(VehicleInsuranceRecord::getEndDate)
                    .orderByDesc(VehicleInsuranceRecord::getId))
            .stream()
            .map(VehicleInsuranceRecord::getEndDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
    vehicle.setInsuranceExpireDate(latestEndDate);
    vehicleMapper.updateById(vehicle);
  }

  private VehicleInsuranceRecord requireInsurance(Long id, Long tenantId) {
    VehicleInsuranceRecord entity = insuranceMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "保险记录不存在");
    }
    return entity;
  }

  private Vehicle requireVehicle(Long vehicleId, Long tenantId) {
    Vehicle vehicle = vehicleMapper.selectById(vehicleId);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId)) {
      throw new BizException(400, "车辆不存在");
    }
    return vehicle;
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

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.contains(keyword);
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private LocalDate parseDate(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), ISO_DATE);
    } catch (Exception ex) {
      throw new BizException(400, "日期格式错误，应为 yyyy-MM-dd");
    }
  }

  private String csv(String value) {
    if (value == null) {
      return "";
    }
    return '"' + value.replace("\"", "\"\"") + '"';
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
