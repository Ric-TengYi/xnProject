package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.SettlementItem;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.SettlementService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.SettlementDetailDto;
import com.xngl.web.dto.contract.SettlementItemDto;
import com.xngl.web.dto.contract.SettlementLineDto;
import com.xngl.web.dto.contract.SettlementStatsDto;
import com.xngl.web.dto.contract.SettlementGenerateDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final SettlementService settlementService;
  private final UserService userService;

  public SettlementController(SettlementService settlementService, UserService userService) {
    this.settlementService = settlementService;
    this.userService = userService;
  }

  @PostMapping("/project/generate")
  public ApiResult<String> generateProject(
      @Valid @RequestBody SettlementGenerateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    long id =
        settlementService.generateProjectSettlement(
            user.getTenantId(),
            user.getId(),
            dto.getTargetId(),
            dto.getPeriodStart(),
            dto.getPeriodEnd(),
            dto.getRemark());
    return ApiResult.ok(String.valueOf(id));
  }

  @PostMapping("/site/generate")
  public ApiResult<String> generateSite(
      @Valid @RequestBody SettlementGenerateDto dto, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    long id =
        settlementService.generateSiteSettlement(
            user.getTenantId(),
            user.getId(),
            dto.getTargetId(),
            dto.getPeriodStart(),
            dto.getPeriodEnd(),
            dto.getRemark());
    return ApiResult.ok(String.valueOf(id));
  }

  @GetMapping
  public ApiResult<PageResult<SettlementItemDto>> list(
      @RequestParam(required = false) String settlementType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<SettlementOrder> page =
        settlementService.pageSettlements(
            user.getTenantId(), settlementType, status, projectId, siteId, pageNo, pageSize);
    List<SettlementItemDto> records = page.getRecords().stream().map(this::toItemDto).toList();
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<SettlementDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    SettlementOrder order = settlementService.getSettlement(id, user.getTenantId());
    List<SettlementItem> items =
        settlementService.listSettlementItems(id, user.getTenantId());
    return ApiResult.ok(toDetailDto(order, items));
  }

  @PostMapping("/{id}/submit")
  public ApiResult<Void> submit(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    settlementService.submitSettlement(id, user.getTenantId());
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/approve")
  public ApiResult<Void> approve(@PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    settlementService.approveSettlement(id, user.getTenantId());
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long id,
      @RequestBody(required = false) ApprovalActionDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    settlementService.rejectSettlement(
        id, user.getTenantId(), dto != null ? dto.getReason() : null);
    return ApiResult.ok(null);
  }

  @GetMapping("/stats")
  public ApiResult<SettlementStatsDto> stats(HttpServletRequest request) {
    User user = requireCurrentUser(request);
    Map<String, Object> raw = settlementService.getSettlementStats(user.getTenantId());
    SettlementStatsDto stats = new SettlementStatsDto();
    stats.setPendingAmount((BigDecimal) raw.get("pendingAmount"));
    stats.setSettledAmount((BigDecimal) raw.get("settledAmount"));
    stats.setTotalOrders((long) raw.get("totalOrders"));
    stats.setDraftOrders((long) raw.get("draftOrders"));
    stats.setPendingOrders((long) raw.get("pendingOrders"));
    stats.setSettledOrders((long) raw.get("settledOrders"));
    return ApiResult.ok(stats);
  }

  private SettlementItemDto toItemDto(SettlementOrder order) {
    SettlementItemDto dto = new SettlementItemDto();
    dto.setId(stringValue(order.getId()));
    dto.setSettlementNo(order.getSettlementNo());
    dto.setSettlementType(order.getSettlementType());
    dto.setTargetProjectId(stringValue(order.getTargetProjectId()));
    dto.setTargetSiteId(stringValue(order.getTargetSiteId()));
    dto.setPeriodStart(formatDate(order.getPeriodStart()));
    dto.setPeriodEnd(formatDate(order.getPeriodEnd()));
    dto.setTotalVolume(order.getTotalVolume());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setAdjustAmount(order.getAdjustAmount());
    dto.setPayableAmount(order.getPayableAmount());
    dto.setApprovalStatus(order.getApprovalStatus());
    dto.setSettlementStatus(order.getSettlementStatus());
    dto.setCreatorId(stringValue(order.getCreatorId()));
    dto.setCreateTime(formatDateTime(order.getCreateTime()));
    return dto;
  }

  private SettlementDetailDto toDetailDto(SettlementOrder order, List<SettlementItem> items) {
    SettlementDetailDto dto = new SettlementDetailDto();
    dto.setId(stringValue(order.getId()));
    dto.setSettlementNo(order.getSettlementNo());
    dto.setSettlementType(order.getSettlementType());
    dto.setTargetProjectId(stringValue(order.getTargetProjectId()));
    dto.setTargetSiteId(stringValue(order.getTargetSiteId()));
    dto.setPeriodStart(formatDate(order.getPeriodStart()));
    dto.setPeriodEnd(formatDate(order.getPeriodEnd()));
    dto.setTotalVolume(order.getTotalVolume());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setAdjustAmount(order.getAdjustAmount());
    dto.setPayableAmount(order.getPayableAmount());
    dto.setApprovalStatus(order.getApprovalStatus());
    dto.setSettlementStatus(order.getSettlementStatus());
    dto.setCreatorId(stringValue(order.getCreatorId()));
    dto.setCreateTime(formatDateTime(order.getCreateTime()));
    dto.setUnitPrice(order.getUnitPrice());
    dto.setSettlementDate(formatDate(order.getSettlementDate()));
    dto.setProcessInstanceId(order.getProcessInstanceId());
    dto.setRemark(order.getRemark());
    dto.setItems(items.stream().map(this::toLineDto).toList());
    return dto;
  }

  private SettlementLineDto toLineDto(SettlementItem item) {
    SettlementLineDto dto = new SettlementLineDto();
    dto.setId(stringValue(item.getId()));
    dto.setSourceRecordType(item.getSourceRecordType());
    dto.setSourceRecordId(stringValue(item.getSourceRecordId()));
    dto.setProjectId(stringValue(item.getProjectId()));
    dto.setSiteId(stringValue(item.getSiteId()));
    dto.setVehicleId(stringValue(item.getVehicleId()));
    dto.setBizDate(formatDate(item.getBizDate()));
    dto.setVolume(item.getVolume());
    dto.setUnitPrice(item.getUnitPrice());
    dto.setAmount(item.getAmount());
    dto.setRemark(item.getRemark());
    return dto;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (userId == null || userId.isBlank()) {
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

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }
}
