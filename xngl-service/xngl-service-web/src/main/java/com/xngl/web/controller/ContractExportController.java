package com.xngl.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.contract.ContractExportRequestDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractExportController {

  private final ExportTaskService exportTaskService;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  public ContractExportController(
      ExportTaskService exportTaskService,
      UserService userService,
      ObjectMapper objectMapper) {
    this.exportTaskService = exportTaskService;
    this.userService = userService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/export")
  public ApiResult<Map<String, String>> export(
      @Valid @RequestBody ContractExportRequestDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    String queryJson;
    try {
      queryJson = objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new BizException(400, "查询参数序列化失败");
    }
    String exportType = dto.getExportType() != null ? dto.getExportType() : "EXCEL";
    long taskId = exportTaskService.createExportTask(
        user.getTenantId(), user.getId(), "CONTRACT", exportType, queryJson);
    return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
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
