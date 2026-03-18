package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.ContractChangeApply;
import com.xngl.infrastructure.persistence.entity.contract.ContractExtensionApply;
import com.xngl.infrastructure.persistence.entity.contract.ContractTransferApply;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface ContractApplyService {

  // Change
  long createChangeApply(Long tenantId, Long applicantId, Long contractId,
      String changeType, String afterSnapshotJson, String reason);

  IPage<ContractChangeApply> pageChangeApplies(Long tenantId, Long contractId,
      String approvalStatus, int pageNo, int pageSize);

  ContractChangeApply getChangeApply(Long applyId, Long tenantId);

  void submitChangeApply(Long applyId, Long tenantId);

  void approveChangeApply(Long applyId, Long tenantId);

  void rejectChangeApply(Long applyId, Long tenantId, String reason);

  // Extension
  long createExtensionApply(Long tenantId, Long applicantId, Long contractId,
      LocalDate requestedExpireDate, BigDecimal requestedVolumeDelta, String reason);

  IPage<ContractExtensionApply> pageExtensionApplies(Long tenantId, Long contractId,
      String approvalStatus, int pageNo, int pageSize);

  ContractExtensionApply getExtensionApply(Long applyId, Long tenantId);

  void submitExtensionApply(Long applyId, Long tenantId);

  void approveExtensionApply(Long applyId, Long tenantId);

  void rejectExtensionApply(Long applyId, Long tenantId, String reason);

  // Transfer
  long createTransferApply(Long tenantId, Long applicantId, Long sourceContractId,
      Long targetContractId, BigDecimal transferAmount, BigDecimal transferVolume, String reason);

  IPage<ContractTransferApply> pageTransferApplies(Long tenantId,
      String approvalStatus, int pageNo, int pageSize);

  ContractTransferApply getTransferApply(Long applyId, Long tenantId);

  void submitTransferApply(Long applyId, Long tenantId);

  void approveTransferApply(Long applyId, Long tenantId);

  void rejectTransferApply(Long applyId, Long tenantId, String reason);
}
