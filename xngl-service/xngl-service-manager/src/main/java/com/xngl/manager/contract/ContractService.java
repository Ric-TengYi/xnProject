package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
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
}
