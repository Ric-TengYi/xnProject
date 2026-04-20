package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportBatch;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportError;
import com.xngl.manager.contract.ContractImportCommitResult;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractImportService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ContractImportPreviewRequestDto;
import com.xngl.web.dto.contract.ImportBatchDetailDto;
import com.xngl.web.dto.contract.ImportBatchItemDto;
import com.xngl.web.dto.contract.ImportCommitResultDto;
import com.xngl.web.dto.contract.ImportErrorDto;
import com.xngl.web.dto.contract.ImportPreviewDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractImportController {

  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractImportService importService;
  private final UserContext userContext;

  public ContractImportController(ContractImportService importService, UserContext userContext) {
    this.importService = importService;
    this.userContext = userContext;
  }

  @PostMapping("/import-preview")
  public ApiResult<ImportPreviewDto> preview(
      @Valid @RequestBody ContractImportPreviewRequestDto requestBody,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractImportBatch batch = importService.previewImport(
        user.getTenantId(), user.getId(), requestBody.getFileName(), requestBody.getRows());
    List<ContractImportError> errors = importService.listBatchErrors(batch.getId(), user.getTenantId());

    ImportPreviewDto dto = new ImportPreviewDto();
    dto.setBatchId(String.valueOf(batch.getId()));
    dto.setTotalCount(batch.getTotalCount());
    dto.setValidCount(batch.getSuccessCount());
    dto.setErrorCount(batch.getFailCount());
    dto.setErrors(errors.stream().map(this::toErrorDto).toList());
    return ApiResult.ok(dto);
  }

  @PostMapping("/import-commit")
  public ApiResult<ImportCommitResultDto> commit(@RequestBody Map<String, Long> body, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    Long batchId = body.get("batchId");
    if (batchId == null) {
      throw new BizException(400, "batchId 不能为空");
    }
    ContractImportCommitResult result = importService.commitImport(batchId, user.getTenantId());
    ImportCommitResultDto dto = new ImportCommitResultDto();
    dto.setBatchId(result.getBatchId() != null ? String.valueOf(result.getBatchId()) : null);
    dto.setSuccessCount(result.getSuccessCount());
    dto.setFailCount(result.getFailCount());
    dto.setStatus(result.getStatus());
    return ApiResult.ok(dto);
  }

  @GetMapping("/import-batches")
  public ApiResult<PageResult<ImportBatchItemDto>> pageBatches(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<ContractImportBatch> page = importService.pageBatches(
        user.getTenantId(), status, pageNo, pageSize);
    List<ImportBatchItemDto> records = page.getRecords().stream()
        .map(this::toBatchItemDto)
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/import-batches/{id}")
  public ApiResult<ImportBatchDetailDto> getBatchDetail(
      @PathVariable Long id, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractImportBatch batch = importService.getBatchDetail(id, user.getTenantId());
    List<ContractImportError> errors = importService.listBatchErrors(id, user.getTenantId());

    ImportBatchDetailDto dto = new ImportBatchDetailDto();
    copyBatchFields(batch, dto);
    dto.setErrors(errors.stream().map(this::toErrorDto).toList());
    return ApiResult.ok(dto);
  }

  private ImportBatchItemDto toBatchItemDto(ContractImportBatch batch) {
    ImportBatchItemDto dto = new ImportBatchItemDto();
    copyBatchFields(batch, dto);
    return dto;
  }

  private void copyBatchFields(ContractImportBatch batch, ImportBatchItemDto dto) {
    dto.setId(String.valueOf(batch.getId()));
    dto.setBatchNo(batch.getBatchNo());
    dto.setFileName(batch.getFileName());
    dto.setTotalCount(batch.getTotalCount());
    dto.setSuccessCount(batch.getSuccessCount());
    dto.setFailCount(batch.getFailCount());
    dto.setStatus(batch.getStatus());
    dto.setOperatorId(batch.getOperatorId() != null ? String.valueOf(batch.getOperatorId()) : null);
    dto.setCreateTime(batch.getCreateTime() != null ? batch.getCreateTime().format(ISO_DATE_TIME) : null);
  }

  private ImportErrorDto toErrorDto(ContractImportError error) {
    ImportErrorDto dto = new ImportErrorDto();
    dto.setId(String.valueOf(error.getId()));
    dto.setRowNo(error.getRowNo());
    dto.setContractNo(error.getContractNo());
    dto.setErrorCode(error.getErrorCode());
    dto.setErrorMessage(error.getErrorMessage());
    return dto;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
