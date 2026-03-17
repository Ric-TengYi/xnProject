package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序打卡异常申报。当前为 stub，后续对接 mini_checkin_exception_apply。
 */
@RestController
@RequestMapping("/api/mini/checkin-exceptions")
public class MiniCheckinExceptionsController {

  @GetMapping
  public ApiResult<CheckinExceptionListDto> list(
      HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<CheckinExceptionItemDto> records =
        List.of(
            new CheckinExceptionItemDto(1L, "CK-001", "TIME_ERROR", "设备时间异常", "SUBMITTED", "2024-03-15 10:00"));
    return ApiResult.ok(new CheckinExceptionListDto(records, records.size()));
  }

  @PostMapping
  public ApiResult<SubmitResultDto> submit(
      HttpServletRequest request, @RequestBody CheckinExceptionSubmitDto body) {
    requireUserId(request);
    return ApiResult.ok(new SubmitResultDto(1L));
  }

  private void requireUserId(HttpServletRequest request) {
    if (request.getAttribute("userId") == null) {
      throw new com.xngl.web.exception.BizException(401, "未登录或 token 无效");
    }
  }

  @lombok.Data
  public static class CheckinExceptionListDto {
    private List<CheckinExceptionItemDto> records;
    private int total;

    public CheckinExceptionListDto(List<CheckinExceptionItemDto> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class CheckinExceptionItemDto {
    private Long id;
    private String checkinRecordId;
    private String exceptionType;
    private String reason;
    private String status;
    private String createTime;

    public CheckinExceptionItemDto(Long id, String checkinRecordId, String exceptionType, String reason, String status, String createTime) {
      this.id = id;
      this.checkinRecordId = checkinRecordId;
      this.exceptionType = exceptionType;
      this.reason = reason;
      this.status = status;
      this.createTime = createTime;
    }
  }

  @lombok.Data
  public static class CheckinExceptionSubmitDto {
    private String checkinRecordId;
    private String exceptionType;
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
