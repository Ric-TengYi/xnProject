package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehiclePersonnelCertificate;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehiclePersonnelCertificateMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehiclePersonnelCertificateListItemDto;
import com.xngl.web.dto.vehicle.VehiclePersonnelCertificateSummaryDto;
import com.xngl.web.dto.vehicle.VehiclePersonnelCertificateUpsertDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
@RequestMapping("/api/vehicle-personnel-certificates")
public class VehiclePersonnelCertificatesController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final VehiclePersonnelCertificateMapper certificateMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserService userService;

  public VehiclePersonnelCertificatesController(
      VehiclePersonnelCertificateMapper certificateMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserService userService) {
    this.certificateMapper = certificateMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<VehiclePersonnelCertificateListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String roleType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehiclePersonnelCertificateListItemDto> rows =
        new ArrayList<>(
            loadRows(currentUser.getTenantId(), keyword, roleType, status, orgId, vehicleId));
    rows.sort(
        Comparator.comparing(
                VehiclePersonnelCertificateListItemDto::getRemainingDays,
                Comparator.nullsLast(Integer::compareTo))
            .thenComparing(
                VehiclePersonnelCertificateListItemDto::getPersonName,
                Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/summary")
  public ApiResult<VehiclePersonnelCertificateSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String roleType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehiclePersonnelCertificateListItemDto> rows =
        loadRows(currentUser.getTenantId(), keyword, roleType, status, orgId, vehicleId);
    BigDecimal totalFeeAmount =
        rows.stream()
            .map(VehiclePersonnelCertificateListItemDto::getFeeAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal paidAmount =
        rows.stream()
            .map(VehiclePersonnelCertificateListItemDto::getPaidAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal unpaidAmount =
        rows.stream()
            .map(VehiclePersonnelCertificateListItemDto::getUnpaidAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    return ApiResult.ok(
        new VehiclePersonnelCertificateSummaryDto(
            rows.size(),
            (int) rows.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count(),
            (int) rows.stream().filter(item -> "EXPIRING".equalsIgnoreCase(item.getStatus())).count(),
            (int) rows.stream().filter(item -> "EXPIRED".equalsIgnoreCase(item.getStatus())).count(),
            totalFeeAmount,
            paidAmount,
            unpaidAmount));
  }

  @PostMapping
  public ApiResult<VehiclePersonnelCertificateListItemDto> create(
      @RequestBody VehiclePersonnelCertificateUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId());
    VehiclePersonnelCertificate entity = new VehiclePersonnelCertificate();
    entity.setTenantId(currentUser.getTenantId());
    applyUpsert(entity, body, currentUser.getTenantId());
    certificateMapper.insert(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehiclePersonnelCertificateListItemDto> update(
      @PathVariable Long id,
      @RequestBody VehiclePersonnelCertificateUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehiclePersonnelCertificate entity = requireEntity(id, currentUser.getTenantId());
    validateUpsert(body, currentUser.getTenantId());
    applyUpsert(entity, body, currentUser.getTenantId());
    certificateMapper.updateById(entity);
    return ApiResult.ok(loadDto(id, currentUser.getTenantId()));
  }

  private List<VehiclePersonnelCertificateListItemDto> loadRows(
      Long tenantId,
      String keyword,
      String roleType,
      String status,
      Long orgId,
      Long vehicleId) {
    List<VehiclePersonnelCertificate> rows =
        certificateMapper.selectList(
            new LambdaQueryWrapper<VehiclePersonnelCertificate>()
                .eq(VehiclePersonnelCertificate::getTenantId, tenantId)
                .eq(orgId != null, VehiclePersonnelCertificate::getOrgId, orgId)
                .eq(vehicleId != null, VehiclePersonnelCertificate::getVehicleId, vehicleId)
                .orderByAsc(VehiclePersonnelCertificate::getDriverLicenseExpireDate)
                .orderByAsc(VehiclePersonnelCertificate::getTransportLicenseExpireDate)
                .orderByDesc(VehiclePersonnelCertificate::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(rows);
    Map<Long, Org> orgMap = loadOrgMap(rows, vehicleMap);
    String keywordValue = trimToNull(keyword);
    String roleValue = defaultValue(roleType, null);
    String statusValue = defaultValue(status, null);
    return rows.stream()
        .map(item -> toDto(item, vehicleMap.get(item.getVehicleId()), orgMap))
        .filter(item -> matchKeyword(item, keywordValue))
        .filter(item -> roleValue == null || roleValue.equalsIgnoreCase(item.getRoleType()))
        .filter(item -> statusValue == null || statusValue.equalsIgnoreCase(item.getStatus()))
        .toList();
  }

  private VehiclePersonnelCertificateListItemDto loadDto(Long id, Long tenantId) {
    VehiclePersonnelCertificate entity = requireEntity(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity));
    Map<Long, Org> orgMap = loadOrgMap(List.of(entity), vehicleMap);
    return toDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap);
  }

  private Map<Long, Vehicle> loadVehicleMap(List<VehiclePersonnelCertificate> rows) {
    LinkedHashSet<Long> ids =
        rows.stream()
            .map(VehiclePersonnelCertificate::getVehicleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    return vehicleMapper.selectBatchIds(ids).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadOrgMap(
      List<VehiclePersonnelCertificate> rows, Map<Long, Vehicle> vehicleMap) {
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    for (VehiclePersonnelCertificate item : rows) {
      if (item.getOrgId() != null) {
        ids.add(item.getOrgId());
      } else {
        Vehicle vehicle = vehicleMap.get(item.getVehicleId());
        if (vehicle != null && vehicle.getOrgId() != null) {
          ids.add(vehicle.getOrgId());
        }
      }
    }
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(ids).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private VehiclePersonnelCertificateListItemDto toDto(
      VehiclePersonnelCertificate entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehiclePersonnelCertificateListItemDto dto = new VehiclePersonnelCertificateListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setPersonName(entity.getPersonName());
    dto.setMobile(entity.getMobile());
    dto.setRoleType(defaultValue(entity.getRoleType(), "DRIVER"));
    dto.setRoleTypeLabel(resolveRoleLabel(dto.getRoleType()));
    Long currentOrgId = entity.getOrgId() != null ? entity.getOrgId() : vehicle != null ? vehicle.getOrgId() : null;
    dto.setOrgId(currentOrgId != null ? String.valueOf(currentOrgId) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(currentOrgId), currentOrgId));
    dto.setVehicleId(entity.getVehicleId() != null ? String.valueOf(entity.getVehicleId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setIdCardNo(entity.getIdCardNo());
    dto.setDriverLicenseNo(entity.getDriverLicenseNo());
    dto.setDriverLicenseExpireDate(formatDate(entity.getDriverLicenseExpireDate()));
    dto.setTransportLicenseNo(entity.getTransportLicenseNo());
    dto.setTransportLicenseExpireDate(formatDate(entity.getTransportLicenseExpireDate()));
    String resolvedStatus = resolveStatus(entity);
    dto.setStatus(resolvedStatus);
    dto.setStatusLabel(resolveStatusLabel(resolvedStatus));
    dto.setRemainingDays(resolveRemainingDays(entity));
    dto.setFeeAmount(defaultDecimal(entity.getFeeAmount()));
    dto.setPaidAmount(defaultDecimal(entity.getPaidAmount()));
    dto.setUnpaidAmount(defaultDecimal(entity.getFeeAmount()).subtract(defaultDecimal(entity.getPaidAmount())));
    dto.setFeeDueDate(formatDate(entity.getFeeDueDate()));
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private boolean matchKeyword(VehiclePersonnelCertificateListItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getPersonName(), keyword)
        || contains(item.getMobile(), keyword)
        || contains(item.getOrgName(), keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getDriverLicenseNo(), keyword)
        || contains(item.getTransportLicenseNo(), keyword)
        || contains(item.getIdCardNo(), keyword);
  }

  private void validateUpsert(VehiclePersonnelCertificateUpsertDto body, Long tenantId) {
    if (body == null || !StringUtils.hasText(body.getPersonName())) {
      throw new BizException(400, "人员姓名不能为空");
    }
    if (body.getVehicleId() != null) {
      requireVehicle(body.getVehicleId(), tenantId);
    }
    if (body.getPaidAmount() != null
        && body.getFeeAmount() != null
        && body.getPaidAmount().compareTo(body.getFeeAmount()) > 0) {
      throw new BizException(400, "已缴金额不能大于费用金额");
    }
  }

  private void applyUpsert(
      VehiclePersonnelCertificate entity, VehiclePersonnelCertificateUpsertDto body, Long tenantId) {
    Vehicle vehicle = body.getVehicleId() != null ? requireVehicle(body.getVehicleId(), tenantId) : null;
    entity.setVehicleId(vehicle != null ? vehicle.getId() : null);
    entity.setOrgId(
        body.getOrgId() != null ? body.getOrgId() : vehicle != null ? vehicle.getOrgId() : null);
    entity.setPersonName(body.getPersonName().trim());
    entity.setMobile(trimToNull(body.getMobile()));
    entity.setRoleType(defaultValue(body.getRoleType(), "DRIVER"));
    entity.setIdCardNo(trimToNull(body.getIdCardNo()));
    entity.setDriverLicenseNo(trimToNull(body.getDriverLicenseNo()));
    entity.setDriverLicenseExpireDate(body.getDriverLicenseExpireDate());
    entity.setTransportLicenseNo(trimToNull(body.getTransportLicenseNo()));
    entity.setTransportLicenseExpireDate(body.getTransportLicenseExpireDate());
    entity.setFeeAmount(defaultDecimal(body.getFeeAmount()));
    entity.setPaidAmount(defaultDecimal(body.getPaidAmount()));
    entity.setFeeDueDate(body.getFeeDueDate());
    entity.setStatus(defaultValue(body.getStatus(), "ACTIVE"));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private VehiclePersonnelCertificate requireEntity(Long id, Long tenantId) {
    VehiclePersonnelCertificate entity = certificateMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "人证记录不存在");
    }
    return entity;
  }

  private Vehicle requireVehicle(Long id, Long tenantId) {
    Vehicle vehicle = vehicleMapper.selectById(id);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId)) {
      throw new BizException(404, "车辆不存在");
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

  private String resolveRoleLabel(String roleType) {
    return switch (defaultValue(roleType, "DRIVER")) {
      case "CAPTAIN" -> "队长";
      case "DISPATCHER" -> "调度员";
      case "SAFETY_OFFICER" -> "安全员";
      case "LOGISTICS" -> "后勤";
      default -> "司机";
    };
  }

  private String resolveStatus(VehiclePersonnelCertificate entity) {
    String explicitStatus = defaultValue(entity.getStatus(), null);
    if (explicitStatus != null && !"ACTIVE".equalsIgnoreCase(explicitStatus)) {
      return explicitStatus;
    }
    LocalDate now = LocalDate.now();
    LocalDate nearestExpire = resolveNearestExpireDate(entity);
    if (nearestExpire == null) {
      return defaultValue(entity.getStatus(), "ACTIVE");
    }
    if (nearestExpire.isBefore(now)) {
      return "EXPIRED";
    }
    long days = ChronoUnit.DAYS.between(now, nearestExpire);
    if (days <= 30) {
      return "EXPIRING";
    }
    return "ACTIVE";
  }

  private String resolveStatusLabel(String status) {
    return switch (defaultValue(status, "ACTIVE")) {
      case "EXPIRING" -> "即将到期";
      case "EXPIRED" -> "已过期";
      case "DISABLED" -> "已停用";
      default -> "有效";
    };
  }

  private Integer resolveRemainingDays(VehiclePersonnelCertificate entity) {
    LocalDate nearestExpire = resolveNearestExpireDate(entity);
    if (nearestExpire == null) {
      return null;
    }
    return (int) ChronoUnit.DAYS.between(LocalDate.now(), nearestExpire);
  }

  private LocalDate resolveNearestExpireDate(VehiclePersonnelCertificate entity) {
    LocalDate driverExpire = entity.getDriverLicenseExpireDate();
    LocalDate transportExpire = entity.getTransportLicenseExpireDate();
    if (driverExpire == null) {
      return transportExpire;
    }
    if (transportExpire == null) {
      return driverExpire;
    }
    return driverExpire.isBefore(transportExpire) ? driverExpire : transportExpire;
  }

  private String resolveOrgName(Org org, Long orgId) {
    if (org != null && StringUtils.hasText(org.getOrgName())) {
      return org.getOrgName();
    }
    return orgId != null ? "单位#" + orgId : null;
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

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }
}
