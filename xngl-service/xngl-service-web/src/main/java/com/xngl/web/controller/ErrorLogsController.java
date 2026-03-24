package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.ErrorLog;
import com.xngl.manager.log.ErrorLogService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.ErrorLogListItemDto;
import com.xngl.web.support.CsvExportSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/error-logs")
public class ErrorLogsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ErrorLogService errorLogService;

  public ErrorLogsController(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }

  @GetMapping
  public ApiResult<PageResult<ErrorLogListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    LocalDateTime start = parseTime(startTime);
    LocalDateTime end = parseTime(endTime);
    IPage<ErrorLog> page =
        errorLogService.page(tenantId, userId, keyword, level, start, end, pageNo, pageSize);
    List<ErrorLogListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime) {
    List<ErrorLog> rows =
        errorLogService
            .page(tenantId, userId, keyword, level, parseTime(startTime), parseTime(endTime), 1, 2000)
            .getRecords();
    return CsvExportSupport.csvResponse(
        "error_logs",
        List.of("记录时间", "级别", "异常类型", "异常信息", "请求URI", "请求方式", "操作人", "IP"),
        rows.stream()
            .map(
                log ->
                    List.of(
                        CsvExportSupport.value(log.getCreateTime()),
                        CsvExportSupport.value(log.getLevel()),
                        CsvExportSupport.value(log.getExceptionType()),
                        CsvExportSupport.value(log.getErrorMessage()),
                        CsvExportSupport.value(log.getRequestUri()),
                        CsvExportSupport.value(log.getHttpMethod()),
                        CsvExportSupport.value(log.getUsername()),
                        CsvExportSupport.value(log.getIp())))
            .toList());
  }

  private ErrorLogListItemDto toListItem(ErrorLog log) {
    return new ErrorLogListItemDto(
        String.valueOf(log.getId()),
        log.getUserId() != null ? String.valueOf(log.getUserId()) : null,
        log.getUsername(),
        log.getLevel(),
        log.getExceptionType(),
        log.getErrorMessage(),
        log.getRequestUri(),
        log.getHttpMethod(),
        log.getIp(),
        log.getStackTrace(),
        log.getCreateTime() != null ? log.getCreateTime().format(ISO) : null);
  }

  private static LocalDateTime parseTime(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return LocalDateTime.parse(value, ISO);
    } catch (Exception e) {
      return null;
    }
  }
}
