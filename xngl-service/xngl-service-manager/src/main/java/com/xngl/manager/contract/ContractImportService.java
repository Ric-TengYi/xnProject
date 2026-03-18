package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportBatch;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportError;
import java.util.List;
import java.util.Map;

public interface ContractImportService {

  ContractImportBatch previewImport(Long tenantId, Long operatorId, String fileName, List<Map<String, String>> rows);

  void commitImport(Long batchId, Long tenantId);

  IPage<ContractImportBatch> pageBatches(Long tenantId, String status, int pageNo, int pageSize);

  ContractImportBatch getBatchDetail(Long batchId, Long tenantId);

  List<ContractImportError> listBatchErrors(Long batchId, Long tenantId);
}
