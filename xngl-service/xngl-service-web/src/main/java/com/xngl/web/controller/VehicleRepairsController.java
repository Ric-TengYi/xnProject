package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleRepairOrder;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleRepairOrderMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleRepairAuditDto;
import com.xngl.web.dto.vehicle.VehicleRepairCompleteDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderListItemDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderSummaryDto;
import com.xngl.web.dto.vehicle.VehicleRepairOrderUpsertDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
@RequestMapping("/api/vehicle-repairs")
public class VehicleRepairsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final VehicleRepairOrderMapper orderMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public VehicleRepairsController(
      VehicleRepairOrderMapper orderMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.orderMapper = orderMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleRepairOrderListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String urgencyLevel,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String applyDateFrom,
      @RequestParam(required = false) String applyDateTo,
      @RequestParam(required = false) String completedDateFrom,
      @RequestParam(required = false) String completedDateTo,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleRepairOrderListItemDto> rows =
        new ArrayList<>(
            loadRows(
                currentUser.getTenantId(),
                keyword,
                status,
                urgencyLevel,
                orgId,
                vehicleId,
                parseDate(applyDateFrom),
                parseDate(applyDateTo),
                parseDate(completedDateFrom),
                parseDate(completedDateTo)));
    rows.sort(
        Comparator.comparing(
                VehicleRepairOrderListItemDto::getApplyDate, Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/{id}")
  public ApiResult<VehicleRepairOrderListItemDto> detail(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadDto(id, currentUser.getTenantId()));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleRepairOrderSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String urgencyLevel,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String applyDateFrom,
      @RequestParam(required = false) String applyDateTo,
      @RequestParam(required = false) String completedDateFrom,
      @RequestParam(required = false) String completedDateTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleRepairOrderListItemDto> rows =
        loadRows(
            currentUser.getTenantId(),
            keyword,
            status,
            urgencyLevel,
            orgId,
            vehicleId,
            parseDate(applyDateFrom),
            parseDate(applyDateTo),
            parseDate(completedDateFrom),
            parseDate(completedDateTo));
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

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String urgencyLevel,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) String applyDateFrom,
      @RequestParam(required = false) String applyDateTo,
      @RequestParam(required = false) String completedDateFrom,
      @RequestParam(required = false) String completedDateTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleRepairOrderListItemDto> rows =
        loadRows(
            currentUser.getTenantId(),
            keyword,
            status,
            urgencyLevel,
            orgId,
            vehicleId,
            parseDate(applyDateFrom),
            parseDate(applyDateTo),
            parseDate(completedDateFrom),
            parseDate(completedDateTo));
    String csv = buildRepairCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicle_repairs.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.getBytes(StandardCharsets.UTF_8));
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
    entity.setRepairManager(body != null ? trimToNull(body.getRepairManager()) : null);
    entity.setTechnicianName(body != null ? trimToNull(body.getTechnicianName()) : null);
    entity.setAcceptanceResult(body != null ? trimToNull(body.getAcceptanceResult()) : null);
    entity.setSignoffStatus(defaultStatus(body != null ? body.getSignoffStatus() : null, "SIGNED"));
    entity.setAttachmentUrls(body != null ? trimToNull(body.getAttachmentUrls()) : null);
    entity.setPartsCost(body != null ? defaultDecimal(body.getPartsCost()) : ZERO);
    entity.setLaborCost(body != null ? defaultDecimal(body.getLaborCost()) : ZERO);
    entity.setOtherCost(body != null ? defaultDecimal(body.getOtherCost()) : ZERO);
    entity.setActualAmount(resolveActualAmount(body));
    entity.setRemark(body != null ? trimToNull(body.getRemark()) : entity.getRemark());
    if (entity.getApprovedBy() == null) {
      entity.setApprovedBy(resolveUserName(currentUser));
      entity.setApprovedTime(LocalDateTime.now());
    }
    orderMapper.updateById(entity);
    return ApiResult.ok(loadDto(entity.getId(), currentUser.getTenantId()));
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleRepairOrder entity = requireOrder(id, currentUser.getTenantId());
    if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
      throw new BizException(400, "已完成维修单不支持删除");
    }
    orderMapper.deleteById(id);
    return ApiResult.ok();
  }

  private List<VehicleRepairOrderListItemDto> loadRows(
      Long tenantId,
      String keyword,
      String status,
      String urgencyLevel,
      Long orgId,
      Long vehicleId,
      LocalDate applyDateFrom,
      LocalDate applyDateTo,
      LocalDate completedDateFrom,
      LocalDate completedDateTo) {
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
        .filter(item -> matchDateRange(item.getApplyDate(), applyDateFrom, applyDateTo))
        .filter(item -> matchDateRange(item.getCompletedDate(), completedDateFrom, completedDateTo))
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

  private String buildRepairCsv(List<VehicleRepairOrderListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("维修单号,车牌号,所属单位,紧急度,维修原因,维修内容,诊断结果,安全影响,预算金额,申请日期,申请人,状态,审批人,审批时间,完工日期,维修单位,维修负责人,维修技师,验收结果,签字状态,实际金额,配件费,人工费,其他费用,预算偏差,审批意见,备注\n");
    for (VehicleRepairOrderListItemDto row : rows) {
      builder
          .append(csv(row.getOrderNo())).append(',')
          .append(csv(row.getPlateNo())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getUrgencyLabel())).append(',')
          .append(csv(row.getRepairReason())).append(',')
          .append(csv(row.getRepairContent())).append(',')
          .append(csv(row.getDiagnosisResult())).append(',')
          .append(csv(row.getSafetyImpact())).append(',')
          .append(defaultDecimal(row.getBudgetAmount())).append(',')
          .append(csv(row.getApplyDate())).append(',')
          .append(csv(row.getApplicantName())).append(',')
          .append(csv(row.getStatusLabel())).append(',')
          .append(csv(row.getApprovedBy())).append(',')
          .append(csv(row.getApprovedTime())).append(',')
          .append(csv(row.getCompletedDate())).append(',')
          .append(csv(row.getVendorName())).append(',')
          .append(csv(row.getRepairManager())).append(',')
          .append(csv(row.getTechnicianName())).append(',')
          .append(csv(row.getAcceptanceResult())).append(',')
          .append(csv(row.getSignoffStatusLabel())).append(',')
          .append(defaultDecimal(row.getActualAmount())).append(',')
          .append(defaultDecimal(row.getPartsCost())).append(',')
          .append(defaultDecimal(row.getLaborCost())).append(',')
          .append(defaultDecimal(row.getOtherCost())).append(',')
          .append(defaultDecimal(row.getCostVariance())).append(',')
          .append(csv(row.getAuditRemark())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
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
    dto.setDiagnosisResult(entity.getDiagnosisResult());
    dto.setSafetyImpact(entity.getSafetyImpact());
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
    dto.setRepairManager(entity.getRepairManager());
    dto.setTechnicianName(entity.getTechnicianName());
    dto.setAcceptanceResult(entity.getAcceptanceResult());
    dto.setSignoffStatus(defaultStatus(entity.getSignoffStatus(), "UNSIGNED"));
    dto.setSignoffStatusLabel(resolveSignoffStatusLabel(dto.getSignoffStatus()));
    dto.setAttachmentUrls(entity.getAttachmentUrls());
    dto.setActualAmount(defaultDecimal(entity.getActualAmount()));
    dto.setPartsCost(defaultDecimal(entity.getPartsCost()));
    dto.setLaborCost(defaultDecimal(entity.getLaborCost()));
    dto.setOtherCost(defaultDecimal(entity.getOtherCost()));
    dto.setCostVariance(defaultDecimal(entity.getActualAmount()).subtract(defaultDecimal(entity.getBudgetAmount())));
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
    entity.setDiagnosisResult(trimToNull(body.getDiagnosisResult()));
    entity.setSafetyImpact(trimToNull(body.getSafetyImpact()));
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
    return userContext.requireCurrentUser(request);
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

  private String resolveSignoffStatusLabel(String value) {
    return switch (defaultStatus(value, "UNSIGNED")) {
      case "SIGNED" -> "已签字";
      case "WAIVED" -> "免签";
      default -> "未签字";
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

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private BigDecimal resolveActualAmount(VehicleRepairCompleteDto body) {
    BigDecimal explicit = body != null ? body.getActualAmount() : null;
    if (explicit != null && explicit.compareTo(ZERO) > 0) {
      return explicit;
    }
    BigDecimal parts = body != null ? defaultDecimal(body.getPartsCost()) : ZERO;
    BigDecimal labor = body != null ? defaultDecimal(body.getLaborCost()) : ZERO;
    BigDecimal other = body != null ? defaultDecimal(body.getOtherCost()) : ZERO;
    return parts.add(labor).add(other);
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
