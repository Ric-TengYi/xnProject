package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序司机端：首页聚合、任务、预警。当前为 stub，后续对接 mini_driver_task 与预警数据。
 */
@RestController
@RequestMapping("/api/mini/drivers")
public class MiniDriversController {

  @GetMapping("/home")
  public ApiResult<DriverHomeDto> home(HttpServletRequest request) {
    requireUserId(request);
    List<Object> tasks =
        List.of(
            new TaskItem(1L, "滨海新区B标段", "东区消纳场", "IN_PROGRESS"));
    List<Object> alerts =
        List.of(
            new AlertItem(1L, "OVERSPEED", "超速预警", "2024-03-15 09:00"));
    List<Object> permits =
        List.of(
            new PermitItem(1L, "PZ-2024-001", "2024-12-31", "VALID"));
    return ApiResult.ok(new DriverHomeDto(tasks, alerts, permits));
  }

  @GetMapping("/tasks")
  public ApiResult<TaskListDto> tasks(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<Object> records = List.of(new TaskItem(1L, "滨海新区B标段", "东区消纳场", "IN_PROGRESS"));
    return ApiResult.ok(new TaskListDto(records, records.size()));
  }

  @GetMapping("/alerts")
  public ApiResult<AlertListDto> alerts(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<Object> records = List.of(new AlertItem(1L, "OVERSPEED", "超速预警", "2024-03-15 09:00"));
    return ApiResult.ok(new AlertListDto(records, records.size()));
  }

  private void requireUserId(HttpServletRequest request) {
    if (request.getAttribute("userId") == null) {
      throw new com.xngl.web.exception.BizException(401, "未登录或 token 无效");
    }
  }

  @lombok.Data
  public static class DriverHomeDto {
    private List<Object> tasks;
    private List<Object> alerts;
    private List<Object> permits;

    public DriverHomeDto(List<Object> tasks, List<Object> alerts, List<Object> permits) {
      this.tasks = tasks;
      this.alerts = alerts;
      this.permits = permits;
    }
  }

  @lombok.Data
  public static class TaskItem {
    private Long id;
    private String projectName;
    private String siteName;
    private String status;

    public TaskItem(Long id, String projectName, String siteName, String status) {
      this.id = id;
      this.projectName = projectName;
      this.siteName = siteName;
      this.status = status;
    }
  }

  @lombok.Data
  public static class AlertItem {
    private Long id;
    private String type;
    private String message;
    private String time;

    public AlertItem(Long id, String type, String message, String time) {
      this.id = id;
      this.type = type;
      this.message = message;
      this.time = time;
    }
  }

  @lombok.Data
  public static class PermitItem {
    private Long id;
    private String permitNo;
    private String validUntil;
    private String status;

    public PermitItem(Long id, String permitNo, String validUntil, String status) {
      this.id = id;
      this.permitNo = permitNo;
      this.validUntil = validUntil;
      this.status = status;
    }
  }

  @lombok.Data
  public static class TaskListDto {
    private List<Object> records;
    private int total;

    public TaskListDto(List<Object> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class AlertListDto {
    private List<Object> records;
    private int total;

    public AlertListDto(List<Object> records, int total) {
      this.records = records;
      this.total = total;
    }
  }
}
