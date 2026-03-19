package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractChangeApply;
import com.xngl.infrastructure.persistence.entity.contract.ContractExtensionApply;
import com.xngl.infrastructure.persistence.entity.contract.ContractTransferApply;
import com.xngl.infrastructure.persistence.mapper.ContractChangeApplyMapper;
import com.xngl.infrastructure.persistence.mapper.ContractExtensionApplyMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTransferApplyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractApplyServiceImpl implements ContractApplyService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final Set<String> ACTIVE_CONTRACT_STATUSES =
      Set.of("EFFECTIVE", "EXECUTING", "IN_PROGRESS");
  private static final DateTimeFormatter NO_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final String DRAFT = "DRAFT";
  private static final String APPROVING = "APPROVING";
  private static final String APPROVED = "APPROVED";
  private static final String REJECTED = "REJECTED";

  private final ContractMapper contractMapper;
  private final ContractChangeApplyMapper changeApplyMapper;
  private final ContractExtensionApplyMapper extensionApplyMapper;
  private final ContractTransferApplyMapper transferApplyMapper;
  private final ObjectMapper objectMapper;

  public ContractApplyServiceImpl(
      ContractMapper contractMapper,
      ContractChangeApplyMapper changeApplyMapper,
      ContractExtensionApplyMapper extensionApplyMapper,
      ContractTransferApplyMapper transferApplyMapper,
      ObjectMapper objectMapper) {
    this.contractMapper = contractMapper;
    this.changeApplyMapper = changeApplyMapper;
    this.extensionApplyMapper = extensionApplyMapper;
    this.transferApplyMapper = transferApplyMapper;
    this.objectMapper = objectMapper;
  }

  // ==================== Change Apply ====================

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createChangeApply(Long tenantId, Long applicantId, Long contractId,
      String changeType, String afterSnapshotJson, String reason) {
    Contract contract = requireEffectiveContract(contractId, tenantId);

    ContractChangeApply apply = new ContractChangeApply();
    apply.setTenantId(tenantId);
    apply.setChangeNo(generateNo("BG"));
    apply.setContractId(contractId);
    apply.setChangeType(changeType);
    apply.setBeforeSnapshotJson(serializeContract(contract));
    apply.setAfterSnapshotJson(afterSnapshotJson);
    apply.setReason(reason);
    apply.setApprovalStatus(DRAFT);
    apply.setApplicantId(applicantId);
    changeApplyMapper.insert(apply);
    return apply.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createSiteChangeApply(Long tenantId, Long applicantId, Long contractId,
      Long newSiteId, String reason) {
    Contract contract = requireEffectiveContract(contractId, tenantId);

    ContractChangeApply apply = new ContractChangeApply();
    apply.setTenantId(tenantId);
    apply.setChangeNo(generateNo("BG"));
    apply.setContractId(contractId);
    apply.setChangeType("SITE_CHANGE");
    apply.setBeforeSnapshotJson(serializeContract(contract));
    apply.setOriginalSiteId(contract.getSiteId());
    apply.setNewSiteId(newSiteId);
    apply.setReason(reason);
    apply.setApprovalStatus(DRAFT);
    apply.setApplicantId(applicantId);
    changeApplyMapper.insert(apply);
    return apply.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createVolumeChangeApply(Long tenantId, Long applicantId, Long contractId,
      BigDecimal newVolume, String reason) {
    Contract contract = requireEffectiveContract(contractId, tenantId);
    BigDecimal originalVolume = contract.getAgreedVolume() != null ? contract.getAgreedVolume() : ZERO;
    BigDecimal volumeDelta = newVolume.subtract(originalVolume);

    ContractChangeApply apply = new ContractChangeApply();
    apply.setTenantId(tenantId);
    apply.setChangeNo(generateNo("BG"));
    apply.setContractId(contractId);
    apply.setChangeType("VOLUME_CHANGE");
    apply.setBeforeSnapshotJson(serializeContract(contract));
    apply.setOriginalVolume(originalVolume);
    apply.setNewVolume(newVolume);
    apply.setVolumeDelta(volumeDelta);
    apply.setReason(reason);
    apply.setApprovalStatus(DRAFT);
    apply.setApplicantId(applicantId);
    changeApplyMapper.insert(apply);
    return apply.getId();
  }

  @Override
  public IPage<ContractChangeApply> pageChangeApplies(Long tenantId, Long contractId,
      String approvalStatus, int pageNo, int pageSize) {
    LambdaQueryWrapper<ContractChangeApply> query = new LambdaQueryWrapper<>();
    query.eq(ContractChangeApply::getTenantId, tenantId);
    if (contractId != null) {
      query.eq(ContractChangeApply::getContractId, contractId);
    }
    if (StringUtils.hasText(approvalStatus)) {
      query.eq(ContractChangeApply::getApprovalStatus, approvalStatus.trim());
    }
    query.orderByDesc(ContractChangeApply::getId);
    return changeApplyMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public ContractChangeApply getChangeApply(Long applyId, Long tenantId) {
    ContractChangeApply apply = changeApplyMapper.selectById(applyId);
    if (apply == null || !tenantId.equals(apply.getTenantId())) {
      throw new ContractServiceException(404, "变更申请不存在");
    }
    return apply;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void submitChangeApply(Long applyId, Long tenantId) {
    ContractChangeApply apply = getChangeApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), DRAFT, "提交");
    ContractChangeApply update = new ContractChangeApply();
    update.setId(applyId);
    update.setApprovalStatus(APPROVING);
    changeApplyMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void approveChangeApply(Long applyId, Long tenantId) {
    ContractChangeApply apply = getChangeApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "审批通过");

    ContractChangeApply applyUpdate = new ContractChangeApply();
    applyUpdate.setId(applyId);
    applyUpdate.setApprovalStatus(APPROVED);
    changeApplyMapper.updateById(applyUpdate);

    applyChangeToContract(apply);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void rejectChangeApply(Long applyId, Long tenantId, String reason) {
    ContractChangeApply apply = getChangeApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "驳回");
    ContractChangeApply update = new ContractChangeApply();
    update.setId(applyId);
    update.setApprovalStatus(REJECTED);
    update.setReason(buildRejectReason(apply.getReason(), reason));
    changeApplyMapper.updateById(update);
  }

  // ==================== Extension Apply ====================

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createExtensionApply(Long tenantId, Long applicantId, Long contractId,
      LocalDate requestedExpireDate, BigDecimal requestedVolumeDelta, String reason) {
    Contract contract = requireEffectiveContract(contractId, tenantId);
    LocalDate originalExpireDate = contract.getExpireDate();
    if (originalExpireDate != null && !requestedExpireDate.isAfter(originalExpireDate)) {
      throw new ContractServiceException(400, "申请到期日期必须晚于原到期日期");
    }

    ContractExtensionApply apply = new ContractExtensionApply();
    apply.setTenantId(tenantId);
    apply.setApplyNo(generateNo("YQ"));
    apply.setContractId(contractId);
    apply.setOriginalExpireDate(originalExpireDate);
    apply.setRequestedExpireDate(requestedExpireDate);
    apply.setRequestedVolumeDelta(requestedVolumeDelta);
    apply.setReason(reason);
    apply.setApprovalStatus(DRAFT);
    apply.setApplicantId(applicantId);
    extensionApplyMapper.insert(apply);
    return apply.getId();
  }

  @Override
  public IPage<ContractExtensionApply> pageExtensionApplies(Long tenantId, Long contractId,
      String approvalStatus, int pageNo, int pageSize) {
    LambdaQueryWrapper<ContractExtensionApply> query = new LambdaQueryWrapper<>();
    query.eq(ContractExtensionApply::getTenantId, tenantId);
    if (contractId != null) {
      query.eq(ContractExtensionApply::getContractId, contractId);
    }
    if (StringUtils.hasText(approvalStatus)) {
      query.eq(ContractExtensionApply::getApprovalStatus, approvalStatus.trim());
    }
    query.orderByDesc(ContractExtensionApply::getId);
    return extensionApplyMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public ContractExtensionApply getExtensionApply(Long applyId, Long tenantId) {
    ContractExtensionApply apply = extensionApplyMapper.selectById(applyId);
    if (apply == null || !tenantId.equals(apply.getTenantId())) {
      throw new ContractServiceException(404, "延期申请不存在");
    }
    return apply;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void submitExtensionApply(Long applyId, Long tenantId) {
    ContractExtensionApply apply = getExtensionApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), DRAFT, "提交");
    ContractExtensionApply update = new ContractExtensionApply();
    update.setId(applyId);
    update.setApprovalStatus(APPROVING);
    extensionApplyMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void approveExtensionApply(Long applyId, Long tenantId) {
    ContractExtensionApply apply = getExtensionApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "审批通过");

    ContractExtensionApply applyUpdate = new ContractExtensionApply();
    applyUpdate.setId(applyId);
    applyUpdate.setApprovalStatus(APPROVED);
    extensionApplyMapper.updateById(applyUpdate);

    Contract contractUpdate = new Contract();
    contractUpdate.setId(apply.getContractId());
    contractUpdate.setExpireDate(apply.getRequestedExpireDate());
    if (apply.getRequestedVolumeDelta() != null
        && apply.getRequestedVolumeDelta().compareTo(ZERO) != 0) {
      Contract current = contractMapper.selectById(apply.getContractId());
      BigDecimal currentVolume = current.getAgreedVolume() != null ? current.getAgreedVolume() : ZERO;
      contractUpdate.setAgreedVolume(currentVolume.add(apply.getRequestedVolumeDelta()));
    }
    contractMapper.updateById(contractUpdate);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void rejectExtensionApply(Long applyId, Long tenantId, String reason) {
    ContractExtensionApply apply = getExtensionApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "驳回");
    ContractExtensionApply update = new ContractExtensionApply();
    update.setId(applyId);
    update.setApprovalStatus(REJECTED);
    update.setReason(buildRejectReason(apply.getReason(), reason));
    extensionApplyMapper.updateById(update);
  }

  // ==================== Transfer Apply ====================

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createTransferApply(Long tenantId, Long applicantId, Long sourceContractId,
      Long targetContractId, BigDecimal transferAmount, BigDecimal transferVolume, String reason) {
    if (sourceContractId.equals(targetContractId)) {
      throw new ContractServiceException(400, "源合同和目标合同不能相同");
    }
    Contract source = requireEffectiveContract(sourceContractId, tenantId);
    requireEffectiveContract(targetContractId, tenantId);

    if (transferAmount != null && transferAmount.compareTo(ZERO) > 0) {
      BigDecimal available = safeAmount(source.getContractAmount())
          .subtract(safeAmount(source.getSettledAmount()));
      if (transferAmount.compareTo(available) > 0) {
        throw new ContractServiceException(409, "源合同可转金额不足");
      }
    }
    if (transferVolume != null && transferVolume.compareTo(ZERO) > 0) {
      BigDecimal availableVolume = safeAmount(source.getAgreedVolume());
      if (transferVolume.compareTo(availableVolume) > 0) {
        throw new ContractServiceException(409, "源合同可转方量不足");
      }
    }

    ContractTransferApply apply = new ContractTransferApply();
    apply.setTenantId(tenantId);
    apply.setTransferNo(generateNo("NB"));
    apply.setSourceContractId(sourceContractId);
    apply.setTargetContractId(targetContractId);
    apply.setTransferAmount(transferAmount);
    apply.setTransferVolume(transferVolume);
    apply.setReason(reason);
    apply.setApprovalStatus(DRAFT);
    apply.setApplicantId(applicantId);
    transferApplyMapper.insert(apply);
    return apply.getId();
  }

  @Override
  public IPage<ContractTransferApply> pageTransferApplies(Long tenantId,
      String approvalStatus, int pageNo, int pageSize) {
    LambdaQueryWrapper<ContractTransferApply> query = new LambdaQueryWrapper<>();
    query.eq(ContractTransferApply::getTenantId, tenantId);
    if (StringUtils.hasText(approvalStatus)) {
      query.eq(ContractTransferApply::getApprovalStatus, approvalStatus.trim());
    }
    query.orderByDesc(ContractTransferApply::getId);
    return transferApplyMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public ContractTransferApply getTransferApply(Long applyId, Long tenantId) {
    ContractTransferApply apply = transferApplyMapper.selectById(applyId);
    if (apply == null || !tenantId.equals(apply.getTenantId())) {
      throw new ContractServiceException(404, "转移申请不存在");
    }
    return apply;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void submitTransferApply(Long applyId, Long tenantId) {
    ContractTransferApply apply = getTransferApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), DRAFT, "提交");
    ContractTransferApply update = new ContractTransferApply();
    update.setId(applyId);
    update.setApprovalStatus(APPROVING);
    transferApplyMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void approveTransferApply(Long applyId, Long tenantId) {
    ContractTransferApply apply = getTransferApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "审批通过");

    ContractTransferApply applyUpdate = new ContractTransferApply();
    applyUpdate.setId(applyId);
    applyUpdate.setApprovalStatus(APPROVED);
    transferApplyMapper.updateById(applyUpdate);

    Contract source = contractMapper.selectById(apply.getSourceContractId());
    Contract target = contractMapper.selectById(apply.getTargetContractId());

    Contract sourceUpdate = new Contract();
    sourceUpdate.setId(source.getId());
    Contract targetUpdate = new Contract();
    targetUpdate.setId(target.getId());

    if (apply.getTransferAmount() != null && apply.getTransferAmount().compareTo(ZERO) > 0) {
      sourceUpdate.setContractAmount(
          safeAmount(source.getContractAmount()).subtract(apply.getTransferAmount()));
      targetUpdate.setContractAmount(
          safeAmount(target.getContractAmount()).add(apply.getTransferAmount()));
    }
    if (apply.getTransferVolume() != null && apply.getTransferVolume().compareTo(ZERO) > 0) {
      sourceUpdate.setAgreedVolume(
          safeAmount(source.getAgreedVolume()).subtract(apply.getTransferVolume()));
      targetUpdate.setAgreedVolume(
          safeAmount(target.getAgreedVolume()).add(apply.getTransferVolume()));
    }

    contractMapper.updateById(sourceUpdate);
    contractMapper.updateById(targetUpdate);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void rejectTransferApply(Long applyId, Long tenantId, String reason) {
    ContractTransferApply apply = getTransferApply(applyId, tenantId);
    requireStatus(apply.getApprovalStatus(), APPROVING, "驳回");
    ContractTransferApply update = new ContractTransferApply();
    update.setId(applyId);
    update.setApprovalStatus(REJECTED);
    update.setReason(buildRejectReason(apply.getReason(), reason));
    transferApplyMapper.updateById(update);
  }

  // ==================== Private helpers ====================

  private Contract requireEffectiveContract(Long contractId, Long tenantId) {
    Contract contract = contractMapper.selectById(contractId);
    if (contract == null || !tenantId.equals(contract.getTenantId())) {
      throw new ContractServiceException(404, "合同不存在");
    }
    String status = contract.getContractStatus();
    if (status == null || !ACTIVE_CONTRACT_STATUSES.contains(status.toUpperCase())) {
      throw new ContractServiceException(409, "仅生效中的合同允许此操作");
    }
    return contract;
  }

  private void requireStatus(String current, String expected, String action) {
    if (!expected.equals(current)) {
      throw new ContractServiceException(409,
          "当前状态为 " + current + "，无法执行" + action + "操作");
    }
  }

  @SuppressWarnings("unchecked")
  private void applyChangeToContract(ContractChangeApply apply) {
    Contract update = new Contract();
    update.setId(apply.getContractId());

    String changeType = apply.getChangeType();
    if ("SITE_CHANGE".equals(changeType) && apply.getNewSiteId() != null) {
      update.setSiteId(apply.getNewSiteId());
    } else if ("VOLUME_CHANGE".equals(changeType) && apply.getNewVolume() != null) {
      update.setAgreedVolume(apply.getNewVolume());
    } else {
      String json = apply.getAfterSnapshotJson();
      if (StringUtils.hasText(json)) {
        try {
          Map<String, Object> snapshot = objectMapper.readValue(json, Map.class);
          if (snapshot.containsKey("agreedVolume")) {
            update.setAgreedVolume(new BigDecimal(String.valueOf(snapshot.get("agreedVolume"))));
          }
          if (snapshot.containsKey("unitPrice")) {
            update.setUnitPrice(new BigDecimal(String.valueOf(snapshot.get("unitPrice"))));
          }
          if (snapshot.containsKey("contractAmount")) {
            update.setContractAmount(new BigDecimal(String.valueOf(snapshot.get("contractAmount"))));
          }
          if (snapshot.containsKey("siteId")) {
            update.setSiteId(Long.parseLong(String.valueOf(snapshot.get("siteId"))));
          }
          if (snapshot.containsKey("expireDate")) {
            update.setExpireDate(LocalDate.parse(String.valueOf(snapshot.get("expireDate"))));
          }
          if (snapshot.containsKey("remark")) {
            update.setRemark(String.valueOf(snapshot.get("remark")));
          }
        } catch (ContractServiceException e) {
          throw e;
        } catch (Exception e) {
          throw new ContractServiceException(500, "解析变更快照失败: " + e.getMessage());
        }
      }
    }

    Contract current = contractMapper.selectById(apply.getContractId());
    Integer version = current.getChangeVersion() != null ? current.getChangeVersion() : 0;
    update.setChangeVersion(version + 1);

    contractMapper.updateById(update);
  }

  private String serializeContract(Contract contract) {
    try {
      return objectMapper.writeValueAsString(contract);
    } catch (Exception e) {
      throw new ContractServiceException(500, "序列化合同快照失败");
    }
  }

  private String generateNo(String prefix) {
    String timePart = LocalDateTime.now().format(NO_TIME_FMT);
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return prefix + timePart + random;
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private String buildRejectReason(String originalReason, String rejectReason) {
    if (!StringUtils.hasText(rejectReason)) {
      return originalReason;
    }
    if (!StringUtils.hasText(originalReason)) {
      return "驳回原因：" + rejectReason.trim();
    }
    return originalReason + " | 驳回原因：" + rejectReason.trim();
  }
}
