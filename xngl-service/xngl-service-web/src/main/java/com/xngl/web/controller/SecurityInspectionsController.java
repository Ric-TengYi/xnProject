package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspection;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
@RequestMapping("/api/security/inspections")
public class SecurityInspectionsController {

  private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final SecurityInspectionMapper inspectionMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final UserService userService;

  public SecurityInspectionsController(
      SecurityInspectionMapper inspectionMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      UserService userService) {
    this.inspectionMapper = inspectionMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<Map<String, Object>>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String objectType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String resultLevel,
      @RequestParam(required = false) String checkScene,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<SecurityInspection> rows =
        inspectionMapper.selectList(
            new LambdaQueryWrapper<SecurityInspection>()
                .eq(SecurityInspection::getTenantId, currentUser.getTenantId())
                .eq(StringUtils.hasText(objectType), SecurityInspection::getObjectType, objectType)
                .eq(StringUtils.hasText(status), SecurityInspection::getStatus, status)
                .eq(StringUtils.hasText(resultLevel), SecurityInspection::getResultLevel, resultLevel)
                .eq(StringUtils.hasText(checkScene), SecurityInspection::getCheckScene, checkScene)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(SecurityInspection::getInspectionNo, keywordValue)
                            .or()
                            .like(SecurityInspection::getTitle, keywordValue)
                            .or()
                            .like(SecurityInspection::getInspectorName, keywordValue)
                            .or()
                            .like(SecurityInspection::getDescription, keywordValue))
                .orderByDesc(SecurityInspection::getCheckTime)
                .orderByDesc(SecurityInspection::getId));
    return ApiResult.ok(enrich(rows));
  }

  @GetMapping("/{id}")
  public ApiResult<Map<String, Object>> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    SecurityInspection entity = requireEntity(id, currentUser.getTenantId());
    return ApiResult.ok(enrich(List.of(entity)).stream().findFirst().orElseGet(LinkedHashMap::new));
  }

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<SecurityInspection> rows =
        inspectionMapper.selectList(
            new LambdaQueryWrapper<SecurityInspection>()
                .eq(SecurityInspection::getTenantId, currentUser.getTenantId())
                .orderByDesc(SecurityInspection::getCheckTime));
    YearMonth currentMonth = YearMonth.now();
    long monthCount =
        rows.stream()
            .filter(item -> item.getCheckTime() != null && YearMonth.from(item.getCheckTime()).equals(currentMonth))
            .count();
    long issueCount = rows.stream().map(SecurityInspection::getIssueCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
    long closedIssueCount =
        rows.stream()
            .filter(item -> "CLOSED".equalsIgnoreCase(item.getStatus()))
            .map(SecurityInspection::getIssueCount)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("monthInspectionCount", monthCount);
    result.put("issueCount", issueCount);
    result.put("closedIssueCount", closedIssueCount);
    result.put("openInspectionCount", rows.stream().filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus())).count());
    result.put("failCount", rows.stream().filter(item -> "FAIL".equalsIgnoreCase(item.getResultLevel())).count());
    result.put("passCount", rows.stream().filter(item -> "PASS".equalsIgnoreCase(item.getResultLevel())).count());
    result.put("rectifyingCount", rows.stream().filter(item -> "RECTIFYING".equalsIgnoreCase(item.getStatus())).count());
    result.put(
        "overdueRectifyCount",
        rows.stream()
            .filter(item -> item.getRectifyDeadline() != null && item.getRectifyDeadline().isBefore(LocalDateTime.now()))
            .filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus()))
            .count());
    result.put(
        "objectTypeBuckets",
        rows.stream()
            .filter(item -> StringUtils.hasText(item.getObjectType()))
            .collect(Collectors.groupingBy(SecurityInspection::getObjectType, LinkedHashMap::new, Collectors.counting())));
    result.put("dangerLevelBuckets", buildBuckets(rows, SecurityInspection::getDangerLevel));
    result.put("hazardCategoryBuckets", buildBuckets(rows, SecurityInspection::getHazardCategory));
    return ApiResult.ok(result);
  }

  @PostMapping
  public ApiResult<Map<String, Object>> create(
      @RequestBody InspectionUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validate(body);
    SecurityInspection entity = new SecurityInspection();
    entity.setTenantId(currentUser.getTenantId());
    entity.setInspectionNo("SEC-" + LocalDateTime.now().format(NO_FORMATTER));
    mapToEntity(body, entity, currentUser);
    inspectionMapper.insert(entity);
    SecurityInspection saved = inspectionMapper.selectById(entity.getId());
    return ApiResult.ok(enrich(List.of(saved != null ? saved : entity)).stream().findFirst().orElseGet(LinkedHashMap::new));
  }

  @PostMapping("/{id}/rectify")
  public ApiResult<Void> rectify(
      @PathVariable Long id, @RequestBody RectifyRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    SecurityInspection entity = requireEntity(id, currentUser.getTenantId());
    entity.setRectifyRemark(trimToNull(body != null ? body.getRectifyRemark() : null));
    entity.setRectifyTime(LocalDateTime.now());
    entity.setStatus(defaultValue(body != null ? body.getStatus() : null, "CLOSED"));
    if (body != null && body.getResultLevel() != null) {
      entity.setResultLevel(defaultValue(body.getResultLevel(), entity.getResultLevel()));
    }
    if (body != null) {
      entity.setNextCheckTime(body.getNextCheckTime());
    }
    inspectionMapper.updateById(entity);
    return ApiResult.ok();
  }

  private void validate(InspectionUpsertRequest body) {
    if (body == null || !StringUtils.hasText(body.getObjectType()) || !StringUtils.hasText(body.getTitle())) {
      throw new BizException(400, "检查对象类型和标题不能为空");
    }
  }

  private void mapToEntity(InspectionUpsertRequest body, SecurityInspection entity, User currentUser) {
    entity.setObjectType(body.getObjectType().trim().toUpperCase());
    entity.setObjectId(body.getObjectId());
    entity.setTitle(body.getTitle().trim());
    entity.setCheckScene(trimToNull(body.getCheckScene()));
    entity.setCheckType(trimToNull(body.getCheckType()));
    entity.setHazardCategory(trimToNull(body.getHazardCategory()));
    entity.setResultLevel(defaultValue(body.getResultLevel(), "PASS"));
    entity.setDangerLevel(defaultValue(body.getDangerLevel(), body.getIssueCount() != null && body.getIssueCount() > 2 ? "HIGH" : body.getIssueCount() != null && body.getIssueCount() > 0 ? "MEDIUM" : "LOW"));
    entity.setIssueCount(body.getIssueCount() != null ? body.getIssueCount() : 0);
    entity.setStatus(defaultValue(body.getStatus(), entity.getIssueCount() > 0 ? "OPEN" : "CLOSED"));
    entity.setProjectId(body.getProjectId());
    entity.setSiteId(body.getSiteId());
    entity.setVehicleId(body.getVehicleId());
    entity.setUserId(body.getUserId());
    entity.setInspectorId(currentUser.getId());
    entity.setInspectorName(StringUtils.hasText(currentUser.getName()) ? currentUser.getName() : currentUser.getUsername());
    entity.setRectifyOwner(trimToNull(body.getRectifyOwner()));
    entity.setRectifyOwnerPhone(trimToNull(body.getRectifyOwnerPhone()));
    entity.setDescription(trimToNull(body.getDescription()));
    entity.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    entity.setEstimatedCost(body.getEstimatedCost());
    entity.setRectifyDeadline(body.getRectifyDeadline());
    entity.setCheckTime(body.getCheckTime() != null ? body.getCheckTime() : LocalDateTime.now());
    entity.setNextCheckTime(body.getNextCheckTime());
  }

  private SecurityInspection requireEntity(Long id, Long tenantId) {
    SecurityInspection entity = inspectionMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "安全检查记录不存在");
    }
    return entity;
  }

  private List<Map<String, Object>> enrich(List<SecurityInspection> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, Project> projectMap = loadProjects(rows.stream().map(SecurityInspection::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap = loadSites(rows.stream().map(SecurityInspection::getSiteId).collect(Collectors.toSet()));
    Map<Long, Vehicle> vehicleMap = loadVehicles(rows.stream().map(SecurityInspection::getVehicleId).collect(Collectors.toSet()));
    List<Map<String, Object>> result = new ArrayList<>();
    for (SecurityInspection row : rows) {
      result.add(toItem(row, projectMap, siteMap, vehicleMap));
    }
    result.sort(
        Comparator.comparing(
                (Map<String, Object> item) -> (LocalDateTime) item.get("checkTime"),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());
    return result;
  }

  private Map<String, Object> toItem(
      SecurityInspection row,
      Map<Long, Project> projectMap,
      Map<Long, Site> siteMap,
      Map<Long, Vehicle> vehicleMap) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", row.getId());
    item.put("inspectionNo", row.getInspectionNo());
    item.put("objectType", row.getObjectType());
    item.put("objectId", row.getObjectId());
    item.put("title", row.getTitle());
    item.put("checkScene", row.getCheckScene());
    item.put("checkType", row.getCheckType());
    item.put("hazardCategory", row.getHazardCategory());
    item.put("resultLevel", row.getResultLevel());
    item.put("dangerLevel", row.getDangerLevel());
    item.put("issueCount", row.getIssueCount());
    item.put("status", row.getStatus());
    item.put("projectId", row.getProjectId());
    item.put("projectName", resolveProjectName(row.getProjectId() != null ? projectMap.get(row.getProjectId()) : null, row.getProjectId()));
    item.put("siteId", row.getSiteId());
    item.put("siteName", resolveSiteName(row.getSiteId() != null ? siteMap.get(row.getSiteId()) : null, row.getSiteId()));
    item.put("vehicleId", row.getVehicleId());
    item.put("vehicleNo", resolveVehicleNo(row.getVehicleId() != null ? vehicleMap.get(row.getVehicleId()) : null, row.getVehicleId()));
    item.put("userId", row.getUserId());
    item.put("inspectorId", row.getInspectorId());
    item.put("inspectorName", row.getInspectorName());
    item.put("rectifyOwner", row.getRectifyOwner());
    item.put("rectifyOwnerPhone", row.getRectifyOwnerPhone());
    item.put("description", row.getDescription());
    item.put("attachmentUrls", row.getAttachmentUrls());
    item.put("estimatedCost", row.getEstimatedCost());
    item.put("rectifyDeadline", row.getRectifyDeadline());
    item.put("rectifyRemark", row.getRectifyRemark());
    item.put("rectifyTime", row.getRectifyTime());
    item.put("checkTime", row.getCheckTime());
    item.put("nextCheckTime", row.getNextCheckTime());
    item.put(
        "isOverdue",
        row.getRectifyDeadline() != null
            && row.getRectifyDeadline().isBefore(LocalDateTime.now())
            && !"CLOSED".equalsIgnoreCase(row.getStatus()));
    return item;
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

  private List<Map<String, Object>> buildBuckets(
      List<SecurityInspection> rows, Function<SecurityInspection, String> classifier) {
    return rows.stream()
        .map(classifier)
        .map(value -> StringUtils.hasText(value) ? value : "UNKNOWN")
        .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .map(
            entry -> {
              Map<String, Object> item = new LinkedHashMap<>();
              item.put("code", entry.getKey());
              item.put("count", entry.getValue());
              return item;
            })
        .toList();
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
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

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  @Data
  public static class InspectionUpsertRequest {
    private String objectType;
    private Long objectId;
    private String title;
    private String checkScene;
    private String checkType;
    private String hazardCategory;
    private String resultLevel;
    private String dangerLevel;
    private Integer issueCount;
    private String status;
    private Long projectId;
    private Long siteId;
    private Long vehicleId;
    private Long userId;
    private String rectifyOwner;
    private String rectifyOwnerPhone;
    private String description;
    private String attachmentUrls;
    private java.math.BigDecimal estimatedCost;
    private LocalDateTime rectifyDeadline;
    private LocalDateTime checkTime;
    private LocalDateTime nextCheckTime;
  }

  @Data
  public static class RectifyRequest {
    private String status;
    private String resultLevel;
    private String rectifyRemark;
    private LocalDateTime nextCheckTime;
  }
}
