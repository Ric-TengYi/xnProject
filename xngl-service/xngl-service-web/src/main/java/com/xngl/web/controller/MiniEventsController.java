package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.event.ManualEvent;
import com.xngl.infrastructure.persistence.entity.event.ManualEventAuditLog;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SitePersonnelConfig;
import com.xngl.infrastructure.persistence.mapper.ManualEventAuditLogMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SitePersonnelConfigMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.mini.MiniEventCreateDto;
import com.xngl.web.dto.mini.MiniEventDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/events")
public class MiniEventsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter EVENT_NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final ManualEventMapper manualEventMapper;
  private final ManualEventAuditLogMapper manualEventAuditLogMapper;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final SitePersonnelConfigMapper sitePersonnelConfigMapper;
  private final UserContext userContext;

  public MiniEventsController(
      ManualEventMapper manualEventMapper,
      ManualEventAuditLogMapper manualEventAuditLogMapper,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      SitePersonnelConfigMapper sitePersonnelConfigMapper,
      UserContext userContext) {
    this.manualEventMapper = manualEventMapper;
    this.manualEventAuditLogMapper = manualEventAuditLogMapper;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.sitePersonnelConfigMapper = sitePersonnelConfigMapper;
    this.userContext = userContext;
  }

  @PostMapping
  public ApiResult<MiniEventDto> create(
      @RequestBody MiniEventCreateDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateCreate(body);
    Project project = body.getProjectId() != null ? requireProject(body.getProjectId()) : null;
    Site site = body.getSiteId() != null ? requireAccessibleSite(currentUser, body.getSiteId()) : null;

    ManualEvent event = new ManualEvent();
    event.setTenantId(currentUser.getTenantId());
    event.setEventNo(nextManualEventNo());
    event.setEventType(normalizeText(body.getEventType(), "MINI_EVENT"));
    event.setTitle(body.getTitle().trim());
    event.setContent(trimToNull(body.getContent()));
    event.setSourceChannel("MINI");
    event.setReportAddress(trimToNull(body.getReportAddress()));
    event.setProjectId(project != null ? project.getId() : null);
    event.setSiteId(site != null ? site.getId() : null);
    event.setReporterId(currentUser.getId());
    event.setReporterName(resolveUserDisplayName(currentUser));
    event.setContactPhone(currentUser.getMobile());
    event.setPriority(normalizeText(body.getPriority(), "MEDIUM"));
    event.setStatus("PENDING_AUDIT");
    event.setCurrentAuditNode("MANUAL_EVENT_AUDIT");
    event.setOccurTime(LocalDateTime.now());
    event.setDeadlineTime(parseDateTime(body.getDeadlineTime()));
    event.setReportTime(LocalDateTime.now());
    event.setAttachmentUrls(trimToNull(body.getAttachmentUrls()));
    manualEventMapper.insert(event);
    insertAuditLog(currentUser, event, "SUBMIT", "PENDING_AUDIT", "小程序事件上报");
    return ApiResult.ok(toDto(event, project, site));
  }

  @GetMapping
  public ApiResult<List<MiniEventDto>> list(
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ManualEvent> rows =
        manualEventMapper.selectList(
            new LambdaQueryWrapper<ManualEvent>()
                .eq(ManualEvent::getTenantId, currentUser.getTenantId())
                .eq(ManualEvent::getReporterId, currentUser.getId())
                .eq(ManualEvent::getSourceChannel, "MINI")
                .eq(StringUtils.hasText(eventType), ManualEvent::getEventType, eventType != null ? eventType.trim().toUpperCase() : null)
                .eq(StringUtils.hasText(status), ManualEvent::getStatus, status != null ? status.trim().toUpperCase() : null)
                .orderByDesc(ManualEvent::getReportTime)
                .orderByDesc(ManualEvent::getId));
    Map<Long, Project> projectMap =
        projectMapper.selectBatchIds(
                rows.stream()
                    .map(ManualEvent::getProjectId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)))
            .stream()
            .collect(java.util.stream.Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
    Map<Long, Site> siteMap =
        siteMapper.selectBatchIds(
                rows.stream()
                    .map(ManualEvent::getSiteId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)))
            .stream()
            .collect(java.util.stream.Collectors.toMap(Site::getId, item -> item, (left, right) -> left));
    return ApiResult.ok(
        rows.stream()
            .map(row -> toDto(row, projectMap.get(row.getProjectId()), siteMap.get(row.getSiteId())))
            .toList());
  }

  @GetMapping("/{id}")
  public ApiResult<Map<String, Object>> detail(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ManualEvent event = requireOwnMiniEvent(id, currentUser);
    Project project = event.getProjectId() != null ? projectMapper.selectById(event.getProjectId()) : null;
    Site site = event.getSiteId() != null ? siteMapper.selectById(event.getSiteId()) : null;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("record", toDto(event, project, site));
    result.put(
        "auditLogs",
        manualEventAuditLogMapper.selectList(
            new LambdaQueryWrapper<ManualEventAuditLog>()
                .eq(ManualEventAuditLog::getTenantId, currentUser.getTenantId())
                .eq(ManualEventAuditLog::getEventId, id)
                .orderByAsc(ManualEventAuditLog::getAuditTime)
                .orderByAsc(ManualEventAuditLog::getId)));
    return ApiResult.ok(result);
  }

  private void validateCreate(MiniEventCreateDto body) {
    if (body == null || !StringUtils.hasText(body.getTitle())) {
      throw new BizException(400, "事件标题不能为空");
    }
  }

  private ManualEvent requireOwnMiniEvent(Long id, User currentUser) {
    ManualEvent event = manualEventMapper.selectById(id);
    if (event == null
        || !Objects.equals(event.getTenantId(), currentUser.getTenantId())
        || !Objects.equals(event.getReporterId(), currentUser.getId())
        || !"MINI".equalsIgnoreCase(event.getSourceChannel())) {
      throw new BizException(404, "事件不存在");
    }
    return event;
  }

  private Project requireProject(Long id) {
    Project project = projectMapper.selectById(id);
    if (project == null || Objects.equals(project.getDeleted(), 1)) {
      throw new BizException(404, "项目不存在");
    }
    return project;
  }

  private Site requireAccessibleSite(User user, Long siteId) {
    Site site = siteMapper.selectById(siteId);
    if (site == null || Objects.equals(site.getDeleted(), 1)) {
      throw new BizException(404, "场地不存在");
    }
    validateSiteAccessIfPresent(user, siteId);
    return site;
  }

  private void validateSiteAccessIfPresent(User user, Long siteId) {
    if (siteId == null || isAdminUser(user)) {
      return;
    }
    boolean accessible =
        sitePersonnelConfigMapper.selectCount(
                new LambdaQueryWrapper<SitePersonnelConfig>()
                    .eq(SitePersonnelConfig::getTenantId, user.getTenantId())
                    .eq(SitePersonnelConfig::getUserId, user.getId())
                    .eq(SitePersonnelConfig::getSiteId, siteId)
                    .eq(SitePersonnelConfig::getAccountEnabled, 1))
            > 0;
    if (!accessible) {
      throw new BizException(403, "当前账号无权操作该场地");
    }
  }

  private MiniEventDto toDto(ManualEvent event, Project project, Site site) {
    return new MiniEventDto(
        String.valueOf(event.getId()),
        event.getEventNo(),
        event.getEventType(),
        event.getTitle(),
        event.getContent(),
        event.getProjectId() != null ? String.valueOf(event.getProjectId()) : null,
        project != null ? project.getName() : null,
        event.getSiteId() != null ? String.valueOf(event.getSiteId()) : null,
        site != null ? site.getName() : null,
        event.getPriority(),
        event.getStatus(),
        event.getReportAddress(),
        event.getAttachmentUrls(),
        event.getDeadlineTime() != null ? event.getDeadlineTime().format(ISO) : null,
        event.getReportTime() != null ? event.getReportTime().format(ISO) : null,
        event.getCloseTime() != null ? event.getCloseTime().format(ISO) : null,
        event.getCloseRemark());
  }

  private void insertAuditLog(User currentUser, ManualEvent event, String action, String resultStatus, String comment) {
    ManualEventAuditLog log = new ManualEventAuditLog();
    log.setTenantId(currentUser.getTenantId());
    log.setEventId(event.getId());
    log.setNodeCode(event.getCurrentAuditNode());
    log.setAction(action);
    log.setResultStatus(resultStatus);
    log.setAuditorId(currentUser.getId());
    log.setAuditorName(resolveUserDisplayName(currentUser));
    log.setComment(trimToNull(comment));
    log.setAuditTime(LocalDateTime.now());
    manualEventAuditLogMapper.insert(log);
  }

  private String nextManualEventNo() {
    return "ME-"
        + LocalDateTime.now().format(EVENT_NO)
        + "-"
        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.trim(), ISO);
    } catch (Exception ignored) {
      try {
        return OffsetDateTime.parse(value.trim()).toLocalDateTime();
      } catch (Exception ex) {
        throw new BizException(400, "时间格式错误");
      }
    }
  }

  private String normalizeText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : defaultValue;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String resolveUserDisplayName(User user) {
    return StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getUsername();
  }

  private boolean isAdminUser(User user) {
    return user != null
        && StringUtils.hasText(user.getUserType())
        && Arrays.asList("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN")
            .contains(user.getUserType().trim().toUpperCase());
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
