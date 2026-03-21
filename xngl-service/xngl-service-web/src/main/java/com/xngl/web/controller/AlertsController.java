package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertEvent;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.alert.AlertPushRule;
import com.xngl.infrastructure.persistence.entity.alert.AlertRule;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspection;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.AlertEventMapper;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.infrastructure.persistence.mapper.AlertPushRuleMapper;
import com.xngl.infrastructure.persistence.mapper.AlertRuleMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.manager.sysparam.entity.SysParam;
import com.xngl.manager.sysparam.mapper.SysParamMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final Set<String> AUTO_GENERATE_RULE_CODES =
      Set.of(
          "PROJECT_PROGRESS_LAG",
          "PROJECT_PERMIT_EXPIRING",
          "CONTRACT_EXPIRING_SOON",
          "CONTRACT_PAYMENT_OVERDUE",
          "PERSONNEL_LICENSE_EXPIRING",
          "PERSONNEL_VIOLATION_SCORE");

  private final AlertEventMapper alertEventMapper;
  private final AlertFenceMapper alertFenceMapper;
  private final AlertRuleMapper alertRuleMapper;
  private final AlertPushRuleMapper alertPushRuleMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final SecurityInspectionMapper securityInspectionMapper;
  private final DisposalPermitMapper disposalPermitMapper;
  private final UserMapper userMapper;
  private final SysParamMapper sysParamMapper;
  private final UserService userService;

  public AlertsController(
      AlertEventMapper alertEventMapper,
      AlertFenceMapper alertFenceMapper,
      AlertRuleMapper alertRuleMapper,
      AlertPushRuleMapper alertPushRuleMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      SecurityInspectionMapper securityInspectionMapper,
      DisposalPermitMapper disposalPermitMapper,
      UserMapper userMapper,
      SysParamMapper sysParamMapper,
      UserService userService) {
    this.alertEventMapper = alertEventMapper;
    this.alertFenceMapper = alertFenceMapper;
    this.alertRuleMapper = alertRuleMapper;
    this.alertPushRuleMapper = alertPushRuleMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.securityInspectionMapper = securityInspectionMapper;
    this.disposalPermitMapper = disposalPermitMapper;
    this.userMapper = userMapper;
    this.sysParamMapper = sysParamMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<Map<String, Object>>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String targetType,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sourceChannel,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<AlertEvent> rows =
        alertEventMapper.selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getTenantId, currentUser.getTenantId())
                .eq(StringUtils.hasText(targetType), AlertEvent::getTargetType, targetType)
                .eq(StringUtils.hasText(level), AlertEvent::getAlertLevel, level)
                .eq(StringUtils.hasText(status), AlertEvent::getAlertStatus, status)
                .eq(StringUtils.hasText(sourceChannel), AlertEvent::getSourceChannel, sourceChannel)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(AlertEvent::getAlertNo, keywordValue)
                            .or()
                            .like(AlertEvent::getTitle, keywordValue)
                            .or()
                            .like(AlertEvent::getRuleCode, keywordValue)
                            .or()
                            .like(AlertEvent::getContent, keywordValue))
                .orderByDesc(AlertEvent::getOccurTime)
                .orderByDesc(AlertEvent::getId));
    return ApiResult.ok(enrich(rows));
  }

  @GetMapping("/{id}")
  public ApiResult<Map<String, Object>> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    AlertEvent entity = requireEntity(id, currentUser.getTenantId());
    return ApiResult.ok(enrich(List.of(entity)).stream().findFirst().orElseGet(LinkedHashMap::new));
  }

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<AlertEvent> rows = queryTenantAlerts(currentUser.getTenantId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", rows.size());
    result.put("pending", rows.stream().filter(item -> "PENDING".equalsIgnoreCase(item.getAlertStatus())).count());
    result.put("processing", rows.stream().filter(item -> "PROCESSING".equalsIgnoreCase(item.getAlertStatus())).count());
    result.put("closed", rows.stream().filter(item -> "CLOSED".equalsIgnoreCase(item.getAlertStatus())).count());
    result.put("confirmed", rows.stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.getAlertStatus())).count());
    result.put("vehicleCount", rows.stream().filter(item -> "VEHICLE".equalsIgnoreCase(item.getTargetType())).count());
    result.put("siteCount", rows.stream().filter(item -> "SITE".equalsIgnoreCase(item.getTargetType())).count());
    result.put("projectCount", rows.stream().filter(item -> "PROJECT".equalsIgnoreCase(item.getTargetType())).count());
    result.put("contractCount", rows.stream().filter(item -> "CONTRACT".equalsIgnoreCase(item.getTargetType())).count());
    result.put("userCount", rows.stream().filter(item -> "USER".equalsIgnoreCase(item.getTargetType())).count());
    result.put("highRiskCount", rows.stream().filter(item -> "L3".equalsIgnoreCase(resolveLevel(item))).count());
    result.put("avgHandleMinutes", calcAvgHandleMinutes(rows));
    result.put("overdueCount", calcOverdueCount(rows));
    result.put("enabledRuleCount", countEnabledRules(currentUser.getTenantId()));
    result.put("enabledFenceCount", countEnabledFences(currentUser.getTenantId()));
    result.put("enabledPushCount", countEnabledPushRules(currentUser.getTenantId()));
    return ApiResult.ok(result);
  }

  @GetMapping("/analytics")
  public ApiResult<Map<String, Object>> analytics(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<AlertEvent> rows = queryTenantAlerts(currentUser.getTenantId());
    List<AlertRule> rules =
        alertRuleMapper.selectList(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getTenantId, currentUser.getTenantId())
                .orderByAsc(AlertRule::getRuleCode));
    Map<String, String> ruleNameMap =
        rules.stream()
            .filter(item -> StringUtils.hasText(item.getRuleCode()))
            .collect(Collectors.toMap(AlertRule::getRuleCode, item -> StringUtils.hasText(item.getRuleName()) ? item.getRuleName() : item.getRuleCode(), (left, right) -> left));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("levelBuckets", buildBuckets(rows, this::resolveLevel, null));
    result.put("targetBuckets", buildBuckets(rows, AlertEvent::getTargetType, null));
    result.put("sourceBuckets", buildBuckets(rows, AlertEvent::getSourceChannel, null));
    result.put("statusBuckets", buildBuckets(rows, AlertEvent::getAlertStatus, null));
    result.put("ruleBuckets", buildBuckets(rows, AlertEvent::getRuleCode, ruleNameMap));
    result.put("trend7d", buildTrend(rows, 7));
    result.put("modelCoverage", buildModelCoverage(currentUser.getTenantId(), rules));
    return ApiResult.ok(result);
  }

  @GetMapping("/top-risk")
  public ApiResult<List<Map<String, Object>>> topRisk(HttpServletRequest request) {
    return topRisk("VEHICLE", request);
  }

  @GetMapping("/top-risk-targets")
  public ApiResult<List<Map<String, Object>>> topRisk(
      @RequestParam(required = false) String targetType, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String targetTypeValue = defaultValue(targetType, "VEHICLE");
    List<AlertEvent> rows =
        alertEventMapper.selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getTenantId, currentUser.getTenantId())
                .eq(AlertEvent::getTargetType, targetTypeValue)
                .orderByDesc(AlertEvent::getOccurTime));
    Map<Long, Long> countMap =
        rows.stream()
            .map(item -> resolveTopRiskId(item, targetTypeValue))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    if (countMap.isEmpty()) {
      return ApiResult.ok(List.of());
    }
    if ("CONTRACT".equalsIgnoreCase(targetTypeValue)) {
      Map<Long, Contract> contractMap = loadContracts(countMap.keySet());
      return ApiResult.ok(
          countMap.entrySet().stream()
              .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
              .limit(5)
              .map(
                  entry -> {
                    Contract contract = contractMap.get(entry.getKey());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("targetId", entry.getKey());
                    item.put("targetType", "CONTRACT");
                    item.put(
                        "targetName",
                        contract != null && StringUtils.hasText(contract.getContractNo())
                            ? contract.getContractNo()
                            : "合同#" + entry.getKey());
                    item.put("extraName", contract != null ? contract.getContractType() : null);
                    item.put("count", entry.getValue());
                    return item;
                  })
              .toList());
    }
    if ("USER".equalsIgnoreCase(targetTypeValue)) {
      Map<Long, User> userMap = loadUsers(countMap.keySet());
      return ApiResult.ok(
          countMap.entrySet().stream()
              .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
              .limit(5)
              .map(
                  entry -> {
                    User user = userMap.get(entry.getKey());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("targetId", entry.getKey());
                    item.put("targetType", "USER");
                    item.put(
                        "targetName",
                        user != null && StringUtils.hasText(user.getName())
                            ? user.getName()
                            : user != null && StringUtils.hasText(user.getUsername())
                                ? user.getUsername()
                                : "人员#" + entry.getKey());
                    item.put("extraName", user != null ? user.getMobile() : null);
                    item.put("count", entry.getValue());
                    return item;
                  })
              .toList());
    }
    Map<Long, Vehicle> vehicleMap = loadVehicles(countMap.keySet());
    return ApiResult.ok(
        countMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(
                entry -> {
                  Vehicle vehicle = vehicleMap.get(entry.getKey());
                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("targetId", entry.getKey());
                  item.put("targetType", "VEHICLE");
                  item.put("targetName", vehicle != null ? vehicle.getPlateNo() : "车辆#" + entry.getKey());
                  item.put("extraName", vehicle != null ? vehicle.getFleetName() : null);
                  item.put("vehicleId", entry.getKey());
                  item.put("vehicleNo", vehicle != null ? vehicle.getPlateNo() : "车辆#" + entry.getKey());
                  item.put("fleetName", vehicle != null ? vehicle.getFleetName() : null);
                  item.put("count", entry.getValue());
                  return item;
                })
            .toList());
  }

  @GetMapping("/fence-status")
  public ApiResult<List<AlertFence>> fenceStatus(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        alertFenceMapper.selectList(
            new LambdaQueryWrapper<AlertFence>()
                .eq(AlertFence::getTenantId, currentUser.getTenantId())
                .eq(AlertFence::getStatus, "ENABLED")
                .orderByAsc(AlertFence::getFenceCode)));
  }

  @PostMapping("/generate")
  public ApiResult<Map<String, Object>> generate(
      @RequestBody(required = false) AlertGenerateRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(generateForTenant(currentUser.getTenantId(), resolveGenerateTargetTypes(body)));
  }

  public Map<String, Object> generateForTenant(Long tenantId, Collection<String> requestedTargetTypes) {
    Set<String> targetTypes = normalizeTargetTypes(requestedTargetTypes);
    Map<String, AlertRule> enabledRuleMap = loadEnabledRuleMap(tenantId);
    AlertGenerationContext context =
        new AlertGenerationContext(tenantId, queryActiveAutoAlerts(tenantId, enabledRuleMap.keySet()));

    if (targetTypes.contains("PROJECT")) {
      generateProjectAlerts(context, enabledRuleMap.get("PROJECT_PROGRESS_LAG"));
      generateProjectPermitAlerts(context, enabledRuleMap.get("PROJECT_PERMIT_EXPIRING"));
    }
    if (targetTypes.contains("CONTRACT")) {
      generateContractAlerts(
          context,
          enabledRuleMap.get("CONTRACT_EXPIRING_SOON"),
          enabledRuleMap.get("CONTRACT_PAYMENT_OVERDUE"));
    }
    if (targetTypes.contains("USER")) {
      generatePersonnelAlerts(
          context,
          enabledRuleMap.get("PERSONNEL_LICENSE_EXPIRING"),
          enabledRuleMap.get("PERSONNEL_VIOLATION_SCORE"));
    }

    closeStaleAutoAlerts(context);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("tenantId", tenantId);
    result.put("targetTypes", targetTypes);
    result.put("createdCount", context.createdCount);
    result.put("updatedCount", context.updatedCount);
    result.put("closedCount", context.closedCount);
    result.put("activeCount", context.activeKeys.size());
    return result;
  }

  @PostMapping("/{id}/handle")
  public ApiResult<Void> handle(
      @PathVariable Long id, @RequestBody AlertHandleRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    AlertEvent entity = requireEntity(id, currentUser.getTenantId());
    entity.setAlertStatus(defaultValue(body != null ? body.getStatus() : null, "PROCESSING"));
    entity.setHandleRemark(trimToNull(body != null ? body.getHandleRemark() : null));
    if ("CLOSED".equalsIgnoreCase(entity.getAlertStatus()) || "CONFIRMED".equalsIgnoreCase(entity.getAlertStatus())) {
      entity.setResolveTime(LocalDateTime.now());
    }
    alertEventMapper.updateById(entity);
    return ApiResult.ok();
  }

  @PostMapping("/{id}/close")
  public ApiResult<Void> close(
      @PathVariable Long id, @RequestBody AlertHandleRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    AlertEvent entity = requireEntity(id, currentUser.getTenantId());
    entity.setAlertStatus("CLOSED");
    entity.setHandleRemark(trimToNull(body != null ? body.getHandleRemark() : null));
    entity.setResolveTime(LocalDateTime.now());
    alertEventMapper.updateById(entity);
    return ApiResult.ok();
  }

  private List<AlertEvent> queryTenantAlerts(Long tenantId) {
    return alertEventMapper.selectList(
        new LambdaQueryWrapper<AlertEvent>()
            .eq(AlertEvent::getTenantId, tenantId)
            .orderByDesc(AlertEvent::getOccurTime)
            .orderByDesc(AlertEvent::getId));
  }

  private Map<String, Object> buildModelCoverage(Long tenantId, List<AlertRule> rules) {
    List<AlertFence> fences =
        alertFenceMapper.selectList(
            new LambdaQueryWrapper<AlertFence>()
                .eq(AlertFence::getTenantId, tenantId)
                .orderByAsc(AlertFence::getFenceCode));
    List<AlertPushRule> pushRules =
        alertPushRuleMapper.selectList(
            new LambdaQueryWrapper<AlertPushRule>()
                .eq(AlertPushRule::getTenantId, tenantId)
                .orderByAsc(AlertPushRule::getRuleCode));
    Set<String> fenceRuleCodes = fences.stream().map(AlertFence::getRuleCode).filter(StringUtils::hasText).collect(Collectors.toSet());
    Set<String> pushRuleCodes = pushRules.stream().map(AlertPushRule::getRuleCode).filter(StringUtils::hasText).collect(Collectors.toSet());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("totalRules", rules.size());
    result.put("enabledRules", rules.stream().filter(item -> "ENABLED".equalsIgnoreCase(item.getStatus())).count());
    result.put("connectedFenceRules", rules.stream().filter(item -> fenceRuleCodes.contains(item.getRuleCode())).count());
    result.put("connectedPushRules", rules.stream().filter(item -> pushRuleCodes.contains(item.getRuleCode())).count());
    result.put(
        "sceneCoverage",
        rules.stream()
            .filter(item -> StringUtils.hasText(item.getRuleScene()))
            .collect(Collectors.groupingBy(AlertRule::getRuleScene, LinkedHashMap::new, Collectors.counting())));
    return result;
  }

  private List<Map<String, Object>> buildTrend(List<AlertEvent> rows, int days) {
    LocalDate end = LocalDate.now();
    LocalDate start = end.minusDays(days - 1L);
    Map<LocalDate, Long> countMap =
        rows.stream()
            .filter(item -> item.getOccurTime() != null)
            .map(item -> item.getOccurTime().toLocalDate())
            .filter(date -> !date.isBefore(start) && !date.isAfter(end))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    List<Map<String, Object>> result = new ArrayList<>();
    for (int i = 0; i < days; i++) {
      LocalDate date = start.plusDays(i);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("date", date.toString());
      item.put("count", countMap.getOrDefault(date, 0L));
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> buildBuckets(
      List<AlertEvent> rows, Function<AlertEvent, String> classifier, Map<String, String> labelMap) {
    Map<String, Long> countMap =
        rows.stream()
            .map(classifier)
            .map(value -> StringUtils.hasText(value) ? value : "UNKNOWN")
            .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    return countMap.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .map(
            entry -> {
              Map<String, Object> item = new LinkedHashMap<>();
              item.put("code", entry.getKey());
              item.put(
                  "label",
                  labelMap != null && StringUtils.hasText(labelMap.get(entry.getKey()))
                      ? labelMap.get(entry.getKey())
                      : entry.getKey());
              item.put("count", entry.getValue());
              return item;
            })
        .toList();
  }

  private long calcAvgHandleMinutes(List<AlertEvent> rows) {
    return Math.round(
        rows.stream()
            .filter(item -> item.getOccurTime() != null && item.getResolveTime() != null)
            .mapToLong(item -> ChronoUnit.MINUTES.between(item.getOccurTime(), item.getResolveTime()))
            .average()
            .orElse(0));
  }

  private long calcOverdueCount(List<AlertEvent> rows) {
    LocalDateTime now = LocalDateTime.now();
    return rows.stream()
        .filter(item -> !"CLOSED".equalsIgnoreCase(item.getAlertStatus()) && !"CONFIRMED".equalsIgnoreCase(item.getAlertStatus()))
        .filter(item -> item.getOccurTime() != null && ChronoUnit.HOURS.between(item.getOccurTime(), now) >= 24)
        .count();
  }

  private long countEnabledRules(Long tenantId) {
    return alertRuleMapper.selectCount(
        new LambdaQueryWrapper<AlertRule>()
            .eq(AlertRule::getTenantId, tenantId)
            .eq(AlertRule::getStatus, "ENABLED"));
  }

  private long countEnabledFences(Long tenantId) {
    return alertFenceMapper.selectCount(
        new LambdaQueryWrapper<AlertFence>()
            .eq(AlertFence::getTenantId, tenantId)
            .eq(AlertFence::getStatus, "ENABLED"));
  }

  private long countEnabledPushRules(Long tenantId) {
    return alertPushRuleMapper.selectCount(
        new LambdaQueryWrapper<AlertPushRule>()
            .eq(AlertPushRule::getTenantId, tenantId)
            .eq(AlertPushRule::getStatus, "ENABLED"));
  }

  private AlertEvent requireEntity(Long id, Long tenantId) {
    AlertEvent entity = alertEventMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "预警事件不存在");
    }
    return entity;
  }

  private List<Map<String, Object>> enrich(List<AlertEvent> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, Project> projectMap = loadProjects(rows.stream().map(AlertEvent::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap = loadSites(rows.stream().map(AlertEvent::getSiteId).collect(Collectors.toSet()));
    Map<Long, Vehicle> vehicleMap = loadVehicles(rows.stream().map(AlertEvent::getVehicleId).collect(Collectors.toSet()));
    Map<Long, Contract> contractMap = loadContracts(rows.stream().map(AlertEvent::getContractId).collect(Collectors.toSet()));
    Set<Long> userIds =
        rows.stream()
            .flatMap(
                row ->
                    java.util.stream.Stream.of(
                        row.getUserId(),
                        "USER".equalsIgnoreCase(row.getTargetType()) ? row.getTargetId() : null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, User> userMap = loadUsers(userIds);
    List<Map<String, Object>> result = new ArrayList<>();
    for (AlertEvent row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", row.getId());
      item.put("alertNo", row.getAlertNo());
      item.put("title", row.getTitle());
      item.put("alertType", row.getAlertType());
      item.put("ruleCode", row.getRuleCode());
      item.put("targetType", row.getTargetType());
      item.put("targetId", row.getTargetId());
      item.put("projectId", row.getProjectId());
      item.put("projectName", resolveProjectName(row.getProjectId() != null ? projectMap.get(row.getProjectId()) : null, row.getProjectId()));
      item.put("siteId", row.getSiteId());
      item.put("siteName", resolveSiteName(row.getSiteId() != null ? siteMap.get(row.getSiteId()) : null, row.getSiteId()));
      item.put("vehicleId", row.getVehicleId());
      item.put("vehicleNo", resolveVehicleNo(row.getVehicleId() != null ? vehicleMap.get(row.getVehicleId()) : null, row.getVehicleId()));
      item.put("userId", row.getUserId());
      item.put("userName", resolveUserName(row.getUserId() != null ? userMap.get(row.getUserId()) : null, row.getUserId()));
      item.put("contractId", row.getContractId());
      item.put("contractNo", resolveContractNo(row.getContractId() != null ? contractMap.get(row.getContractId()) : null, row.getContractId()));
      item.put("targetName", resolveTargetName(row, projectMap, siteMap, vehicleMap, contractMap, userMap));
      item.put("relatedId", row.getRelatedId());
      item.put("relatedType", row.getRelatedType());
      item.put("level", resolveLevel(row));
      item.put("status", row.getAlertStatus());
      item.put("content", row.getContent());
      item.put("sourceChannel", row.getSourceChannel());
      item.put("snapshotJson", row.getSnapshotJson());
      item.put("latestPositionJson", row.getLatestPositionJson());
      item.put("handleRemark", row.getHandleRemark());
      item.put("occurTime", row.getOccurTime());
      item.put("resolveTime", row.getResolveTime());
      item.put(
          "durationMinutes",
          row.getOccurTime() != null
              ? ChronoUnit.MINUTES.between(
                  row.getOccurTime(),
                  row.getResolveTime() != null ? row.getResolveTime() : LocalDateTime.now())
              : null);
      result.add(item);
    }
    result.sort(
        Comparator.comparing(
                (Map<String, Object> item) -> (LocalDateTime) item.get("occurTime"),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());
    return result;
  }

  private String resolveLevel(AlertEvent row) {
    return StringUtils.hasText(row.getAlertLevel()) ? row.getAlertLevel() : row.getLevel();
  }

  private Map<Long, Project> loadProjects(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return projectMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
  }

  private Map<Long, Site> loadSites(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return siteMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Site::getId, item -> item, (left, right) -> left));
  }

  private Map<Long, Vehicle> loadVehicles(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return vehicleMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Vehicle::getId, item -> item, (left, right) -> left));
  }

  private Map<Long, Contract> loadContracts(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return contractMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Contract::getId, item -> item, (left, right) -> left));
  }

  private Map<Long, User> loadUsers(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return userMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));
  }

  private String resolveProjectName(Project project, Long id) {
    if (project != null && StringUtils.hasText(project.getName())) {
      return project.getName();
    }
    return id != null ? "项目#" + id : "-";
  }

  private String resolveSiteName(Site site, Long id) {
    if (site != null && StringUtils.hasText(site.getName())) {
      return site.getName();
    }
    return id != null ? "场地#" + id : "-";
  }

  private String resolveVehicleNo(Vehicle vehicle, Long id) {
    if (vehicle != null && StringUtils.hasText(vehicle.getPlateNo())) {
      return vehicle.getPlateNo();
    }
    return id != null ? "车辆#" + id : "-";
  }

  private String resolveContractNo(Contract contract, Long id) {
    if (contract != null && StringUtils.hasText(contract.getContractNo())) {
      return contract.getContractNo();
    }
    return id != null ? "合同#" + id : "-";
  }

  private String resolveUserName(User user, Long id) {
    if (user != null && StringUtils.hasText(user.getName())) {
      return user.getName();
    }
    if (user != null && StringUtils.hasText(user.getUsername())) {
      return user.getUsername();
    }
    return id != null ? "人员#" + id : "-";
  }

  private String resolveTargetName(
      AlertEvent row,
      Map<Long, Project> projectMap,
      Map<Long, Site> siteMap,
      Map<Long, Vehicle> vehicleMap,
      Map<Long, Contract> contractMap,
      Map<Long, User> userMap) {
    if ("PROJECT".equalsIgnoreCase(row.getTargetType())) {
      return resolveProjectName(projectMap.get(row.getTargetId()), row.getTargetId());
    }
    if ("SITE".equalsIgnoreCase(row.getTargetType())) {
      return resolveSiteName(siteMap.get(row.getTargetId()), row.getTargetId());
    }
    if ("CONTRACT".equalsIgnoreCase(row.getTargetType())) {
      return resolveContractNo(contractMap.get(row.getContractId() != null ? row.getContractId() : row.getTargetId()), row.getTargetId());
    }
    if ("USER".equalsIgnoreCase(row.getTargetType())) {
      return resolveUserName(userMap.get(row.getUserId() != null ? row.getUserId() : row.getTargetId()), row.getTargetId());
    }
    return resolveVehicleNo(vehicleMap.get(row.getVehicleId() != null ? row.getVehicleId() : row.getTargetId()), row.getTargetId());
  }

  private Long resolveTopRiskId(AlertEvent row, String targetType) {
    if ("CONTRACT".equalsIgnoreCase(targetType)) {
      return row.getContractId() != null ? row.getContractId() : row.getTargetId();
    }
    if ("USER".equalsIgnoreCase(targetType)) {
      return row.getUserId() != null ? row.getUserId() : row.getTargetId();
    }
    return row.getVehicleId() != null ? row.getVehicleId() : row.getTargetId();
  }

  private Set<String> resolveGenerateTargetTypes(AlertGenerateRequest body) {
    List<String> requestTypes = body != null ? body.getTargetTypes() : null;
    if (requestTypes == null || requestTypes.isEmpty()) {
      return Set.of("PROJECT", "CONTRACT", "USER");
    }
    return normalizeTargetTypes(requestTypes);
  }

  private Set<String> normalizeTargetTypes(Collection<String> requestTypes) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String item : requestTypes) {
      if (StringUtils.hasText(item)) {
        result.add(item.trim().toUpperCase());
      }
    }
    return result.isEmpty() ? Set.of("PROJECT", "CONTRACT", "USER") : result;
  }

  private Map<String, AlertRule> loadEnabledRuleMap(Long tenantId) {
    return alertRuleMapper.selectList(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getTenantId, tenantId)
                .eq(AlertRule::getStatus, "ENABLED")
                .in(AlertRule::getRuleCode, AUTO_GENERATE_RULE_CODES))
        .stream()
        .filter(item -> StringUtils.hasText(item.getRuleCode()))
        .collect(Collectors.toMap(AlertRule::getRuleCode, item -> item, (left, right) -> left));
  }

  private Map<String, AlertEvent> queryActiveAutoAlerts(Long tenantId, Collection<String> ruleCodes) {
    if (ruleCodes == null || ruleCodes.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, AlertEvent> result = new LinkedHashMap<>();
    List<AlertEvent> rows =
        alertEventMapper.selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getTenantId, tenantId)
                .in(AlertEvent::getRuleCode, ruleCodes)
                .in(AlertEvent::getAlertStatus, List.of("PENDING", "PROCESSING"))
                .orderByDesc(AlertEvent::getOccurTime)
                .orderByDesc(AlertEvent::getId));
    for (AlertEvent row : rows) {
      result.putIfAbsent(buildAutoAlertKey(row.getRuleCode(), row.getTargetType(), row.getTargetId()), row);
    }
    return result;
  }

  private void generateProjectAlerts(AlertGenerationContext context, AlertRule rule) {
    if (rule == null) {
      return;
    }
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, context.tenantId));
    if (contracts.isEmpty()) {
      return;
    }
    Map<Long, Project> projectMap =
        loadProjects(contracts.stream().map(Contract::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap =
        loadSites(contracts.stream().map(Contract::getSiteId).collect(Collectors.toSet()));
    Map<Long, List<ContractTicket>> ticketsByContract =
        loadTicketsByContract(contracts.stream().map(Contract::getId).collect(Collectors.toSet()));
    Map<Long, List<Contract>> contractsByProject =
        contracts.stream()
            .filter(item -> item.getProjectId() != null)
            .collect(Collectors.groupingBy(Contract::getProjectId, LinkedHashMap::new, Collectors.toList()));

    int threshold = extractThreshold(rule.getThresholdJson(), 65);
    LocalDate today = LocalDate.now();
    for (Map.Entry<Long, List<Contract>> entry : contractsByProject.entrySet()) {
      Project project = projectMap.get(entry.getKey());
      if (project == null || project.getId() == null) {
        continue;
      }
      BigDecimal totalVolume = ZERO;
      BigDecimal usedVolume = ZERO;
      BigDecimal todayVolume = ZERO;
      Site matchedSite = null;
      for (Contract contract : entry.getValue()) {
        totalVolume = totalVolume.add(defaultDecimal(contract.getAgreedVolume()));
        if (matchedSite == null && contract.getSiteId() != null) {
          matchedSite = siteMap.get(contract.getSiteId());
        }
        for (ContractTicket ticket :
            ticketsByContract.getOrDefault(contract.getId(), Collections.emptyList())) {
          LocalDate ticketDate = ticket.getTicketDate();
          if (ticketDate == null || ticketDate.isAfter(today)) {
            continue;
          }
          BigDecimal volume = defaultDecimal(ticket.getVolume());
          usedVolume = usedVolume.add(volume);
          if (ticketDate.isEqual(today)) {
            todayVolume = todayVolume.add(volume);
          }
        }
      }
      if (totalVolume.compareTo(ZERO) <= 0) {
        continue;
      }
      int progressPercent = calculatePercent(usedVolume, totalVolume);
      boolean lagging = progressPercent < threshold;
      boolean stalled = todayVolume.compareTo(ZERO) <= 0 && usedVolume.compareTo(totalVolume) < 0;
      boolean blocked = Objects.equals(project.getStatus(), 2);
      if (!lagging && !stalled && !blocked) {
        continue;
      }
      String level =
          blocked || progressPercent + 15 < threshold ? "L3" : defaultLevel(rule.getLevel());
      String projectName = resolveProjectName(project, project.getId());
      String siteName =
          matchedSite != null ? resolveSiteName(matchedSite, matchedSite.getId()) : "-";
      StringBuilder content = new StringBuilder(projectName).append(" 当前进度 ").append(progressPercent).append("%");
      if (lagging) {
        content.append("，低于阈值 ").append(threshold).append("%");
      }
      if (stalled) {
        content.append("，今日暂无新增消纳量");
      }
      if (blocked) {
        content.append("，项目状态处于预警状态");
      }
      AutoAlertCandidate candidate = new AutoAlertCandidate();
      candidate.ruleCode = rule.getRuleCode();
      candidate.alertType = "PROJECT_PROGRESS";
      candidate.targetType = "PROJECT";
      candidate.targetId = project.getId();
      candidate.projectId = project.getId();
      candidate.siteId = matchedSite != null ? matchedSite.getId() : null;
      candidate.level = level;
      candidate.sourceChannel = "REPORT";
      candidate.title = projectName + " 项目进度预警";
      candidate.content = content.toString();
      candidate.snapshotJson =
          "{\"projectName\":\""
              + jsonEscape(projectName)
              + "\",\"siteName\":\""
              + jsonEscape(siteName)
              + "\",\"progressPercent\":"
              + progressPercent
              + ",\"threshold\":"
              + threshold
              + ",\"todayVolume\":"
              + formatDecimal(todayVolume)
              + "}";
      upsertAutoAlert(context, candidate);
    }
  }

  private void generateProjectPermitAlerts(AlertGenerationContext context, AlertRule rule) {
    if (rule == null) {
      return;
    }
    List<DisposalPermit> permits =
        disposalPermitMapper.selectList(
            new LambdaQueryWrapper<DisposalPermit>()
                .eq(DisposalPermit::getTenantId, context.tenantId)
                .isNotNull(DisposalPermit::getProjectId)
                .isNotNull(DisposalPermit::getExpireDate)
                .ne(DisposalPermit::getStatus, "VOID")
                .orderByAsc(DisposalPermit::getExpireDate)
                .orderByAsc(DisposalPermit::getId));
    if (permits.isEmpty()) {
      return;
    }
    Map<Long, Project> projectMap =
        loadProjects(permits.stream().map(DisposalPermit::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap =
        loadSites(permits.stream().map(DisposalPermit::getSiteId).collect(Collectors.toSet()));
    Map<Long, List<DisposalPermit>> permitsByProject =
        permits.stream()
            .filter(item -> item.getProjectId() != null)
            .collect(Collectors.groupingBy(DisposalPermit::getProjectId, LinkedHashMap::new, Collectors.toList()));

    int warnDays = readNumberParam(context.tenantId, "permit.expire.warn.days", extractThreshold(rule.getThresholdJson(), 30));
    LocalDate today = LocalDate.now();
    for (Map.Entry<Long, List<DisposalPermit>> entry : permitsByProject.entrySet()) {
      Project project = projectMap.get(entry.getKey());
      if (project == null || project.getId() == null) {
        continue;
      }
      List<DisposalPermit> expiringPermits =
          entry.getValue().stream()
              .filter(item -> item.getExpireDate() != null)
              .filter(item -> {
                long days = ChronoUnit.DAYS.between(today, item.getExpireDate());
                return days >= 0 && days <= warnDays;
              })
              .toList();
      if (expiringPermits.isEmpty()) {
        continue;
      }
      DisposalPermit nearest = expiringPermits.get(0);
      long nearestExpireDays = ChronoUnit.DAYS.between(today, nearest.getExpireDate());
      Site matchedSite = nearest.getSiteId() != null ? siteMap.get(nearest.getSiteId()) : null;
      AutoAlertCandidate candidate = new AutoAlertCandidate();
      candidate.ruleCode = rule.getRuleCode();
      candidate.alertType = "DISPOSAL_PERMIT_EXPIRE";
      candidate.targetType = "PROJECT";
      candidate.targetId = project.getId();
      candidate.projectId = project.getId();
      candidate.siteId = matchedSite != null ? matchedSite.getId() : null;
      candidate.relatedId = nearest.getId();
      candidate.relatedType = "DISPOSAL_PERMIT";
      candidate.level = nearestExpireDays <= 7 ? "L3" : defaultLevel(rule.getLevel());
      candidate.sourceChannel = "SYSTEM";
      candidate.title = resolveProjectName(project, project.getId()) + " 处置证有效期预警";
      candidate.content =
          "项目关联处置证中有 "
              + expiringPermits.size()
              + " 张即将到期，最近一张为 "
              + nearest.getPermitNo()
              + "，剩余 "
              + nearestExpireDays
              + " 天。";
      candidate.snapshotJson =
          "{\"projectName\":\""
              + jsonEscape(resolveProjectName(project, project.getId()))
              + "\",\"siteName\":\""
              + jsonEscape(matchedSite != null ? resolveSiteName(matchedSite, matchedSite.getId()) : "-")
              + "\",\"permitNo\":\""
              + jsonEscape(nearest.getPermitNo())
              + "\",\"expiringCount\":"
              + expiringPermits.size()
              + ",\"expireDate\":\""
              + nearest.getExpireDate()
              + "\",\"daysToExpire\":"
              + nearestExpireDays
              + "}";
      upsertAutoAlert(context, candidate);
    }
  }

  private void generateContractAlerts(
      AlertGenerationContext context, AlertRule expireRule, AlertRule paymentRule) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, context.tenantId));
    if (contracts.isEmpty()) {
      return;
    }
    int expireWarnDays =
        Math.max(
            extractThreshold(expireRule != null ? expireRule.getThresholdJson() : null, 45),
            readNumberParam(context.tenantId, "alert.contract.expire.warn.days", 45));
    int paymentOverdueDays =
        extractThreshold(paymentRule != null ? paymentRule.getThresholdJson() : null, 15);
    LocalDate today = LocalDate.now();
    for (Contract contract : contracts) {
      if (contract.getId() == null) {
        continue;
      }
      LocalDate expireDate = contract.getExpireDate();
      BigDecimal totalAmount =
          defaultDecimal(
              contract.getContractAmount() != null ? contract.getContractAmount() : contract.getAmount());
      BigDecimal receivedAmount = defaultDecimal(contract.getReceivedAmount());
      BigDecimal outstandingAmount = totalAmount.subtract(receivedAmount).max(ZERO);

      if (expireRule != null && expireDate != null) {
        long daysToExpire = ChronoUnit.DAYS.between(today, expireDate);
        if (daysToExpire >= 0 && daysToExpire <= expireWarnDays) {
          AutoAlertCandidate candidate = new AutoAlertCandidate();
          candidate.ruleCode = expireRule.getRuleCode();
          candidate.alertType = "CONTRACT_EXPIRE";
          candidate.targetType = "CONTRACT";
          candidate.targetId = contract.getId();
          candidate.projectId = contract.getProjectId();
          candidate.siteId = contract.getSiteId();
          candidate.contractId = contract.getId();
          candidate.level = daysToExpire <= 7 ? "L3" : defaultLevel(expireRule.getLevel());
          candidate.sourceChannel = "SYSTEM";
          candidate.title = resolveContractNo(contract, contract.getId()) + " 临期预警";
          candidate.content =
              "合同剩余 "
                  + daysToExpire
                  + " 天到期，需尽快安排续签、变更或结算收口。";
          candidate.snapshotJson =
              "{\"contractNo\":\""
                  + jsonEscape(resolveContractNo(contract, contract.getId()))
                  + "\",\"expireDate\":\""
                  + expireDate
                  + "\",\"daysToExpire\":"
                  + daysToExpire
                  + "}";
          upsertAutoAlert(context, candidate);
        }
      }

      if (paymentRule != null
          && expireDate != null
          && outstandingAmount.compareTo(ZERO) > 0
          && expireDate.isBefore(today)) {
        long overdueDays = ChronoUnit.DAYS.between(expireDate, today);
        if (overdueDays >= paymentOverdueDays) {
          AutoAlertCandidate candidate = new AutoAlertCandidate();
          candidate.ruleCode = paymentRule.getRuleCode();
          candidate.alertType = "CONTRACT_PAYMENT_OVERDUE";
          candidate.targetType = "CONTRACT";
          candidate.targetId = contract.getId();
          candidate.projectId = contract.getProjectId();
          candidate.siteId = contract.getSiteId();
          candidate.contractId = contract.getId();
          candidate.level = overdueDays >= paymentOverdueDays * 2L ? "L3" : defaultLevel(paymentRule.getLevel());
          candidate.sourceChannel = "SYSTEM";
          candidate.title = resolveContractNo(contract, contract.getId()) + " 应收款超期预警";
          candidate.content =
              "合同已逾期 "
                  + overdueDays
                  + " 天，未回款 "
                  + outstandingAmount.stripTrailingZeros().toPlainString()
                  + " 元，请尽快跟进。";
          candidate.snapshotJson =
              "{\"contractNo\":\""
                  + jsonEscape(resolveContractNo(contract, contract.getId()))
                  + "\",\"overdueDays\":"
                  + overdueDays
                  + ",\"outstandingAmount\":"
                  + formatDecimal(outstandingAmount)
                  + "}";
          upsertAutoAlert(context, candidate);
        }
      }
    }
  }

  private void generatePersonnelAlerts(
      AlertGenerationContext context, AlertRule licenseRule, AlertRule violationRule) {
    List<User> users =
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getTenantId, context.tenantId));
    if (users.isEmpty()) {
      return;
    }
    List<SecurityInspection> inspections =
        securityInspectionMapper.selectList(
            new LambdaQueryWrapper<SecurityInspection>()
                .eq(SecurityInspection::getTenantId, context.tenantId)
                .and(
                    wrapper ->
                        wrapper
                            .eq(SecurityInspection::getObjectType, "PERSON")
                            .or()
                            .isNotNull(SecurityInspection::getUserId))
                .orderByDesc(SecurityInspection::getCheckTime)
                .orderByDesc(SecurityInspection::getId));
    Map<Long, List<SecurityInspection>> inspectionMap =
        inspections.stream()
            .map(
                item -> {
                  if (item.getUserId() != null) {
                    return new java.util.AbstractMap.SimpleEntry<>(item.getUserId(), item);
                  }
                  return new java.util.AbstractMap.SimpleEntry<>(item.getObjectId(), item);
                })
            .filter(entry -> entry.getKey() != null)
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    LinkedHashMap::new,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    int licenseWarnDays =
        Math.max(
            extractThreshold(licenseRule != null ? licenseRule.getThresholdJson() : null, 30),
            readNumberParam(context.tenantId, "alert.personnel.license.warn.days", 30));
    int violationThreshold =
        extractThreshold(violationRule != null ? violationRule.getThresholdJson() : null, 70);
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    for (User user : users) {
      if (user.getId() == null) {
        continue;
      }
      List<SecurityInspection> userInspections =
          inspectionMap.getOrDefault(user.getId(), Collections.emptyList());
      boolean driverLike =
          !userInspections.isEmpty()
              || (StringUtils.hasText(user.getUserType())
                  && user.getUserType().toUpperCase().contains("DRIVER"));

      if (licenseRule != null && driverLike && user.getPasswordExpireTime() != null) {
        long daysToExpire = ChronoUnit.DAYS.between(today, user.getPasswordExpireTime().toLocalDate());
        if (daysToExpire >= 0 && daysToExpire <= licenseWarnDays) {
          String userName = resolveUserName(user, user.getId());
          AutoAlertCandidate candidate = new AutoAlertCandidate();
          candidate.ruleCode = licenseRule.getRuleCode();
          candidate.alertType = "PERSONNEL_LICENSE";
          candidate.targetType = "USER";
          candidate.targetId = user.getId();
          candidate.userId = user.getId();
          candidate.level = daysToExpire <= 7 ? "L3" : defaultLevel(licenseRule.getLevel());
          candidate.sourceChannel = "SYSTEM";
          candidate.title = userName + " 资质临期预警";
          candidate.content =
              "人员账号/证照有效期进入预警窗口，剩余 "
                  + daysToExpire
                  + " 天，请及时完成复核或延期。";
          candidate.snapshotJson =
              "{\"userName\":\""
                  + jsonEscape(userName)
                  + "\",\"expireDate\":\""
                  + user.getPasswordExpireTime().toLocalDate()
                  + "\",\"daysToExpire\":"
                  + daysToExpire
                  + "}";
          upsertAutoAlert(context, candidate);
        }
      }

      if (violationRule != null && !userInspections.isEmpty()) {
        List<SecurityInspection> recentInspections =
            userInspections.stream()
                .filter(
                    item ->
                        item.getCheckTime() != null
                            && !item.getCheckTime().isBefore(now.minusDays(30)))
                .toList();
        int riskScore = calculatePersonnelRiskScore(recentInspections, now);
        if (riskScore >= violationThreshold) {
          String userName = resolveUserName(user, user.getId());
          AutoAlertCandidate candidate = new AutoAlertCandidate();
          candidate.ruleCode = violationRule.getRuleCode();
          candidate.alertType = "PERSONNEL_RISK";
          candidate.targetType = "USER";
          candidate.targetId = user.getId();
          candidate.userId = user.getId();
          candidate.level = riskScore >= 85 ? "L3" : defaultLevel(violationRule.getLevel());
          candidate.sourceChannel = "SYSTEM";
          candidate.title = userName + " 驾驶风险预警";
          candidate.content =
              "近 30 天安全检查风险评分达到 "
                  + riskScore
                  + " 分，请复核违章行为、整改进度与安全教育落实情况。";
          candidate.snapshotJson =
              "{\"userName\":\""
                  + jsonEscape(userName)
                  + "\",\"score\":"
                  + riskScore
                  + ",\"inspectionCount\":"
                  + recentInspections.size()
                  + "}";
          upsertAutoAlert(context, candidate);
        }
      }
    }
  }

  private void upsertAutoAlert(AlertGenerationContext context, AutoAlertCandidate candidate) {
    if (candidate == null
        || !StringUtils.hasText(candidate.ruleCode)
        || !StringUtils.hasText(candidate.targetType)
        || candidate.targetId == null) {
      return;
    }
    String key = buildAutoAlertKey(candidate.ruleCode, candidate.targetType, candidate.targetId);
    context.activeKeys.add(key);
    AlertEvent existing = context.activeAlertMap.get(key);
    if (existing == null) {
      AlertEvent entity = new AlertEvent();
      entity.setTenantId(context.tenantId);
      entity.setAlertNo(buildAlertNo());
      applyAutoAlertCandidate(entity, candidate);
      entity.setAlertStatus("PENDING");
      entity.setStatus(0);
      entity.setOccurTime(LocalDateTime.now());
      alertEventMapper.insert(entity);
      context.activeAlertMap.put(key, entity);
      context.createdCount++;
      return;
    }
    applyAutoAlertCandidate(existing, candidate);
    existing.setResolveTime(null);
    alertEventMapper.updateById(existing);
    context.updatedCount++;
  }

  private void applyAutoAlertCandidate(AlertEvent entity, AutoAlertCandidate candidate) {
    entity.setTitle(candidate.title);
    entity.setAlertType(candidate.alertType);
    entity.setRuleCode(candidate.ruleCode);
    entity.setTargetType(candidate.targetType);
    entity.setTargetId(candidate.targetId);
    entity.setProjectId(candidate.projectId);
    entity.setSiteId(candidate.siteId);
    entity.setVehicleId(candidate.vehicleId);
    entity.setUserId(candidate.userId);
    entity.setContractId(candidate.contractId);
    entity.setRelatedId(candidate.relatedId);
    entity.setRelatedType(candidate.relatedType);
    entity.setLevel(candidate.level);
    entity.setAlertLevel(candidate.level);
    entity.setSourceChannel(candidate.sourceChannel);
    entity.setContent(candidate.content);
    entity.setSnapshotJson(candidate.snapshotJson);
  }

  private void closeStaleAutoAlerts(AlertGenerationContext context) {
    LocalDateTime now = LocalDateTime.now();
    for (Map.Entry<String, AlertEvent> entry : context.activeAlertMap.entrySet()) {
      if (context.activeKeys.contains(entry.getKey())) {
        continue;
      }
      AlertEvent entity = entry.getValue();
      if (entity == null || !isOpenStatus(entity.getAlertStatus())) {
        continue;
      }
      entity.setAlertStatus("CLOSED");
      entity.setResolveTime(now);
      entity.setHandleRemark("系统自动关闭：当前预警指标已恢复至阈值内");
      alertEventMapper.updateById(entity);
      context.closedCount++;
    }
  }

  private Map<Long, List<ContractTicket>> loadTicketsByContract(Set<Long> contractIds) {
    Set<Long> validIds = contractIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return contractTicketMapper.selectList(
            new LambdaQueryWrapper<ContractTicket>()
                .in(ContractTicket::getContractId, validIds)
                .orderByDesc(ContractTicket::getTicketDate)
                .orderByDesc(ContractTicket::getId))
        .stream()
        .filter(item -> item.getContractId() != null)
        .collect(Collectors.groupingBy(ContractTicket::getContractId, LinkedHashMap::new, Collectors.toList()));
  }

  private int calculatePersonnelRiskScore(List<SecurityInspection> inspections, LocalDateTime now) {
    if (inspections == null || inspections.isEmpty()) {
      return 0;
    }
    int score = 0;
    for (SecurityInspection inspection : inspections) {
      int issueCount = inspection.getIssueCount() != null ? inspection.getIssueCount() : 0;
      boolean fail =
          !"PASS".equalsIgnoreCase(inspection.getResultLevel())
              || issueCount > 0
              || "OPEN".equalsIgnoreCase(inspection.getStatus())
              || "RECTIFYING".equalsIgnoreCase(inspection.getStatus());
      if (fail) {
        score += 20;
      }
      score += Math.min(issueCount, 3) * 12;
      if (inspection.getRectifyDeadline() != null
          && inspection.getRectifyDeadline().isBefore(now)
          && !"CLOSED".equalsIgnoreCase(inspection.getStatus())) {
        score += 18;
      }
      if (StringUtils.hasText(inspection.getDangerLevel())
          && ("HIGH".equalsIgnoreCase(inspection.getDangerLevel())
              || "MAJOR".equalsIgnoreCase(inspection.getDangerLevel())
              || "SEVERE".equalsIgnoreCase(inspection.getDangerLevel()))) {
        score += 20;
      }
      if (inspection.getCheckTime() != null && !inspection.getCheckTime().isBefore(now.minusDays(7)) && fail) {
        score += 8;
      }
    }
    return Math.min(score, 100);
  }

  private int readNumberParam(Long tenantId, String paramKey, int defaultValue) {
    if (tenantId == null || !StringUtils.hasText(paramKey)) {
      return defaultValue;
    }
    SysParam entity =
        sysParamMapper.selectOne(
            new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getTenantId, tenantId)
                .eq(SysParam::getParamKey, paramKey)
                .eq(SysParam::getStatus, "ENABLED"));
    if (entity == null || !StringUtils.hasText(entity.getParamValue())) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(entity.getParamValue().trim());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private int extractThreshold(String thresholdJson, int defaultValue) {
    if (!StringUtils.hasText(thresholdJson)) {
      return defaultValue;
    }
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("\"threshold\"\\s*:\\s*(\\d+)").matcher(thresholdJson);
    if (matcher.find()) {
      try {
        return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException ex) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private int calculatePercent(BigDecimal used, BigDecimal total) {
    if (total == null || total.compareTo(ZERO) <= 0) {
      return 0;
    }
    return used.multiply(BigDecimal.valueOf(100))
        .divide(total, 0, java.math.RoundingMode.DOWN)
        .intValue();
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private String defaultLevel(String level) {
    return StringUtils.hasText(level) ? level.trim().toUpperCase() : "L2";
  }

  private boolean isOpenStatus(String status) {
    return "PENDING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status);
  }

  private String buildAutoAlertKey(String ruleCode, String targetType, Long targetId) {
    return defaultValue(ruleCode, "-")
        + "#"
        + defaultValue(targetType, "-")
        + "#"
        + (targetId != null ? targetId : 0);
  }

  private String buildAlertNo() {
    return "AL-" + System.currentTimeMillis();
  }

  private String jsonEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String formatDecimal(BigDecimal value) {
    return defaultDecimal(value).stripTrailingZeros().toPlainString();
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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

  @Data
  public static class AlertHandleRequest {
    private String status;
    private String handleRemark;
  }

  @Data
  public static class AlertGenerateRequest {
    private List<String> targetTypes;
  }

  private static class AlertGenerationContext {
    private final Long tenantId;
    private final Map<String, AlertEvent> activeAlertMap;
    private final Set<String> activeKeys = new LinkedHashSet<>();
    private int createdCount;
    private int updatedCount;
    private int closedCount;

    private AlertGenerationContext(Long tenantId, Map<String, AlertEvent> activeAlertMap) {
      this.tenantId = tenantId;
      this.activeAlertMap = new LinkedHashMap<>(activeAlertMap);
    }
  }

  private static class AutoAlertCandidate {
    private String ruleCode;
    private String alertType;
    private String title;
    private String targetType;
    private Long targetId;
    private Long projectId;
    private Long siteId;
    private Long vehicleId;
    private Long userId;
    private Long contractId;
    private Long relatedId;
    private String relatedType;
    private String level;
    private String sourceChannel;
    private String content;
    private String snapshotJson;
  }
}
