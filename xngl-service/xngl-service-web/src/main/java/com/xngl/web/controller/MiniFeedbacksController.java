package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序问题反馈 stub。
 *
 * <p>真实 `/api/mini/feedbacks` 已由 {@link MiniWorkOrdersController} 提供，stub 保留在独立路径避免重复映射。
 */
@RestController
@RequestMapping("/api/mini/stub/feedbacks")
public class MiniFeedbacksController {

  @GetMapping
  public ApiResult<FeedbackListDto> list(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<FeedbackItemDto> records =
        List.of(
            new FeedbackItemDto(1L, "OTHER", "现场设备显示异常", "OPEN", "2024-03-15 10:00"));
    return ApiResult.ok(new FeedbackListDto(records, records.size()));
  }

  @PostMapping
  public ApiResult<SubmitResultDto> submit(
      HttpServletRequest request, @RequestBody FeedbackSubmitDto body) {
    requireUserId(request);
    return ApiResult.ok(new SubmitResultDto(1L));
  }

  @PostMapping("/{id}/close")
  public ApiResult<Void> close(HttpServletRequest request, @PathVariable Long id) {
    requireUserId(request);
    return ApiResult.ok();
  }

  private void requireUserId(HttpServletRequest request) {
    if (request.getAttribute("userId") == null) {
      throw new com.xngl.web.exception.BizException(401, "未登录或 token 无效");
    }
  }

  @lombok.Data
  public static class FeedbackListDto {
    private List<FeedbackItemDto> records;
    private int total;

    public FeedbackListDto(List<FeedbackItemDto> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class FeedbackItemDto {
    private Long id;
    private String feedbackType;
    private String content;
    private String status;
    private String createTime;

    public FeedbackItemDto(Long id, String feedbackType, String content, String status, String createTime) {
      this.id = id;
      this.feedbackType = feedbackType;
      this.content = content;
      this.status = status;
      this.createTime = createTime;
    }
  }

  @lombok.Data
  public static class FeedbackSubmitDto {
    private String feedbackType;
    private String content;
    private Long projectId;
    private Long siteId;
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
