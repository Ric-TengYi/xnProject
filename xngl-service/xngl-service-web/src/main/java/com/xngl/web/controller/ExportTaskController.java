package com.xngl.web.controller;

import com.xngl.infrastructure.persistence.entity.contract.ReportExportTask;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.contract.ExportTaskDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
  private final UserContext userContext;

  public ExportTaskController(ExportTaskService exportTaskService, UserService userService, UserContext userContext) {
    this.exportTaskService = exportTaskService;
    this.userService = userService;
    this.userContext = userContext;
  }

  @GetMapping("/{id}")
  public ApiResult<ExportTaskDto> getTask(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ReportExportTask task = exportTaskService.getExportTask(id, user.getTenantId());
    return ApiResult.ok(toDto(task));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ReportExportTask task = exportTaskService.getExportTask(id, user.getTenantId());
    if (!"COMPLETED".equalsIgnoreCase(task.getStatus())) {
      throw new BizException(409, "导出任务尚未完成");
    }
    if (!StringUtils.hasText(task.getFileUrl())) {
      throw new BizException(404, "导出文件不存在");
    }
    Path path = Path.of(task.getFileUrl());
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new BizException(404, "导出文件不存在或已过期");
    }
    FileSystemResource resource = new FileSystemResource(path);
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(StringUtils.hasText(task.getFileName()) ? task.getFileName() : path.getFileName().toString())
        .build();
    try {
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
          .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
          .contentLength(Files.size(path))
          .body(resource);
    } catch (IOException ex) {
      throw new BizException(500, "读取导出文件失败");
    }
  }

  private ExportTaskDto toDto(ReportExportTask task) {
    ExportTaskDto dto = new ExportTaskDto();
    dto.setId(String.valueOf(task.getId()));
    dto.setBizType(task.getBizType());
    dto.setExportType(task.getExportType());
    dto.setFileName(task.getFileName());
    dto.setFileUrl("/api/export-tasks/" + task.getId() + "/download");
    dto.setStatus(task.getStatus());
    dto.setFailReason(task.getFailReason());
    dto.setCreatorId(task.getCreatorId() != null ? String.valueOf(task.getCreatorId()) : null);
    dto.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().format(ISO_DATE_TIME) : null);
    dto.setExpireTime(task.getExpireTime() != null ? task.getExpireTime().format(ISO_DATE_TIME) : null);
    return dto;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
