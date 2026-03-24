package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractMaterial;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ContractService {

  Contract getContract(Long contractId, Long tenantId);

  List<Contract> listContractsByIds(Collection<Long> contractIds, Long tenantId);

  IPage<ContractReceipt> pageReceipts(
      Long tenantId,
      Long contractId,
      String keyword,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      int pageNo,
      int pageSize);

  List<ContractReceipt> listReceiptsByContract(Long contractId, Long tenantId);

  ContractReceipt getReceipt(Long receiptId, Long tenantId);

  long createReceipt(
      Long contractId, Long operatorId, Long operatorTenantId, CreateContractReceiptCommand command);

  long cancelReceipt(Long receiptId, Long operatorId, Long operatorTenantId, String cancelRemark);

  IPage<Contract> pageContracts(Long tenantId, String contractType, String contractStatus,
      String keyword, Long projectId, Long siteId, LocalDate startDate, LocalDate endDate,
      int pageNo, int pageSize);

  IPage<Contract> pageContractsAdvanced(Long tenantId, ContractQueryParams params);

  ContractDetailVo getContractDetail(Long contractId, Long tenantId);

  List<ContractApprovalRecordVo> getContractApprovalRecords(Long contractId, Long tenantId);

  List<ContractMaterialVo> getContractMaterials(Long contractId, Long tenantId);

  ContractMaterial getContractMaterial(Long materialId, Long contractId, Long tenantId);

  List<ContractInvoiceVo> getContractInvoices(Long contractId, Long tenantId);

  List<ContractTicketVo> getContractTickets(Long contractId, Long tenantId);

  long createContract(Long tenantId, Long applicantId, Contract contract);

  void updateContract(Long contractId, Long tenantId, Contract updates);

  void submitContract(Long contractId, Long tenantId, Long operatorId);

  void approveContract(Long contractId, Long tenantId, Long operatorId);

  void rejectContract(Long contractId, Long tenantId, Long operatorId, String reason);

  ContractStatsResult getContractStats(Long tenantId);
}
