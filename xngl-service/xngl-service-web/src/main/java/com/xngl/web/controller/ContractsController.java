package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractApprovalRecordVo;
import com.xngl.manager.contract.ContractDetailVo;
import com.xngl.manager.contract.ContractInvoiceVo;
import com.xngl.manager.contract.ContractMaterialVo;
import com.xngl.manager.contract.ContractQueryParams;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.contract.ContractTicketVo;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ApprovalActionDto;
import com.xngl.web.dto.contract.ContractCreateDto;
import com.xngl.web.dto.contract.ContractDetailDto;
import com.xngl.web.dto.contract.ContractItemDto;
import com.xngl.web.dto.contract.ContractStatsDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private final UserContext userContext;
  @org.springframework.beans.factory.annotation.Value("${app.contract.doc-dir:/data/xngl/contract-docs}")
  private String contractDocDir;

  public ContractsController(ContractService contractService, UserContext userContext) {
    this.contractService = contractService;
    this.userContext = userContext;
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
    contractService.submitContract(id, currentUser.getTenantId(), currentUser.getId());
    return ApiResult.ok();
  }

  @PostMapping("/{id}/approve")
  public ApiResult<Void> approve(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    userContext.requireApprovalPermission(currentUser);
    contractService.approveContract(id, currentUser.getTenantId(), currentUser.getId());
    return ApiResult.ok();
  }

  @PostMapping("/{id}/reject")
  public ApiResult<Void> reject(@PathVariable Long id,
      @RequestBody(required = false) ApprovalActionDto dto, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    userContext.requireApprovalPermission(currentUser);
    String reason = dto != null ? dto.getReason() : null;
    contractService.rejectContract(id, currentUser.getTenantId(), currentUser.getId(), reason);
    return ApiResult.ok();
  }

  @GetMapping("/stats")
  public ApiResult<ContractStatsDto> stats(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    var raw = contractService.getContractStats(currentUser.getTenantId());
    ContractStatsDto dto = new ContractStatsDto();
    dto.setTotalContracts(raw.getTotalContracts());
    dto.setEffectiveContracts(raw.getEffectiveContracts());
    dto.setMonthlyReceiptAmount(raw.getMonthlyReceiptAmount());
    dto.setMonthlyReceiptCount(raw.getMonthlyReceiptCount());
    dto.setPendingReceiptAmount(raw.getPendingReceiptAmount());
    dto.setTotalSettlementOrders(raw.getTotalSettlementOrders());
    dto.setPendingSettlementOrders(raw.getPendingSettlementOrders());
    return ApiResult.ok(dto);
  }

  @GetMapping("/search")
  public ApiResult<PageResult<ContractItemDto>> advancedSearch(
      @RequestParam(required = false) String contractType,
      @RequestParam(required = false) String contractStatus,
      @RequestParam(required = false) String approvalStatus,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) Long constructionOrgId,
      @RequestParam(required = false) Long transportOrgId,
      @RequestParam(required = false) Boolean isThreeParty,
      @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveStartDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveEndDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expireStartDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expireEndDate,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractQueryParams params = new ContractQueryParams();
    params.setContractType(contractType);
    params.setContractStatus(contractStatus);
    params.setApprovalStatus(approvalStatus);
    params.setKeyword(keyword);
    params.setProjectId(projectId);
    params.setSiteId(siteId);
    params.setConstructionOrgId(constructionOrgId);
    params.setTransportOrgId(transportOrgId);
    params.setIsThreeParty(isThreeParty);
    params.setSourceType(sourceType);
    params.setStartDate(startDate);
    params.setEndDate(endDate);
    params.setEffectiveStartDate(effectiveStartDate);
    params.setEffectiveEndDate(effectiveEndDate);
    params.setExpireStartDate(expireStartDate);
    params.setExpireEndDate(expireEndDate);
    params.setPageNo(pageNo);
    params.setPageSize(pageSize);
    IPage<Contract> page = contractService.pageContractsAdvanced(currentUser.getTenantId(), params);
    List<ContractItemDto> records = page.getRecords().stream()
        .map(this::toItemDto)
        .toList();
    return ApiResult.ok(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
  }

  @GetMapping("/{id}/detail")
  public ApiResult<ContractDetailVo> getDetail(@PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ContractDetailVo detail = contractService.getContractDetail(id, currentUser.getTenantId());
    return ApiResult.ok(detail);
  }

  @GetMapping("/{id}/approval-records")
  public ApiResult<List<ContractApprovalRecordVo>> getApprovalRecords(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ContractApprovalRecordVo> records = contractService.getContractApprovalRecords(id, currentUser.getTenantId());
    return ApiResult.ok(records);
  }

  @GetMapping("/{id}/materials")
  public ApiResult<List<ContractMaterialVo>> getMaterials(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ContractMaterialVo> materials = contractService.getContractMaterials(id, currentUser.getTenantId());
    return ApiResult.ok(materials);
  }

  @GetMapping("/{id}/materials/{materialId}/download")
  public ResponseEntity<Resource> downloadMaterial(
      @PathVariable Long id, @PathVariable Long materialId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    var material = contractService.getContractMaterial(materialId, id, currentUser.getTenantId());
    if (!StringUtils.hasText(material.getFileUrl())) {
      throw new BizException(404, "办事材料文件不存在");
    }
    Path path = Path.of(material.getFileUrl()).normalize();
    Path allowedDir = Path.of(contractDocDir).normalize();
    if (!path.startsWith(allowedDir)) {
      throw new BizException(403, "文件路径不合法");
    }
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new BizException(404, "办事材料文件不存在");
    }
    FileSystemResource resource = new FileSystemResource(path);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(path.getFileName().toString())
            .build();
    try {
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
          .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
          .contentLength(Files.size(path))
          .body(resource);
    } catch (IOException ex) {
      throw new BizException(500, "读取办事材料失败");
    }
  }

  @GetMapping("/{id}/invoices")
  public ApiResult<List<ContractInvoiceVo>> getInvoices(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ContractInvoiceVo> invoices = contractService.getContractInvoices(id, currentUser.getTenantId());
    return ApiResult.ok(invoices);
  }

  @GetMapping("/{id}/tickets")
  public ApiResult<List<ContractTicketVo>> getTickets(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<ContractTicketVo> tickets = contractService.getContractTickets(id, currentUser.getTenantId());
    return ApiResult.ok(tickets);
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
    c.setPartyId(parseLong(dto.getPartyId()));
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
    return userContext.requireCurrentUser(request);
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

}
