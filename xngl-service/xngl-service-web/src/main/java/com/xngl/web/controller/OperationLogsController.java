package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.OperationLog;
import com.xngl.manager.log.OperationLogService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.OperationLogListItemDto;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final OperationLogService operationLogService;

  public OperationLogsController(OperationLogService operationLogService) {
    this.operationLogService = operationLogService;
  }

  @GetMapping
  public ApiResult<PageResult<OperationLogListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    LocalDateTime start = parseTime(startTime);
    LocalDateTime end = parseTime(endTime);
    IPage<OperationLog> page =
        operationLogService.page(tenantId, userId, module, start, end, pageNo, pageSize);
    List<OperationLogListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  private OperationLogListItemDto toListItem(OperationLog log) {
    return new OperationLogListItemDto(
        String.valueOf(log.getId()),
        log.getUserId() != null ? String.valueOf(log.getUserId()) : null,
        log.getUsername(),
        log.getModule(),
        log.getOperation(),
        log.getMethod(),
        log.getRequestUri(),
        log.getIp(),
        log.getDurationMs() != null ? log.getDurationMs().longValue() : 0L,
        log.getCreateTime() != null ? log.getCreateTime().format(ISO) : null);
  }

  private static LocalDateTime parseTime(String s) {
    if (s == null || s.isEmpty()) return null;
    try {
      return LocalDateTime.parse(s, ISO);
    } catch (Exception e) {
      return null;
    }
  }
}
