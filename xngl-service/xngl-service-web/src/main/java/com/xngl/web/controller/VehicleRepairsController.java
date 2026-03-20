package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleRepairOrder;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleRepairOrderMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleRepairAuditDto;
import com.xngl.web.dto.vehicle.VehicleRepairCompleteDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderListItemDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderSummaryDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderUpsertDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
@RequestMapping("/api/vehicle-repairs")
public class VehicleRepairsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final VehicleRepairOrderMapper orderMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserService userService;

  public VehicleRepairsController(
      VehicleRepairOrderMapper orderMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserService userService) {
    this.orderMapper = orderMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleRepairOrderListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String urgencyLevel,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleRepairOrderListItemDto> rows =
        new ArrayList<>(loadRows(currentUser.getTenantId(), keyword, status, urgencyLevel, orgId, vehicleId));
    rows.sort(
        Comparator.comparing(
                VehicleRepairOrderListItemDto::getApplyDate, Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleRepairOrderSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String urgencyLevel,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleRepairOrderListItemDto> rows =
        loadRows(currentUser.getTenantId(), keyword, status, urgencyLevel, orgId, vehicleId);
    return ApiResult.ok(
        new VehicleRepairOrderSummaryDto(
            rows.size(),
            (int) rows.stream().filter(item -> "PENDING_APPROVAL".equalsIgnoreCase(item.getStatus())).count(),
            (int) rows.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus())).count(),
            (int) rows.stream().filter(item -> "IN_PROGRESS".equalsIgnoreCase(item.getStatus())).count(),
            (int) rows.stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus())).count(),
            rows.stream().map(VehicleRepairOrderListItemDto::getBudgetAmount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add),
            rows.stream().map(VehicleRepairOrderListItemDto::getActualAmount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add)));
  }

  @PostMapping
  public ApiResult<VehicleRepairOrderListItemDto> create(
      @RequestBody VehicleRepairOrderUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId());
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    VehicleRepairOrder entity = new VehicleRepairOrder();
    entity.setTenantId(currentUser.getTenantId());
    entity.setVehicleId(vehicle.getId());
    entity.setOrgId(vehicle.getOrgId());
    entity.setOrderNo("REP-" + System.currentTimeMillis());
    applyUpsert(entity, body, currentUser);
    orderMapper.insert(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleRepairOrderListItemDto> update(
      @PathVariable Long id,
      @RequestBody VehicleRepairOrderUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId());
    VehicleRepairOrder entity = requireOrder(id, currentUser.getTenantId());
    if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
      throw new BizException(400, "已完成维修单不支持修改");
    }
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    entity.setVehicleId(vehicle.getId());
    entity.setOrgId(vehicle.getOrgId());
    applyUpsert(entity, body, currentUser);
    orderMapper.updateById(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PostMapping("/{id}/approve")
  public ApiResult<VehicleRepairOrderListItemDto> approve(
      @PathVariable Long id, @RequestBody VehicleRepairAuditDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleRepairOrder entity = requireOrder(id, currentUser.getTenantId());
    entity.setStatus("APPROVED");
    entity.setAuditRemark(trimToNull(body != null ? body.getComment() : null));
    entity.setApprovedBy(resolveUserName(currentUser));
    entity.setApprovedTime(LocalDateTime.now());
    orderMapper.updateById(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PostMapping("/{id}/reject")
  public ApiResult<VehicleRepairOrderListItemDto> reject(
      @PathVariable Long id, @RequestBody VehicleRepairAuditDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleRepairOrder entity = requireOrder(id, currentUser.getTenantId());
    entity.setStatus("REJECTED");
    entity.setAuditRemark(trimToNull(body != null ? body.getComment() : null));
    entity.setApprovedBy(resolveUserName(currentUser));
    entity.setApprovedTime(LocalDateTime.now());
    orderMapper.updateById(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @PostMapping("/{id}/complete")
  public ApiResult<VehicleRepairOrderListItemDto> complete(
      @PathVariable Long id,
      @RequestBody VehicleRepairCompleteDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleRepairOrder entity = requireOrder(id, currentUser.getTenantId());
    entity.setStatus("COMPLETED");
    entity.setCompletedDate(body != null && body.getCompletedDate() != null ? body.getCompletedDate() : LocalDate.now());
    entity.setVendorName(body != null ? trimToNull(body.getVendorName()) : null);
    entity.setActualAmount(body != null ? body.getActualAmount() : ZERO);
    entity.setRemark(body != null ? trimToNull(body.getRemark()) : entity.getRemark());
    if (entity.getApprovedBy() == null) {
      entity.setApprovedBy(resolveUserName(currentUser));
      entity.setApprovedTime(LocalDateTime.now());
    }
    orderMapper.updateById(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  private List<VehicleRepairOrderListItemDto> loadRows(
      Long tenantId, String keyword, String status, String urgencyLevel, Long orgId, Long vehicleId) {
    List<VehicleRepairOrder> rows =
        orderMapper.selectList(
            new LambdaQueryWrapper<VehicleRepairOrder>()
                .eq(VehicleRepairOrder::getTenantId, tenantId)
                .eq(vehicleId != null, VehicleRepairOrder::getVehicleId, vehicleId)
                .orderByDesc(VehicleRepairOrder::getApplyDate)
                .orderByDesc(VehicleRepairOrder::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(rows.stream().map(VehicleRepairOrder::getVehicleId).toList());
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    String keywordValue = trimToNull(keyword);
    String statusValue = trimToNull(status);
    String urgencyValue = trimToNull(urgencyLevel);
    return rows.stream()
        .map(item -> toDto(item, vehicleMap.get(item.getVehicleId()), orgMap))
        .filter(item -> matchKeyword(item, keywordValue))
        .filter(item -> !StringUtils.hasText(statusValue) || statusValue.equalsIgnoreCase(item.getStatus()))
        .filter(item -> !StringUtils.hasText(urgencyValue) || urgencyValue.equalsIgnoreCase(item.getUrgencyLevel()))
        .filter(item -> orgId == null || Objects.equals(parseLong(item.getOrgId()), orgId))
        .toList();
  }

  private VehicleRepairOrderListItemDto loadDto(Long id, Long tenantId) {
    VehicleRepairOrder entity = requireOrder(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity.getVehicleId()));
    Map<Long, Org> orgMap = loadOrgMap(vehicleMap.values().stream().toList());
    return toDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap);
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

  private VehicleRepairOrderListItemDto toDto(
      VehicleRepairOrder entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehicleRepairOrderListItemDto dto = new VehicleRepairOrderListItemDto();
    dto.setId(String.valueOf(entity.getId()));
    dto.setOrderNo(entity.getOrderNo());
    dto.setVehicleId(entity.getVehicleId() != null ? String.valueOf(entity.getVehicleId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setOrgId(entity.getOrgId() != null ? String.valueOf(entity.getOrgId()) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(entity.getOrgId()), entity.getOrgId()));
    String urgencyLevel = defaultStatus(entity.getUrgencyLevel(), "MEDIUM");
    dto.setUrgencyLevel(urgencyLevel);
    dto.setUrgencyLabel(resolveUrgencyLabel(urgencyLevel));
    dto.setRepairReason(entity.getRepairReason());
    dto.setRepairContent(entity.getRepairContent());
    dto.setBudgetAmount(defaultDecimal(entity.getBudgetAmount()));
    dto.setApplyDate(formatDate(entity.getApplyDate()));
    dto.setApplicantName(entity.getApplicantName());
    String status = defaultStatus(entity.getStatus(), "PENDING_APPROVAL");
    dto.setStatus(status);
    dto.setStatusLabel(resolveStatusLabel(status));
    dto.setApprovedBy(entity.getApprovedBy());
    dto.setApprovedTime(formatDateTime(entity.getApprovedTime()));
    dto.setCompletedDate(formatDate(entity.getCompletedDate()));
    dto.setVendorName(entity.getVendorName());
    dto.setActualAmount(entity.getActualAmount());
    dto.setAuditRemark(entity.getAuditRemark());
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private boolean matchKeyword(VehicleRepairOrderListItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(item.getOrderNo(), keyword)
        || contains(item.getPlateNo(), keyword)
        || contains(item.getOrgName(), keyword)
        || contains(item.getRepairReason(), keyword)
        || contains(item.getRepairContent(), keyword)
        || contains(item.getApplicantName(), keyword);
  }

  private void validateUpsert(VehicleRepairOrderUpsertDto body, Long tenantId) {
    if (body == null || body.getVehicleId() == null) {
      throw new BizException(400, "请选择车辆");
    }
    requireVehicle(body.getVehicleId(), tenantId);
    if (!StringUtils.hasText(body.getRepairReason())) {
      throw new BizException(400, "维修原因不能为空");
    }
  }

  private void applyUpsert(VehicleRepairOrder entity, VehicleRepairOrderUpsertDto body, User currentUser) {
    entity.setUrgencyLevel(defaultStatus(body.getUrgencyLevel(), "MEDIUM"));
    entity.setRepairReason(body.getRepairReason().trim());
    entity.setRepairContent(trimToNull(body.getRepairContent()));
    entity.setBudgetAmount(defaultDecimal(body.getBudgetAmount()));
    entity.setApplyDate(body.getApplyDate() != null ? body.getApplyDate() : LocalDate.now());
    entity.setApplicantName(StringUtils.hasText(body.getApplicantName()) ? body.getApplicantName().trim() : resolveUserName(currentUser));
    entity.setStatus(defaultStatus(body.getStatus(), entity.getId() == null ? "PENDING_APPROVAL" : entity.getStatus()));
    entity.setRemark(trimToNull(body.getRemark()));
  }

  private VehicleRepairOrder requireOrder(Long id, Long tenantId) {
    VehicleRepairOrder entity = orderMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "维修单不存在");
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

  private PageResult<VehicleRepairOrderListItemDto> paginate(
      List<VehicleRepairOrderListItemDto> rows, int pageNo, int pageSize) {
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

  private String resolveUrgencyLabel(String value) {
    return switch (defaultStatus(value, "MEDIUM")) {
      case "HIGH" -> "高";
      case "LOW" -> "低";
      default -> "中";
    };
  }

  private String resolveStatusLabel(String value) {
    return switch (defaultStatus(value, "PENDING_APPROVAL")) {
      case "DRAFT" -> "草稿";
      case "APPROVED" -> "已批准";
      case "REJECTED" -> "已驳回";
      case "IN_PROGRESS" -> "维修中";
      case "COMPLETED" -> "已完成";
      default -> "待审批";
    };
  }

  private String resolveUserName(User currentUser) {
    return StringUtils.hasText(currentUser.getName()) ? currentUser.getName() : currentUser.getUsername();
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.toLowerCase());
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String defaultStatus(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
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
