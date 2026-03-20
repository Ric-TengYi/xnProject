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
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.report.ReportTrendItemDto;
import com.xngl.web.dto.report.SiteReportItemDto;
import com.xngl.web.dto.report.SiteReportSummaryDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
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

  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final SiteService siteService;
  private final ExportTaskService exportTaskService;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  public SiteReportsController(
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      SiteService siteService,
      ExportTaskService exportTaskService,
      UserService userService,
      ObjectMapper objectMapper) {
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.siteService = siteService;
    this.exportTaskService = exportTaskService;
    this.userService = userService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/summary")
  public ApiResult<SiteReportSummaryDto> summary(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date);
    List<SiteReportItemDto> rows = buildSiteRows(user.getTenantId(), period, siteId, keyword);
    return ApiResult.ok(toSummary(period, rows));
  }

  @GetMapping("/list")
  public ApiResult<PageResult<SiteReportItemDto>> list(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date);
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
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow current = resolvePeriodWindow(periodType, date);
    List<ReportTrendItemDto> records = new ArrayList<>();
    int safeLimit = Math.max(limit, 1);
    for (int i = safeLimit - 1; i >= 0; i--) {
      PeriodWindow period = shiftPeriod(current, -i);
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
    try {
      long taskId =
          exportTaskService.createExportTask(
              user.getTenantId(),
              user.getId(),
              "SITE_REPORT",
              "EXCEL",
              objectMapper.writeValueAsString(body));
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

  private PeriodWindow resolvePeriodWindow(String rawType, LocalDate date) {
    String type = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase() : "MONTH";
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

  private PeriodWindow shiftPeriod(PeriodWindow current, int step) {
    return switch (current.type()) {
      case "DAY" -> {
        LocalDate next = current.start().plusDays(step);
        yield new PeriodWindow(next, next, ISO_DATE.format(next), "DAY");
      }
      case "YEAR" -> {
        LocalDate next = current.start().plusYears(step);
        yield resolvePeriodWindow("YEAR", next);
      }
      default -> {
        LocalDate next = current.start().plusMonths(step);
        yield resolvePeriodWindow("MONTH", next);
      }
    };
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (!StringUtils.hasText(userId)) {
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

  private record PeriodWindow(LocalDate start, LocalDate end, String label, String type) {}
}
