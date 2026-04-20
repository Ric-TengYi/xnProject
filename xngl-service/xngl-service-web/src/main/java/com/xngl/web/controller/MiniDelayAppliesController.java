package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序延期申报 stub。
 *
 * <p>真实 `/api/mini/delay-applies` 端点已经由 {@link MiniWorkOrdersController} 提供，stub 保留在独立路径，
 * 避免启动时与真实控制器产生重复映射。
 */
@RestController
@RequestMapping("/api/mini/stub/delay-applies")
public class MiniDelayAppliesController {

  @GetMapping
  public ApiResult<DelayApplyListDto> list(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<DelayApplyItemDto> records =
        List.of(
            new DelayApplyItemDto(1L, "PROJECT", 1L, "滨海新区B标段", "2024-06-30", "SUBMITTED", "2024-03-15 10:00"));
    return ApiResult.ok(new DelayApplyListDto(records, records.size()));
  }

  @PostMapping
  public ApiResult<SubmitResultDto> submit(
      HttpServletRequest request, @RequestBody DelayApplySubmitDto body) {
    requireUserId(request);
    return ApiResult.ok(new SubmitResultDto(1L));
  }

  private void requireUserId(HttpServletRequest request) {
    if (request.getAttribute("userId") == null) {
      throw new com.xngl.web.exception.BizException(401, "未登录或 token 无效");
    }
  }

  @lombok.Data
  public static class DelayApplyListDto {
    private List<DelayApplyItemDto> records;
    private int total;

    public DelayApplyListDto(List<DelayApplyItemDto> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class DelayApplyItemDto {
    private Long id;
    private String bizType;
    private Long projectId;
    private String projectName;
    private String requestedEndTime;
    private String status;
    private String createTime;

    public DelayApplyItemDto(Long id, String bizType, Long projectId, String projectName, String requestedEndTime, String status, String createTime) {
      this.id = id;
      this.bizType = bizType;
      this.projectId = projectId;
      this.projectName = projectName;
      this.requestedEndTime = requestedEndTime;
      this.status = status;
      this.createTime = createTime;
    }
  }

  @lombok.Data
  public static class DelayApplySubmitDto {
    private String bizType;
    private String bizId;
    private Long projectId;
    private String requestedEndTime;
    private String reason;
    private java.util.List<String> attachmentIds;
  }

  @lombok.Data
  public static class SubmitResultDto {
    private Long id;

    public SubmitResultDto(Long id) {
      this.id = id;
    }
  }
}
