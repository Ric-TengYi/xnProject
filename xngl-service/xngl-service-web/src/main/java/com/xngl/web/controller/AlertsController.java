package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertEvent;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.alert.AlertPushRule;
import com.xngl.infrastructure.persistence.entity.alert.AlertRule;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.AlertEventMapper;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.infrastructure.persistence.mapper.AlertPushRuleMapper;
import com.xngl.infrastructure.persistence.mapper.AlertRuleMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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

  private final AlertEventMapper alertEventMapper;
  private final AlertFenceMapper alertFenceMapper;
  private final AlertRuleMapper alertRuleMapper;
  private final AlertPushRuleMapper alertPushRuleMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final ContractMapper contractMapper;
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
      UserService userService) {
    this.alertEventMapper = alertEventMapper;
    this.alertFenceMapper = alertFenceMapper;
    this.alertRuleMapper = alertRuleMapper;
    this.alertPushRuleMapper = alertPushRuleMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.contractMapper = contractMapper;
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
    User currentUser = requireCurrentUser(request);
    List<AlertEvent> rows =
        alertEventMapper.selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getTenantId, currentUser.getTenantId())
                .eq(AlertEvent::getTargetType, "VEHICLE")
                .orderByDesc(AlertEvent::getOccurTime));
    Map<Long, Long> countByVehicle =
        rows.stream()
            .filter(item -> item.getVehicleId() != null)
            .collect(Collectors.groupingBy(AlertEvent::getVehicleId, Collectors.counting()));
    if (countByVehicle.isEmpty()) {
      return ApiResult.ok(List.of());
    }
    Map<Long, Vehicle> vehicleMap = loadVehicles(countByVehicle.keySet());
    return ApiResult.ok(
        countByVehicle.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(
                entry -> {
                  Vehicle vehicle = vehicleMap.get(entry.getKey());
                  Map<String, Object> item = new LinkedHashMap<>();
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
      item.put("contractId", row.getContractId());
      item.put("contractNo", resolveContractNo(row.getContractId() != null ? contractMap.get(row.getContractId()) : null, row.getContractId()));
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
}
