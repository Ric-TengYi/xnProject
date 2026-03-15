package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractReceiptMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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

  private final ContractMapper contractMapper;
  private final ContractReceiptMapper contractReceiptMapper;

  public ContractServiceImpl(
      ContractMapper contractMapper, ContractReceiptMapper contractReceiptMapper) {
    this.contractMapper = contractMapper;
    this.contractReceiptMapper = contractReceiptMapper;
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
      int pageSize) {
    LambdaQueryWrapper<ContractReceipt> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(ContractReceipt::getTenantId, tenantId);
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
    reversal.setVoucherNo(original.getVoucherNo());
    reversal.setBankFlowNo(original.getBankFlowNo());
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
    return expectedTenantId == null || actualTenantId == null || expectedTenantId.equals(actualTenantId);
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
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
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
}
