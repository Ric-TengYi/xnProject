package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.project.ProjectPaymentChangeResultVo;
import com.xngl.manager.project.ProjectPaymentCreateCommand;
import com.xngl.manager.project.ProjectPaymentRecordVo;
import com.xngl.manager.project.ProjectPaymentService;
import com.xngl.manager.project.ProjectPaymentSummaryVo;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.project.ProjectPaymentCancelDto;
import com.xngl.web.dto.project.ProjectPaymentChangeResultDto;
import com.xngl.web.dto.project.ProjectPaymentCreateDto;
import com.xngl.web.dto.project.ProjectPaymentRecordDto;
import com.xngl.web.dto.project.ProjectPaymentSummaryDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectPaymentsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ProjectPaymentService projectPaymentService;
  private final UserService userService;

  public ProjectPaymentsController(
      ProjectPaymentService projectPaymentService, UserService userService) {
    this.projectPaymentService = projectPaymentService;
    this.userService = userService;
  }

  @GetMapping("/payments")
  public ApiResult<PageResult<ProjectPaymentRecordDto>> list(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String paymentType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        pageRecords(
            currentUser.getTenantId(),
            projectId,
            keyword,
            paymentType,
            status,
            startDate,
            endDate,
            pageNo,
            pageSize));
  }

  @GetMapping("/{projectId}/payments")
  public ApiResult<PageResult<ProjectPaymentRecordDto>> listByProject(
      @PathVariable Long projectId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String paymentType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        pageRecords(
            currentUser.getTenantId(),
            projectId,
            keyword,
            paymentType,
            status,
            startDate,
            endDate,
            pageNo,
            pageSize));
  }

  @GetMapping("/{projectId}/payments/summary")
  public ApiResult<ProjectPaymentSummaryDto> summary(
      @PathVariable Long projectId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ProjectPaymentSummaryVo summary =
        projectPaymentService.getSummary(currentUser.getTenantId(), projectId);
    return ApiResult.ok(toSummaryDto(summary));
  }

  @PostMapping("/{projectId}/payments")
  public ApiResult<ProjectPaymentChangeResultDto> create(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectPaymentCreateDto dto,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ProjectPaymentChangeResultVo result =
        projectPaymentService.createPayment(
            currentUser.getTenantId(),
            currentUser.getId(),
            projectId,
            toCreateCommand(dto));
    return ApiResult.ok(toChangeResultDto(result));
  }

  @PostMapping("/payments/{paymentId}/cancel")
  public ApiResult<ProjectPaymentChangeResultDto> cancel(
      @PathVariable Long paymentId,
      @RequestBody(required = false) ProjectPaymentCancelDto dto,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ProjectPaymentChangeResultVo result =
        projectPaymentService.cancelPayment(
            currentUser.getTenantId(),
            currentUser.getId(),
            paymentId,
            dto != null ? dto.getReason() : null);
    return ApiResult.ok(toChangeResultDto(result));
  }

  private ProjectPaymentCreateCommand toCreateCommand(ProjectPaymentCreateDto dto) {
    ProjectPaymentCreateCommand command = new ProjectPaymentCreateCommand();
    command.setPaymentNo(dto.getPaymentNo());
    command.setPaymentType(dto.getPaymentType());
    command.setAmount(dto.getAmount());
    command.setPaymentDate(dto.getPaymentDate());
    command.setVoucherNo(dto.getVoucherNo());
    command.setSourceType(dto.getSourceType());
    command.setSourceId(parseNullableLong(dto.getSourceId()));
    command.setRemark(dto.getRemark());
    return command;
  }

  private ProjectPaymentRecordDto toRecordDto(ProjectPaymentRecordVo record) {
    return new ProjectPaymentRecordDto(
        stringValue(record.getId()),
        stringValue(record.getProjectId()),
        record.getProjectName(),
        record.getProjectCode(),
        record.getPaymentNo(),
        record.getPaymentType(),
        record.getAmount(),
        formatDate(record.getPaymentDate()),
        record.getVoucherNo(),
        record.getStatus(),
        record.getSourceType(),
        stringValue(record.getSourceId()),
        record.getRemark(),
        stringValue(record.getOperatorId()),
        stringValue(record.getCancelOperatorId()),
        formatDateTime(record.getCancelTime()),
        record.getCancelReason(),
        formatDateTime(record.getCreateTime()),
        formatDateTime(record.getUpdateTime()));
  }

  private ProjectPaymentChangeResultDto toChangeResultDto(ProjectPaymentChangeResultVo result) {
    return new ProjectPaymentChangeResultDto(
        stringValue(result.getPaymentId()), result.getPaymentNo(), toSummaryDto(result.getSummary()));
  }

  private ProjectPaymentSummaryDto toSummaryDto(ProjectPaymentSummaryVo summary) {
    return new ProjectPaymentSummaryDto(
        stringValue(summary.getProjectId()),
        summary.getProjectName(),
        summary.getProjectCode(),
        summary.getTotalAmount(),
        summary.getPaidAmount(),
        summary.getDebtAmount(),
        formatDate(summary.getLastPaymentDate()),
        summary.getStatus());
  }

  private User requireCurrentUser(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (userId == null || userId.isBlank()) {
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

  private Long parseNullableLong(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      throw new BizException(400, "sourceId 格式不正确");
    }
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(java.time.LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private PageResult<ProjectPaymentRecordDto> pageRecords(
      Long tenantId,
      Long projectId,
      String keyword,
      String paymentType,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      int pageNo,
      int pageSize) {
    IPage<ProjectPaymentRecordVo> page =
        projectPaymentService.pageRecords(
            tenantId, projectId, keyword, paymentType, status, startDate, endDate, pageNo, pageSize);
    List<ProjectPaymentRecordDto> records = page.getRecords().stream().map(this::toRecordDto).toList();
    return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
  }
}
