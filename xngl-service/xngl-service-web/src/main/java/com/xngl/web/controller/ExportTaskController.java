package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.contract.ReportExportTask;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.contract.ExportTaskDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export-tasks")
public class ExportTaskController {

  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ExportTaskService exportTaskService;
  private final UserService userService;

  public ExportTaskController(ExportTaskService exportTaskService, UserService userService) {
    this.exportTaskService = exportTaskService;
    this.userService = userService;
  }

  @GetMapping("/{id}")
  public ApiResult<ExportTaskDto> getTask(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ReportExportTask task = exportTaskService.getExportTask(id, user.getTenantId());
    return ApiResult.ok(toDto(task));
  }

  @GetMapping("/{id}/download")
  public ApiResult<ExportTaskDto> download(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ReportExportTask task = exportTaskService.getExportTask(id, user.getTenantId());
    return ApiResult.ok(toDto(task));
  }

  private ExportTaskDto toDto(ReportExportTask task) {
    ExportTaskDto dto = new ExportTaskDto();
    dto.setId(String.valueOf(task.getId()));
    dto.setBizType(task.getBizType());
    dto.setExportType(task.getExportType());
    dto.setFileName(task.getFileName());
    dto.setFileUrl(task.getFileUrl());
    dto.setStatus(task.getStatus());
    dto.setFailReason(task.getFailReason());
    dto.setCreatorId(task.getCreatorId() != null ? String.valueOf(task.getCreatorId()) : null);
    dto.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().format(ISO_DATE_TIME) : null);
    dto.setExpireTime(task.getExpireTime() != null ? task.getExpireTime().format(ISO_DATE_TIME) : null);
    return dto;
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
}
