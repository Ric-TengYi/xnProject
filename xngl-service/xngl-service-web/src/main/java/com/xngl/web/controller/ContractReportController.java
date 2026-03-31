package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractAccessScope;
import com.xngl.manager.contract.ContractReportService;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.MonthlySummaryDto;
import com.xngl.web.dto.contract.MonthlyTrendDto;
import com.xngl.web.dto.contract.MonthlyTypeDto;
import com.xngl.web.dto.contract.UnitStatItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.ContractAccessScopeResolver;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/contracts")
public class ContractReportController {

  private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final ContractReportService reportService;
  private final ExportTaskService exportTaskService;
  private final UserService userService;
  private final UserContext userContext;
  private final ContractAccessScopeResolver contractAccessScopeResolver;
  private final ObjectMapper objectMapper;

  public ContractReportController(
      ContractReportService reportService,
      ExportTaskService exportTaskService,
      UserService userService,
      UserContext userContext,
      ContractAccessScopeResolver contractAccessScopeResolver,
      ObjectMapper objectMapper) {
    this.reportService = reportService;
    this.exportTaskService = exportTaskService;
    this.userService = userService;
    this.userContext = userContext;
    this.contractAccessScopeResolver = contractAccessScopeResolver;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/monthly/summary")
  public ApiResult<MonthlySummaryDto> monthlySummary(
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String effectiveMonth = resolveMonth(month);
    Map<String, Object> data =
        reportService.getMonthlySummary(user.getTenantId(), effectiveMonth, resolveScope(user));
    return ApiResult.ok(toSummaryDto(data));
  }

  @GetMapping("/monthly/trend")
  public ApiResult<List<MonthlyTrendDto>> monthlyTrend(
      @RequestParam(defaultValue = "6") int months,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Map<String, Object>> data =
        reportService.getMonthlyTrend(user.getTenantId(), months, resolveScope(user));
    List<MonthlyTrendDto> result = data.stream().map(this::toTrendDto).toList();
    return ApiResult.ok(result);
  }

  @GetMapping("/monthly/types")
  public ApiResult<List<MonthlyTypeDto>> monthlyTypes(
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String effectiveMonth = resolveMonth(month);
    List<Map<String, Object>> data =
        reportService.getMonthlyTypes(user.getTenantId(), effectiveMonth, resolveScope(user));
    List<MonthlyTypeDto> result = data.stream().map(this::toTypeDto).toList();
    return ApiResult.ok(result);
  }

  @GetMapping("/unit-stats")
  public ApiResult<PageResult<UnitStatItemDto>> unitStats(
      @RequestParam(defaultValue = "construction") String unitType,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<Map<String, Object>> page = reportService.getUnitStats(
        user.getTenantId(), unitType, month, keyword, pageNo, pageSize, resolveScope(user));
    List<UnitStatItemDto> records = page.getRecords().stream()
        .map(this::toUnitStatDto)
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/unit-stats/{orgId}/trend")
  public ApiResult<List<MonthlyTrendDto>> unitTrend(
      @PathVariable Long orgId,
      @RequestParam(defaultValue = "6") int months,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Map<String, Object>> data =
        reportService.getUnitTrend(user.getTenantId(), orgId, months, resolveScope(user));
    List<MonthlyTrendDto> result = data.stream().map(this::toTrendDto).toList();
    return ApiResult.ok(result);
  }

  @PostMapping("/monthly/export")
  public ApiResult<Map<String, String>> exportMonthly(
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String queryJson;
    try {
      queryJson = objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new BizException(400, "查询参数序列化失败");
    }
    long taskId = exportTaskService.createExportTask(
        user.getTenantId(), user.getId(), "MONTHLY_REPORT", "EXCEL", queryJson);
    return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
  }

  // ==================== Daily Report ====================

  @GetMapping("/daily/summary")
  public ApiResult<Map<String, Object>> dailySummary(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    Map<String, Object> data =
        reportService.getDailySummary(user.getTenantId(), date, resolveScope(user));
    return ApiResult.ok(data);
  }

  @GetMapping("/daily/trend")
  public ApiResult<List<Map<String, Object>>> dailyTrend(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Map<String, Object>> data =
        reportService.getDailyTrend(user.getTenantId(), startDate, endDate, resolveScope(user));
    return ApiResult.ok(data);
  }

  // ==================== Yearly Report ====================

  @GetMapping("/yearly/summary")
  public ApiResult<Map<String, Object>> yearlySummary(
      @RequestParam(required = false) Integer year,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    int effectiveYear = year != null ? year : LocalDate.now().getYear();
    Map<String, Object> data =
        reportService.getYearlySummary(user.getTenantId(), effectiveYear, resolveScope(user));
    return ApiResult.ok(data);
  }

  @GetMapping("/yearly/trend")
  public ApiResult<List<Map<String, Object>>> yearlyTrend(
      @RequestParam(defaultValue = "5") int years,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Map<String, Object>> data =
        reportService.getYearlyTrend(user.getTenantId(), years, resolveScope(user));
    return ApiResult.ok(data);
  }

  // ==================== Custom Period Report ====================

  @GetMapping("/custom/summary")
  public ApiResult<Map<String, Object>> customPeriodSummary(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    Map<String, Object> data =
        reportService.getCustomPeriodSummary(user.getTenantId(), startDate, endDate, resolveScope(user));
    return ApiResult.ok(data);
  }

  @GetMapping("/custom/trend")
  public ApiResult<List<Map<String, Object>>> customPeriodTrend(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "day") String groupBy,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<Map<String, Object>> data =
        reportService.getCustomPeriodTrend(
            user.getTenantId(), startDate, endDate, groupBy, resolveScope(user));
    return ApiResult.ok(data);
  }

  private MonthlySummaryDto toSummaryDto(Map<String, Object> data) {
    MonthlySummaryDto dto = new MonthlySummaryDto();
    dto.setMonth(str(data.get("month")));
    dto.setContractCount(toInt(data.get("contractCount")));
    dto.setNewContractCount(toInt(data.get("newContractCount")));
    dto.setContractAmount(toBd(data.get("contractAmount")));
    dto.setReceiptAmount(toBd(data.get("receiptAmount")));
    dto.setSettlementAmount(toBd(data.get("settlementAmount")));
    dto.setAgreedVolume(toBd(data.get("agreedVolume")));
    dto.setActualVolume(toBd(data.get("actualVolume")));
    return dto;
  }

  private MonthlyTrendDto toTrendDto(Map<String, Object> data) {
    MonthlyTrendDto dto = new MonthlyTrendDto();
    dto.setMonth(str(data.get("month")));
    dto.setVolume(toBd(data.get("volume")));
    dto.setAmount(toBd(data.get("amount")));
    dto.setReceiptAmount(toBd(data.get("receiptAmount")));
    return dto;
  }

  private MonthlyTypeDto toTypeDto(Map<String, Object> data) {
    MonthlyTypeDto dto = new MonthlyTypeDto();
    dto.setContractType(str(data.get("contractType")));
    dto.setCount(toInt(data.get("count")));
    dto.setAmount(toBd(data.get("amount")));
    dto.setVolume(toBd(data.get("volume")));
    return dto;
  }

  private UnitStatItemDto toUnitStatDto(Map<String, Object> data) {
    UnitStatItemDto dto = new UnitStatItemDto();
    dto.setOrgId(str(data.get("orgId")));
    dto.setContractCount(toInt(data.get("contractCount")));
    dto.setContractAmount(toBd(data.get("contractAmount")));
    dto.setReceivedAmount(toBd(data.get("receivedAmount")));
    dto.setPendingAmount(toBd(data.get("pendingAmount")));
    dto.setSettledAmount(toBd(data.get("settledAmount")));
    dto.setAgreedVolume(toBd(data.get("agreedVolume")));
    return dto;
  }

  private String resolveMonth(String month) {
    if (month != null && !month.isBlank()) {
      return month.trim();
    }
    return YearMonth.now().format(MONTH_FMT);
  }

  private String str(Object value) {
    return value != null ? value.toString() : null;
  }

  private BigDecimal toBd(Object value) {
    if (value == null) return ZERO;
    if (value instanceof BigDecimal bd) return bd;
    return new BigDecimal(value.toString());
  }

  private Integer toInt(Object value) {
    if (value == null) return 0;
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(value.toString());
  }

  private ContractAccessScope resolveScope(User currentUser) {
    return contractAccessScopeResolver.resolve(currentUser);
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
