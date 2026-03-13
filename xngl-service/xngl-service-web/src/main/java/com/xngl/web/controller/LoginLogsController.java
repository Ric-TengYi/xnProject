package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.system.LoginLog;
import com.xngl.manager.log.LoginLogService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.user.LoginLogListItemDto;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/login-logs")
public class LoginLogsController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final LoginLogService loginLogService;

  public LoginLogsController(LoginLogService loginLogService) {
    this.loginLogService = loginLogService;
  }

  @GetMapping
  public ApiResult<PageResult<LoginLogListItemDto>> list(
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize) {
    LocalDateTime start = parseTime(startTime);
    LocalDateTime end = parseTime(endTime);
    IPage<LoginLog> page =
        loginLogService.page(tenantId, userId, start, end, pageNo, pageSize);
    List<LoginLogListItemDto> records =
        page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
    return ApiResult.ok(
        new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  private LoginLogListItemDto toListItem(LoginLog log) {
    boolean success = log.getSuccessFlag() != null && log.getSuccessFlag() == 1;
    return new LoginLogListItemDto(
        String.valueOf(log.getId()),
        log.getUserId() != null ? String.valueOf(log.getUserId()) : null,
        log.getUsername(),
        log.getLoginTime() != null ? log.getLoginTime().format(ISO) : null,
        log.getIp(),
        success,
        log.getFailReason());
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
