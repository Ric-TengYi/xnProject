package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.SitePersonnelConfig;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SitePersonnelConfigMapper;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.contract.ExportTaskDto;
import com.xngl.web.dto.mini.MiniExportShareDto;
import com.xngl.web.dto.report.ProjectReportSummaryDto;
import com.xngl.web.dto.report.ReportTrendItemDto;
import com.xngl.web.dto.report.SiteReportSummaryDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@RequestMapping("/api/mini/reports")
public class MiniReportsController {

  private final OperationsReportController operationsReportController;
  private final SiteReportsController siteReportsController;
  private final ExportTaskController exportTaskController;
  private final UserService userService;
  private final UserContext userContext;
  private final SitePersonnelConfigMapper sitePersonnelConfigMapper;
  private final ProjectMapper projectMapper;
  private final ContractMapper contractMapper;

  public MiniReportsController(
      OperationsReportController operationsReportController,
      SiteReportsController siteReportsController,
      ExportTaskController exportTaskController,
      UserService userService,
      SitePersonnelConfigMapper sitePersonnelConfigMapper,
      ProjectMapper projectMapper,
      ContractMapper contractMapper,
      UserContext userContext) {
    this.operationsReportController = operationsReportController;
    this.siteReportsController = siteReportsController;
    this.exportTaskController = exportTaskController;
    this.userService = userService;
    this.sitePersonnelConfigMapper = sitePersonnelConfigMapper;
    this.projectMapper = projectMapper;
    this.contractMapper = contractMapper;
    this.userContext = userContext;
  }

  @GetMapping("/projects/summary")
  public ApiResult<ProjectReportSummaryDto> projectSummary(
      @RequestParam Long projectId,
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureProjectAccessible(currentUser, projectId);
    return operationsReportController.projectSummary(periodType, date, projectId, null, request);
  }

  @GetMapping("/projects/trend")
  public ApiResult<List<ReportTrendItemDto>> projectTrend(
      @RequestParam Long projectId,
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureProjectAccessible(currentUser, projectId);
    return operationsReportController.projectTrend(periodType, date, projectId, null, limit, request);
  }

  @PostMapping("/projects/export")
  public ApiResult<Map<String, String>> exportProjectReport(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Long projectId = requireBodyId(body, "projectId");
    ensureProjectAccessible(currentUser, projectId);
    return operationsReportController.exportProjectReport(body, request);
  }

  @GetMapping("/sites/summary")
  public ApiResult<SiteReportSummaryDto> siteSummary(
      @RequestParam Long siteId,
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureSiteAccessible(currentUser, siteId);
    return siteReportsController.summary(periodType, date, startDate, endDate, siteId, null, request);
  }

  @GetMapping("/sites/trend")
  public ApiResult<List<ReportTrendItemDto>> siteTrend(
      @RequestParam Long siteId,
      @RequestParam(required = false) String periodType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "6") int limit,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureSiteAccessible(currentUser, siteId);
    return siteReportsController.trend(periodType, date, startDate, endDate, siteId, null, limit, request);
  }

  @PostMapping("/sites/export")
  public ApiResult<Map<String, String>> exportSiteReport(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Long siteId = requireBodyId(body, "siteId");
    ensureSiteAccessible(currentUser, siteId);
    return siteReportsController.export(body, request);
  }

  @GetMapping("/exports/{taskId}")
  public ApiResult<ExportTaskDto> exportTask(
      @PathVariable Long taskId, HttpServletRequest request) {
    requireCurrentUser(request);
    return exportTaskController.getTask(taskId, request);
  }

  @GetMapping("/share-link/{taskId}")
  public ApiResult<MiniExportShareDto> shareLink(
      @PathVariable Long taskId, HttpServletRequest request) {
    requireCurrentUser(request);
    ExportTaskDto task = exportTaskController.getTask(taskId, request).getData();
    return ApiResult.ok(
        new MiniExportShareDto(
            task.getId(),
            task.getStatus(),
            task.getFileName(),
            task.getFileUrl(),
            task.getExpireTime()));
  }

  private Long requireBodyId(Map<String, Object> body, String field) {
    if (body == null || !body.containsKey(field)) {
      throw new BizException(400, field + " 不能为空");
    }
    Object value = body.get(field);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && StringUtils.hasText(text)) {
      try {
        return Long.parseLong(text.trim());
      } catch (NumberFormatException ex) {
        throw new BizException(400, field + " 格式错误");
      }
    }
    throw new BizException(400, field + " 格式错误");
  }

  private void ensureSiteAccessible(User user, Long siteId) {
    if (siteId == null) {
      throw new BizException(400, "场地不能为空");
    }
    if (isAdminUser(user)) {
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
      throw new BizException(403, "当前账号无权查看该场地报表");
    }
  }

  private void ensureProjectAccessible(User user, Long projectId) {
    if (projectId == null) {
      throw new BizException(400, "项目不能为空");
    }
    Project project = projectMapper.selectById(projectId);
    if (project == null || Objects.equals(project.getDeleted(), 1)) {
      throw new BizException(404, "项目不存在");
    }
    if (isAdminUser(user)) {
      return;
    }
    if (!resolveAccessibleProjectIds(user).contains(projectId)) {
      throw new BizException(403, "当前账号无权查看该项目报表");
    }
  }

  private LinkedHashSet<Long> resolveAccessibleProjectIds(User user) {
    LinkedHashSet<Long> orgIds =
        userService.listOrgIdsByUserId(user.getId()).stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (user.getMainOrgId() != null) {
      orgIds.add(user.getMainOrgId());
    }
    LinkedHashSet<Long> projectIds =
        projectMapper.selectList(
                new LambdaQueryWrapper<Project>().in(!orgIds.isEmpty(), Project::getOrgId, orgIds))
            .stream()
            .map(Project::getId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return projectIds;
    }
    contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, user.getTenantId())
                .and(
                    wrapper ->
                        wrapper
                            .in(Contract::getConstructionOrgId, orgIds)
                            .or()
                            .in(Contract::getTransportOrgId, orgIds)
                            .or()
                            .in(Contract::getSiteOperatorOrgId, orgIds)
                            .or()
                            .in(Contract::getPartyId, orgIds)))
        .stream()
        .map(Contract::getProjectId)
        .filter(Objects::nonNull)
        .forEach(projectIds::add);
    return projectIds;
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
