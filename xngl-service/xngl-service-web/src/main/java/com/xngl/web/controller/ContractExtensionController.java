package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractExtensionApply;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractApplyService;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.ContractExtensionCreateDto;
import com.xngl.web.dto.contract.ContractExtensionItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractExtensionController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractApplyService contractApplyService;
  private final ContractService contractService;
  private final UserService userService;
  private final UserContext userContext;

  public ContractExtensionController(
      ContractApplyService contractApplyService,
      ContractService contractService,
      UserService userService, UserContext userContext) {
    this.contractApplyService = contractApplyService;
    this.contractService = contractService;
    this.userService = userService;
    this.userContext = userContext;
  }

  @PostMapping("/{contractId}/extensions")
  public ApiResult<String> create(
      @PathVariable Long contractId,
      @Valid @RequestBody ContractExtensionCreateDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    long applyId = contractApplyService.createExtensionApply(
        user.getTenantId(), user.getId(), contractId,
        dto.getRequestedExpireDate(), dto.getRequestedVolumeDelta(), dto.getReason());
    return ApiResult.ok(String.valueOf(applyId));
  }

  @GetMapping("/extensions")
  public ApiResult<PageResult<ContractExtensionItemDto>> list(
      @RequestParam(required = false) Long contractId,
      @RequestParam(required = false) String approvalStatus,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<ContractExtensionApply> page = contractApplyService.pageExtensionApplies(
        user.getTenantId(), contractId, approvalStatus, pageNo, pageSize);

    Map<Long, Contract> contractMap = loadContractMap(page.getRecords().stream()
        .map(ContractExtensionApply::getContractId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new)), user.getTenantId());

    List<ContractExtensionItemDto> records = page.getRecords().stream()
        .map(apply -> toItemDto(apply, contractMap.get(apply.getContractId())))
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/extensions/{applyId}")
  public ApiResult<ContractExtensionItemDto> get(
      @PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractExtensionApply apply = contractApplyService.getExtensionApply(applyId, user.getTenantId());
    Contract contract = safeGetContract(apply.getContractId(), user.getTenantId());
    return ApiResult.ok(toItemDto(apply, contract));
  }

  @PostMapping("/extensions/{applyId}/submit")
  public ApiResult<Void> submit(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.submitExtensionApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/extensions/{applyId}/approve")
  public ApiResult<Void> approve(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.approveExtensionApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/extensions/{applyId}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long applyId,
      @RequestBody(required = false) ApprovalActionDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.rejectExtensionApply(
        applyId, user.getTenantId(), dto != null ? dto.getReason() : null);
    return ApiResult.ok();
  }

  private ContractExtensionItemDto toItemDto(ContractExtensionApply apply, Contract contract) {
    return new ContractExtensionItemDto(
        str(apply.getId()),
        apply.getApplyNo(),
        str(apply.getContractId()),
        contract != null ? contract.getContractNo() : null,
        formatDate(apply.getOriginalExpireDate()),
        formatDate(apply.getRequestedExpireDate()),
        apply.getRequestedVolumeDelta(),
        apply.getReason(),
        apply.getApprovalStatus(),
        str(apply.getApplicantId()),
        formatDt(apply.getCreateTime()));
  }

  private Map<Long, Contract> loadContractMap(java.util.Collection<Long> ids, Long tenantId) {
    return contractService.listContractsByIds(ids, tenantId).stream()
        .collect(Collectors.toMap(Contract::getId, c -> c));
  }

  private Contract safeGetContract(Long contractId, Long tenantId) {
    try {
      return contractService.getContract(contractId, tenantId);
    } catch (Exception e) {
      return null;
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private String str(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDt(LocalDateTime value) {
    return value != null ? value.format(ISO_DT) : null;
  }
}
