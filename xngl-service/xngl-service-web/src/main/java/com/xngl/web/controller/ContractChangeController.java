package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractChangeApply;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractApplyService;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.ContractChangeCreateDto;
import com.xngl.web.dto.contract.ContractChangeItemDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class ContractChangeController {

  private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractApplyService contractApplyService;
  private final ContractService contractService;
  private final UserService userService;

  public ContractChangeController(
      ContractApplyService contractApplyService,
      ContractService contractService,
      UserService userService) {
    this.contractApplyService = contractApplyService;
    this.contractService = contractService;
    this.userService = userService;
  }

  @PostMapping("/{contractId}/change-applications")
  public ApiResult<String> create(
      @PathVariable Long contractId,
      @Valid @RequestBody ContractChangeCreateDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    long applyId = contractApplyService.createChangeApply(
        user.getTenantId(), user.getId(), contractId,
        dto.getChangeType(), dto.getAfterSnapshotJson(), dto.getReason());
    return ApiResult.ok(String.valueOf(applyId));
  }

  @GetMapping("/change-applications")
  public ApiResult<PageResult<ContractChangeItemDto>> list(
      @RequestParam(required = false) Long contractId,
      @RequestParam(required = false) String approvalStatus,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<ContractChangeApply> page = contractApplyService.pageChangeApplies(
        user.getTenantId(), contractId, approvalStatus, pageNo, pageSize);

    Map<Long, Contract> contractMap = loadContractMap(page.getRecords().stream()
        .map(ContractChangeApply::getContractId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new)), user.getTenantId());

    List<ContractChangeItemDto> records = page.getRecords().stream()
        .map(apply -> toItemDto(apply, contractMap.get(apply.getContractId())))
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/change-applications/{applyId}")
  public ApiResult<ContractChangeItemDto> get(
      @PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractChangeApply apply = contractApplyService.getChangeApply(applyId, user.getTenantId());
    Contract contract = safeGetContract(apply.getContractId(), user.getTenantId());
    return ApiResult.ok(toItemDto(apply, contract));
  }

  @PostMapping("/change-applications/{applyId}/submit")
  public ApiResult<Void> submit(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.submitChangeApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/change-applications/{applyId}/approve")
  public ApiResult<Void> approve(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.approveChangeApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/change-applications/{applyId}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long applyId,
      @RequestBody(required = false) ApprovalActionDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.rejectChangeApply(
        applyId, user.getTenantId(), dto != null ? dto.getReason() : null);
    return ApiResult.ok();
  }

  private ContractChangeItemDto toItemDto(ContractChangeApply apply, Contract contract) {
    return new ContractChangeItemDto(
        str(apply.getId()),
        apply.getChangeNo(),
        str(apply.getContractId()),
        contract != null ? contract.getContractNo() : null,
        apply.getChangeType(),
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

  private String str(Long value) {
    return value != null ? String.valueOf(value) : null;
  }

  private String formatDt(LocalDateTime value) {
    return value != null ? value.format(ISO_DT) : null;
  }
}
