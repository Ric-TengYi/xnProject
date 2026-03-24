package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleViolationRecord;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleViolationRecordMapper;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.report.ProjectDailyReportItemDto;
import com.xngl.web.dto.report.ProjectViolationAnalysisDto;
import com.xngl.web.dto.report.ProjectViolationStatItemDto;
import com.xngl.web.dto.report.ProjectViolationSummaryDto;
import com.xngl.web.dto.report.ProjectRankingItemDto;
import com.xngl.web.dto.report.ProjectReportItemDto;
import com.xngl.web.dto.report.ProjectReportSummaryDto;
import com.xngl.web.dto.report.ReportTrendItemDto;
import com.xngl.web.dto.report.SiteRankingItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class OperationsReportController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final ProjectMapper projectMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final OrgMapper orgMapper;
  private final VehicleMapper vehicleMapper;
  private final VehicleViolationRecordMapper vehicleViolationRecordMapper;
  private final SiteService siteService;
  private final ExportTaskService exportTaskService;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  public OperationsReportController(
      ProjectMapper projectMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      OrgMapper orgMapper,
      VehicleMapper vehicleMapper,
      VehicleViolationRecordMapper vehicleViolationRecordMapper,
      SiteService siteService,
      ExportTaskService exportTaskService,
      UserContext userContext,
      ObjectMapper objectMapper) {
    this.projectMapper = projectMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.orgMapper = orgMapper;
    this.vehicleMapper = vehicleMapper;
    this.vehicleViolationRecordMapper = vehicleViolationRecordMapper;
    this.siteService = siteService;
    this.exportTaskService = exportTaskService;
    this.userContext = userContext;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/projects/daily")
  public ApiResult<PageResult<ProjectDailyReportItemDto>> projectDaily(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<ProjectDailyReportItemDto> all =
        buildProjectDailyRows(user.getTenantId(), targetDate, keyword).stream()
            .sorted(
                Comparator.comparing(
                        ProjectDailyReportItemDto::getTodayVolume,
                        Comparator.nullsFirst(BigDecimal::compareTo))
                    .reversed())
            .toList();
    return ApiResult.ok(paginate(all, pageNo, pageSize));
  }

  @PostMapping("/projects/daily/export")
  public ApiResult<Map<String, String>> exportProjectDaily(
      @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    try {
      long taskId =
          exportTaskService.createExportTask(
              user.getTenantId(),
              user.getId(),
              "PROJECT_DAILY_REPORT",
              "EXCEL",
              objectMapper.writeValueAsString(body));
      return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
    } catch (JsonProcessingException e) {
      throw new BizException(400, "查询参数序列化失败");
    }
  }

  @GetMapping("/projects/ranking")
  public ApiResult<List<ProjectRankingItemDto>> projectRanking(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "10") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<ProjectDailyReportItemDto> rows = buildProjectDailyRows(user.getTenantId(), targetDate, null);
    List<ProjectRankingItemDto> result =
        rows.stream()
            .sorted(
                Comparator.comparing(
                        ProjectDailyReportItemDto::getTodayVolume,
                        Comparator.nullsFirst(BigDecimal::compareTo))
                    .reversed())
            .limit(Math.max(limit, 1))
            .map(
                row ->
                    new ProjectRankingItemDto(
                        row.getProjectId(),
                        row.getProjectCode(),
                        row.getProjectName(),
                        row.getOrgName(),
                        row.getProjectTotal(),
                        row.getTotalVolume(),
                        row.getTodayVolume(),
                        0,
                        row.getStatusLabel(),
                        row.getProgressPercent()))
            .toList();
    for (int i = 0; i < result.size(); i++) {
      result.get(i).setRank(i + 1);
    }
    return ApiResult.ok(result);
  }

  @GetMapping("/projects/summary")
  public ApiResult<ProjectReportSummaryDto> projectSummary(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date);
    List<ProjectReportItemDto> rows = buildProjectReportRows(user.getTenantId(), period, projectId, keyword);
    return ApiResult.ok(toProjectSummary(period, rows));
  }

  @GetMapping("/projects/list")
  public ApiResult<PageResult<ProjectReportItemDto>> projectList(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date);
    List<ProjectReportItemDto> rows =
        new ArrayList<>(buildProjectReportRows(user.getTenantId(), period, projectId, keyword));
    rows.sort(
        Comparator.comparing(
                ProjectReportItemDto::getPeriodVolume, Comparator.nullsFirst(BigDecimal::compareTo))
            .reversed()
            .thenComparing(
                ProjectReportItemDto::getProjectName, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginateProjectReport(rows, pageNo, pageSize));
  }

  @GetMapping("/projects/trend")
  public ApiResult<List<ReportTrendItemDto>> projectTrend(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow current = resolvePeriodWindow(periodType, date);
    List<ReportTrendItemDto> records = new ArrayList<>();
    int safeLimit = Math.max(limit, 1);
    for (int i = safeLimit - 1; i >= 0; i--) {
      PeriodWindow period = shiftPeriod(current, -i);
      List<ProjectReportItemDto> rows =
          buildProjectReportRows(user.getTenantId(), period, projectId, keyword);
      ProjectReportSummaryDto summary = toProjectSummary(period, rows);
      records.add(
          new ReportTrendItemDto(
              period.label(),
              summary.getPeriodVolume(),
              summary.getPeriodAmount(),
              summary.getTotalTrips(),
              summary.getActiveProjectCount()));
    }
    return ApiResult.ok(records);
  }

  @PostMapping("/projects/export")
  public ApiResult<Map<String, String>> exportProjectReport(
      @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    try {
      long taskId =
          exportTaskService.createExportTask(
              user.getTenantId(),
              user.getId(),
              "PROJECT_REPORT",
              "EXCEL",
              objectMapper.writeValueAsString(body));
      return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
    } catch (JsonProcessingException e) {
      throw new BizException(400, "查询参数序列化失败");
    }
  }

  @GetMapping("/projects/violations")
  public ApiResult<ProjectViolationAnalysisDto> projectViolations(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String violationType,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriodWindow(periodType, date);
    return ApiResult.ok(buildProjectViolationAnalysis(user.getTenantId(), period, keyword, violationType));
  }

  @GetMapping("/sites/ranking")
  public ApiResult<List<SiteRankingItemDto>> siteRanking(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "10") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<SiteRankingItemDto> rows = buildSiteRankingRows(user.getTenantId(), targetDate);
    List<SiteRankingItemDto> result =
        rows.stream()
            .sorted(
                Comparator.comparing(
                        SiteRankingItemDto::getToday, Comparator.nullsFirst(BigDecimal::compareTo))
                    .reversed())
            .limit(Math.max(limit, 1))
            .toList();
    for (int i = 0; i < result.size(); i++) {
      result.get(i).setRank(i + 1);
    }
    return ApiResult.ok(result);
  }

  private List<ProjectDailyReportItemDto> buildProjectDailyRows(
      Long tenantId, LocalDate targetDate, String keyword) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    if (contracts.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, List<Contract>> contractsByProject =
        contracts.stream()
            .filter(contract -> contract.getProjectId() != null)
            .collect(Collectors.groupingBy(Contract::getProjectId, LinkedHashMap::new, Collectors.toList()));
    if (contractsByProject.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, Project> projectMap =
        projectMapper.selectBatchIds(contractsByProject.keySet()).stream()
            .filter(project -> project.getId() != null)
            .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));
    Map<Long, Org> orgMap = loadOrgMap(projectMap.values());
    Map<Long, List<ContractTicket>> ticketsByContract =
        loadTicketsByContract(
            contracts.stream()
                .map(Contract::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

    return contractsByProject.entrySet().stream()
        .map(entry -> toProjectDailyRow(entry.getKey(), entry.getValue(), projectMap, orgMap, ticketsByContract, targetDate))
        .filter(Objects::nonNull)
        .filter(item -> matchProjectKeyword(item, keyword))
        .toList();
  }

  private List<ProjectReportItemDto> buildProjectReportRows(
      Long tenantId, PeriodWindow period, Long projectId, String keyword) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    if (contracts.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, List<Contract>> contractsByProject =
        contracts.stream()
            .filter(contract -> contract.getProjectId() != null)
            .filter(contract -> projectId == null || Objects.equals(contract.getProjectId(), projectId))
            .collect(
                Collectors.groupingBy(
                    Contract::getProjectId, LinkedHashMap::new, Collectors.toList()));
    if (contractsByProject.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, Project> projectMap =
        projectMapper.selectBatchIds(contractsByProject.keySet()).stream()
            .filter(project -> project.getId() != null)
            .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));
    Map<Long, Org> orgMap = loadOrgMap(projectMap.values());
    Map<Long, List<ContractTicket>> ticketsByContract =
        loadTicketsByContract(
            contracts.stream()
                .map(Contract::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

    return contractsByProject.entrySet().stream()
        .map(
            entry ->
                toProjectReportItem(
                    period,
                    entry.getKey(),
                    entry.getValue(),
                    projectMap,
                    orgMap,
                    ticketsByContract))
        .filter(Objects::nonNull)
        .filter(item -> matchProjectReportKeyword(item, keyword))
        .toList();
  }

  private ProjectDailyReportItemDto toProjectDailyRow(
      Long projectId,
      List<Contract> projectContracts,
      Map<Long, Project> projectMap,
      Map<Long, Org> orgMap,
      Map<Long, List<ContractTicket>> ticketsByContract,
      LocalDate targetDate) {
    Project project = projectMap.get(projectId);
    if (project == null) {
      return null;
    }

    BigDecimal projectTotal =
        projectContracts.stream()
            .map(this::resolveContractVolume)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal todayVolume = ZERO;
    BigDecimal totalVolume = ZERO;
    int trips = 0;
    for (Contract contract : projectContracts) {
      for (ContractTicket ticket : ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
        LocalDate ticketDate = resolveTicketDate(ticket);
        if (ticketDate == null || ticketDate.isAfter(targetDate)) {
          continue;
        }
        BigDecimal volume = ticket.getVolume() != null ? ticket.getVolume() : ZERO;
        totalVolume = totalVolume.add(volume);
        if (ticketDate.isEqual(targetDate)) {
          todayVolume = todayVolume.add(volume);
          trips++;
        }
      }
    }

    Org org = project.getOrgId() != null ? orgMap.get(project.getOrgId()) : null;
    return new ProjectDailyReportItemDto(
        String.valueOf(project.getId()),
        project.getCode(),
        project.getName(),
        targetDate.format(ISO_DATE),
        org != null ? org.getOrgName() : null,
        trips,
        trips,
        todayVolume,
        totalVolume,
        projectTotal,
        calculatePercent(totalVolume, projectTotal),
        resolveProjectStatusLabel(project.getStatus()));
  }

  private boolean matchProjectKeyword(ProjectDailyReportItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String value = keyword.trim();
    return contains(item.getProjectName(), value)
        || contains(item.getProjectCode(), value)
        || contains(item.getOrgName(), value);
  }

  private ProjectReportItemDto toProjectReportItem(
      PeriodWindow period,
      Long projectId,
      List<Contract> projectContracts,
      Map<Long, Project> projectMap,
      Map<Long, Org> orgMap,
      Map<Long, List<ContractTicket>> ticketsByContract) {
    Project project = projectMap.get(projectId);
    if (project == null) {
      return null;
    }

    BigDecimal projectTotal =
        projectContracts.stream()
            .map(this::resolveContractVolume)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal periodVolume = ZERO;
    BigDecimal periodAmount = ZERO;
    BigDecimal accumulatedVolume = ZERO;
    int periodTrips = 0;
    for (Contract contract : projectContracts) {
      for (ContractTicket ticket :
          ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
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

    Org org = project.getOrgId() != null ? orgMap.get(project.getOrgId()) : null;
    BigDecimal remainingVolume = projectTotal.subtract(accumulatedVolume).max(ZERO);
    return new ProjectReportItemDto(
        String.valueOf(project.getId()),
        project.getCode(),
        project.getName(),
        org != null ? org.getOrgName() : null,
        period.label(),
        periodVolume,
        periodAmount,
        periodTrips,
        accumulatedVolume,
        projectTotal,
        remainingVolume,
        calculatePercent(accumulatedVolume, projectTotal),
        resolveProjectStatusLabel(project.getStatus()));
  }

  private boolean matchProjectReportKeyword(ProjectReportItemDto item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String value = keyword.trim();
    return contains(item.getProjectName(), value)
        || contains(item.getProjectCode(), value)
        || contains(item.getOrgName(), value)
        || contains(item.getStatus(), value);
  }

  private ProjectReportSummaryDto toProjectSummary(PeriodWindow period, List<ProjectReportItemDto> rows) {
    BigDecimal periodVolume =
        rows.stream().map(ProjectReportItemDto::getPeriodVolume).reduce(ZERO, BigDecimal::add);
    BigDecimal periodAmount =
        rows.stream().map(ProjectReportItemDto::getPeriodAmount).reduce(ZERO, BigDecimal::add);
    BigDecimal projectTotal =
        rows.stream().map(ProjectReportItemDto::getProjectTotal).reduce(ZERO, BigDecimal::add);
    BigDecimal accumulatedVolume =
        rows.stream().map(ProjectReportItemDto::getAccumulatedVolume).reduce(ZERO, BigDecimal::add);
    int totalTrips =
        rows.stream()
            .map(ProjectReportItemDto::getPeriodTrips)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    int activeProjects =
        (int)
            rows.stream()
                .filter(
                    row -> row.getPeriodVolume() != null && row.getPeriodVolume().compareTo(ZERO) > 0)
                .count();
    return new ProjectReportSummaryDto(
        period.type(),
        period.label(),
        rows.size(),
        activeProjects,
        totalTrips,
        periodVolume,
        periodAmount,
        projectTotal,
        accumulatedVolume,
        calculatePercent(accumulatedVolume, projectTotal));
  }

  private List<SiteRankingItemDto> buildSiteRankingRows(Long tenantId, LocalDate targetDate) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
    if (contracts.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Site> siteMap =
        siteService.list().stream()
            .filter(site -> site.getId() != null)
            .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
    Map<Long, List<Contract>> contractsBySite =
        contracts.stream()
            .filter(contract -> contract.getSiteId() != null && siteMap.containsKey(contract.getSiteId()))
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
        .map(entry -> toSiteRankingItem(entry.getKey(), entry.getValue(), siteMap.get(entry.getKey()), ticketsByContract, targetDate))
        .filter(Objects::nonNull)
        .toList();
  }

  private SiteRankingItemDto toSiteRankingItem(
      Long siteId,
      List<Contract> siteContracts,
      Site site,
      Map<Long, List<ContractTicket>> ticketsByContract,
      LocalDate targetDate) {
    if (site == null) {
      return null;
    }
    BigDecimal used = ZERO;
    BigDecimal today = ZERO;
    for (Contract contract : siteContracts) {
      for (ContractTicket ticket : ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
        LocalDate ticketDate = resolveTicketDate(ticket);
        if (ticketDate == null || ticketDate.isAfter(targetDate)) {
          continue;
        }
        BigDecimal volume = ticket.getVolume() != null ? ticket.getVolume() : ZERO;
        used = used.add(volume);
        if (ticketDate.isEqual(targetDate)) {
          today = today.add(volume);
        }
      }
    }
    BigDecimal derivedCapacity = deriveSiteCapacity(site, used);
    return new SiteRankingItemDto(
        String.valueOf(siteId),
        site.getName(),
        resolveSiteType(site),
        derivedCapacity,
        used,
        today,
        0,
        resolveSiteStatus(site.getStatus(), used, derivedCapacity));
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

  private Map<Long, Org> loadOrgMap(Iterable<Project> projects) {
    LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
    for (Project project : projects) {
      if (project != null && project.getOrgId() != null) {
        orgIds.add(project.getOrgId());
      }
    }
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(org -> org.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private PageResult<ProjectDailyReportItemDto> paginate(
      List<ProjectDailyReportItemDto> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(
        safePageNo,
        safePageSize,
        rows.size(),
        rows.subList(fromIndex, toIndex));
  }

  private PageResult<ProjectReportItemDto> paginateProjectReport(
      List<ProjectReportItemDto> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(
        safePageNo, safePageSize, rows.size(), new ArrayList<>(rows.subList(fromIndex, toIndex)));
  }

  private BigDecimal resolveContractVolume(Contract contract) {
    return contract != null && contract.getAgreedVolume() != null ? contract.getAgreedVolume() : ZERO;
  }

  private LocalDate resolveTicketDate(ContractTicket ticket) {
    if (ticket.getTicketDate() != null) {
      return ticket.getTicketDate();
    }
    LocalDateTime createTime = ticket.getCreateTime();
    return createTime != null ? createTime.toLocalDate() : null;
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

  private BigDecimal deriveSiteCapacity(Site site, BigDecimal used) {
    if (site != null && site.getCapacity() != null && site.getCapacity().compareTo(ZERO) > 0) {
      return site.getCapacity();
    }
    BigDecimal base = BigDecimal.valueOf(((site.getId() != null ? site.getId() : 1L) % 7) + 3L)
        .multiply(BigDecimal.valueOf(100000L));
    BigDecimal dynamic = used.multiply(BigDecimal.valueOf(1.2));
    return dynamic.compareTo(base) > 0 ? dynamic : base;
  }

  private String resolveProjectStatusLabel(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 0 -> "立项";
      case 1 -> "在建";
      case 2 -> "预警";
      case 3 -> "完工";
      default -> "状态" + status;
    };
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
    String code = site.getCode();
    long suffix = site.getId() != null ? site.getId() % 4L : 0L;
    if (StringUtils.hasText(code) && code.startsWith("GY") || suffix == 1L) {
      return "国有场地";
    }
    if (StringUtils.hasText(code) && code.startsWith("JT") || suffix == 2L) {
      return "集体场地";
    }
    if (StringUtils.hasText(code) && code.startsWith("GC") || suffix == 3L) {
      return "工程场地";
    }
    return "短驳场地";
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

  private ProjectViolationAnalysisDto buildProjectViolationAnalysis(
      Long tenantId, PeriodWindow period, String keyword, String violationType) {
    LambdaQueryWrapper<VehicleViolationRecord> query =
        new LambdaQueryWrapper<VehicleViolationRecord>()
            .eq(VehicleViolationRecord::getTenantId, tenantId)
            .ge(VehicleViolationRecord::getTriggerTime, period.start().atStartOfDay())
            .lt(VehicleViolationRecord::getTriggerTime, period.end().plusDays(1).atStartOfDay())
            .orderByDesc(VehicleViolationRecord::getTriggerTime)
            .orderByDesc(VehicleViolationRecord::getId);
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(VehicleViolationRecord::getPlateNo, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getTriggerLocation, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getOperatorName, effectiveKeyword)
                  .or()
                  .like(VehicleViolationRecord::getPenaltyResult, effectiveKeyword));
    }
    if (StringUtils.hasText(violationType)) {
      query.eq(VehicleViolationRecord::getViolationType, violationType.trim());
    }

    List<VehicleViolationRecord> records = vehicleViolationRecordMapper.selectList(query);
    if (records.isEmpty()) {
      return new ProjectViolationAnalysisDto(
          new ProjectViolationSummaryDto(period.label(), 0, 0, 0, 0, 0, 0),
          Collections.emptyList(),
          Collections.emptyList(),
          Collections.emptyList());
    }

    Map<Long, Vehicle> vehicleMap =
        vehicleMapper.selectBatchIds(
                records.stream()
                    .map(VehicleViolationRecord::getVehicleId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));

    Map<Long, Org> orgMap =
        orgMapper.selectBatchIds(
                records.stream()
                    .map(VehicleViolationRecord::getOrgId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));

    List<ProjectViolationStatItemDto> byFleet =
        buildViolationStats(
            records,
            record -> {
              Vehicle vehicle =
                  record.getVehicleId() != null ? vehicleMap.get(record.getVehicleId()) : null;
              return vehicle != null && StringUtils.hasText(vehicle.getFleetName())
                  ? vehicle.getFleetName()
                  : "未分配车队";
            });
    List<ProjectViolationStatItemDto> byPlate =
        buildViolationStats(
            records,
            record ->
                StringUtils.hasText(record.getPlateNo()) ? record.getPlateNo() : "未识别车牌");
    List<ProjectViolationStatItemDto> byTeam =
        buildViolationStats(
            records,
            record -> {
              Org org = record.getOrgId() != null ? orgMap.get(record.getOrgId()) : null;
              if (org != null && StringUtils.hasText(org.getOrgName())) {
                return org.getOrgName();
              }
              return StringUtils.hasText(record.getOperatorName())
                  ? record.getOperatorName()
                  : "未分配中队";
            });

    int handledCount = (int) records.stream().filter(this::isHandledViolation).count();
    int pendingCount = records.size() - handledCount;
    ProjectViolationSummaryDto summary =
        new ProjectViolationSummaryDto(
            period.label(),
            records.size(),
            handledCount,
            pendingCount,
            (int)
                records.stream()
                    .map(VehicleViolationRecord::getPlateNo)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .count(),
            byFleet.size(),
            byTeam.size());
    return new ProjectViolationAnalysisDto(summary, byFleet, byPlate, byTeam);
  }

  private List<ProjectViolationStatItemDto> buildViolationStats(
      List<VehicleViolationRecord> records, Function<VehicleViolationRecord, String> classifier) {
    Map<String, List<VehicleViolationRecord>> grouped =
        records.stream()
            .collect(
                Collectors.groupingBy(
                    record -> {
                      String name = classifier.apply(record);
                      return StringUtils.hasText(name) ? name : "未分类";
                    },
                    LinkedHashMap::new,
                    Collectors.toList()));
    return grouped.entrySet().stream()
        .map(
            entry -> {
              List<VehicleViolationRecord> rows = entry.getValue();
              int handled = (int) rows.stream().filter(this::isHandledViolation).count();
              int pending = rows.size() - handled;
              LocalDateTime latest =
                  rows.stream()
                      .map(VehicleViolationRecord::getTriggerTime)
                      .filter(Objects::nonNull)
                      .max(LocalDateTime::compareTo)
                      .orElse(null);
              return new ProjectViolationStatItemDto(
                  entry.getKey(),
                  rows.size(),
                  handled,
                  pending,
                  latest != null ? latest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            })
        .sorted(
            Comparator.comparingInt(ProjectViolationStatItemDto::getViolationCount)
                .reversed()
                .thenComparing(ProjectViolationStatItemDto::getName))
        .toList();
  }

  private boolean isHandledViolation(VehicleViolationRecord record) {
    if (record == null || !StringUtils.hasText(record.getActionStatus())) {
      return false;
    }
    String actionStatus = record.getActionStatus().trim().toUpperCase();
    return "PROCESSED".equals(actionStatus) || "RELEASED".equals(actionStatus);
  }

  private boolean contains(String source, String keyword) {
    return source != null && source.contains(keyword);
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
        yield new PeriodWindow(
            yearMonth.atDay(1), yearMonth.atEndOfMonth(), YM_FORMATTER.format(baseDate), "MONTH");
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
    return userContext.requireCurrentUser(request);
  }

  private record PeriodWindow(LocalDate start, LocalDate end, String label, String type) {}
}
