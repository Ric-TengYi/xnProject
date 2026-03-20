package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.event.ManualEvent;
import com.xngl.infrastructure.persistence.entity.event.ManualEventAuditLog;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ManualEventAuditLogMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class ManualEventsController {

  private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final ManualEventMapper eventMapper;
  private final ManualEventAuditLogMapper auditLogMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final VehicleMapper vehicleMapper;
  private final UserService userService;

  public ManualEventsController(
      ManualEventMapper eventMapper,
      ManualEventAuditLogMapper auditLogMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      VehicleMapper vehicleMapper,
      UserService userService) {
    this.eventMapper = eventMapper;
    this.auditLogMapper = auditLogMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.vehicleMapper = vehicleMapper;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<List<Map<String, Object>>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String sourceChannel,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String keywordValue = trimToNull(keyword);
    List<ManualEvent> events =
        eventMapper.selectList(
            new LambdaQueryWrapper<ManualEvent>()
                .eq(ManualEvent::getTenantId, currentUser.getTenantId())
                .eq(StringUtils.hasText(eventType), ManualEvent::getEventType, eventType)
                .eq(StringUtils.hasText(status), ManualEvent::getStatus, status)
                .eq(StringUtils.hasText(priority), ManualEvent::getPriority, priority)
                .eq(StringUtils.hasText(sourceChannel), ManualEvent::getSourceChannel, sourceChannel)
                .and(
                    StringUtils.hasText(keywordValue),
                    wrapper ->
                        wrapper
                            .like(ManualEvent::getEventNo, keywordValue)
                            .or()
                            .like(ManualEvent::getTitle, keywordValue)
                            .or()
                            .like(ManualEvent::getReporterName, keywordValue)
                            .or()
                            .like(ManualEvent::getContent, keywordValue))
                .orderByDesc(ManualEvent::getReportTime)
                .orderByDesc(ManualEvent::getId));
    return ApiResult.ok(enrich(events, currentUser.getTenantId()));
  }

  @GetMapping("/pending-audits")
  public ApiResult<List<Map<String, Object>>> pendingAudits(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ManualEvent> events =
        eventMapper.selectList(
            new LambdaQueryWrapper<ManualEvent>()
                .eq(ManualEvent::getTenantId, currentUser.getTenantId())
                .eq(ManualEvent::getStatus, "PENDING_AUDIT")
                .orderByDesc(ManualEvent::getReportTime)
                .orderByDesc(ManualEvent::getId));
    return ApiResult.ok(enrich(events, currentUser.getTenantId()));
  }

  @GetMapping("/summary")
  public ApiResult<Map<String, Object>> summary(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ManualEvent> events =
        eventMapper.selectList(
            new LambdaQueryWrapper<ManualEvent>()
                .eq(ManualEvent::getTenantId, currentUser.getTenantId())
                .orderByDesc(ManualEvent::getReportTime)
                .orderByDesc(ManualEvent::getId));
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", events.size());
    result.put("draftCount", events.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus())).count());
    result.put(
        "pendingAuditCount",
        events.stream().filter(item -> "PENDING_AUDIT".equalsIgnoreCase(item.getStatus())).count());
    result.put("processingCount", events.stream().filter(item -> "PROCESSING".equalsIgnoreCase(item.getStatus())).count());
    result.put("rejectedCount", events.stream().filter(item -> "REJECTED".equalsIgnoreCase(item.getStatus())).count());
    result.put("closedCount", events.stream().filter(item -> "CLOSED".equalsIgnoreCase(item.getStatus())).count());
    result.put("highPriorityCount", events.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getPriority())).count());
    result.put(
        "overdueCount",
        events.stream()
            .filter(item -> item.getDeadlineTime() != null && item.getDeadlineTime().isBefore(now))
            .filter(item -> !"CLOSED".equalsIgnoreCase(item.getStatus()))
            .count());
    result.put(
        "todayCount",
        events.stream()
            .filter(item -> item.getReportTime() != null)
            .filter(item -> item.getReportTime().toLocalDate().isEqual(LocalDate.now()))
            .count());
    result.put("typeBuckets", buildBuckets(events, ManualEvent::getEventType));
    result.put("sourceBuckets", buildBuckets(events, ManualEvent::getSourceChannel));
    return ApiResult.ok(result);
  }

  @GetMapping("/{id}")
  public ApiResult<Map<String, Object>> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("record", enrich(List.of(event), currentUser.getTenantId()).stream().findFirst().orElseGet(LinkedHashMap::new));
    result.put(
        "auditLogs",
        auditLogMapper.selectList(
            new LambdaQueryWrapper<ManualEventAuditLog>()
                .eq(ManualEventAuditLog::getTenantId, currentUser.getTenantId())
                .eq(ManualEventAuditLog::getEventId, id)
                .orderByAsc(ManualEventAuditLog::getAuditTime)
                .orderByAsc(ManualEventAuditLog::getId)));
    return ApiResult.ok(result);
  }

  @PostMapping
  public ApiResult<Map<String, Object>> create(
      @RequestBody EventUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validate(body);
    ManualEvent event = new ManualEvent();
    event.setTenantId(currentUser.getTenantId());
    event.setEventNo("EV-" + LocalDateTime.now().format(NO_FORMATTER));
    mapToEntity(body, event, currentUser, false);
    event.setReportTime(LocalDateTime.now());
    eventMapper.insert(event);
    insertAuditLog(currentUser, event, "CREATE", event.getStatus(), "创建事件");
    return get(event.getId(), request);
  }

  @PutMapping("/{id}")
  public ApiResult<Map<String, Object>> update(
      @PathVariable Long id, @RequestBody EventUpsertRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validate(body);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    if (!"DRAFT".equalsIgnoreCase(event.getStatus()) && !"REJECTED".equalsIgnoreCase(event.getStatus())) {
      throw new BizException(400, "仅草稿或已退回事件支持编辑");
    }
    mapToEntity(body, event, currentUser, true);
    eventMapper.updateById(event);
    insertAuditLog(currentUser, event, "UPDATE", event.getStatus(), "更新事件信息");
    return get(event.getId(), request);
  }

  @PostMapping("/{id}/submit")
  public ApiResult<Void> submit(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    event.setStatus("PENDING_AUDIT");
    event.setCurrentAuditNode("MANUAL_EVENT_AUDIT");
    eventMapper.updateById(event);
    insertAuditLog(currentUser, event, "SUBMIT", "PENDING_AUDIT", "提交审核");
    return ApiResult.ok();
  }

  @PostMapping("/{id}/approve")
  public ApiResult<Void> approve(
      @PathVariable Long id, @RequestBody AuditActionRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    event.setStatus("PROCESSING");
    event.setCurrentAuditNode("EVENT_DISPATCH");
    eventMapper.updateById(event);
    insertAuditLog(currentUser, event, "APPROVE", "PROCESSING", trimToNull(body != null ? body.getComment() : null));
    return ApiResult.ok();
  }

  @PostMapping("/{id}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long id, @RequestBody AuditActionRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    event.setStatus("REJECTED");
    event.setCurrentAuditNode("APPLICANT_REWORK");
    eventMapper.updateById(event);
    insertAuditLog(currentUser, event, "REJECT", "REJECTED", trimToNull(body != null ? body.getComment() : null));
    return ApiResult.ok();
  }

  @PostMapping("/{id}/close")
  public ApiResult<Void> close(
      @PathVariable Long id, @RequestBody AuditActionRequest body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireEvent(id, currentUser.getTenantId());
    event.setStatus("CLOSED");
    event.setCurrentAuditNode("DONE");
    event.setCloseTime(LocalDateTime.now());
    event.setCloseRemark(trimToNull(body != null ? body.getComment() : null));
    eventMapper.updateById(event);
    insertAuditLog(currentUser, event, "CLOSE", "CLOSED", event.getCloseRemark());
    return ApiResult.ok();
  }

  private void validate(EventUpsertRequest body) {
    if (body == null || !StringUtils.hasText(body.getEventType()) || !StringUtils.hasText(body.getTitle())) {
      throw new BizException(400, "事件类型和标题不能为空");
    }
  }

  private void mapToEntity(EventUpsertRequest body, ManualEvent event, User currentUser, boolean keepReporter) {
    event.setEventType(body.getEventType().trim().toUpperCase());
    event.setTitle(body.getTitle().trim());
    event.setContent(trimToNull(body.getContent()));
    event.setSourceChannel(defaultValue(body.getSourceChannel(), "WEB"));
    event.setReportAddress(trimToNull(body.getReportAddress()));
    event.setProjectId(body.getProjectId());
    event.setSiteId(body.getSiteId());
    event.setVehicleId(body.getVehicleId());
    if (!keepReporter) {
      event.setReporterId(currentUser.getId());
      event.setReporterName(StringUtils.hasText(currentUser.getName()) ? currentUser.getName() : currentUser.getUsername());
    }
    event.setContactPhone(trimToNull(body.getContactPhone()));
    event.setPriority(defaultValue(body.getPriority(), "MEDIUM"));
    event.setStatus(defaultValue(body.getStatus(), "DRAFT"));
    event.setCurrentAuditNode(defaultValue(body.getCurrentAuditNode(), "MANUAL_EVENT_AUDIT"));
    event.setOccurTime(body.getOccurTime() != null ? body.getOccurTime() : LocalDateTime.now());
    event.setDeadlineTime(body.getDeadlineTime());
    event.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    event.setAssigneeName(trimToNull(body.getAssigneeName()));
    event.setAssigneePhone(trimToNull(body.getAssigneePhone()));
    event.setDispatchRemark(trimToNull(body.getDispatchRemark()));
  }

  private void insertAuditLog(User currentUser, ManualEvent event, String action, String resultStatus, String comment) {
    ManualEventAuditLog log = new ManualEventAuditLog();
    log.setTenantId(currentUser.getTenantId());
    log.setEventId(event.getId());
    log.setNodeCode(defaultValue(event.getCurrentAuditNode(), "MANUAL_EVENT_AUDIT"));
    log.setAction(action);
    log.setResultStatus(resultStatus);
    log.setAuditorId(currentUser.getId());
    log.setAuditorName(StringUtils.hasText(currentUser.getName()) ? currentUser.getName() : currentUser.getUsername());
    log.setComment(trimToNull(comment));
    log.setAuditTime(LocalDateTime.now());
    auditLogMapper.insert(log);
  }

  private ManualEvent requireEvent(Long id, Long tenantId) {
    ManualEvent event = eventMapper.selectById(id);
    if (event == null || !Objects.equals(event.getTenantId(), tenantId)) {
      throw new BizException(404, "事件不存在");
    }
    return event;
  }

  private List<Map<String, Object>> enrich(List<ManualEvent> events, Long tenantId) {
    if (events.isEmpty()) {
      return List.of();
    }
    Map<Long, Project> projectMap = loadProjects(events.stream().map(ManualEvent::getProjectId).collect(Collectors.toSet()));
    Map<Long, Site> siteMap = loadSites(events.stream().map(ManualEvent::getSiteId).collect(Collectors.toSet()));
    Map<Long, Vehicle> vehicleMap = loadVehicles(events.stream().map(ManualEvent::getVehicleId).collect(Collectors.toSet()));
    Map<Long, List<ManualEventAuditLog>> auditMap = loadAuditLogs(tenantId, events.stream().map(ManualEvent::getId).collect(Collectors.toSet()));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (ManualEvent event : events) {
      List<ManualEventAuditLog> logs = auditMap.getOrDefault(event.getId(), List.of());
      ManualEventAuditLog lastLog = logs.stream().max(Comparator.comparing(ManualEventAuditLog::getAuditTime, Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", event.getId());
      row.put("eventNo", event.getEventNo());
      row.put("eventType", event.getEventType());
      row.put("title", event.getTitle());
      row.put("content", event.getContent());
      row.put("priority", event.getPriority());
      row.put("status", event.getStatus());
      row.put("reporterId", event.getReporterId());
      row.put("reporterName", event.getReporterName());
      row.put("projectId", event.getProjectId());
      row.put("projectName", resolveProjectName(event.getProjectId() != null ? projectMap.get(event.getProjectId()) : null, event.getProjectId()));
      row.put("siteId", event.getSiteId());
      row.put("siteName", resolveSiteName(event.getSiteId() != null ? siteMap.get(event.getSiteId()) : null, event.getSiteId()));
      row.put("vehicleId", event.getVehicleId());
      row.put("vehicleNo", resolveVehicleNo(event.getVehicleId() != null ? vehicleMap.get(event.getVehicleId()) : null, event.getVehicleId()));
      row.put("sourceChannel", event.getSourceChannel());
      row.put("reportAddress", event.getReportAddress());
      row.put("currentAuditNode", event.getCurrentAuditNode());
      row.put("occurTime", event.getOccurTime());
      row.put("deadlineTime", event.getDeadlineTime());
      row.put("reportTime", event.getReportTime());
      row.put("closeTime", event.getCloseTime());
      row.put("closeRemark", event.getCloseRemark());
      row.put("contactPhone", event.getContactPhone());
      row.put("attachmentUrls", event.getAttachmentUrls());
      row.put("assigneeName", event.getAssigneeName());
      row.put("assigneePhone", event.getAssigneePhone());
      row.put("dispatchRemark", event.getDispatchRemark());
      row.put(
          "isOverdue",
          event.getDeadlineTime() != null
              && event.getDeadlineTime().isBefore(LocalDateTime.now())
              && !"CLOSED".equalsIgnoreCase(event.getStatus()));
      row.put("auditCount", logs.size());
      row.put("lastAuditTime", lastLog != null ? lastLog.getAuditTime() : null);
      row.put("lastAuditAction", lastLog != null ? lastLog.getAction() : null);
      rows.add(row);
    }
    rows.sort(
        Comparator.comparing(
                (Map<String, Object> row) -> (LocalDateTime) row.get("reportTime"),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());
    return rows;
  }

  private Map<Long, List<ManualEventAuditLog>> loadAuditLogs(Long tenantId, Set<Long> ids) {
    Set<Long> validIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return auditLogMapper.selectList(
            new LambdaQueryWrapper<ManualEventAuditLog>()
                .eq(ManualEventAuditLog::getTenantId, tenantId)
                .in(ManualEventAuditLog::getEventId, validIds)
                .orderByAsc(ManualEventAuditLog::getAuditTime)
                .orderByAsc(ManualEventAuditLog::getId))
        .stream()
        .collect(Collectors.groupingBy(ManualEventAuditLog::getEventId, LinkedHashMap::new, Collectors.mapping(Function.identity(), Collectors.toList())));
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
      List<ManualEvent> events, Function<ManualEvent, String> classifier) {
    return events.stream()
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
  public static class EventUpsertRequest {
    private String eventType;
    private String title;
    private String content;
    private String sourceChannel;
    private String reportAddress;
    private Long projectId;
    private Long siteId;
    private Long vehicleId;
    private String contactPhone;
    private String priority;
    private String status;
    private String currentAuditNode;
    private LocalDateTime occurTime;
    private LocalDateTime deadlineTime;
    private String attachmentUrls;
    private String assigneeName;
    private String assigneePhone;
    private String dispatchRemark;
  }

  @Data
  public static class AuditActionRequest {
    private String comment;
  }
}
