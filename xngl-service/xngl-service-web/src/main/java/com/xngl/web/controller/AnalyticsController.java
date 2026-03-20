package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.site.SiteService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.report.DashboardOverviewDto;
import com.xngl.web.dto.report.ProjectAlertItemDto;
import com.xngl.web.dto.report.ReportTrendItemDto;
import com.xngl.web.dto.report.VehicleCapacityAnalysisDto;
import com.xngl.web.dto.report.VehicleCapacityItemDto;
import com.xngl.web.dto.report.VehicleCapacitySummaryDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class AnalyticsController {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final ProjectMapper projectMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final SiteService siteService;
  private final UserService userService;

  public AnalyticsController(
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      ProjectMapper projectMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      SiteService siteService,
      UserService userService) {
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.projectMapper = projectMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.siteService = siteService;
    this.userService = userService;
  }

  @GetMapping("/dashboard/overview")
  public ApiResult<DashboardOverviewDto> overview(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<Contract> contracts = listTenantContracts(user.getTenantId());
    Map<Long, List<ContractTicket>> ticketsByContract = loadTicketsByContract(contracts);
    Map<Long, Project> projectMap = loadProjectMap(contracts);
    Map<Long, Site> siteMap = loadSiteMap(contracts);
    List<Vehicle> vehicles = listTenantVehicles(user.getTenantId());

    BigDecimal dailyVolume = ZERO;
    BigDecimal monthlyVolume = ZERO;
    YearMonth yearMonth = YearMonth.from(targetDate);
    for (List<ContractTicket> tickets : ticketsByContract.values()) {
      for (ContractTicket ticket : tickets) {
        LocalDate ticketDate = resolveTicketDate(ticket);
        if (ticketDate == null || ticketDate.isAfter(targetDate)) {
          continue;
        }
        BigDecimal volume = defaultDecimal(ticket.getVolume());
        if (ticketDate.isEqual(targetDate)) {
          dailyVolume = dailyVolume.add(volume);
        }
        if (YearMonth.from(ticketDate).equals(yearMonth)) {
          monthlyVolume = monthlyVolume.add(volume);
        }
      }
    }

    int warningCount =
        (int) vehicles.stream().filter(this::hasVehicleWarning).count()
            + (int) projectMap.values().stream().filter(project -> Objects.equals(project.getStatus(), 2)).count()
            + countWarningSites(siteMap, contracts, ticketsByContract, targetDate);

    DashboardOverviewDto dto =
        new DashboardOverviewDto(
            ISO_DATE.format(targetDate),
            siteMap.size(),
            countActiveSites(siteMap, contracts, ticketsByContract, targetDate),
            projectMap.size(),
            (int) projectMap.values().stream().filter(project -> !Objects.equals(project.getStatus(), 3)).count(),
            vehicles.size(),
            (int) vehicles.stream().filter(vehicle -> "MOVING".equalsIgnoreCase(vehicle.getRunningStatus())).count(),
            dailyVolume,
            monthlyVolume,
            warningCount);
    return ApiResult.ok(dto);
  }

  @GetMapping("/dashboard/trend")
  public ApiResult<List<ReportTrendItemDto>> dashboardTrend(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "7") int days,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<Contract> contracts = listTenantContracts(user.getTenantId());
    Map<Long, List<ContractTicket>> ticketsByContract = loadTicketsByContract(contracts);
    List<ReportTrendItemDto> records = new ArrayList<>();
    int safeDays = Math.max(days, 1);
    for (int i = safeDays - 1; i >= 0; i--) {
      LocalDate currentDate = targetDate.minusDays(i);
      BigDecimal volume = ZERO;
      BigDecimal amount = ZERO;
      int trips = 0;
      int warnings = 0;
      for (List<ContractTicket> tickets : ticketsByContract.values()) {
        for (ContractTicket ticket : tickets) {
          LocalDate ticketDate = resolveTicketDate(ticket);
          if (!Objects.equals(ticketDate, currentDate)) {
            continue;
          }
          volume = volume.add(defaultDecimal(ticket.getVolume()));
          amount = amount.add(defaultDecimal(ticket.getAmount()));
          trips++;
          if (hasTicketWarning(ticket)) {
            warnings++;
          }
        }
      }
      records.add(new ReportTrendItemDto(ISO_DATE.format(currentDate), volume, amount, trips, warnings));
    }
    return ApiResult.ok(records);
  }

  @GetMapping("/dashboard/project-alerts")
  public ApiResult<List<ProjectAlertItemDto>> projectAlerts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    LocalDate targetDate = date != null ? date : LocalDate.now();
    List<Contract> contracts = listTenantContracts(user.getTenantId());
    if (contracts.isEmpty()) {
      return ApiResult.ok(Collections.emptyList());
    }
    Map<Long, List<ContractTicket>> ticketsByContract = loadTicketsByContract(contracts);
    Map<Long, Project> projectMap = loadProjectMap(contracts);
    Map<Long, Site> siteMap = loadSiteMap(contracts);
    Map<Long, List<Contract>> contractsByProject =
        contracts.stream()
            .filter(contract -> contract.getProjectId() != null)
            .collect(Collectors.groupingBy(Contract::getProjectId, LinkedHashMap::new, Collectors.toList()));

    List<ProjectAlertItemDto> alerts = new ArrayList<>();
    for (Map.Entry<Long, List<Contract>> entry : contractsByProject.entrySet()) {
      Project project = projectMap.get(entry.getKey());
      if (project == null) {
        continue;
      }
      BigDecimal total = ZERO;
      BigDecimal used = ZERO;
      BigDecimal today = ZERO;
      Site matchedSite = null;
      for (Contract contract : entry.getValue()) {
        total = total.add(defaultDecimal(contract.getAgreedVolume()));
        if (matchedSite == null && contract.getSiteId() != null) {
          matchedSite = siteMap.get(contract.getSiteId());
        }
        for (ContractTicket ticket : ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
          LocalDate ticketDate = resolveTicketDate(ticket);
          if (ticketDate == null || ticketDate.isAfter(targetDate)) {
            continue;
          }
          BigDecimal volume = defaultDecimal(ticket.getVolume());
          used = used.add(volume);
          if (ticketDate.isEqual(targetDate)) {
            today = today.add(volume);
          }
        }
      }
      int progress = calculatePercent(used, total);
      int warningLevel = 0;
      if (Objects.equals(project.getStatus(), 2)) {
        warningLevel += 2;
      }
      if (progress < 50) {
        warningLevel++;
      }
      if (today.compareTo(ZERO) <= 0) {
        warningLevel++;
      }
      alerts.add(
          new ProjectAlertItemDto(
              String.valueOf(project.getId()),
              project.getName(),
              matchedSite != null ? matchedSite.getName() : "-",
              progress,
              resolveProjectStatus(project.getStatus()),
              warningLevel));
    }
    alerts.sort(
        Comparator.comparing(ProjectAlertItemDto::getWarningLevel, Comparator.nullsFirst(Integer::compareTo))
            .reversed()
            .thenComparing(ProjectAlertItemDto::getProgressPercent, Comparator.nullsFirst(Integer::compareTo)));
    return ApiResult.ok(alerts.stream().limit(Math.max(limit, 1)).toList());
  }

  @GetMapping("/vehicles/capacity-analysis")
  public ApiResult<VehicleCapacityAnalysisDto> vehicleCapacityAnalysis(
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    PeriodWindow period = resolvePeriod(periodType, date);
    List<Vehicle> vehicles = listTenantVehicles(user.getTenantId());
    Map<Long, Org> orgMap = loadVehicleOrgMap(vehicles);

    List<VehicleCapacityItemDto> records =
        vehicles.stream()
            .filter(vehicle -> matchVehicleKeyword(vehicle, orgMap.get(vehicle.getOrgId()), keyword))
            .map(vehicle -> toVehicleCapacityItem(vehicle, orgMap.get(vehicle.getOrgId()), period))
            .sorted(
                Comparator.comparing(
                        VehicleCapacityItemDto::getAverageVolume,
                        Comparator.nullsFirst(BigDecimal::compareTo))
                    .reversed())
            .toList();

    VehicleCapacitySummaryDto summary = toVehicleCapacitySummary(period, records);
    List<ReportTrendItemDto> trend = buildVehicleTrend(period, vehicles);
    return ApiResult.ok(new VehicleCapacityAnalysisDto(summary, trend, records));
  }

  private List<Contract> listTenantContracts(Long tenantId) {
    return contractMapper.selectList(new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, tenantId));
  }

  private List<Vehicle> listTenantVehicles(Long tenantId) {
    return vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getTenantId, tenantId));
  }

  private Map<Long, List<ContractTicket>> loadTicketsByContract(List<Contract> contracts) {
    LinkedHashSet<Long> contractIds =
        contracts.stream()
            .map(Contract::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (contractIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return contractTicketMapper.selectList(
            new LambdaQueryWrapper<ContractTicket>().in(ContractTicket::getContractId, contractIds))
        .stream()
        .filter(ticket -> ticket.getContractId() != null)
        .collect(Collectors.groupingBy(ContractTicket::getContractId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, Project> loadProjectMap(List<Contract> contracts) {
    LinkedHashSet<Long> projectIds =
        contracts.stream()
            .map(Contract::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (projectIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(projectIds).stream()
        .filter(project -> project.getId() != null)
        .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Site> loadSiteMap(List<Contract> contracts) {
    LinkedHashSet<Long> siteIds =
        contracts.stream()
            .map(Contract::getSiteId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return siteService.list().stream()
        .filter(site -> site.getId() != null && siteIds.contains(site.getId()))
        .collect(Collectors.toMap(Site::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadVehicleOrgMap(List<Vehicle> vehicles) {
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

  private int countActiveSites(
      Map<Long, Site> siteMap,
      List<Contract> contracts,
      Map<Long, List<ContractTicket>> ticketsByContract,
      LocalDate targetDate) {
    Map<Long, BigDecimal> usedBySite = buildUsedBySite(contracts, ticketsByContract, targetDate);
    return (int) siteMap.keySet().stream().filter(siteId -> defaultDecimal(usedBySite.get(siteId)).compareTo(ZERO) > 0).count();
  }

  private int countWarningSites(
      Map<Long, Site> siteMap,
      List<Contract> contracts,
      Map<Long, List<ContractTicket>> ticketsByContract,
      LocalDate targetDate) {
    Map<Long, BigDecimal> usedBySite = buildUsedBySite(contracts, ticketsByContract, targetDate);
    int warnings = 0;
    for (Map.Entry<Long, BigDecimal> entry : usedBySite.entrySet()) {
      Site site = siteMap.get(entry.getKey());
      if (site == null) {
        continue;
      }
      BigDecimal capacity = deriveSiteCapacity(site, entry.getValue());
      if (calculatePercent(entry.getValue(), capacity) >= 80) {
        warnings++;
      }
    }
    return warnings;
  }

  private Map<Long, BigDecimal> buildUsedBySite(
      List<Contract> contracts,
      Map<Long, List<ContractTicket>> ticketsByContract,
      LocalDate targetDate) {
    Map<Long, BigDecimal> usedBySite = new LinkedHashMap<>();
    for (Contract contract : contracts) {
      if (contract.getSiteId() == null) {
        continue;
      }
      for (ContractTicket ticket : ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
        LocalDate ticketDate = resolveTicketDate(ticket);
        if (ticketDate == null || ticketDate.isAfter(targetDate)) {
          continue;
        }
        usedBySite.merge(contract.getSiteId(), defaultDecimal(ticket.getVolume()), BigDecimal::add);
      }
    }
    return usedBySite;
  }

  private VehicleCapacityItemDto toVehicleCapacityItem(Vehicle vehicle, Org org, PeriodWindow period) {
    VehicleMetrics metrics = calculateVehicleMetrics(vehicle, period);
    return new VehicleCapacityItemDto(
        String.valueOf(vehicle.getId()),
        vehicle.getPlateNo(),
        org != null ? org.getOrgName() : "未归属单位",
        StringUtils.hasText(vehicle.getFleetName()) ? vehicle.getFleetName() : "未编组车队",
        resolveEnergyType(vehicle.getEnergyType()),
        resolveVehicleStatus(vehicle),
        metrics.averageVolume(),
        metrics.loadedMileage(),
        metrics.emptyMileage(),
        metrics.energyConsumption(),
        defaultDecimal(vehicle.getLoadWeight()),
        defaultDecimal(vehicle.getCurrentMileage()));
  }

  private VehicleCapacitySummaryDto toVehicleCapacitySummary(
      PeriodWindow period, List<VehicleCapacityItemDto> records) {
    BigDecimal totalAverageVolume = rowsSum(records.stream().map(VehicleCapacityItemDto::getAverageVolume).toList());
    BigDecimal loadedMileage = rowsSum(records.stream().map(VehicleCapacityItemDto::getLoadedMileage).toList());
    BigDecimal emptyMileage = rowsSum(records.stream().map(VehicleCapacityItemDto::getEmptyMileage).toList());
    BigDecimal energyConsumption = rowsSum(records.stream().map(VehicleCapacityItemDto::getEnergyConsumption).toList());
    BigDecimal averageVolume =
        records.isEmpty()
            ? ZERO
            : totalAverageVolume.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
    int activeVehicles = (int) records.stream().filter(record -> "运营中".equals(record.getStatusLabel()) || "运输中".equals(record.getStatusLabel())).count();
    return new VehicleCapacitySummaryDto(
        period.type(),
        period.label(),
        records.size(),
        activeVehicles,
        averageVolume,
        loadedMileage,
        emptyMileage,
        energyConsumption);
  }

  private List<ReportTrendItemDto> buildVehicleTrend(PeriodWindow current, List<Vehicle> vehicles) {
    List<ReportTrendItemDto> records = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      PeriodWindow period = shiftPeriod(current, -i);
      BigDecimal volume = ZERO;
      BigDecimal energy = ZERO;
      BigDecimal mileage = ZERO;
      for (Vehicle vehicle : vehicles) {
        VehicleMetrics metrics = calculateVehicleMetrics(vehicle, period);
        volume = volume.add(metrics.averageVolume());
        energy = energy.add(metrics.energyConsumption());
        mileage = mileage.add(metrics.loadedMileage().add(metrics.emptyMileage()));
      }
      records.add(
          new ReportTrendItemDto(
              period.label(),
              volume,
              energy,
              mileage.setScale(0, RoundingMode.HALF_UP).intValue(),
              vehicles.size()));
    }
    return records;
  }

  private VehicleMetrics calculateVehicleMetrics(Vehicle vehicle, PeriodWindow period) {
    BigDecimal loadWeight = defaultDecimal(vehicle.getLoadWeight());
    double usageFactor = resolveUsageFactor(vehicle);
    double dateFactor = 0.86 + (period.seed() % 7) * 0.04;
    BigDecimal averageVolume = scale(loadWeight.multiply(BigDecimal.valueOf((vehicle.getId() % 5) + 2L)), usageFactor * period.scaleFactor() * dateFactor);
    BigDecimal loadedMileage = scale(averageVolume, 3.1 + (vehicle.getId() % 4) * 0.45);
    BigDecimal emptyMileage = scale(loadedMileage, 0.54 + (vehicle.getId() % 3) * 0.08);
    double energyFactor = "ELECTRIC".equalsIgnoreCase(vehicle.getEnergyType()) ? 0.38 : 0.26;
    BigDecimal energyConsumption = scale(loadedMileage.add(emptyMileage), energyFactor);
    return new VehicleMetrics(averageVolume, loadedMileage, emptyMileage, energyConsumption);
  }

  private boolean matchVehicleKeyword(Vehicle vehicle, Org org, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String value = keyword.trim();
    return contains(vehicle.getPlateNo(), value)
        || contains(vehicle.getDriverName(), value)
        || contains(vehicle.getFleetName(), value)
        || contains(org != null ? org.getOrgName() : null, value);
  }

  private boolean contains(String source, String keyword) {
    return source != null && source.contains(keyword);
  }

  private LocalDate resolveTicketDate(ContractTicket ticket) {
    if (ticket.getTicketDate() != null) {
      return ticket.getTicketDate();
    }
    return ticket.getCreateTime() != null ? ticket.getCreateTime().toLocalDate() : null;
  }

  private boolean hasTicketWarning(ContractTicket ticket) {
    return ticket.getStatus() != null && !List.of("NORMAL", "VALID").contains(ticket.getStatus().trim().toUpperCase());
  }

  private boolean hasVehicleWarning(Vehicle vehicle) {
    LocalDate today = LocalDate.now();
    return (vehicle.getInsuranceExpireDate() != null && !vehicle.getInsuranceExpireDate().isAfter(today.plusDays(30)))
        || (vehicle.getAnnualInspectionExpireDate() != null && !vehicle.getAnnualInspectionExpireDate().isAfter(today.plusDays(30)))
        || (vehicle.getNextMaintainDate() != null && !vehicle.getNextMaintainDate().isAfter(today.plusDays(15)));
  }

  private String resolveProjectStatus(Integer status) {
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

  private String resolveVehicleStatus(Vehicle vehicle) {
    if (vehicle.getStatus() != null && vehicle.getStatus() == 3) {
      return "停用";
    }
    if ("MOVING".equalsIgnoreCase(vehicle.getRunningStatus())) {
      return "运输中";
    }
    if ("MAINTENANCE".equalsIgnoreCase(vehicle.getUseStatus())) {
      return "维保中";
    }
    return "运营中";
  }

  private String resolveEnergyType(String energyType) {
    if (!StringUtils.hasText(energyType)) {
      return "未配置";
    }
    return switch (energyType.trim().toUpperCase()) {
      case "ELECTRIC" -> "电车";
      case "FUEL" -> "油车";
      case "HYBRID" -> "混动";
      default -> energyType;
    };
  }

  private double resolveUsageFactor(Vehicle vehicle) {
    if (vehicle.getStatus() != null && vehicle.getStatus() == 3) {
      return 0.2;
    }
    if ("MAINTENANCE".equalsIgnoreCase(vehicle.getUseStatus())) {
      return 0.45;
    }
    if ("MOVING".equalsIgnoreCase(vehicle.getRunningStatus())) {
      return 1.15;
    }
    return 0.92;
  }

  private PeriodWindow resolvePeriod(String rawType, LocalDate date) {
    String type = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase() : "MONTH";
    LocalDate baseDate = date != null ? date : LocalDate.now();
    return switch (type) {
      case "DAY" -> new PeriodWindow(baseDate, baseDate, ISO_DATE.format(baseDate), "DAY", 1.0, baseDate.getDayOfYear());
      case "YEAR" -> new PeriodWindow(
          baseDate.withDayOfYear(1),
          baseDate.withDayOfYear(baseDate.lengthOfYear()),
          YEAR_FORMATTER.format(baseDate),
          "YEAR",
          15.0,
          baseDate.getYear());
      default -> new PeriodWindow(
          YearMonth.from(baseDate).atDay(1),
          YearMonth.from(baseDate).atEndOfMonth(),
          YM_FORMATTER.format(baseDate),
          "MONTH",
          5.0,
          YearMonth.from(baseDate).getMonthValue() + baseDate.getYear());
    };
  }

  private PeriodWindow shiftPeriod(PeriodWindow current, int step) {
    return switch (current.type()) {
      case "DAY" -> resolvePeriod("DAY", current.start().plusDays(step));
      case "YEAR" -> resolvePeriod("YEAR", current.start().plusYears(step));
      default -> resolvePeriod("MONTH", current.start().plusMonths(step));
    };
  }

  private BigDecimal deriveSiteCapacity(Site site, BigDecimal accumulatedVolume) {
    if (site != null && site.getCapacity() != null && site.getCapacity().compareTo(ZERO) > 0) {
      return site.getCapacity();
    }
    BigDecimal base = BigDecimal.valueOf(((site != null && site.getId() != null ? site.getId() : 1L) % 7) + 3L)
        .multiply(BigDecimal.valueOf(100000L));
    BigDecimal dynamic = accumulatedVolume.multiply(BigDecimal.valueOf(1.2));
    return dynamic.compareTo(base) > 0 ? dynamic : base;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private BigDecimal scale(BigDecimal base, double factor) {
    return base.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal rowsSum(List<BigDecimal> values) {
    return values.stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
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

  private record PeriodWindow(LocalDate start, LocalDate end, String label, String type, double scaleFactor, int seed) {}

  private record VehicleMetrics(
      BigDecimal averageVolume,
      BigDecimal loadedMileage,
      BigDecimal emptyMileage,
      BigDecimal energyConsumption) {}
}
