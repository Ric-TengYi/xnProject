package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractApprovalRecord;
import com.xngl.infrastructure.persistence.entity.contract.ContractInvoice;
import com.xngl.infrastructure.persistence.entity.contract.ContractMaterial;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.mapper.ContractApprovalRecordMapper;
import com.xngl.infrastructure.persistence.mapper.ContractInvoiceMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMaterialMapper;
import com.xngl.infrastructure.persistence.mapper.ContractReceiptMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.manager.message.ApprovalMessageCommand;
import com.xngl.manager.message.MessageRecordService;
import com.xngl.manager.role.RoleService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ContractServiceImpl implements ContractService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final String RECEIPT_STATUS_NORMAL = "NORMAL";
  private static final String RECEIPT_STATUS_CANCELLED = "CANCELLED";
  private static final String RECEIPT_TYPE_MANUAL = "MANUAL";
  private static final String RECEIPT_TYPE_REVERSAL = "REVERSAL";
  private static final Set<String> ACTIVE_CONTRACT_STATUSES =
      Set.of("EFFECTIVE", "EXECUTING", "IN_PROGRESS");
  private static final DateTimeFormatter RECEIPT_NO_TIME =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final Path contractDocDir;

  private final ContractMapper contractMapper;
  private final ContractReceiptMapper contractReceiptMapper;
  private final ContractApprovalRecordMapper approvalRecordMapper;
  private final ContractMaterialMapper materialMapper;
  private final ContractInvoiceMapper invoiceMapper;
  private final ContractTicketMapper ticketMapper;
  private final MessageRecordService messageRecordService;
  private final RoleService roleService;

  public ContractServiceImpl(
      ContractMapper contractMapper,
      ContractReceiptMapper contractReceiptMapper,
      ContractApprovalRecordMapper approvalRecordMapper,
      ContractMaterialMapper materialMapper,
      ContractInvoiceMapper invoiceMapper,
      ContractTicketMapper ticketMapper,
      MessageRecordService messageRecordService,
      RoleService roleService,
      @Value("${app.contract.doc-dir:/data/xngl/contract-docs}") String contractDocDirPath) {
    this.contractMapper = contractMapper;
    this.contractReceiptMapper = contractReceiptMapper;
    this.approvalRecordMapper = approvalRecordMapper;
    this.materialMapper = materialMapper;
    this.invoiceMapper = invoiceMapper;
    this.ticketMapper = ticketMapper;
    this.messageRecordService = messageRecordService;
    this.roleService = roleService;
    this.contractDocDir = Path.of(contractDocDirPath);
  }

  @Override
  public Contract getContract(Long contractId, Long tenantId) {
    Contract contract = contractMapper.selectById(contractId);
    if (contract == null || !isTenantAccessible(tenantId, contract.getTenantId())) {
      throw new ContractServiceException(404, "合同不存在");
    }
    return contract;
  }

  @Override
  public List<Contract> listContractsByIds(Collection<Long> contractIds, Long tenantId) {
    if (CollectionUtils.isEmpty(contractIds)) {
      return Collections.emptyList();
    }
    return contractMapper.selectBatchIds(contractIds).stream()
        .filter(contract -> isTenantAccessible(tenantId, contract.getTenantId()))
        .toList();
  }

  @Override
  public IPage<ContractReceipt> pageReceipts(
      Long tenantId,
      Long contractId,
      String keyword,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      int pageNo,
      int pageSize,
      ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return emptyPage(pageNo, pageSize);
    }
    LambdaQueryWrapper<ContractReceipt> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(ContractReceipt::getTenantId, tenantId);
    }
    Set<Long> accessibleContractIds = resolveAccessibleContractIds(tenantId, accessScope);
    if (accessScope != null && !accessScope.isTenantWideAccess()) {
      if (contractId != null && !accessibleContractIds.contains(contractId)) {
        return emptyPage(pageNo, pageSize);
      }
      if (accessibleContractIds.isEmpty()) {
        return emptyPage(pageNo, pageSize);
      }
      query.in(ContractReceipt::getContractId, accessibleContractIds);
    }
    if (contractId != null) {
      query.eq(ContractReceipt::getContractId, contractId);
    }
    if (StringUtils.hasText(keyword)) {
      query.and(
          wrapper ->
              wrapper.like(ContractReceipt::getReceiptNo, keyword)
                  .or()
                  .like(ContractReceipt::getVoucherNo, keyword)
                  .or()
                  .like(ContractReceipt::getBankFlowNo, keyword));
    }
    if (StringUtils.hasText(status)) {
      query.eq(ContractReceipt::getStatus, status.trim());
    }
    if (startDate != null) {
      query.ge(ContractReceipt::getReceiptDate, startDate);
    }
    if (endDate != null) {
      query.le(ContractReceipt::getReceiptDate, endDate);
    }
    query.orderByDesc(ContractReceipt::getReceiptDate).orderByDesc(ContractReceipt::getId);
    return contractReceiptMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public List<ContractReceipt> listReceiptsByContract(Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    LambdaQueryWrapper<ContractReceipt> query =
        new LambdaQueryWrapper<ContractReceipt>()
            .eq(ContractReceipt::getContractId, contractId)
            .orderByDesc(ContractReceipt::getReceiptDate)
            .orderByDesc(ContractReceipt::getId);
    if (tenantId != null) {
      query.eq(ContractReceipt::getTenantId, tenantId);
    }
    return contractReceiptMapper.selectList(query);
  }

  @Override
  public ContractReceipt getReceipt(Long receiptId, Long tenantId) {
    ContractReceipt receipt = contractReceiptMapper.selectById(receiptId);
    if (receipt == null || !isTenantAccessible(tenantId, receipt.getTenantId())) {
      throw new ContractServiceException(404, "入账记录不存在");
    }
    return receipt;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createReceipt(
      Long contractId, Long operatorId, Long operatorTenantId, CreateContractReceiptCommand command) {
    validateCreateCommand(command);
    Contract contract = getContract(contractId, operatorTenantId);
    ensureContractCanReceive(contract);

    Long effectiveTenantId = resolveTenantId(contract.getTenantId(), operatorTenantId);
    BigDecimal currentReceivedAmount = safeAmount(contract.getReceivedAmount());
    BigDecimal nextReceivedAmount = currentReceivedAmount.add(command.getAmount());
    BigDecimal contractAmount = resolveContractAmount(contract);
    if (contractAmount.compareTo(ZERO) > 0 && nextReceivedAmount.compareTo(contractAmount) > 0) {
      throw new ContractServiceException(409, "累计入账金额不能超过合同总金额");
    }

    ContractReceipt receipt = new ContractReceipt();
    receipt.setTenantId(effectiveTenantId);
    receipt.setContractId(contractId);
    receipt.setReceiptNo(generateReceiptNo());
    receipt.setReceiptDate(command.getReceiptDate());
    receipt.setAmount(command.getAmount());
    receipt.setReceiptType(normalizeReceiptType(command.getReceiptType()));
    receipt.setVoucherNo(trimToNull(command.getVoucherNo()));
    receipt.setBankFlowNo(trimToNull(command.getBankFlowNo()));
    receipt.setStatus(RECEIPT_STATUS_NORMAL);
    receipt.setOperatorId(operatorId);
    receipt.setRemark(trimToNull(command.getRemark()));
    contractReceiptMapper.insert(receipt);

    Contract update = new Contract();
    update.setId(contractId);
    update.setReceivedAmount(nextReceivedAmount);
    if (contract.getTenantId() == null && effectiveTenantId != null) {
      update.setTenantId(effectiveTenantId);
    }
    contractMapper.updateById(update);
    return receipt.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long cancelReceipt(
      Long receiptId, Long operatorId, Long operatorTenantId, String cancelRemark) {
    ContractReceipt original = getReceipt(receiptId, operatorTenantId);
    if (RECEIPT_STATUS_CANCELLED.equalsIgnoreCase(original.getStatus())) {
      throw new ContractServiceException(409, "该入账记录已冲销");
    }
    if (original.getAmount() == null || original.getAmount().compareTo(ZERO) <= 0) {
      throw new ContractServiceException(409, "仅正常入账流水允许冲销");
    }

    Contract contract = getContract(original.getContractId(), operatorTenantId);
    BigDecimal currentReceivedAmount = safeAmount(contract.getReceivedAmount());
    if (currentReceivedAmount.compareTo(original.getAmount()) < 0) {
      throw new ContractServiceException(409, "合同累计入账金额异常，无法冲销");
    }

    ContractReceipt cancelled = new ContractReceipt();
    cancelled.setId(original.getId());
    cancelled.setStatus(RECEIPT_STATUS_CANCELLED);
    cancelled.setRemark(buildCancelledRemark(original.getRemark(), cancelRemark));
    contractReceiptMapper.updateById(cancelled);

    Long effectiveTenantId = resolveTenantId(original.getTenantId(), operatorTenantId);
    ContractReceipt reversal = new ContractReceipt();
    reversal.setTenantId(effectiveTenantId);
    reversal.setContractId(original.getContractId());
    reversal.setReceiptNo(generateReceiptNo());
    reversal.setReceiptDate(LocalDate.now());
    reversal.setAmount(original.getAmount().negate());
    reversal.setReceiptType(RECEIPT_TYPE_REVERSAL);
    reversal.setStatus(RECEIPT_STATUS_NORMAL);
    reversal.setOperatorId(operatorId);
    reversal.setRemark(buildReversalRemark(original.getReceiptNo(), cancelRemark));
    contractReceiptMapper.insert(reversal);

    Contract update = new Contract();
    update.setId(contract.getId());
    update.setReceivedAmount(currentReceivedAmount.subtract(original.getAmount()));
    if (contract.getTenantId() == null && effectiveTenantId != null) {
      update.setTenantId(effectiveTenantId);
    }
    contractMapper.updateById(update);
    return reversal.getId();
  }

  private void validateCreateCommand(CreateContractReceiptCommand command) {
    if (command == null) {
      throw new ContractServiceException(400, "入账参数不能为空");
    }
    if (command.getAmount() == null || command.getAmount().compareTo(ZERO) <= 0) {
      throw new ContractServiceException(400, "入账金额必须大于 0");
    }
    if (command.getReceiptDate() == null) {
      throw new ContractServiceException(400, "入账日期不能为空");
    }
  }

  private void ensureContractCanReceive(Contract contract) {
    if (contract == null) {
      throw new ContractServiceException(404, "合同不存在");
    }
    String contractStatus = trimToNull(contract.getContractStatus());
    if (contractStatus == null) {
      return;
    }
    if (!ACTIVE_CONTRACT_STATUSES.contains(contractStatus.toUpperCase())) {
      throw new ContractServiceException(409, "仅生效中或执行中的合同允许入账");
    }
  }

  private boolean isTenantAccessible(Long expectedTenantId, Long actualTenantId) {
    if (expectedTenantId == null) {
      return true; // 超管场景，不限制租户
    }
    return actualTenantId != null && expectedTenantId.equals(actualTenantId);
  }

  private Long resolveTenantId(Long contractTenantId, Long operatorTenantId) {
    if (contractTenantId != null && operatorTenantId != null && !contractTenantId.equals(operatorTenantId)) {
      throw new ContractServiceException(403, "无权操作其他租户合同");
    }
    return contractTenantId != null ? contractTenantId : operatorTenantId;
  }

  private BigDecimal resolveContractAmount(Contract contract) {
    if (contract.getContractAmount() != null) {
      return contract.getContractAmount();
    }
    if (contract.getAmount() != null) {
      return contract.getAmount();
    }
    return ZERO;
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value == null ? ZERO : value;
  }

  private String normalizeReceiptType(String receiptType) {
    return StringUtils.hasText(receiptType) ? receiptType.trim().toUpperCase() : RECEIPT_TYPE_MANUAL;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String generateReceiptNo() {
    String timePart = LocalDateTime.now().format(RECEIPT_NO_TIME);
    int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
    return "CR" + timePart + random;
  }

  private String buildCancelledRemark(String originalRemark, String cancelRemark) {
    String suffix = StringUtils.hasText(cancelRemark) ? cancelRemark.trim() : "已发起冲销";
    if (!StringUtils.hasText(originalRemark)) {
      return suffix;
    }
    return originalRemark + " | " + suffix;
  }

  private String buildReversalRemark(String originalReceiptNo, String cancelRemark) {
    String base = "冲销原入账流水：" + originalReceiptNo;
    if (!StringUtils.hasText(cancelRemark)) {
      return base;
    }
    return base + "，原因：" + cancelRemark.trim();
  }

  @Override
  public IPage<Contract> pageContracts(Long tenantId, String contractType, String contractStatus,
      String keyword, Long projectId, Long siteId, LocalDate startDate, LocalDate endDate,
      int pageNo, int pageSize, ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return emptyPage(pageNo, pageSize);
    }
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(query, accessScope);
    if (StringUtils.hasText(contractType)) {
      query.eq(Contract::getContractType, contractType.trim());
    }
    if (StringUtils.hasText(contractStatus)) {
      query.eq(Contract::getContractStatus, contractStatus.trim());
    }
    if (StringUtils.hasText(keyword)) {
      query.and(wrapper -> wrapper
          .like(Contract::getName, keyword)
          .or()
          .like(Contract::getContractNo, keyword)
          .or()
          .like(Contract::getCode, keyword));
    }
    if (projectId != null) {
      query.eq(Contract::getProjectId, projectId);
    }
    if (siteId != null) {
      query.eq(Contract::getSiteId, siteId);
    }
    if (startDate != null) {
      query.ge(Contract::getSignDate, startDate);
    }
    if (endDate != null) {
      query.le(Contract::getSignDate, endDate);
    }
    query.orderByDesc(Contract::getCreateTime);
    return contractMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  public IPage<Contract> pageContractsWithPermissionFilter(Long userId, Long tenantId, String contractType,
      String contractStatus, String keyword, Long projectId, Long siteId, LocalDate startDate, LocalDate endDate,
      int pageNo, int pageSize) {
    IPage<Contract> page = pageContracts(tenantId, contractType, contractStatus, keyword, projectId, siteId,
        startDate, endDate, pageNo, pageSize, null);

    List<Contract> filtered = page.getRecords().stream()
        .filter(contract -> roleService.canAccessOrganization(userId, contract.getSiteId(), "ORG_AND_CHILDREN"))
        .toList();

    page.setRecords(filtered);
    return page;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createContract(Long tenantId, Long applicantId, Contract contract) {
    boolean offlineContract =
        StringUtils.hasText(contract.getSourceType())
            && "OFFLINE".equalsIgnoreCase(contract.getSourceType().trim());
    if (contract.getContractNo() == null || contract.getContractNo().isBlank()) {
      contract.setContractNo(generateContractNo());
    }
    Long existing = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getContractNo, contract.getContractNo())
            .eq(Contract::getTenantId, tenantId));
    if (existing > 0) {
      throw new ContractServiceException(409, "合同编号已存在: " + contract.getContractNo());
    }
    contract.setTenantId(tenantId);
    contract.setApplicantId(applicantId);
    contract.setContractStatus(offlineContract ? "EFFECTIVE" : "DRAFT");
    contract.setApprovalStatus(offlineContract ? "APPROVED" : "DRAFT");
    contract.setChangeVersion(0);
    contract.setReceivedAmount(ZERO);
    contract.setSettledAmount(ZERO);
    if (contract.getSourceType() == null || contract.getSourceType().isBlank()) {
      contract.setSourceType("ONLINE");
    }
    if (contract.getIsThreeParty() == null) {
      contract.setIsThreeParty(false);
    }
    contractMapper.insert(contract);
    return contract.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateContract(Long contractId, Long tenantId, Contract updates) {
    Contract existing = getContract(contractId, tenantId);
    if (!"DRAFT".equalsIgnoreCase(existing.getContractStatus())) {
      throw new ContractServiceException(409, "仅草稿状态的合同允许编辑");
    }
    updates.setId(contractId);
    updates.setTenantId(null);
    updates.setContractStatus(null);
    updates.setApprovalStatus(null);
    updates.setChangeVersion(null);
    updates.setReceivedAmount(null);
    updates.setSettledAmount(null);
    contractMapper.updateById(updates);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void submitContract(Long contractId, Long tenantId, Long operatorId) {
    Contract existing = getContract(contractId, tenantId);
    if (!"DRAFT".equalsIgnoreCase(existing.getContractStatus())
        && !"REJECTED".equalsIgnoreCase(existing.getContractStatus())) {
      throw new ContractServiceException(409, "仅草稿或被驳回的合同允许提交审批");
    }
    Contract update = new Contract();
    update.setId(contractId);
    update.setContractStatus("APPROVING");
    update.setApprovalStatus("APPROVING");
    update.setRejectReason(null);
    contractMapper.updateById(update);
    saveApprovalRecord(existing, operatorId, "SUBMIT", "APPROVING", "合同已提交审批");
    saveApprovalMaterial(existing, operatorId, "SUBMIT", "APPROVING", "系统自动生成提交审批文书");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void approveContract(Long contractId, Long tenantId, Long operatorId) {
    Contract existing = getContract(contractId, tenantId);
    if (!"APPROVING".equalsIgnoreCase(existing.getContractStatus())) {
      throw new ContractServiceException(409, "仅审批中的合同允许通过");
    }
    Contract update = new Contract();
    update.setId(contractId);
    update.setContractStatus("EFFECTIVE");
    update.setApprovalStatus("APPROVED");
    contractMapper.updateById(update);
    saveApprovalRecord(existing, operatorId, "APPROVE", "EFFECTIVE", "合同审批通过");
    saveApprovalMaterial(existing, operatorId, "APPROVE", "EFFECTIVE", "系统自动生成审批通过文书");
    pushApprovalNotification(
        existing,
        "合同审批已通过",
        "合同 " + resolveContractNo(existing) + " 已审批通过，请及时跟进后续流程。");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void rejectContract(Long contractId, Long tenantId, Long operatorId, String reason) {
    Contract existing = getContract(contractId, tenantId);
    if (!"APPROVING".equalsIgnoreCase(existing.getContractStatus())) {
      throw new ContractServiceException(409, "仅审批中的合同允许驳回");
    }
    Contract update = new Contract();
    update.setId(contractId);
    update.setContractStatus("REJECTED");
    update.setApprovalStatus("REJECTED");
    update.setRejectReason(StringUtils.hasText(reason) ? reason.trim() : null);
    contractMapper.updateById(update);
    String remark = StringUtils.hasText(reason) ? reason.trim() : "合同审批驳回";
    saveApprovalRecord(existing, operatorId, "REJECT", "REJECTED", remark);
    saveApprovalMaterial(existing, operatorId, "REJECT", "REJECTED", "系统自动生成审批驳回文书：" + remark);
    pushApprovalNotification(
        existing,
        "合同审批已驳回",
        "合同 " + resolveContractNo(existing) + " 已被驳回，原因：" + remark);
  }

  @Override
  public ContractStatsResult getContractStats(Long tenantId, ContractAccessScope accessScope) {
    ContractStatsResult result = new ContractStatsResult();
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      result.setTotalContracts(0L);
      result.setEffectiveContracts(0L);
      result.setMonthlyReceiptAmount(ZERO);
      result.setMonthlyReceiptCount(0);
      result.setPendingReceiptAmount(ZERO);
      result.setTotalSettlementOrders(0L);
      result.setPendingSettlementOrders(0L);
      return result;
    }

    LambdaQueryWrapper<Contract> totalQuery = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      totalQuery.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(totalQuery, accessScope);
    result.setTotalContracts(contractMapper.selectCount(totalQuery));

    LambdaQueryWrapper<Contract> effectiveQuery = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      effectiveQuery.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(effectiveQuery, accessScope);
    effectiveQuery.in(Contract::getContractStatus, ACTIVE_CONTRACT_STATUSES);
    result.setEffectiveContracts(contractMapper.selectCount(effectiveQuery));

    LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
    LambdaQueryWrapper<ContractReceipt> receiptQuery = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      receiptQuery.eq(ContractReceipt::getTenantId, tenantId);
    }
    Set<Long> accessibleContractIds = resolveAccessibleContractIds(tenantId, accessScope);
    if (accessScope != null && !accessScope.isTenantWideAccess()) {
      if (accessibleContractIds.isEmpty()) {
        result.setMonthlyReceiptAmount(ZERO);
        result.setMonthlyReceiptCount(0);
      } else {
        receiptQuery.in(ContractReceipt::getContractId, accessibleContractIds);
      }
    }
    receiptQuery.ge(ContractReceipt::getReceiptDate, monthStart);
    receiptQuery.eq(ContractReceipt::getStatus, RECEIPT_STATUS_NORMAL);
    receiptQuery.select(ContractReceipt::getAmount);
    if (accessScope == null || accessScope.isTenantWideAccess() || !accessibleContractIds.isEmpty()) {
      List<ContractReceipt> monthlyReceipts = contractReceiptMapper.selectList(receiptQuery);
      BigDecimal monthlyAmount = monthlyReceipts.stream()
          .map(r -> safeAmount(r.getAmount()))
          .reduce(ZERO, BigDecimal::add);
      result.setMonthlyReceiptAmount(monthlyAmount);
      result.setMonthlyReceiptCount(monthlyReceipts.size());
    }

    LambdaQueryWrapper<Contract> pendingQuery = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      pendingQuery.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(pendingQuery, accessScope);
    pendingQuery.in(Contract::getContractStatus, ACTIVE_CONTRACT_STATUSES);
    pendingQuery.select(Contract::getContractAmount, Contract::getAmount, Contract::getReceivedAmount);
    List<Contract> activeContracts = contractMapper.selectList(pendingQuery);
    BigDecimal pendingAmount = activeContracts.stream()
        .map(c -> safeAmount(resolveContractAmount(c)).subtract(safeAmount(c.getReceivedAmount())))
        .filter(diff -> diff.compareTo(ZERO) > 0)
        .reduce(ZERO, BigDecimal::add);
    result.setPendingReceiptAmount(pendingAmount);

    result.setTotalSettlementOrders(0L);
    result.setPendingSettlementOrders(0L);

    return result;
  }

  private String generateContractNo() {
    for (int attempt = 0; attempt < 3; attempt++) {
      String timePart = LocalDateTime.now().format(RECEIPT_NO_TIME);
      int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
      String no = "HT" + timePart + random;
      Long existing = contractMapper.selectCount(
          new LambdaQueryWrapper<Contract>().eq(Contract::getContractNo, no));
      if (existing == 0) {
        return no;
      }
    }
    throw new ContractServiceException(500, "合同编号生成失败，请重试");
  }

  @Override
  public IPage<Contract> pageContractsAdvanced(Long tenantId, ContractQueryParams params, ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return emptyPage(params.getPageNo(), params.getPageSize());
    }
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(query, accessScope);
    if (StringUtils.hasText(params.getContractType())) {
      query.eq(Contract::getContractType, params.getContractType().trim());
    }
    if (StringUtils.hasText(params.getContractStatus())) {
      query.eq(Contract::getContractStatus, params.getContractStatus().trim());
    }
    if (StringUtils.hasText(params.getApprovalStatus())) {
      query.eq(Contract::getApprovalStatus, params.getApprovalStatus().trim());
    }
    if (StringUtils.hasText(params.getKeyword())) {
      String kw = params.getKeyword().trim();
      query.and(wrapper -> wrapper
          .like(Contract::getName, kw)
          .or()
          .like(Contract::getContractNo, kw)
          .or()
          .like(Contract::getCode, kw));
    }
    if (params.getProjectId() != null) {
      query.eq(Contract::getProjectId, params.getProjectId());
    }
    if (params.getSiteId() != null) {
      query.eq(Contract::getSiteId, params.getSiteId());
    }
    if (params.getConstructionOrgId() != null) {
      query.eq(Contract::getConstructionOrgId, params.getConstructionOrgId());
    }
    if (params.getTransportOrgId() != null) {
      query.eq(Contract::getTransportOrgId, params.getTransportOrgId());
    }
    if (params.getIsThreeParty() != null) {
      query.eq(Contract::getIsThreeParty, params.getIsThreeParty());
    }
    if (StringUtils.hasText(params.getSourceType())) {
      query.eq(Contract::getSourceType, params.getSourceType().trim());
    }
    if (params.getStartDate() != null) {
      query.ge(Contract::getSignDate, params.getStartDate());
    }
    if (params.getEndDate() != null) {
      query.le(Contract::getSignDate, params.getEndDate());
    }
    if (params.getEffectiveStartDate() != null) {
      query.ge(Contract::getEffectiveDate, params.getEffectiveStartDate());
    }
    if (params.getEffectiveEndDate() != null) {
      query.le(Contract::getEffectiveDate, params.getEffectiveEndDate());
    }
    if (params.getExpireStartDate() != null) {
      query.ge(Contract::getExpireDate, params.getExpireStartDate());
    }
    if (params.getExpireEndDate() != null) {
      query.le(Contract::getExpireDate, params.getExpireEndDate());
    }
    query.orderByDesc(Contract::getCreateTime);
    return contractMapper.selectPage(new Page<>(params.getPageNo(), params.getPageSize()), query);
  }

  private void applyContractScope(
      LambdaQueryWrapper<Contract> query, ContractAccessScope accessScope) {
    if (query == null || accessScope == null || accessScope.isTenantWideAccess()) {
      return;
    }
    if (!accessScope.hasAnyAccess()) {
      query.apply("1 = 0");
      return;
    }
    query.and(
        wrapper -> {
          boolean hasClause = false;
          if (!accessScope.getProjectIds().isEmpty()) {
            wrapper.in(Contract::getProjectId, accessScope.getProjectIds());
            hasClause = true;
          }
          if (!accessScope.getSiteIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in(Contract::getSiteId, accessScope.getSiteIds());
            hasClause = true;
          }
          if (!accessScope.getOrgIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in(Contract::getConstructionOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getTransportOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getSiteOperatorOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getPartyId, accessScope.getOrgIds());
            hasClause = true;
          }
          if (!hasClause) {
            wrapper.apply("1 = 0");
          }
        });
  }

  private Set<Long> resolveAccessibleContractIds(Long tenantId, ContractAccessScope accessScope) {
    if (accessScope == null || accessScope.isTenantWideAccess()) {
      return Set.of();
    }
    if (!accessScope.hasAnyAccess()) {
      return Set.of();
    }
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(Contract::getTenantId, tenantId);
    }
    applyContractScope(query, accessScope);
    query.select(Contract::getId);
    return contractMapper.selectList(query).stream()
        .map(Contract::getId)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private <T> IPage<T> emptyPage(int pageNo, int pageSize) {
    Page<T> page = new Page<>(pageNo, pageSize);
    page.setTotal(0L);
    page.setRecords(List.of());
    return page;
  }

  @Override
  public ContractDetailVo getContractDetail(Long contractId, Long tenantId) {
    Contract contract = getContract(contractId, tenantId);
    ContractDetailVo vo = new ContractDetailVo();
    vo.setId(contract.getId());
    vo.setTenantId(contract.getTenantId());
    vo.setContractNo(resolveContractNo(contract));
    vo.setCode(contract.getCode());
    vo.setName(contract.getName());
    vo.setContractType(contract.getContractType());
    vo.setProjectId(contract.getProjectId());
    vo.setSiteId(contract.getSiteId());
    vo.setPartyId(contract.getPartyId());
    vo.setConstructionOrgId(contract.getConstructionOrgId());
    vo.setTransportOrgId(contract.getTransportOrgId());
    vo.setSiteOperatorOrgId(contract.getSiteOperatorOrgId());
    vo.setSignDate(contract.getSignDate());
    vo.setEffectiveDate(contract.getEffectiveDate());
    vo.setExpireDate(contract.getExpireDate());
    vo.setAgreedVolume(contract.getAgreedVolume());
    vo.setUnitPrice(contract.getUnitPrice());
    vo.setUnitPriceInside(contract.getUnitPriceInside());
    vo.setUnitPriceOutside(contract.getUnitPriceOutside());
    vo.setContractAmount(resolveContractAmount(contract));
    vo.setReceivedAmount(safeAmount(contract.getReceivedAmount()));
    vo.setSettledAmount(safeAmount(contract.getSettledAmount()));
    vo.setPendingAmount(vo.getContractAmount().subtract(vo.getReceivedAmount()));
    vo.setChangeVersion(contract.getChangeVersion());
    vo.setContractStatus(contract.getContractStatus());
    vo.setApprovalStatus(contract.getApprovalStatus());
    vo.setRejectReason(contract.getRejectReason());
    vo.setIsThreeParty(contract.getIsThreeParty());
    vo.setSourceType(contract.getSourceType());
    vo.setApplicantId(contract.getApplicantId());
    vo.setRemark(contract.getRemark());
    return vo;
  }

  @Override
  public List<ContractApprovalRecordVo> getContractApprovalRecords(Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    LambdaQueryWrapper<ContractApprovalRecord> query = new LambdaQueryWrapper<>();
    query.eq(ContractApprovalRecord::getContractId, contractId);
    if (tenantId != null) {
      query.eq(ContractApprovalRecord::getTenantId, tenantId);
    }
    query.orderByDesc(ContractApprovalRecord::getOperateTime);
    List<ContractApprovalRecord> records = approvalRecordMapper.selectList(query);
    return records.stream().map(this::toApprovalRecordVo).toList();
  }

  @Override
  public List<ContractMaterialVo> getContractMaterials(Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    LambdaQueryWrapper<ContractMaterial> query = new LambdaQueryWrapper<>();
    query.eq(ContractMaterial::getContractId, contractId);
    if (tenantId != null) {
      query.eq(ContractMaterial::getTenantId, tenantId);
    }
    query.orderByDesc(ContractMaterial::getCreateTime);
    List<ContractMaterial> materials = materialMapper.selectList(query);
    return materials.stream().map(this::toMaterialVo).toList();
  }

  @Override
  public ContractMaterial getContractMaterial(Long materialId, Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    ContractMaterial material = materialMapper.selectById(materialId);
    if (material == null || !contractId.equals(material.getContractId())
        || !isTenantAccessible(tenantId, material.getTenantId())) {
      throw new ContractServiceException(404, "办事材料不存在");
    }
    return material;
  }

  @Override
  public List<ContractInvoiceVo> getContractInvoices(Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    LambdaQueryWrapper<ContractInvoice> query = new LambdaQueryWrapper<>();
    query.eq(ContractInvoice::getContractId, contractId);
    if (tenantId != null) {
      query.eq(ContractInvoice::getTenantId, tenantId);
    }
    query.orderByDesc(ContractInvoice::getInvoiceDate);
    List<ContractInvoice> invoices = invoiceMapper.selectList(query);
    return invoices.stream().map(this::toInvoiceVo).toList();
  }

  @Override
  public List<ContractTicketVo> getContractTickets(Long contractId, Long tenantId) {
    getContract(contractId, tenantId);
    LambdaQueryWrapper<ContractTicket> query = new LambdaQueryWrapper<>();
    query.eq(ContractTicket::getContractId, contractId);
    if (tenantId != null) {
      query.eq(ContractTicket::getTenantId, tenantId);
    }
    query.orderByDesc(ContractTicket::getTicketDate);
    List<ContractTicket> tickets = ticketMapper.selectList(query);
    return tickets.stream().map(this::toTicketVo).toList();
  }

  private String resolveContractNo(Contract contract) {
    return StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : contract.getCode();
  }

  private void saveApprovalRecord(
      Contract contract, Long operatorId, String actionType, String toStatus, String remark) {
    ContractApprovalRecord record = new ContractApprovalRecord();
    record.setTenantId(contract.getTenantId());
    record.setContractId(contract.getId());
    record.setActionType(actionType);
    record.setOperatorId(operatorId);
    record.setFromStatus(contract.getContractStatus());
    record.setToStatus(toStatus);
    record.setRemark(remark);
    record.setOperateTime(LocalDateTime.now());
    approvalRecordMapper.insert(record);
  }

  private void saveApprovalMaterial(
      Contract contract, Long operatorId, String actionType, String toStatus, String remark) {
    try {
      Files.createDirectories(contractDocDir);
      String actionLabel = resolveActionName(actionType);
      String fileName =
          sanitizeFileName(resolveContractNo(contract) + "_" + actionType.toLowerCase() + "_"
              + LocalDateTime.now().format(RECEIPT_NO_TIME) + ".txt");
      Path path = contractDocDir.resolve(fileName);
      String content = buildApprovalDocument(contract, actionLabel, toStatus, remark, operatorId);
      Files.writeString(path, content, StandardCharsets.UTF_8);

      ContractMaterial material = new ContractMaterial();
      material.setTenantId(contract.getTenantId());
      material.setContractId(contract.getId());
      material.setMaterialName(actionLabel + "文书");
      material.setMaterialType("APPROVAL_DOCUMENT");
      material.setFileUrl(path.toString());
      material.setFileSize(Files.size(path));
      material.setUploaderId(operatorId);
      material.setRemark(remark);
      materialMapper.insert(material);
    } catch (IOException ex) {
      throw new ContractServiceException(500, "生成合同审批文书失败");
    }
  }

  private String buildApprovalDocument(
      Contract contract, String actionLabel, String toStatus, String remark, Long operatorId) {
    return String.join(
        "\n",
        "合同审批文书",
        "合同编号: " + resolveContractNo(contract),
        "合同名称: " + (StringUtils.hasText(contract.getName()) ? contract.getName() : "-"),
        "合同类型: " + (StringUtils.hasText(contract.getContractType()) ? contract.getContractType() : "-"),
        "审批动作: " + actionLabel,
        "原状态: " + (StringUtils.hasText(contract.getContractStatus()) ? contract.getContractStatus() : "-"),
        "目标状态: " + toStatus,
        "审批意见: " + (StringUtils.hasText(remark) ? remark : "-"),
        "操作人ID: " + (operatorId != null ? operatorId : "-"),
        "生成时间: " + LocalDateTime.now(),
        "项目ID: " + (contract.getProjectId() != null ? contract.getProjectId() : "-"),
        "场地ID: " + (contract.getSiteId() != null ? contract.getSiteId() : "-"),
        "合同金额: " + safeAmount(resolveContractAmount(contract)),
        "约定方量: " + safeAmount(contract.getAgreedVolume()));
  }

  private String sanitizeFileName(String value) {
    return value.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private void pushApprovalNotification(Contract contract, String title, String content) {
    messageRecordService.pushApprovalResult(
        new ApprovalMessageCommand(
            contract.getTenantId(),
            contract.getApplicantId(),
            title,
            content,
            "审批通知",
            "/contracts/" + contract.getId(),
            "CONTRACT",
            String.valueOf(contract.getId()),
            "合同审批"));
  }

  private ContractApprovalRecordVo toApprovalRecordVo(ContractApprovalRecord record) {
    ContractApprovalRecordVo vo = new ContractApprovalRecordVo();
    vo.setId(record.getId());
    vo.setContractId(record.getContractId());
    vo.setActionType(record.getActionType());
    vo.setActionName(resolveActionName(record.getActionType()));
    vo.setOperatorId(record.getOperatorId());
    vo.setFromStatus(record.getFromStatus());
    vo.setToStatus(record.getToStatus());
    vo.setRemark(record.getRemark());
    vo.setOperateTime(record.getOperateTime());
    return vo;
  }

  private String resolveActionName(String actionType) {
    if (actionType == null) return null;
    return switch (actionType) {
      case "SUBMIT" -> "提交审批";
      case "APPROVE" -> "审批通过";
      case "REJECT" -> "审批驳回";
      case "CANCEL" -> "撤销审批";
      default -> actionType;
    };
  }

  private ContractMaterialVo toMaterialVo(ContractMaterial material) {
    ContractMaterialVo vo = new ContractMaterialVo();
    vo.setId(material.getId());
    vo.setContractId(material.getContractId());
    vo.setMaterialName(material.getMaterialName());
    vo.setMaterialType(material.getMaterialType());
    vo.setFileUrl(material.getFileUrl());
    vo.setFileSize(material.getFileSize());
    vo.setUploaderId(material.getUploaderId());
    vo.setUploadTime(material.getCreateTime());
    vo.setRemark(material.getRemark());
    return vo;
  }

  private ContractInvoiceVo toInvoiceVo(ContractInvoice invoice) {
    ContractInvoiceVo vo = new ContractInvoiceVo();
    vo.setId(invoice.getId());
    vo.setContractId(invoice.getContractId());
    vo.setInvoiceNo(invoice.getInvoiceNo());
    vo.setInvoiceType(invoice.getInvoiceType());
    vo.setInvoiceDate(invoice.getInvoiceDate());
    vo.setAmount(invoice.getAmount());
    vo.setTaxRate(invoice.getTaxRate());
    vo.setTaxAmount(invoice.getTaxAmount());
    vo.setStatus(invoice.getStatus());
    vo.setRemark(invoice.getRemark());
    vo.setCreateTime(invoice.getCreateTime());
    return vo;
  }

  private ContractTicketVo toTicketVo(ContractTicket ticket) {
    ContractTicketVo vo = new ContractTicketVo();
    vo.setId(ticket.getId());
    vo.setContractId(ticket.getContractId());
    vo.setTicketNo(ticket.getTicketNo());
    vo.setTicketType(ticket.getTicketType());
    vo.setTicketDate(ticket.getTicketDate());
    vo.setAmount(ticket.getAmount());
    vo.setVolume(ticket.getVolume());
    vo.setStatus(ticket.getStatus());
    vo.setRemark(ticket.getRemark());
    vo.setCreatorId(ticket.getCreatorId());
    vo.setCreateTime(ticket.getCreateTime());
    return vo;
  }
}
