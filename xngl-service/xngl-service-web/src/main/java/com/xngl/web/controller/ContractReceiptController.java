package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.contract.ContractAccessScope;
import com.xngl.manager.contract.CreateContractReceiptCommand;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ContractReceiptCancelDto;
import com.xngl.web.dto.contract.ContractReceiptCreateDto;
import com.xngl.web.dto.contract.ContractReceiptDetailDto;
import com.xngl.web.dto.contract.ContractReceiptItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.ContractAccessScopeResolver;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractReceiptController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractService contractService;
  private final UserService userService;
  private final UserContext userContext;
  private final ContractAccessScopeResolver contractAccessScopeResolver;

  public ContractReceiptController(
      ContractService contractService,
      UserService userService,
      UserContext userContext,
      ContractAccessScopeResolver contractAccessScopeResolver) {
    this.contractService = contractService;
    this.userService = userService;
    this.userContext = userContext;
    this.contractAccessScopeResolver = contractAccessScopeResolver;
  }

  @GetMapping("/receipts")
  public ApiResult<PageResult<ContractReceiptItemDto>> list(
      @RequestParam(required = false) Long contractId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractAccessScope accessScope = resolveScope(currentUser);
    IPage<ContractReceipt> page =
        contractService.pageReceipts(
            currentUser.getTenantId(),
            contractId,
            keyword,
            status,
            startDate,
            endDate,
            pageNo,
            pageSize,
            accessScope);
    Map<Long, Contract> contractMap =
        contractService.listContractsByIds(
                page.getRecords().stream()
                    .map(ContractReceipt::getContractId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)),
                currentUser.getTenantId())
            .stream()
            .collect(Collectors.toMap(Contract::getId, contract -> contract));
    List<ContractReceiptItemDto> records =
        page.getRecords().stream()
            .map(receipt -> toItemDto(receipt, contractMap.get(receipt.getContractId())))
            .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{contractId}/receipts")
  public ApiResult<List<ContractReceiptItemDto>> listByContract(
      @PathVariable Long contractId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Contract contract = requireAccessibleContract(contractId, currentUser);
    List<ContractReceiptItemDto> records =
        contractService.listReceiptsByContract(contractId, currentUser.getTenantId()).stream()
            .map(receipt -> toItemDto(receipt, contract))
            .toList();
    return ApiResult.ok(records);
  }

  @GetMapping("/receipts/{receiptId}")
  public ApiResult<ContractReceiptDetailDto> get(
      @PathVariable Long receiptId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractReceipt receipt = contractService.getReceipt(receiptId, currentUser.getTenantId());
    Contract contract = requireAccessibleContract(receipt.getContractId(), currentUser);
    return ApiResult.ok(toDetailDto(receipt, contract));
  }

  @PostMapping("/{contractId}/receipts")
  public ApiResult<String> create(
      @PathVariable Long contractId,
      @Valid @RequestBody ContractReceiptCreateDto dto,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    requireAccessibleContract(contractId, currentUser);
    long receiptId =
        contractService.createReceipt(
            contractId,
            currentUser.getId(),
            currentUser.getTenantId(),
            new CreateContractReceiptCommand(
                dto.getAmount(),
                dto.getReceiptDate(),
                dto.getReceiptType(),
                dto.getVoucherNo(),
                dto.getBankFlowNo(),
                dto.getRemark()));
    return ApiResult.ok(String.valueOf(receiptId));
  }

  @PutMapping("/receipts/{receiptId}/cancel")
  public ApiResult<String> cancel(
      @PathVariable Long receiptId,
      @RequestBody(required = false) ContractReceiptCancelDto dto,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractReceipt receipt = contractService.getReceipt(receiptId, currentUser.getTenantId());
    requireAccessibleContract(receipt.getContractId(), currentUser);
    long reversalId =
        contractService.cancelReceipt(
            receiptId,
            currentUser.getId(),
            currentUser.getTenantId(),
            dto != null ? dto.getRemark() : null);
    return ApiResult.ok(String.valueOf(reversalId));
  }

  private ContractReceiptItemDto toItemDto(ContractReceipt receipt, Contract contract) {
    return new ContractReceiptItemDto(
        stringValue(receipt.getId()),
        stringValue(receipt.getContractId()),
        resolveContractNo(contract),
        contract != null ? contract.getName() : null,
        receipt.getReceiptNo(),
        formatDate(receipt.getReceiptDate()),
        receipt.getAmount(),
        receipt.getReceiptType(),
        receipt.getVoucherNo(),
        receipt.getBankFlowNo(),
        receipt.getStatus(),
        stringValue(receipt.getOperatorId()),
        receipt.getRemark(),
        formatDateTime(receipt.getCreateTime()));
  }

  private ContractReceiptDetailDto toDetailDto(ContractReceipt receipt, Contract contract) {
    ContractReceiptDetailDto dto = new ContractReceiptDetailDto();
    ContractReceiptItemDto item = toItemDto(receipt, contract);
    dto.setId(item.getId());
    dto.setContractId(item.getContractId());
    dto.setContractNo(item.getContractNo());
    dto.setContractName(item.getContractName());
    dto.setReceiptNo(item.getReceiptNo());
    dto.setReceiptDate(item.getReceiptDate());
    dto.setAmount(item.getAmount());
    dto.setReceiptType(item.getReceiptType());
    dto.setVoucherNo(item.getVoucherNo());
    dto.setBankFlowNo(item.getBankFlowNo());
    dto.setStatus(item.getStatus());
    dto.setOperatorId(item.getOperatorId());
    dto.setRemark(item.getRemark());
    dto.setCreateTime(item.getCreateTime());
    dto.setContractAmount(resolveContractAmount(contract));
    dto.setReceivedAmount(contract != null ? contract.getReceivedAmount() : null);
    dto.setContractStatus(contract != null ? contract.getContractStatus() : null);
    return dto;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private ContractAccessScope resolveScope(User currentUser) {
    return contractAccessScopeResolver.resolve(currentUser);
  }

  private Contract requireAccessibleContract(Long contractId, User currentUser) {
    Contract contract = contractService.getContract(contractId, currentUser.getTenantId());
    if (!resolveScope(currentUser).matchesContract(contract)) {
      throw new BizException(403, "当前账号无权查看该合同");
    }
    return contract;
  }

  private BigDecimal resolveContractAmount(Contract contract) {
    if (contract == null) {
      return null;
    }
    return contract.getContractAmount() != null ? contract.getContractAmount() : contract.getAmount();
  }

  private String resolveContractNo(Contract contract) {
    if (contract == null) {
      return null;
    }
    return StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : contract.getCode();
  }

  private String formatDate(LocalDate value) {
    return value != null ? value.format(ISO_DATE) : null;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private String stringValue(Long value) {
    return value != null ? String.valueOf(value) : null;
  }
}
