package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTransferApply;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractApplyService;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.ContractTransferCreateDto;
import com.xngl.web.dto.contract.ContractTransferItemDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class ContractTransferController {

  private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractApplyService contractApplyService;
  private final ContractService contractService;
  private final UserService userService;

  public ContractTransferController(
      ContractApplyService contractApplyService,
      ContractService contractService,
      UserService userService) {
    this.contractApplyService = contractApplyService;
    this.contractService = contractService;
    this.userService = userService;
  }

  @PostMapping("/transfers")
  public ApiResult<String> create(
      @Valid @RequestBody ContractTransferCreateDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    long applyId = contractApplyService.createTransferApply(
        user.getTenantId(), user.getId(),
        dto.getSourceContractId(), dto.getTargetContractId(),
        dto.getTransferAmount(), dto.getTransferVolume(), dto.getReason());
    return ApiResult.ok(String.valueOf(applyId));
  }

  @GetMapping("/transfers")
  public ApiResult<PageResult<ContractTransferItemDto>> list(
      @RequestParam(required = false) String approvalStatus,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    IPage<ContractTransferApply> page = contractApplyService.pageTransferApplies(
        user.getTenantId(), approvalStatus, pageNo, pageSize);

    Set<Long> contractIds = new LinkedHashSet<>();
    page.getRecords().forEach(apply -> {
      if (apply.getSourceContractId() != null) contractIds.add(apply.getSourceContractId());
      if (apply.getTargetContractId() != null) contractIds.add(apply.getTargetContractId());
    });
    Map<Long, Contract> contractMap = loadContractMap(contractIds, user.getTenantId());

    List<ContractTransferItemDto> records = page.getRecords().stream()
        .map(apply -> toItemDto(apply,
            contractMap.get(apply.getSourceContractId()),
            contractMap.get(apply.getTargetContractId())))
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/transfers/{applyId}")
  public ApiResult<ContractTransferItemDto> get(
      @PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    ContractTransferApply apply = contractApplyService.getTransferApply(applyId, user.getTenantId());
    Contract source = safeGetContract(apply.getSourceContractId(), user.getTenantId());
    Contract target = safeGetContract(apply.getTargetContractId(), user.getTenantId());
    return ApiResult.ok(toItemDto(apply, source, target));
  }

  @PostMapping("/transfers/{applyId}/submit")
  public ApiResult<Void> submit(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.submitTransferApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/transfers/{applyId}/approve")
  public ApiResult<Void> approve(@PathVariable Long applyId, HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.approveTransferApply(applyId, user.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/transfers/{applyId}/reject")
  public ApiResult<Void> reject(
      @PathVariable Long applyId,
      @RequestBody(required = false) ApprovalActionDto dto,
      HttpServletRequest request) {
    User user = requireCurrentUser(request);
    contractApplyService.rejectTransferApply(
        applyId, user.getTenantId(), dto != null ? dto.getReason() : null);
    return ApiResult.ok();
  }

  private ContractTransferItemDto toItemDto(
      ContractTransferApply apply, Contract source, Contract target) {
    return new ContractTransferItemDto(
        str(apply.getId()),
        apply.getTransferNo(),
        str(apply.getSourceContractId()),
        source != null ? source.getContractNo() : null,
        str(apply.getTargetContractId()),
        target != null ? target.getContractNo() : null,
        apply.getTransferAmount(),
        apply.getTransferVolume(),
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
