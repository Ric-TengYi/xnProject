package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.report.ReportTrendItemDto;
import com.xngl.web.dto.report.SiteReportItemDto;
import com.xngl.web.dto.report.SiteReportSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/sites")
public class SiteReportsController {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
  private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final String EXPORT_DIR = "xngl-exports/site-reports";

  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final SiteService siteService;
  private final ExportTaskService exportTaskService;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  public SiteReportsController(
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      SiteService siteService,
      ExportTaskService exportTaskService,
      UserContext userContext,
      ObjectMapper objectMapper) {
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.siteService = siteService;
    this.exportTaskService = exportTaskService;
    this.userContext = userContext;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/summary")
  public ApiResult<SiteReportSummaryDto> summary(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date, startDate, endDate);
    List<SiteReportItemDto> rows = buildSiteRows(user.getTenantId(), period, siteId, keyword);
    return ApiResult.ok(toSummary(period, rows));
  }

  @GetMapping("/list")
  public ApiResult<PageResult<SiteReportItemDto>> list(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date, startDate, endDate);
    List<SiteReportItemDto> rows =
        new ArrayList<>(buildSiteRows(user.getTenantId(), period, siteId, keyword));
    rows.sort(
        Comparator.comparing(SiteReportItemDto::getPeriodVolume, Comparator.nullsFirst(BigDecimal::compareTo))
            .reversed()
            .thenComparing(SiteReportItemDto::getSiteName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/trend")
  public ApiResult<List<ReportTrendItemDto>> trend(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    List<ReportTrendItemDto> records = new ArrayList<>();
    for (PeriodWindow period : resolveTrendWindows(periodType, date, startDate, endDate, limit)) {
      List<SiteReportItemDto> rows = buildSiteRows(user.getTenantId(), period, siteId, keyword);
      SiteReportSummaryDto summary = toSummary(period, rows);
      records.add(
          new ReportTrendItemDto(
              period.label(),
              summary.getPeriodVolume(),
              summary.getPeriodAmount(),
              summary.getTotalTrips(),
              summary.getActiveSiteCount()));
    }
    return ApiResult.ok(records);
  }

  @PostMapping("/export")
  public ApiResult<Map<String, String>> export(
      @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    Map<String, Object> safeBody = body != null ? body : Collections.emptyMap();
    try {
      String periodType = readString(safeBody.get("periodType"));
      LocalDate date = parseDate(safeBody.get("date"));
      LocalDate startDate = parseDate(safeBody.get("startDate"));
      LocalDate endDate = parseDate(safeBody.get("endDate"));
      Long siteId = parseLong(safeBody.get("siteId"));
      String keyword = readString(safeBody.get("keyword"));
      long taskId =
          exportTaskService.createExportTask(
              user.getTenantId(),
              user.getId(),
              "SITE_REPORT",
              "EXCEL",
              objectMapper.writeValueAsString(safeBody));
      generateSiteReportCsv(
          taskId,
          user.getTenantId(),
          resolvePeriodWindow(periodType, date, startDate, endDate),
          siteId,
          keyword);
      return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
    } catch (JsonProcessingException ex) {
      throw new BizException(400, "查询参数序列化失败");
    }
  }

  private List<SiteReportItemDto> buildSiteRows(
      Long tenantId, PeriodWindow period, Long siteId, String keyword) {
    List<Contract> contracts =
        contractMapper.selectList(new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    if (contracts.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, Site> siteMap =
        siteService.list().stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    Map<Long, List<Contract>> contractsBySite =
        contracts.stream()
            .filter(contract -> contract.getSiteId() != null)
            .filter(contract -> siteMap.containsKey(contract.getSiteId()))
            .filter(contract -> siteId == null || Objects.equals(contract.getSiteId(), siteId))
            .collect(Collectors.groupingBy(Contract::getSiteId, LinkedHashMap::new, Collectors.toList()));
    if (contractsBySite.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, List<ContractTicket>> ticketsByContract =
        loadTicketsByContract(
            contracts.stream()
                .map(Contract::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

    return contractsBySite.entrySet().stream()
        .map(entry -> toSiteReportItem(period, siteMap.get(entry.getKey()), entry.getValue(), ticketsByContract))
        .filter(Objects::nonNull)
        .filter(item -> matchKeyword(item, keyword))
        .toList();
  }

  private SiteReportItemDto toSiteReportItem(
      PeriodWindow period,
      Site site,
      List<Contract> siteContracts,
      Map<Long, List<ContractTicket>> ticketsByContract) {
    if (site == null) {
      return null;
    }
    BigDecimal periodVolume = ZERO;
    BigDecimal periodAmount = ZERO;
    BigDecimal accumulatedVolume = ZERO;
    int periodTrips = 0;

    for (Contract contract : siteContracts) {
      for (ContractTicket ticket : ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
        LocalDate ticketDate = resolveTicketDate(ticket);
        if (ticketDate == null || ticketDate.isAfter(period.end())) {
          continue;
        }
        BigDecimal volume = defaultDecimal(ticket.getVolume());
        BigDecimal amount = defaultDecimal(ticket.getAmount());
        accumulatedVolume = accumulatedVolume.add(volume);
        if (!ticketDate.isBefore(period.start()) && !ticketDate.isAfter(period.end())) {
          periodVolume = periodVolume.add(volume);
          periodAmount = periodAmount.add(amount);
          periodTrips++;
        }
      }
    }

    BigDecimal capacity = deriveSiteCapacity(site, accumulatedVolume);
    BigDecimal remainingCapacity = capacity.subtract(accumulatedVolume).max(ZERO);
    int utilizationRate = calculatePercent(accumulatedVolume, capacity);
    return new SiteReportItemDto(
        String.valueOf(site.getId()),
        site.getName(),
        site.getCode(),
        resolveSiteType(site),
        period.label(),
        periodVolume,
        periodAmount,
        periodTrips,
        accumulatedVolume,
        capacity,
        remainingCapacity,
        utilizationRate,
        resolveSiteStatus(site.getStatus(), accumulatedVolume, capacity));
  }

  private SiteReportSummaryDto toSummary(PeriodWindow period, List<SiteReportItemDto> rows) {
    BigDecimal periodVolume = rows.stream().map(SiteReportItemDto::getPeriodVolume).reduce(ZERO, BigDecimal::add);
    BigDecimal periodAmount = rows.stream().map(SiteReportItemDto::getPeriodAmount).reduce(ZERO, BigDecimal::add);
    BigDecimal totalCapacity = rows.stream().map(SiteReportItemDto::getCapacity).reduce(ZERO, BigDecimal::add);
    BigDecimal accumulatedVolume = rows.stream().map(SiteReportItemDto::getAccumulatedVolume).reduce(ZERO, BigDecimal::add);
    int totalTrips = rows.stream().map(SiteReportItemDto::getPeriodTrips).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    int activeSites = (int) rows.stream().filter(row -> row.getPeriodVolume() != null && row.getPeriodVolume().compareTo(ZERO) > 0).count();
    return new SiteReportSummaryDto(
        period.type(),
        period.label(),
        rows.size(),
        activeSites,
        totalTrips,
        periodVolume,
        periodAmount,
        totalCapacity,
        accumulatedVolume,
        calculatePercent(accumulatedVolume, totalCapacity));
  }

  private Map<Long, List<ContractTicket>> loadTicketsByContract(LinkedHashSet<Long> contractIds) {
    if (contractIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return contractTicketMapper.selectList(
            new LambdaQueryWrapper<ContractTicket>().in(ContractTicket::getContractId, contractIds))
        .stream()
        .filter(ticket -> ticket.getContractId() != null)
        .collect(Collectors.groupingBy(ContractTicket::getContractId, LinkedHashMap::new, Collectors.toList()));
  }

  private boolean matchKeyword(SiteReportItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String value = keyword.trim();
    return contains(item.getSiteName(), value)
        || contains(item.getSiteCode(), value)
        || contains(item.getSiteType(), value)
        || contains(item.getStatus(), value);
  }

  private boolean contains(String source, String keyword) {
    return source != null && source.contains(keyword);
  }

  private PageResult<SiteReportItemDto> paginate(List<SiteReportItemDto> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(
        safePageNo,
        safePageSize,
        rows.size(),
        new ArrayList<>(rows.subList(fromIndex, toIndex)));
  }

  private LocalDate resolveTicketDate(ContractTicket ticket) {
    if (ticket.getTicketDate() != null) {
      return ticket.getTicketDate();
    }
    return ticket.getCreateTime() != null ? ticket.getCreateTime().toLocalDate() : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private int calculatePercent(BigDecimal current, BigDecimal total) {
    if (current == null || total == null || total.compareTo(ZERO) <= 0) {
      return 0;
    }
    return current.multiply(BigDecimal.valueOf(100))
        .divide(total, 0, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(100))
        .intValue();
  }

  private BigDecimal deriveSiteCapacity(Site site, BigDecimal accumulatedVolume) {
    if (site.getCapacity() != null && site.getCapacity().compareTo(ZERO) > 0) {
      return site.getCapacity();
    }
    BigDecimal base = BigDecimal.valueOf(((site.getId() != null ? site.getId() : 1L) % 7) + 3L)
        .multiply(BigDecimal.valueOf(100000L));
    BigDecimal dynamic = accumulatedVolume.multiply(BigDecimal.valueOf(1.2));
    return dynamic.compareTo(base) > 0 ? dynamic : base;
  }

  private String resolveSiteType(Site site) {
    if (site != null && StringUtils.hasText(site.getSiteType())) {
      return switch (site.getSiteType().trim().toUpperCase()) {
        case "STATE_OWNED" -> "国有场地";
        case "COLLECTIVE" -> "集体场地";
        case "ENGINEERING" -> "工程场地";
        case "SHORT_BARGE" -> "短驳场地";
        default -> site.getSiteType();
      };
    }
    return "场地";
  }

  private String resolveSiteStatus(Integer status, BigDecimal used, BigDecimal capacity) {
    if (used != null && capacity != null && capacity.compareTo(ZERO) > 0) {
      BigDecimal percent = used.multiply(BigDecimal.valueOf(100)).divide(capacity, 0, RoundingMode.HALF_UP);
      if (percent.compareTo(BigDecimal.valueOf(95)) >= 0) {
        return "满载";
      }
      if (percent.compareTo(BigDecimal.valueOf(80)) >= 0) {
        return "预警";
      }
    }
    if (status != null && status == 0) {
      return "停用";
    }
    return "正常";
  }

  private PeriodWindow resolvePeriodWindow(
      String rawType, LocalDate date, LocalDate startDate, LocalDate endDate) {
    String type = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase() : "MONTH";
    if ("CUSTOM".equals(type) || startDate != null || endDate != null) {
      if (startDate == null || endDate == null) {
        throw new BizException(400, "自定义时间查询需要同时传入开始和结束日期");
      }
      if (endDate.isBefore(startDate)) {
        throw new BizException(400, "结束日期不能早于开始日期");
      }
      return new PeriodWindow(
          startDate,
          endDate,
          ISO_DATE.format(startDate) + " ~ " + ISO_DATE.format(endDate),
          "CUSTOM");
    }
    LocalDate baseDate = date != null ? date : LocalDate.now();
    return switch (type) {
      case "DAY" -> new PeriodWindow(baseDate, baseDate, ISO_DATE.format(baseDate), "DAY");
      case "YEAR" -> {
        LocalDate start = baseDate.withDayOfYear(1);
        LocalDate end = baseDate.withDayOfYear(baseDate.lengthOfYear());
        yield new PeriodWindow(start, end, YEAR_FORMATTER.format(baseDate), "YEAR");
      }
      default -> {
        YearMonth yearMonth = YearMonth.from(baseDate);
        yield new PeriodWindow(yearMonth.atDay(1), yearMonth.atEndOfMonth(), YM_FORMATTER.format(baseDate), "MONTH");
      }
    };
  }

  private List<PeriodWindow> resolveTrendWindows(
      String rawType,
      LocalDate date,
      LocalDate startDate,
      LocalDate endDate,
      int limit) {
    PeriodWindow current = resolvePeriodWindow(rawType, date, startDate, endDate);
    if ("CUSTOM".equals(current.type())) {
      return resolveCustomTrendWindows(current.start(), current.end());
    }
    List<PeriodWindow> windows = new ArrayList<>();
    int safeLimit = Math.max(limit, 1);
    for (int i = safeLimit - 1; i >= 0; i--) {
      windows.add(shiftPeriod(current, -i));
    }
    return windows;
  }

  private PeriodWindow shiftPeriod(PeriodWindow current, int step) {
    return switch (current.type()) {
      case "DAY" -> {
        LocalDate next = current.start().plusDays(step);
        yield new PeriodWindow(next, next, ISO_DATE.format(next), "DAY");
      }
      case "YEAR" -> {
        LocalDate next = current.start().plusYears(step);
        yield resolvePeriodWindow("YEAR", next, null, null);
      }
      default -> {
        LocalDate next = current.start().plusMonths(step);
        yield resolvePeriodWindow("MONTH", next, null, null);
      }
    };
  }

  private List<PeriodWindow> resolveCustomTrendWindows(LocalDate startDate, LocalDate endDate) {
    long dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    List<PeriodWindow> windows = new ArrayList<>();
    if (dayCount <= 31) {
      LocalDate cursor = startDate;
      while (!cursor.isAfter(endDate)) {
        windows.add(new PeriodWindow(cursor, cursor, ISO_DATE.format(cursor), "DAY"));
        cursor = cursor.plusDays(1);
      }
      return windows;
    }
    YearMonth month = YearMonth.from(startDate);
    YearMonth last = YearMonth.from(endDate);
    while (!month.isAfter(last)) {
      LocalDate monthStart = month.atDay(1);
      LocalDate monthEnd = month.atEndOfMonth();
      LocalDate actualStart = monthStart.isBefore(startDate) ? startDate : monthStart;
      LocalDate actualEnd = monthEnd.isAfter(endDate) ? endDate : monthEnd;
      windows.add(new PeriodWindow(actualStart, actualEnd, YM_FORMATTER.format(actualStart), "MONTH"));
      month = month.plusMonths(1);
    }
    return windows;
  }

  private void generateSiteReportCsv(
      Long taskId, Long tenantId, PeriodWindow period, Long siteId, String keyword) {
    exportTaskService.markProcessing(taskId, tenantId);
    try {
      List<SiteReportItemDto> rows = new ArrayList<>(buildSiteRows(tenantId, period, siteId, keyword));
      rows.sort(
          Comparator.comparing(SiteReportItemDto::getPeriodVolume, Comparator.nullsFirst(BigDecimal::compareTo))
              .reversed()
              .thenComparing(SiteReportItemDto::getSiteName, Comparator.nullsLast(String::compareTo)));
      Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), EXPORT_DIR);
      Files.createDirectories(exportDir);
      String fileName = "site_reports_" + LocalDateTime.now().format(FILE_TIME) + ".csv";
      Path filePath = exportDir.resolve(fileName);
      writeCsv(filePath, rows);
      exportTaskService.completeExportTask(taskId, tenantId, fileName, filePath.toString());
    } catch (Exception ex) {
      exportTaskService.failExportTask(taskId, tenantId, truncateFailReason(ex.getMessage()));
      throw ex instanceof RuntimeException runtimeException
          ? runtimeException
          : new BizException(500, "场地报表导出失败");
    }
  }

  private void writeCsv(Path filePath, List<SiteReportItemDto> rows) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
      writer.write('\uFEFF');
      writer.write("场地名称,场地编码,场地类型,统计周期,本期消纳量,本期结算金额,本期趟次,累计消纳量,总容量,剩余容量,容量使用率,状态");
      writer.newLine();
      for (SiteReportItemDto row : rows) {
        List<String> values =
            List.of(
                valueOf(row.getSiteName()),
                valueOf(row.getSiteCode()),
                valueOf(row.getSiteType()),
                valueOf(row.getReportPeriod()),
                decimalOf(row.getPeriodVolume()),
                decimalOf(row.getPeriodAmount()),
                numberOf(row.getPeriodTrips()),
                decimalOf(row.getAccumulatedVolume()),
                decimalOf(row.getCapacity()),
                decimalOf(row.getRemainingCapacity()),
                numberOf(row.getUtilizationRate()) + "%",
                valueOf(row.getStatus()));
        writer.write(String.join(",", values.stream().map(this::escapeCsv).toList()));
        writer.newLine();
      }
    }
  }

  private String escapeCsv(String value) {
    String sanitized = value == null ? "" : value;
    boolean needQuote =
        sanitized.contains(",")
            || sanitized.contains("\"")
            || sanitized.contains("\n")
            || sanitized.contains("\r");
    if (!needQuote) {
      return sanitized;
    }
    return "\"" + sanitized.replace("\"", "\"\"") + "\"";
  }

  private String decimalOf(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  private String numberOf(Integer value) {
    return value == null ? "0" : String.valueOf(value);
  }

  private String valueOf(String value) {
    return value == null ? "" : value;
  }

  private String truncateFailReason(String message) {
    if (!StringUtils.hasText(message)) {
      return "导出失败";
    }
    String trimmed = message.trim();
    return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
  }

  private String readString(Object value) {
    return value == null ? null : String.valueOf(value).trim();
  }

  private LocalDate parseDate(Object value) {
    String text = readString(value);
    return StringUtils.hasText(text) ? LocalDate.parse(text) : null;
  }

  private Long parseLong(Object value) {
    String text = readString(value);
    if (!StringUtils.hasText(text)) {
      return null;
    }
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException ex) {
      throw new BizException(400, "场地参数格式不正确");
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private record PeriodWindow(LocalDate start, LocalDate end, String label, String type) {}
}
