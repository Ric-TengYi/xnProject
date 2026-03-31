package com.xngl.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractAccessScope;
import com.xngl.manager.contract.ContractExportFileService;
import com.xngl.manager.contract.ContractQueryParams;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.contract.ContractExportRequestDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.ContractAccessScopeResolver;
import com.xngl.web.support.UserContext;
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
  private final ContractExportFileService contractExportFileService;
  private final UserService userService;
  private final UserContext userContext;
  private final ContractAccessScopeResolver contractAccessScopeResolver;
  private final ObjectMapper objectMapper;

  public ContractExportController(
      ExportTaskService exportTaskService,
      ContractExportFileService contractExportFileService,
      UserService userService,
      UserContext userContext,
      ContractAccessScopeResolver contractAccessScopeResolver,
      ObjectMapper objectMapper) {
    this.exportTaskService = exportTaskService;
    this.contractExportFileService = contractExportFileService;
    this.userService = userService;
    this.userContext = userContext;
    this.contractAccessScopeResolver = contractAccessScopeResolver;
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
    String exportType = dto.getExportType() != null ? dto.getExportType() : "CSV";
    long taskId = exportTaskService.createExportTask(
        user.getTenantId(), user.getId(), "CONTRACT", exportType, queryJson);
    contractExportFileService.generateContractCsv(
        taskId, user.getTenantId(), toQueryParams(dto), resolveScope(user));
    return ApiResult.ok(Map.of("taskId", String.valueOf(taskId)));
  }

  private ContractQueryParams toQueryParams(ContractExportRequestDto dto) {
    ContractQueryParams params = new ContractQueryParams();
    params.setContractType(dto.getContractType());
    params.setContractStatus(dto.getContractStatus());
    params.setApprovalStatus(dto.getApprovalStatus());
    params.setKeyword(dto.getKeyword());
    params.setProjectId(dto.getProjectId());
    params.setSiteId(dto.getSiteId());
    params.setConstructionOrgId(dto.getConstructionOrgId());
    params.setTransportOrgId(dto.getTransportOrgId());
    params.setIsThreeParty(dto.getIsThreeParty());
    params.setSourceType(dto.getSourceType());
    params.setStartDate(dto.getStartDate());
    params.setEndDate(dto.getEndDate());
    params.setEffectiveStartDate(dto.getEffectiveStartDate());
    params.setEffectiveEndDate(dto.getEffectiveEndDate());
    params.setExpireStartDate(dto.getExpireStartDate());
    params.setExpireEndDate(dto.getExpireEndDate());
    return params;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private ContractAccessScope resolveScope(User currentUser) {
    return contractAccessScopeResolver.resolve(currentUser);
  }
}
