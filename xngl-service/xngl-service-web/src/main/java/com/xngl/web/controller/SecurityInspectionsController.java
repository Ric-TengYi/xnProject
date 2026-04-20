package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.alert.AlertEvent;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniSafetyLearningRecord;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspectionAction;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspection;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import com.xngl.infrastructure.persistence.entity.site.SiteDocument;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleInsuranceRecord;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleMaintenanceRecord;
import com.xngl.infrastructure.persistence.entity.vehicle.VehiclePersonnelCertificate;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.AlertEventMapper;
import com.xngl.infrastructure.persistence.mapper.MiniSafetyLearningRecordMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionActionMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDocumentMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleInsuranceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMaintenanceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehiclePersonnelCertificateMapper;
import com.xngl.manager.message.MessageRecordService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.CollaborationAccessScope;
import com.xngl.web.support.CollaborationAccessScopeResolver;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final SecurityInspectionMapper inspectionMapper;
  private final SecurityInspectionActionMapper inspectionActionMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final UserMapper userMapper;
  private final VehiclePersonnelCertificateMapper vehiclePersonnelCertificateMapper;
  private final MiniSafetyLearningRecordMapper miniSafetyLearningRecordMapper;
  private final VehicleInsuranceRecordMapper vehicleInsuranceRecordMapper;
  private final VehicleMaintenanceRecordMapper vehicleMaintenanceRecordMapper;
  private final AlertEventMapper alertEventMapper;
  private final SiteDocumentMapper siteDocumentMapper;
  private final SiteDeviceMapper siteDeviceMapper;
  private final MessageRecordService messageRecordService;
  private final UserContext userContext;
  private final CollaborationAccessScopeResolver collaborationAccessScopeResolver;

  public SecurityInspectionsController(
      SecurityInspectionMapper inspectionMapper,
      SecurityInspectionActionMapper inspectionActionMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      UserMapper userMapper,
      VehiclePersonnelCertificateMapper vehiclePersonnelCertificateMapper,
      MiniSafetyLearningRecordMapper miniSafetyLearningRecordMapper,
      VehicleInsuranceRecordMapper vehicleInsuranceRecordMapper,
      VehicleMaintenanceRecordMapper vehicleMaintenanceRecordMapper,
      AlertEventMapper alertEventMapper,
      SiteDocumentMapper siteDocumentMapper,
      SiteDeviceMapper siteDeviceMapper,
      MessageRecordService messageRecordService,
      UserContext userContext,
      CollaborationAccessScopeResolver collaborationAccessScopeResolver) {
    this.inspectionMapper = inspectionMapper;
    this.inspectionActionMapper = inspectionActionMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.userMapper = userMapper;
    this.vehiclePersonnelCertificateMapper = vehiclePersonnelCertificateMapper;
    this.miniSafetyLearningRecordMapper = miniSafetyLearningRecordMapper;
    this.vehicleInsuranceRecordMapper = vehicleInsuranceRecordMapper;
    this.vehicleMaintenanceRecordMapper = vehicleMaintenanceRecordMapper;
    this.alertEventMapper = alertEventMapper;
    this.siteDocumentMapper = siteDocumentMapper;
    this.siteDeviceMapper = siteDeviceMapper;
    this.messageRecordService = messageRecordService;
    this.userContext = userContext;
    this.collaborationAccessScopeResolver = collaborationAccessScopeResolver;
  }

  @GetMapping
  public ApiResult<List<Map<String, Object>>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String objectType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String resultLevel,
      @RequestParam(required = false) String checkScene,
      @RequestParam(required = false) String dangerLevel,
      @RequestParam(required = false) String hazardCategory,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Boolean overdueOnly,
      @RequestParam(required = false) String checkTimeFrom,
      @RequestParam(required = false) String checkTimeTo,
      @RequestParam(required = false) String rectifyDeadlineFrom,
      @RequestParam(required = false) String rectifyDeadlineTo,
      @RequestParam(required = false) String nextCheckTimeFrom,
      @RequestParam(required = false) String nextCheckTimeTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    CollaborationAccessScope scope = collaborationAccessScopeResolver.resolve(currentUser);
    List<SecurityInspection> rows =
        queryInspections(
            currentUser.getTenantId(),
            keyword,
            objectType,
            status,
            resultLevel,
            checkScene,
            dangerLevel,
            hazardCategory,
            projectId,
            siteId,
            vehicleId,
            userId,
            overdueOnly,
            parseDateTime(checkTimeFrom),
            parseDateTime(checkTimeTo),
            parseDateTime(rectifyDeadlineFrom),
            parseDateTime(rectifyDeadlineTo),
            parseDateTime(nextCheckTimeFrom),
            parseDateTime(nextCheckTimeTo));
    rows = filterVisibleInspections(rows, scope);
    return ApiResult.ok(enrich(rows, false));
  }

  @GetMapping("/{id:\\d+}")
  public ApiResult<Map<String, Object>> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    SecurityInspection entity =
        requireEntity(id, currentUser.getTenantId(), collaborationAccessScopeResolver.resolve(currentUser));
    return ApiResult.ok(enrich(List.of(entity), true).stream().findFirst().orElseGet(LinkedHashMap::new));
  }

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String objectType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String resultLevel,
      @RequestParam(required = false) String checkScene,
      @RequestParam(required = false) String dangerLevel,
      @RequestParam(required = false) String hazardCategory,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Boolean overdueOnly,
      @RequestParam(required = false) String checkTimeFrom,
      @RequestParam(required = false) String checkTimeTo,
      @RequestParam(required = false) String rectifyDeadlineFrom,
      @RequestParam(required = false) String rectifyDeadlineTo,
      @RequestParam(required = false) String nextCheckTimeFrom,
      @RequestParam(required = false) String nextCheckTimeTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    CollaborationAccessScope scope = collaborationAccessScopeResolver.resolve(currentUser);
    List<SecurityInspection> rows =
        queryInspections(
            currentUser.getTenantId(),
            keyword,
            objectType,
            status,
            resultLevel,
            checkScene,
            dangerLevel,
            hazardCategory,
            projectId,
            siteId,
            vehicleId,
            userId,
            overdueOnly,
            parseDateTime(checkTimeFrom),
            parseDateTime(checkTimeTo),
            parseDateTime(rectifyDeadlineFrom),
            parseDateTime(rectifyDeadlineTo),
            parseDateTime(nextCheckTimeFrom),
            parseDateTime(nextCheckTimeTo));
    rows = filterVisibleInspections(rows, scope);
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

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String objectType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String resultLevel,
      @RequestParam(required = false) String checkScene,
      @RequestParam(required = false) String dangerLevel,
      @RequestParam(required = false) String hazardCategory,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Boolean overdueOnly,
      @RequestParam(required = false) String checkTimeFrom,
      @RequestParam(required = false) String checkTimeTo,
      @RequestParam(required = false) String rectifyDeadlineFrom,
      @RequestParam(required = false) String rectifyDeadlineTo,
      @RequestParam(required = false) String nextCheckTimeFrom,
      @RequestParam(required = false) String nextCheckTimeTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    CollaborationAccessScope scope = collaborationAccessScopeResolver.resolve(currentUser);
    List<Map<String, Object>> rows =
        enrich(
            filterVisibleInspections(
                queryInspections(
                currentUser.getTenantId(),
                keyword,
                objectType,
                status,
                resultLevel,
                checkScene,
                dangerLevel,
                hazardCategory,
                projectId,
                siteId,
                vehicleId,
                userId,
                overdueOnly,
                parseDateTime(checkTimeFrom),
                parseDateTime(checkTimeTo),
                parseDateTime(rectifyDeadlineFrom),
                parseDateTime(rectifyDeadlineTo),
                parseDateTime(nextCheckTimeFrom),
                parseDateTime(nextCheckTimeTo)),
            scope),
            false);
    return csvResponse("security_inspections.csv", buildInspectionCsv(rows));
  }

  @PostMapping
  public ApiResult<Map<String, Object>> create(
      @RequestBody InspectionUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    CollaborationAccessScope scope = collaborationAccessScopeResolver.resolve(currentUser);
    normalizeRequest(body);
    validate(body);
    validateAccessibleReferences(body, currentUser.getTenantId(), scope);
    SecurityInspection entity = new SecurityInspection();
    entity.setTenantId(currentUser.getTenantId());
    entity.setInspectionNo(generateInspectionNo());
    mapToEntity(body, entity, currentUser);
    inspectionMapper.insert(entity);
    insertAction(entity, currentUser, "CREATE", "创建检查", null, entity.getStatus(), null, entity.getResultLevel(), entity.getDescription(), entity.getNextCheckTime());
    SecurityInspection saved = inspectionMapper.selectById(entity.getId());
    return ApiResult.ok(enrich(List.of(saved != null ? saved : entity), true).stream().findFirst().orElseGet(LinkedHashMap::new));
  }

  @PostMapping("/{id}/rectify")
  public ApiResult<Void> rectify(
      @PathVariable Long id, @RequestBody RectifyRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    SecurityInspection entity =
        requireEntity(id, currentUser.getTenantId(), collaborationAccessScopeResolver.resolve(currentUser));
    if (!"OPEN".equalsIgnoreCase(entity.getStatus()) && !"RECTIFYING".equalsIgnoreCase(entity.getStatus())) {
      throw new BizException(400, "仅待整改或整改中的台账支持整改处理");
    }
    String beforeStatus = entity.getStatus();
    String beforeResultLevel = entity.getResultLevel();
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
    insertAction(
        entity,
        currentUser,
        "RECTIFY",
        "整改处理",
        beforeStatus,
        entity.getStatus(),
        beforeResultLevel,
        entity.getResultLevel(),
        entity.getRectifyRemark(),
        entity.getNextCheckTime());
    pushInspectionMessages(
        entity,
        "安全台账已整改更新",
        "安全检查 "
            + entity.getInspectionNo()
            + "（"
            + entity.getTitle()
            + "）已完成整改处理，当前状态："
            + entity.getStatus()
            + "。"
            + (StringUtils.hasText(entity.getRectifyRemark()) ? " 整改说明：" + entity.getRectifyRemark() : ""),
        "安全台账");
    return ApiResult.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    SecurityInspection entity =
        requireEntity(id, currentUser.getTenantId(), collaborationAccessScopeResolver.resolve(currentUser));
    insertAction(
        entity,
        currentUser,
        "DELETE",
        "删除检查",
        entity.getStatus(),
        "DELETED",
        entity.getResultLevel(),
        entity.getResultLevel(),
        "删除安全检查记录",
        entity.getNextCheckTime());
    inspectionMapper.deleteById(id);
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

  private void normalizeRequest(InspectionUpsertRequest body) {
    if (body == null || !StringUtils.hasText(body.getObjectType())) {
      return;
    }
    String normalizedType = body.getObjectType().trim().toUpperCase();
    if ("PERSON".equals(normalizedType)) {
      if (body.getUserId() == null && body.getObjectId() != null) {
        body.setUserId(body.getObjectId());
      }
      if (body.getObjectId() == null && body.getUserId() != null) {
        body.setObjectId(body.getUserId());
      }
    } else if ("SITE".equals(normalizedType)) {
      if (body.getSiteId() == null && body.getObjectId() != null) {
        body.setSiteId(body.getObjectId());
      }
      if (body.getObjectId() == null && body.getSiteId() != null) {
        body.setObjectId(body.getSiteId());
      }
    } else if ("VEHICLE".equals(normalizedType)) {
      if (body.getVehicleId() == null && body.getObjectId() != null) {
        body.setVehicleId(body.getObjectId());
      }
      if (body.getObjectId() == null && body.getVehicleId() != null) {
        body.setObjectId(body.getVehicleId());
      }
    }
  }

  private void pushInspectionMessages(
      SecurityInspection entity, String title, String content, String senderName) {
    if (entity == null || entity.getTenantId() == null || entity.getId() == null) {
      return;
    }
    LinkedHashSet<Long> receiverIds = new LinkedHashSet<>();
    if (entity.getInspectorId() != null) {
      receiverIds.add(entity.getInspectorId());
    }
    Long relatedUserId = resolveRelatedUserId(entity);
    if (relatedUserId != null) {
      receiverIds.add(relatedUserId);
    }
    for (Long receiverId : receiverIds) {
      messageRecordService.pushUserMessage(
          entity.getTenantId(),
          receiverId,
          title,
          content,
          "安全台账通知",
          "/alerts/security?inspectionId=" + entity.getId(),
          "SECURITY_INSPECTION",
          String.valueOf(entity.getId()),
          senderName);
    }
  }

  private void validateAccessibleReferences(
      InspectionUpsertRequest body, Long tenantId, CollaborationAccessScope scope) {
    if (body == null) {
      return;
    }
    if (body.getProjectId() != null) {
      Project project = projectMapper.selectById(body.getProjectId());
      if (project == null || !scope.canAccessProject(body.getProjectId())) {
        throw new BizException(404, "关联项目不存在");
      }
    }
    if (body.getSiteId() != null) {
      Site site = siteMapper.selectById(body.getSiteId());
      if (site == null || !scope.canAccessSite(body.getSiteId())) {
        throw new BizException(404, "关联场地不存在");
      }
    }
    if (body.getVehicleId() != null) {
      Vehicle vehicle = vehicleMapper.selectById(body.getVehicleId());
      if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId) || !scope.canAccessVehicle(body.getVehicleId())) {
        throw new BizException(404, "关联车辆不存在");
      }
    }
    Long relatedUserId =
        body.getUserId() != null
            ? body.getUserId()
            : "PERSON".equalsIgnoreCase(body.getObjectType()) ? body.getObjectId() : null;
    if (relatedUserId != null) {
      User relatedUser = userMapper.selectById(relatedUserId);
      if (relatedUser == null
          || !Objects.equals(relatedUser.getTenantId(), tenantId)
          || !scope.canAccessUser(relatedUserId)) {
        throw new BizException(404, "关联人员不存在");
      }
    }
  }

  private SecurityInspection requireEntity(Long id, Long tenantId, CollaborationAccessScope scope) {
    SecurityInspection entity = inspectionMapper.selectById(id);
    if (entity == null
        || !Objects.equals(entity.getTenantId(), tenantId)
        || !scope.matchesSecurityInspection(entity)) {
      throw new BizException(404, "安全检查记录不存在");
    }
    return entity;
  }

  private List<SecurityInspection> filterVisibleInspections(
      List<SecurityInspection> rows, CollaborationAccessScope scope) {
    if (scope.isTenantWideAccess()) {
      return rows;
    }
    return rows.stream().filter(scope::matchesSecurityInspection).toList();
  }

  private List<SecurityInspection> queryInspections(
      Long tenantId,
      String keyword,
      String objectType,
      String status,
      String resultLevel,
      String checkScene,
      String dangerLevel,
      String hazardCategory,
      Long projectId,
      Long siteId,
      Long vehicleId,
      Long userId,
      Boolean overdueOnly,
      LocalDateTime checkTimeFrom,
      LocalDateTime checkTimeTo,
      LocalDateTime rectifyDeadlineFrom,
      LocalDateTime rectifyDeadlineTo,
      LocalDateTime nextCheckTimeFrom,
      LocalDateTime nextCheckTimeTo) {
    String keywordValue = trimToNull(keyword);
    List<SecurityInspection> rows =
        inspectionMapper.selectList(
            new LambdaQueryWrapper<SecurityInspection>()
                .eq(SecurityInspection::getTenantId, tenantId)
                .eq(StringUtils.hasText(objectType), SecurityInspection::getObjectType, objectType)
                .eq(StringUtils.hasText(status), SecurityInspection::getStatus, status)
                .eq(StringUtils.hasText(resultLevel), SecurityInspection::getResultLevel, resultLevel)
                .eq(StringUtils.hasText(checkScene), SecurityInspection::getCheckScene, checkScene)
                .eq(StringUtils.hasText(dangerLevel), SecurityInspection::getDangerLevel, dangerLevel)
                .eq(StringUtils.hasText(hazardCategory), SecurityInspection::getHazardCategory, hazardCategory)
                .eq(projectId != null, SecurityInspection::getProjectId, projectId)
                .eq(siteId != null, SecurityInspection::getSiteId, siteId)
                .eq(vehicleId != null, SecurityInspection::getVehicleId, vehicleId)
                .eq(userId != null, SecurityInspection::getUserId, userId)
                .ge(checkTimeFrom != null, SecurityInspection::getCheckTime, checkTimeFrom)
                .le(checkTimeTo != null, SecurityInspection::getCheckTime, checkTimeTo)
                .ge(rectifyDeadlineFrom != null, SecurityInspection::getRectifyDeadline, rectifyDeadlineFrom)
                .le(rectifyDeadlineTo != null, SecurityInspection::getRectifyDeadline, rectifyDeadlineTo)
                .ge(nextCheckTimeFrom != null, SecurityInspection::getNextCheckTime, nextCheckTimeFrom)
                .le(nextCheckTimeTo != null, SecurityInspection::getNextCheckTime, nextCheckTimeTo)
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
    if (!Boolean.TRUE.equals(overdueOnly)) {
      return rows;
    }
    LocalDateTime now = LocalDateTime.now();
    return rows.stream()
        .filter(item -> item.getRectifyDeadline() != null && item.getRectifyDeadline().isBefore(now))
        .filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus()))
        .toList();
  }

  private List<Map<String, Object>> enrich(List<SecurityInspection> rows, boolean includeActions) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, Project> projectMap = loadProjects(rows.stream().map(SecurityInspection::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap = loadSites(rows.stream().map(SecurityInspection::getSiteId).collect(Collectors.toSet()));
    Map<Long, Vehicle> vehicleMap = loadVehicles(rows.stream().map(SecurityInspection::getVehicleId).collect(Collectors.toSet()));
    Map<Long, User> userMap = loadUsers(resolveRelatedUserIds(rows));
    Long tenantId = rows.stream().map(SecurityInspection::getTenantId).filter(Objects::nonNull).findFirst().orElse(null);
    Map<Long, Map<String, Object>> personProfileMap = loadPersonProfiles(tenantId, rows, userMap);
    Map<Long, Map<String, Object>> vehicleProfileMap = loadVehicleProfiles(tenantId, rows);
    Map<Long, Map<String, Object>> siteProfileMap = loadSiteProfiles(tenantId, rows);
    Map<Long, List<Map<String, Object>>> actionMap =
        includeActions ? loadActionMap(rows.stream().map(SecurityInspection::getId).filter(Objects::nonNull).collect(Collectors.toSet())) : Map.of();
    List<Map<String, Object>> result = new ArrayList<>();
    for (SecurityInspection row : rows) {
      result.add(
          toItem(
              row,
              projectMap,
              siteMap,
              vehicleMap,
              userMap,
              personProfileMap,
              vehicleProfileMap,
              siteProfileMap,
              actionMap.getOrDefault(row.getId(), List.of()),
              includeActions));
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
      Map<Long, Vehicle> vehicleMap,
      Map<Long, User> userMap,
      Map<Long, Map<String, Object>> personProfileMap,
      Map<Long, Map<String, Object>> vehicleProfileMap,
      Map<Long, Map<String, Object>> siteProfileMap,
      List<Map<String, Object>> actions,
      boolean includeActions) {
    Long relatedUserId = resolveRelatedUserId(row);
    User relatedUser = relatedUserId != null ? userMap.get(relatedUserId) : null;
    User inspector = row.getInspectorId() != null ? userMap.get(row.getInspectorId()) : null;
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", row.getId());
    item.put("inspectionNo", row.getInspectionNo());
    item.put("objectType", row.getObjectType());
    item.put("objectId", row.getObjectId());
    item.put(
        "objectName",
        resolveObjectName(
            row,
            projectMap,
            siteMap,
            vehicleMap,
            userMap,
            relatedUserId));
    item.put(
        "objectLabel",
        resolveObjectLabel(
            row,
            projectMap,
            siteMap,
            vehicleMap,
            userMap,
            relatedUserId));
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
    item.put("userName", resolveUserName(relatedUser, relatedUserId));
    item.put("userMobile", relatedUser != null ? trimToNull(relatedUser.getMobile()) : null);
    item.put("inspectorId", row.getInspectorId());
    item.put("inspectorName", resolveInspectorName(row.getInspectorName(), inspector));
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
    Map<String, Object> relatedProfile = resolveRelatedProfile(row, relatedUserId, personProfileMap, vehicleProfileMap, siteProfileMap);
    item.put("relatedProfile", relatedProfile);
    item.put("relatedProfileSummary", buildRelatedProfileSummary(relatedProfile));
    if (includeActions) {
      item.put("actions", actions);
    }
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

  private Map<Long, User> loadUsers(Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return userMapper.selectBatchIds(validIds).stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));
  }

  private Map<Long, Map<String, Object>> loadPersonProfiles(Long tenantId, List<SecurityInspection> rows, Map<Long, User> userMap) {
    Set<Long> userIds = rows.stream().map(this::resolveRelatedUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    if (userIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<MiniSafetyLearningRecord>> learningMap = loadPersonLearningMap(tenantId, userIds);
    Map<Long, List<AlertEvent>> alertMap = loadAlertMapByUserId(tenantId, userIds);
    Map<Long, List<VehiclePersonnelCertificate>> certificateMap = loadPersonCertificateMap(tenantId, userIds, userMap);
    Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
    for (Long userId : userIds) {
      List<MiniSafetyLearningRecord> learnings = learningMap.getOrDefault(userId, List.of());
      List<AlertEvent> alerts = alertMap.getOrDefault(userId, List.of());
      List<VehiclePersonnelCertificate> certificates = certificateMap.getOrDefault(userId, List.of());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("profileType", "PERSON");
      item.put("certificateCount", certificates.size());
      item.put("expiringCertificateCount", countExpiringCertificates(certificates));
      item.put("overdueFeeCount", countCertificateFeeOverdues(certificates));
      item.put("learningCount", learnings.size());
      item.put("completedLearningCount", learnings.stream().filter(learning -> "COMPLETED".equalsIgnoreCase(learning.getStatus())).count());
      item.put("studyMinutes", learnings.stream().map(MiniSafetyLearningRecord::getStudiedMinutes).filter(Objects::nonNull).mapToLong(Integer::longValue).sum());
      item.put(
          "lastStudyTime",
          learnings.stream()
              .map(MiniSafetyLearningRecord::getLastStudyTime)
              .filter(Objects::nonNull)
              .max(LocalDateTime::compareTo)
              .orElse(null));
      item.put("openAlertCount", countOpenAlerts(alerts));
      item.put("highRiskAlertCount", countHighRiskAlerts(alerts));
      item.put("certificateOwners", certificates.stream().map(VehiclePersonnelCertificate::getPersonName).filter(StringUtils::hasText).distinct().limit(3).toList());
      result.put(userId, item);
    }
    return result;
  }

  private Map<Long, Map<String, Object>> loadVehicleProfiles(Long tenantId, List<SecurityInspection> rows) {
    Set<Long> vehicleIds =
        rows.stream()
            .map(row -> row.getVehicleId() != null ? row.getVehicleId() : "VEHICLE".equalsIgnoreCase(row.getObjectType()) ? row.getObjectId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (vehicleIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<VehicleInsuranceRecord>> insuranceMap = loadVehicleInsuranceMap(tenantId, vehicleIds);
    Map<Long, List<VehicleMaintenanceRecord>> maintenanceMap = loadVehicleMaintenanceMap(tenantId, vehicleIds);
    Map<Long, List<AlertEvent>> alertMap = loadAlertMapByVehicleId(tenantId, vehicleIds);
    Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
    for (Long vehicleId : vehicleIds) {
      List<VehicleInsuranceRecord> insurances = insuranceMap.getOrDefault(vehicleId, List.of());
      List<VehicleMaintenanceRecord> maintenances = maintenanceMap.getOrDefault(vehicleId, List.of());
      List<AlertEvent> alerts = alertMap.getOrDefault(vehicleId, List.of());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("profileType", "VEHICLE");
      item.put("insuranceCount", insurances.size());
      item.put("activeInsuranceCount", insurances.stream().filter(insurance -> "ACTIVE".equalsIgnoreCase(insurance.getStatus())).count());
      item.put("expiringInsuranceCount", countExpiringInsurances(insurances));
      item.put("expiredInsuranceCount", countExpiredInsurances(insurances));
      item.put("maintenanceCount", maintenances.size());
      item.put(
          "latestMaintenanceDate",
          maintenances.stream()
              .map(VehicleMaintenanceRecord::getServiceDate)
              .filter(Objects::nonNull)
              .max(LocalDate::compareTo)
              .orElse(null));
      item.put(
          "maintenanceCostTotal",
          maintenances.stream()
              .map(VehicleMaintenanceRecord::getCostAmount)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add));
      item.put("openAlertCount", countOpenAlerts(alerts));
      item.put("highRiskAlertCount", countHighRiskAlerts(alerts));
      result.put(vehicleId, item);
    }
    return result;
  }

  private Map<Long, Map<String, Object>> loadSiteProfiles(Long tenantId, List<SecurityInspection> rows) {
    Set<Long> siteIds =
        rows.stream()
            .map(row -> row.getSiteId() != null ? row.getSiteId() : "SITE".equalsIgnoreCase(row.getObjectType()) ? row.getObjectId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (siteIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<SiteDocument>> documentMap = loadSiteDocumentMap(tenantId, siteIds);
    Map<Long, List<SiteDevice>> deviceMap = loadSiteDeviceMap(tenantId, siteIds);
    Map<Long, List<AlertEvent>> alertMap = loadAlertMapBySiteId(tenantId, siteIds);
    Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
    for (Long siteId : siteIds) {
      List<SiteDocument> documents = documentMap.getOrDefault(siteId, List.of());
      List<SiteDevice> devices = deviceMap.getOrDefault(siteId, List.of());
      List<AlertEvent> alerts = alertMap.getOrDefault(siteId, List.of());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("profileType", "SITE");
      item.put("documentCount", documents.size());
      item.put("approvalDocumentCount", documents.stream().filter(document -> "APPROVAL".equalsIgnoreCase(document.getStageCode())).count());
      item.put("operationDocumentCount", documents.stream().filter(document -> "OPERATION".equalsIgnoreCase(document.getStageCode())).count());
      item.put("deviceCount", devices.size());
      item.put("onlineDeviceCount", devices.stream().filter(device -> isOnlineDevice(device.getStatus())).count());
      item.put("offlineDeviceCount", devices.stream().filter(device -> !isOnlineDevice(device.getStatus())).count());
      item.put(
          "latestDocumentTime",
          documents.stream()
              .map(SiteDocument::getCreateTime)
              .filter(Objects::nonNull)
              .max(LocalDateTime::compareTo)
              .orElse(null));
      item.put("openAlertCount", countOpenAlerts(alerts));
      item.put("highRiskAlertCount", countHighRiskAlerts(alerts));
      result.put(siteId, item);
    }
    return result;
  }

  private Map<Long, List<VehiclePersonnelCertificate>> loadPersonCertificateMap(
      Long tenantId, Set<Long> userIds, Map<Long, User> userMap) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    Set<String> names = new LinkedHashSet<>();
    Set<String> mobiles = new LinkedHashSet<>();
    for (Long userId : userIds) {
      User user = userMap.get(userId);
      if (user == null) {
        continue;
      }
      if (StringUtils.hasText(user.getName())) {
        names.add(user.getName().trim());
      }
      if (StringUtils.hasText(user.getUsername())) {
        names.add(user.getUsername().trim());
      }
      if (StringUtils.hasText(user.getMobile())) {
        mobiles.add(user.getMobile().trim());
      }
    }
    if (names.isEmpty() && mobiles.isEmpty()) {
      return Map.of();
    }
    List<VehiclePersonnelCertificate> certificates =
        vehiclePersonnelCertificateMapper.selectList(
            new LambdaQueryWrapper<VehiclePersonnelCertificate>()
                .eq(tenantId != null, VehiclePersonnelCertificate::getTenantId, tenantId)
                .and(
                    wrapper -> {
                      if (!names.isEmpty() && !mobiles.isEmpty()) {
                        wrapper
                            .in(VehiclePersonnelCertificate::getPersonName, names)
                            .or()
                            .in(VehiclePersonnelCertificate::getMobile, mobiles);
                      } else if (!names.isEmpty()) {
                        wrapper.in(VehiclePersonnelCertificate::getPersonName, names);
                      } else if (!mobiles.isEmpty()) {
                        wrapper.in(VehiclePersonnelCertificate::getMobile, mobiles);
                      }
                    })
                .orderByAsc(VehiclePersonnelCertificate::getDriverLicenseExpireDate)
                .orderByAsc(VehiclePersonnelCertificate::getTransportLicenseExpireDate)
                .orderByDesc(VehiclePersonnelCertificate::getId));
    Map<Long, List<VehiclePersonnelCertificate>> result = new LinkedHashMap<>();
    for (Long userId : userIds) {
      User user = userMap.get(userId);
      if (user == null) {
        continue;
      }
      result.put(
          userId,
          certificates.stream()
              .filter(certificate -> matchesCertificateUser(certificate, user))
              .toList());
    }
    return result;
  }

  private Map<Long, List<MiniSafetyLearningRecord>> loadPersonLearningMap(Long tenantId, Set<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return miniSafetyLearningRecordMapper
        .selectList(
            new LambdaQueryWrapper<MiniSafetyLearningRecord>()
                .eq(tenantId != null, MiniSafetyLearningRecord::getTenantId, tenantId)
                .in(MiniSafetyLearningRecord::getUserId, userIds)
                .orderByDesc(MiniSafetyLearningRecord::getLastStudyTime)
                .orderByDesc(MiniSafetyLearningRecord::getId))
        .stream()
        .collect(Collectors.groupingBy(MiniSafetyLearningRecord::getUserId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<VehicleInsuranceRecord>> loadVehicleInsuranceMap(Long tenantId, Set<Long> vehicleIds) {
    if (vehicleIds.isEmpty()) {
      return Map.of();
    }
    return vehicleInsuranceRecordMapper
        .selectList(
            new LambdaQueryWrapper<VehicleInsuranceRecord>()
                .eq(tenantId != null, VehicleInsuranceRecord::getTenantId, tenantId)
                .in(VehicleInsuranceRecord::getVehicleId, vehicleIds)
                .orderByAsc(VehicleInsuranceRecord::getEndDate)
                .orderByDesc(VehicleInsuranceRecord::getId))
        .stream()
        .collect(Collectors.groupingBy(VehicleInsuranceRecord::getVehicleId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<VehicleMaintenanceRecord>> loadVehicleMaintenanceMap(Long tenantId, Set<Long> vehicleIds) {
    if (vehicleIds.isEmpty()) {
      return Map.of();
    }
    return vehicleMaintenanceRecordMapper
        .selectList(
            new LambdaQueryWrapper<VehicleMaintenanceRecord>()
                .eq(tenantId != null, VehicleMaintenanceRecord::getTenantId, tenantId)
                .in(VehicleMaintenanceRecord::getVehicleId, vehicleIds)
                .orderByDesc(VehicleMaintenanceRecord::getServiceDate)
                .orderByDesc(VehicleMaintenanceRecord::getId))
        .stream()
        .collect(Collectors.groupingBy(VehicleMaintenanceRecord::getVehicleId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<SiteDocument>> loadSiteDocumentMap(Long tenantId, Set<Long> siteIds) {
    if (siteIds.isEmpty()) {
      return Map.of();
    }
    return siteDocumentMapper
        .selectList(
            new LambdaQueryWrapper<SiteDocument>()
                .eq(tenantId != null, SiteDocument::getTenantId, tenantId)
                .in(SiteDocument::getSiteId, siteIds)
                .orderByDesc(SiteDocument::getCreateTime)
                .orderByDesc(SiteDocument::getId))
        .stream()
        .collect(Collectors.groupingBy(SiteDocument::getSiteId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<SiteDevice>> loadSiteDeviceMap(Long tenantId, Set<Long> siteIds) {
    if (siteIds.isEmpty()) {
      return Map.of();
    }
    return siteDeviceMapper
        .selectList(
            new LambdaQueryWrapper<SiteDevice>()
                .eq(tenantId != null, SiteDevice::getTenantId, tenantId)
                .in(SiteDevice::getSiteId, siteIds)
                .orderByAsc(SiteDevice::getSiteId)
                .orderByAsc(SiteDevice::getId))
        .stream()
        .collect(Collectors.groupingBy(SiteDevice::getSiteId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<AlertEvent>> loadAlertMapByUserId(Long tenantId, Set<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return alertEventMapper
        .selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(tenantId != null, AlertEvent::getTenantId, tenantId)
                .and(
                    wrapper ->
                        wrapper
                            .in(AlertEvent::getUserId, userIds)
                            .or()
                            .eq(AlertEvent::getTargetType, "USER").in(AlertEvent::getTargetId, userIds))
                .orderByDesc(AlertEvent::getOccurTime)
                .orderByDesc(AlertEvent::getId))
        .stream()
        .collect(Collectors.groupingBy(this::resolveAlertUserId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<AlertEvent>> loadAlertMapByVehicleId(Long tenantId, Set<Long> vehicleIds) {
    if (vehicleIds.isEmpty()) {
      return Map.of();
    }
    return alertEventMapper
        .selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(tenantId != null, AlertEvent::getTenantId, tenantId)
                .and(
                    wrapper ->
                        wrapper
                            .in(AlertEvent::getVehicleId, vehicleIds)
                            .or()
                            .eq(AlertEvent::getTargetType, "VEHICLE").in(AlertEvent::getTargetId, vehicleIds))
                .orderByDesc(AlertEvent::getOccurTime)
                .orderByDesc(AlertEvent::getId))
        .stream()
        .collect(Collectors.groupingBy(this::resolveAlertVehicleId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, List<AlertEvent>> loadAlertMapBySiteId(Long tenantId, Set<Long> siteIds) {
    if (siteIds.isEmpty()) {
      return Map.of();
    }
    return alertEventMapper
        .selectList(
            new LambdaQueryWrapper<AlertEvent>()
                .eq(tenantId != null, AlertEvent::getTenantId, tenantId)
                .and(
                    wrapper ->
                        wrapper
                            .in(AlertEvent::getSiteId, siteIds)
                            .or()
                            .eq(AlertEvent::getTargetType, "SITE").in(AlertEvent::getTargetId, siteIds))
                .orderByDesc(AlertEvent::getOccurTime)
                .orderByDesc(AlertEvent::getId))
        .stream()
        .collect(Collectors.groupingBy(this::resolveAlertSiteId, LinkedHashMap::new, Collectors.toList()));
  }

  private Set<Long> resolveRelatedUserIds(List<SecurityInspection> rows) {
    return rows.stream()
        .flatMap(
            row ->
                java.util.stream.Stream.of(
                    resolveRelatedUserId(row),
                    row.getInspectorId()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Map<String, Object> resolveRelatedProfile(
      SecurityInspection row,
      Long relatedUserId,
      Map<Long, Map<String, Object>> personProfileMap,
      Map<Long, Map<String, Object>> vehicleProfileMap,
      Map<Long, Map<String, Object>> siteProfileMap) {
    if ("PERSON".equalsIgnoreCase(row.getObjectType())) {
      return relatedUserId != null ? personProfileMap.getOrDefault(relatedUserId, Map.of()) : Map.of();
    }
    if ("VEHICLE".equalsIgnoreCase(row.getObjectType())) {
      Long vehicleId = row.getVehicleId() != null ? row.getVehicleId() : row.getObjectId();
      return vehicleId != null ? vehicleProfileMap.getOrDefault(vehicleId, Map.of()) : Map.of();
    }
    if ("SITE".equalsIgnoreCase(row.getObjectType())) {
      Long siteId = row.getSiteId() != null ? row.getSiteId() : row.getObjectId();
      return siteId != null ? siteProfileMap.getOrDefault(siteId, Map.of()) : Map.of();
    }
    return Map.of();
  }

  private String buildRelatedProfileSummary(Map<String, Object> relatedProfile) {
    if (relatedProfile == null || relatedProfile.isEmpty()) {
      return null;
    }
    String profileType = String.valueOf(relatedProfile.get("profileType"));
    if ("PERSON".equalsIgnoreCase(profileType)) {
      return String.format(
          "证照%d，学习%d，未闭环预警%d",
          toInt(relatedProfile.get("certificateCount")),
          toInt(relatedProfile.get("learningCount")),
          toInt(relatedProfile.get("openAlertCount")));
    }
    if ("VEHICLE".equalsIgnoreCase(profileType)) {
      return String.format(
          "保险%d，维保%d，未闭环预警%d",
          toInt(relatedProfile.get("insuranceCount")),
          toInt(relatedProfile.get("maintenanceCount")),
          toInt(relatedProfile.get("openAlertCount")));
    }
    if ("SITE".equalsIgnoreCase(profileType)) {
      return String.format(
          "资料%d，设备%d，未闭环预警%d",
          toInt(relatedProfile.get("documentCount")),
          toInt(relatedProfile.get("deviceCount")),
          toInt(relatedProfile.get("openAlertCount")));
    }
    return null;
  }

  private Long resolveRelatedUserId(SecurityInspection row) {
    if (row.getUserId() != null) {
      return row.getUserId();
    }
    if ("PERSON".equalsIgnoreCase(row.getObjectType())) {
      return row.getObjectId();
    }
    return null;
  }

  private Long resolveAlertUserId(AlertEvent alert) {
    if (alert.getUserId() != null) {
      return alert.getUserId();
    }
    if ("USER".equalsIgnoreCase(alert.getTargetType())) {
      return alert.getTargetId();
    }
    return null;
  }

  private Long resolveAlertVehicleId(AlertEvent alert) {
    if (alert.getVehicleId() != null) {
      return alert.getVehicleId();
    }
    if ("VEHICLE".equalsIgnoreCase(alert.getTargetType())) {
      return alert.getTargetId();
    }
    return null;
  }

  private Long resolveAlertSiteId(AlertEvent alert) {
    if (alert.getSiteId() != null) {
      return alert.getSiteId();
    }
    if ("SITE".equalsIgnoreCase(alert.getTargetType())) {
      return alert.getTargetId();
    }
    return null;
  }

  private boolean matchesCertificateUser(VehiclePersonnelCertificate certificate, User user) {
    if (certificate == null || user == null) {
      return false;
    }
    if (StringUtils.hasText(certificate.getMobile())
        && StringUtils.hasText(user.getMobile())
        && certificate.getMobile().trim().equals(user.getMobile().trim())) {
      return true;
    }
    if (StringUtils.hasText(certificate.getPersonName()) && StringUtils.hasText(user.getName())
        && certificate.getPersonName().trim().equalsIgnoreCase(user.getName().trim())) {
      return true;
    }
    return StringUtils.hasText(certificate.getPersonName())
        && StringUtils.hasText(user.getUsername())
        && certificate.getPersonName().trim().equalsIgnoreCase(user.getUsername().trim());
  }

  private long countExpiringCertificates(List<VehiclePersonnelCertificate> certificates) {
    LocalDate threshold = LocalDate.now().plusDays(30);
    return certificates.stream()
        .filter(
            certificate ->
                (certificate.getDriverLicenseExpireDate() != null && !certificate.getDriverLicenseExpireDate().isAfter(threshold))
                    || (certificate.getTransportLicenseExpireDate() != null && !certificate.getTransportLicenseExpireDate().isAfter(threshold)))
        .count();
  }

  private long countCertificateFeeOverdues(List<VehiclePersonnelCertificate> certificates) {
    LocalDate today = LocalDate.now();
    return certificates.stream()
        .filter(certificate -> certificate.getFeeDueDate() != null && certificate.getFeeDueDate().isBefore(today))
        .filter(
            certificate ->
                defaultDecimal(certificate.getPaidAmount()).compareTo(defaultDecimal(certificate.getFeeAmount())) < 0)
        .count();
  }

  private long countExpiringInsurances(List<VehicleInsuranceRecord> insurances) {
    LocalDate threshold = LocalDate.now().plusDays(30);
    return insurances.stream()
        .filter(insurance -> insurance.getEndDate() != null && !insurance.getEndDate().isAfter(threshold))
        .filter(insurance -> insurance.getEndDate() == null || !insurance.getEndDate().isBefore(LocalDate.now()))
        .count();
  }

  private long countExpiredInsurances(List<VehicleInsuranceRecord> insurances) {
    LocalDate today = LocalDate.now();
    return insurances.stream()
        .filter(insurance -> insurance.getEndDate() != null && insurance.getEndDate().isBefore(today))
        .count();
  }

  private long countOpenAlerts(List<AlertEvent> alerts) {
    return alerts.stream().filter(alert -> !isClosedAlert(alert.getAlertStatus())).count();
  }

  private long countHighRiskAlerts(List<AlertEvent> alerts) {
    return alerts.stream()
        .filter(alert -> !isClosedAlert(alert.getAlertStatus()))
        .filter(
            alert ->
                "L1".equalsIgnoreCase(trimToNull(alert.getAlertLevel()))
                    || "HIGH".equalsIgnoreCase(trimToNull(alert.getAlertLevel()))
                    || "HIGH".equalsIgnoreCase(trimToNull(alert.getLevel())))
        .count();
  }

  private boolean isClosedAlert(String alertStatus) {
    String status = trimToNull(alertStatus);
    return "CLOSED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status);
  }

  private boolean isOnlineDevice(String status) {
    String normalized = trimToNull(status);
    return "ONLINE".equalsIgnoreCase(normalized)
        || "ACTIVE".equalsIgnoreCase(normalized)
        || "ENABLED".equalsIgnoreCase(normalized);
  }

  private Map<Long, List<Map<String, Object>>> loadActionMap(Set<Long> inspectionIds) {
    if (inspectionIds.isEmpty()) {
      return Map.of();
    }
    return inspectionActionMapper
        .selectList(
            new LambdaQueryWrapper<SecurityInspectionAction>()
                .in(SecurityInspectionAction::getInspectionId, inspectionIds)
                .orderByAsc(SecurityInspectionAction::getCreateTime)
                .orderByAsc(SecurityInspectionAction::getId))
        .stream()
        .collect(
            Collectors.groupingBy(
                SecurityInspectionAction::getInspectionId,
                LinkedHashMap::new,
                Collectors.mapping(this::toActionItem, Collectors.toList())));
  }

  private Map<String, Object> toActionItem(SecurityInspectionAction action) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", action.getId());
    item.put("actionType", action.getActionType());
    item.put("actionLabel", action.getActionLabel());
    item.put("beforeStatus", action.getBeforeStatus());
    item.put("afterStatus", action.getAfterStatus());
    item.put("beforeResultLevel", action.getBeforeResultLevel());
    item.put("afterResultLevel", action.getAfterResultLevel());
    item.put("actionRemark", action.getActionRemark());
    item.put("nextCheckTime", action.getNextCheckTime());
    item.put("actorId", action.getActorId());
    item.put("actorName", action.getActorName());
    item.put("actionTime", action.getCreateTime());
    return item;
  }

  private void insertAction(
      SecurityInspection entity,
      User actor,
      String actionType,
      String actionLabel,
      String beforeStatus,
      String afterStatus,
      String beforeResultLevel,
      String afterResultLevel,
      String actionRemark,
      LocalDateTime nextCheckTime) {
    if (entity == null || entity.getId() == null || entity.getTenantId() == null) {
      return;
    }
    SecurityInspectionAction action = new SecurityInspectionAction();
    action.setTenantId(entity.getTenantId());
    action.setInspectionId(entity.getId());
    action.setActionType(actionType);
    action.setActionLabel(actionLabel);
    action.setBeforeStatus(trimToNull(beforeStatus));
    action.setAfterStatus(trimToNull(afterStatus));
    action.setBeforeResultLevel(trimToNull(beforeResultLevel));
    action.setAfterResultLevel(trimToNull(afterResultLevel));
    action.setActionRemark(trimToNull(actionRemark));
    action.setNextCheckTime(nextCheckTime);
    action.setActorId(actor != null ? actor.getId() : null);
    action.setActorName(actor != null ? resolveInspectorName(actor.getName(), actor) : null);
    inspectionActionMapper.insert(action);
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

  private String resolveUserName(User user, Long id) {
    if (user != null && StringUtils.hasText(user.getName())) {
      return user.getName();
    }
    if (user != null && StringUtils.hasText(user.getUsername())) {
      return user.getUsername();
    }
    return id != null ? "人员#" + id : "-";
  }

  private String resolveInspectorName(String currentValue, User user) {
    if (StringUtils.hasText(currentValue)) {
      return currentValue;
    }
    return resolveUserName(user, user != null ? user.getId() : null);
  }

  private String resolveObjectName(
      SecurityInspection row,
      Map<Long, Project> projectMap,
      Map<Long, Site> siteMap,
      Map<Long, Vehicle> vehicleMap,
      Map<Long, User> userMap,
      Long relatedUserId) {
    if ("SITE".equalsIgnoreCase(row.getObjectType())) {
      Long siteId = row.getSiteId() != null ? row.getSiteId() : row.getObjectId();
      return resolveSiteName(siteId != null ? siteMap.get(siteId) : null, siteId);
    }
    if ("VEHICLE".equalsIgnoreCase(row.getObjectType())) {
      Long vehicleId = row.getVehicleId() != null ? row.getVehicleId() : row.getObjectId();
      return resolveVehicleNo(vehicleId != null ? vehicleMap.get(vehicleId) : null, vehicleId);
    }
    if ("PERSON".equalsIgnoreCase(row.getObjectType())) {
      return resolveUserName(relatedUserId != null ? userMap.get(relatedUserId) : null, relatedUserId);
    }
    if (row.getProjectId() != null) {
      return resolveProjectName(projectMap.get(row.getProjectId()), row.getProjectId());
    }
    return row.getObjectId() != null ? "对象#" + row.getObjectId() : "-";
  }

  private String resolveObjectLabel(
      SecurityInspection row,
      Map<Long, Project> projectMap,
      Map<Long, Site> siteMap,
      Map<Long, Vehicle> vehicleMap,
      Map<Long, User> userMap,
      Long relatedUserId) {
    String type = defaultValue(row.getObjectType(), "UNKNOWN");
    return type + " / " + resolveObjectName(row, projectMap, siteMap, vehicleMap, userMap, relatedUserId);
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

  private String generateInspectionNo() {
    return "SEC-" + LocalDateTime.now().format(NO_FORMATTER) + "-" + (System.nanoTime() % 1000);
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (Exception ex) {
      return null;
    }
  }

  private ResponseEntity<byte[]> csvResponse(String fileName, String content) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(content.getBytes(StandardCharsets.UTF_8));
  }

  private String buildInspectionCsv(List<Map<String, Object>> rows) {
    StringBuilder builder =
        new StringBuilder("检查编号,标题,对象类型,关联对象ID,关联对象名称,项目,场地,车辆,关联人员,检查场景,检查类型,隐患类别,隐患等级,检查结果,状态,问题数量,检查人,整改责任人,责任人电话,是否超期,安全档案摘要,证照数,学习记录数,保险数,维保记录数,场地资料数,场地设备数,未闭环预警数,检查时间,整改截止,整改时间,下次复查,预估整改费用,整改说明,检查说明\n");
    for (Map<String, Object> row : rows) {
      @SuppressWarnings("unchecked")
      Map<String, Object> relatedProfile =
          row.get("relatedProfile") instanceof Map<?, ?> profile ? (Map<String, Object>) profile : Map.of();
      builder
          .append(csv(row.get("inspectionNo"))).append(',')
          .append(csv(row.get("title"))).append(',')
          .append(csv(row.get("objectType"))).append(',')
          .append(csv(row.get("objectId"))).append(',')
          .append(csv(row.get("objectName"))).append(',')
          .append(csv(row.get("projectName"))).append(',')
          .append(csv(row.get("siteName"))).append(',')
          .append(csv(row.get("vehicleNo"))).append(',')
          .append(csv(row.get("userName"))).append(',')
          .append(csv(row.get("checkScene"))).append(',')
          .append(csv(row.get("checkType"))).append(',')
          .append(csv(row.get("hazardCategory"))).append(',')
          .append(csv(row.get("dangerLevel"))).append(',')
          .append(csv(row.get("resultLevel"))).append(',')
          .append(csv(row.get("status"))).append(',')
          .append(csv(row.get("issueCount"))).append(',')
          .append(csv(row.get("inspectorName"))).append(',')
          .append(csv(row.get("rectifyOwner"))).append(',')
          .append(csv(row.get("rectifyOwnerPhone"))).append(',')
          .append(csv(Boolean.TRUE.equals(row.get("isOverdue")) ? "是" : "否")).append(',')
          .append(csv(row.get("relatedProfileSummary"))).append(',')
          .append(csv(relatedProfile.get("certificateCount"))).append(',')
          .append(csv(relatedProfile.get("learningCount"))).append(',')
          .append(csv(relatedProfile.get("insuranceCount"))).append(',')
          .append(csv(relatedProfile.get("maintenanceCount"))).append(',')
          .append(csv(relatedProfile.get("documentCount"))).append(',')
          .append(csv(relatedProfile.get("deviceCount"))).append(',')
          .append(csv(relatedProfile.get("openAlertCount"))).append(',')
          .append(csv(formatDateTime((LocalDateTime) row.get("checkTime")))).append(',')
          .append(csv(formatDateTime((LocalDateTime) row.get("rectifyDeadline")))).append(',')
          .append(csv(formatDateTime((LocalDateTime) row.get("rectifyTime")))).append(',')
          .append(csv(formatDateTime((LocalDateTime) row.get("nextCheckTime")))).append(',')
          .append(csv(row.get("estimatedCost"))).append(',')
          .append(csv(row.get("rectifyRemark"))).append(',')
          .append(csv(row.get("description"))).append('\n');
    }
    return builder.toString();
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private long toInt(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value != null) {
      try {
        return Long.parseLong(String.valueOf(value));
      } catch (NumberFormatException ignored) {
        return 0L;
      }
    }
    return 0L;
  }

  private String csv(Object value) {
    if (value == null) {
      return "";
    }
    String text = String.valueOf(value).replace("\"", "\"\"");
    return "\"" + text + "\"";
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
