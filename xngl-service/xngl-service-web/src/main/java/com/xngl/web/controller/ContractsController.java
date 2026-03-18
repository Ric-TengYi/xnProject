package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.ContractCreateDto;
import com.xngl.web.dto.contract.ContractDetailDto;
import com.xngl.web.dto.contract.ContractItemDto;
import com.xngl.web.dto.contract.ContractStatsDto;
import com.xngl.web.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
public class ContractsController {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ContractService contractService;
  private final UserService userService;

  public ContractsController(ContractService contractService, UserService userService) {
    this.contractService = contractService;
    this.userService = userService;
  }

  @GetMapping
  public ApiResult<PageResult<ContractItemDto>> list(
      @RequestParam(required = false) String contractType,
      @RequestParam(required = false) String contractStatus,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    IPage<Contract> page = contractService.pageContracts(
        currentUser.getTenantId(), contractType, contractStatus,
        keyword, projectId, siteId, startDate, endDate, pageNo, pageSize);
    List<ContractItemDto> records = page.getRecords().stream()
        .map(this::toItemDto)
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}")
  public ApiResult<ContractDetailDto> get(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Contract contract = contractService.getContract(id, currentUser.getTenantId());
    return ApiResult.ok(toDetailDto(contract));
  }

  @PostMapping
  public ApiResult<String> create(@Valid @RequestBody ContractCreateDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Contract contract = fromCreateDto(dto);
    long contractId = contractService.createContract(
        currentUser.getTenantId(), currentUser.getId(), contract);
    return ApiResult.ok(String.valueOf(contractId));
  }

  @PutMapping("/{id}")
  public ApiResult<Void> update(@PathVariable Long id,
      @RequestBody ContractCreateDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Contract updates = fromCreateDto(dto);
    contractService.updateContract(id, currentUser.getTenantId(), updates);
    return ApiResult.ok();
  }

  @PostMapping("/{id}/submit")
  public ApiResult<Void> submit(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    contractService.submitContract(id, currentUser.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/{id}/approve")
  public ApiResult<Void> approve(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    contractService.approveContract(id, currentUser.getTenantId());
    return ApiResult.ok();
  }

  @PostMapping("/{id}/reject")
  public ApiResult<Void> reject(@PathVariable Long id,
      @RequestBody(required = false) ApprovalActionDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String reason = dto != null ? dto.getReason() : null;
    contractService.rejectContract(id, currentUser.getTenantId(), reason);
    return ApiResult.ok();
  }

  @GetMapping("/stats")
  public ApiResult<ContractStatsDto> stats(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Map<String, Object> raw = contractService.getContractStats(currentUser.getTenantId());
    ContractStatsDto dto = new ContractStatsDto();
    dto.setTotalContracts(toLong(raw.get("totalContracts")));
    dto.setEffectiveContracts(toLong(raw.get("effectiveContracts")));
    dto.setMonthlyReceiptAmount(toBigDecimal(raw.get("monthlyReceiptAmount")));
    dto.setMonthlyReceiptCount(toLong(raw.get("monthlyReceiptCount")));
    dto.setPendingReceiptAmount(toBigDecimal(raw.get("pendingReceiptAmount")));
    dto.setTotalSettlementOrders(toLong(raw.get("totalSettlementOrders")));
    dto.setPendingSettlementOrders(toLong(raw.get("pendingSettlementOrders")));
    return ApiResult.ok(dto);
  }

  private ContractItemDto toItemDto(Contract c) {
    ContractItemDto dto = new ContractItemDto();
    dto.setId(stringValue(c.getId()));
    dto.setContractNo(resolveContractNo(c));
    dto.setContractType(c.getContractType());
    dto.setName(c.getName());
    dto.setProjectId(stringValue(c.getProjectId()));
    dto.setSiteId(stringValue(c.getSiteId()));
    dto.setConstructionOrgId(stringValue(c.getConstructionOrgId()));
    dto.setTransportOrgId(stringValue(c.getTransportOrgId()));
    dto.setContractAmount(c.getContractAmount());
    dto.setReceivedAmount(c.getReceivedAmount());
    dto.setSettledAmount(c.getSettledAmount());
    dto.setAgreedVolume(c.getAgreedVolume());
    dto.setUnitPrice(c.getUnitPrice());
    dto.setContractStatus(c.getContractStatus());
    dto.setApprovalStatus(c.getApprovalStatus());
    dto.setSignDate(formatDate(c.getSignDate()));
    dto.setEffectiveDate(formatDate(c.getEffectiveDate()));
    dto.setExpireDate(formatDate(c.getExpireDate()));
    dto.setIsThreeParty(c.getIsThreeParty());
    dto.setSourceType(c.getSourceType());
    dto.setCreateTime(formatDateTime(c.getCreateTime()));
    return dto;
  }

  private ContractDetailDto toDetailDto(Contract c) {
    ContractDetailDto dto = new ContractDetailDto();
    dto.setId(stringValue(c.getId()));
    dto.setContractNo(resolveContractNo(c));
    dto.setContractType(c.getContractType());
    dto.setName(c.getName());
    dto.setProjectId(stringValue(c.getProjectId()));
    dto.setSiteId(stringValue(c.getSiteId()));
    dto.setConstructionOrgId(stringValue(c.getConstructionOrgId()));
    dto.setTransportOrgId(stringValue(c.getTransportOrgId()));
    dto.setContractAmount(c.getContractAmount());
    dto.setReceivedAmount(c.getReceivedAmount());
    dto.setSettledAmount(c.getSettledAmount());
    dto.setAgreedVolume(c.getAgreedVolume());
    dto.setUnitPrice(c.getUnitPrice());
    dto.setContractStatus(c.getContractStatus());
    dto.setApprovalStatus(c.getApprovalStatus());
    dto.setSignDate(formatDate(c.getSignDate()));
    dto.setEffectiveDate(formatDate(c.getEffectiveDate()));
    dto.setExpireDate(formatDate(c.getExpireDate()));
    dto.setIsThreeParty(c.getIsThreeParty());
    dto.setSourceType(c.getSourceType());
    dto.setCreateTime(formatDateTime(c.getCreateTime()));
    dto.setSiteOperatorOrgId(stringValue(c.getSiteOperatorOrgId()));
    dto.setUnitPriceInside(c.getUnitPriceInside());
    dto.setUnitPriceOutside(c.getUnitPriceOutside());
    dto.setPartyId(stringValue(c.getPartyId()));
    dto.setChangeVersion(c.getChangeVersion());
    dto.setRemark(c.getRemark());
    dto.setApplicantId(stringValue(c.getApplicantId()));
    dto.setRejectReason(c.getRejectReason());
    dto.setCode(c.getCode());
    dto.setAmount(c.getAmount());
    return dto;
  }

  private Contract fromCreateDto(ContractCreateDto dto) {
    Contract c = new Contract();
    c.setContractNo(dto.getContractNo());
    c.setContractType(dto.getContractType());
    c.setName(dto.getName());
    c.setProjectId(parseLong(dto.getProjectId()));
    c.setSiteId(parseLong(dto.getSiteId()));
    c.setConstructionOrgId(parseLong(dto.getConstructionOrgId()));
    c.setTransportOrgId(parseLong(dto.getTransportOrgId()));
    c.setSiteOperatorOrgId(parseLong(dto.getSiteOperatorOrgId()));
    c.setSignDate(parseDate(dto.getSignDate()));
    c.setEffectiveDate(parseDate(dto.getEffectiveDate()));
    c.setExpireDate(parseDate(dto.getExpireDate()));
    c.setAgreedVolume(dto.getAgreedVolume());
    c.setUnitPrice(dto.getUnitPrice());
    c.setContractAmount(dto.getContractAmount());
    c.setIsThreeParty(dto.getIsThreeParty());
    c.setUnitPriceInside(dto.getUnitPriceInside());
    c.setUnitPriceOutside(dto.getUnitPriceOutside());
    c.setSourceType(dto.getSourceType());
    c.setRemark(dto.getRemark());
    return c;
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

  private String resolveContractNo(Contract contract) {
    return StringUtils.hasText(contract.getContractNo())
        ? contract.getContractNo() : contract.getCode();
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

  private Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new BizException(400, "无效的 ID 值：" + value);
    }
  }

  private LocalDate parseDate(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return LocalDate.parse(value.trim(), ISO_DATE);
  }

  private long toLong(Object value) {
    if (value instanceof Number n) {
      return n.longValue();
    }
    return 0L;
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    return BigDecimal.ZERO;
  }
}
