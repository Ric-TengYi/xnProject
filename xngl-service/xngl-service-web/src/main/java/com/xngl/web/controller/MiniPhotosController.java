package com.xngl.web.controller;

import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序出土拍照 stub。
 *
 * <p>真实 `/api/mini/photos` 已由 {@link MiniWorkOrdersController} 提供，stub 保留在独立路径避免重复映射。
 */
@RestController
@RequestMapping("/api/mini/stub/photos")
public class MiniPhotosController {

  @GetMapping
  public ApiResult<PhotoListDto> list(
      HttpServletRequest request,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false, defaultValue = "1") Integer pageNo,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
    requireUserId(request);
    List<PhotoItemDto> records =
        List.of(
            new PhotoItemDto(1L, 1L, "滨海新区B标段", "2024-03-15 10:30", "出土现场", "PENDING"),
            new PhotoItemDto(2L, 2L, "老旧小区改造", "2024-03-14 14:00", null, "APPROVED"));
    return ApiResult.ok(new PhotoListDto(records, records.size()));
  }

  @PostMapping
  public ApiResult<SubmitResultDto> submit(
      HttpServletRequest request, @RequestBody MiniPhotoSubmitDto body) {
    requireUserId(request);
    return ApiResult.ok(new SubmitResultDto(1L));
  }

  private void requireUserId(HttpServletRequest request) {
    if (request.getAttribute("userId") == null) {
      throw new com.xngl.web.exception.BizException(401, "未登录或 token 无效");
    }
  }

  @lombok.Data
  public static class PhotoListDto {
    private List<PhotoItemDto> records;
    private int total;

    public PhotoListDto(List<PhotoItemDto> records, int total) {
      this.records = records;
      this.total = total;
    }
  }

  @lombok.Data
  public static class PhotoItemDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private String shootTime;
    private String remark;
    private String auditStatus;

    public PhotoItemDto(Long id, Long projectId, String projectName, String shootTime, String remark, String auditStatus) {
      this.id = id;
      this.projectId = projectId;
      this.projectName = projectName;
      this.shootTime = shootTime;
      this.remark = remark;
      this.auditStatus = auditStatus;
    }
  }

  @lombok.Data
  public static class MiniPhotoSubmitDto {
    private Long projectId;
    private Long siteId;
    private String photoType;
    private String remark;
    private String fileId;
  }

  @lombok.Data
  public static class SubmitResultDto {
    private Long id;

    public SubmitResultDto(Long id) {
      this.id = id;
    }
  }
}
