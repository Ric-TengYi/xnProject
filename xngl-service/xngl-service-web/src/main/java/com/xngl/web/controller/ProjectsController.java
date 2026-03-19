package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.manager.project.ProjectPaymentService;
import com.xngl.manager.project.ProjectPaymentSummaryVo;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.project.ProjectDetailDto;
import com.xngl.web.dto.project.ProjectListItemDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ProjectMapper projectMapper;
  private final OrgMapper orgMapper;
  private final ContractMapper contractMapper;
  private final SiteMapper siteMapper;
  private final ProjectPaymentService projectPaymentService;
  private final UserService userService;

  public ProjectsController(
      ProjectMapper projectMapper,
      OrgMapper orgMapper,
      ContractMapper contractMapper,
      SiteMapper siteMapper,
      ProjectPaymentService projectPaymentService,
      UserService userService) {
    this.projectMapper = projectMapper;
    this.orgMapper = orgMapper;
    this.contractMapper = contractMapper;
    this.siteMapper = siteMapper;
    this.projectPaymentService = projectPaymentService;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<ProjectListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);

    LambdaQueryWrapper<Project> query = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(Project::getName, effectiveKeyword)
                  .or()
                  .like(Project::getCode, effectiveKeyword)
                  .or()
                  .like(Project::getAddress, effectiveKeyword));
    }
    if (status != null) {
      query.eq(Project::getStatus, status);
    }
    query.orderByDesc(Project::getUpdateTime).orderByDesc(Project::getId);

    IPage<Project> page = projectMapper.selectPage(new Page<>(pageNo, pageSize), query);
    Map<Long, Org> orgMap = loadOrgMap(page.getRecords());
    List<ProjectListItemDto> records =
        page.getRecords().stream()
            .map(project -> toListItem(project, orgMap.get(project.getOrgId()), currentUser.getTenantId()))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<ProjectDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Project project = projectMapper.selectById(id);
    if (project == null) {
      return ApiResult.fail(404, "项目不存在");
    }
    Org org = project.getOrgId() != null ? orgMapper.selectById(project.getOrgId()) : null;
    return ApiResult.ok(toDetail(project, org, currentUser.getTenantId()));
  }

  @PostMapping
  public ApiResult<?> create(@RequestBody Object body) {
    return ApiResult.ok(null);
  }

  private Map<Long, Org> loadOrgMap(List<Project> projects) {
    LinkedHashSet<Long> orgIds =
        projects.stream()
            .map(Project::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private ProjectListItemDto toListItem(Project project, Org org, Long tenantId) {
    ProjectPaymentSummaryVo paymentSummary = projectPaymentService.getSummary(tenantId, project.getId());
    return new ProjectListItemDto(
        stringValue(project.getId()),
        project.getCode(),
        project.getName(),
        project.getAddress(),
        project.getStatus(),
        resolveProjectStatusLabel(project.getStatus()),
        stringValue(project.getOrgId()),
        org != null ? org.getOrgName() : null,
        countContracts(tenantId, project.getId()),
        countSites(project.getId()),
        paymentSummary.getTotalAmount(),
        paymentSummary.getPaidAmount(),
        paymentSummary.getDebtAmount(),
        formatDate(paymentSummary.getLastPaymentDate()),
        paymentSummary.getStatus(),
        formatDateTime(project.getCreateTime()),
        formatDateTime(project.getUpdateTime()));
  }

  private ProjectDetailDto toDetail(Project project, Org org, Long tenantId) {
    ProjectListItemDto item = toListItem(project, org, tenantId);
    ProjectDetailDto dto = new ProjectDetailDto();
    dto.setId(item.getId());
    dto.setCode(item.getCode());
    dto.setName(item.getName());
    dto.setAddress(item.getAddress());
    dto.setStatus(item.getStatus());
    dto.setStatusLabel(item.getStatusLabel());
    dto.setOrgId(item.getOrgId());
    dto.setOrgName(item.getOrgName());
    dto.setContractCount(item.getContractCount());
    dto.setSiteCount(item.getSiteCount());
    dto.setTotalAmount(item.getTotalAmount());
    dto.setPaidAmount(item.getPaidAmount());
    dto.setDebtAmount(item.getDebtAmount());
    dto.setLastPaymentDate(item.getLastPaymentDate());
    dto.setPaymentStatus(item.getPaymentStatus());
    dto.setCreateTime(item.getCreateTime());
    dto.setUpdateTime(item.getUpdateTime());
    dto.setPaymentStatusLabel(resolvePaymentStatusLabel(item.getPaymentStatus()));
    return dto;
  }

  private long countContracts(Long tenantId, Long projectId) {
    return contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .eq(Contract::getProjectId, projectId));
  }

  private long countSites(Long projectId) {
    return siteMapper.selectCount(
        new LambdaQueryWrapper<Site>().eq(Site::getProjectId, projectId));
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

  private String resolvePaymentStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未结算";
    }
    return switch (status.trim().toUpperCase()) {
      case "SETTLED" -> "已结清";
      case "ARREARS" -> "欠款中";
      default -> status;
    };
  }

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }
}
