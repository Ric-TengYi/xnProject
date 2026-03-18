package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportBatch;
import com.xngl.infrastructure.persistence.entity.contract.ContractImportError;
import com.xngl.infrastructure.persistence.mapper.ContractImportBatchMapper;
import com.xngl.infrastructure.persistence.mapper.ContractImportErrorMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractImportServiceImpl implements ContractImportService {

  private static final DateTimeFormatter BATCH_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final ContractImportBatchMapper batchMapper;
  private final ContractImportErrorMapper errorMapper;
  private final ContractMapper contractMapper;

  public ContractImportServiceImpl(
      ContractImportBatchMapper batchMapper,
      ContractImportErrorMapper errorMapper,
      ContractMapper contractMapper) {
    this.batchMapper = batchMapper;
    this.errorMapper = errorMapper;
    this.contractMapper = contractMapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ContractImportBatch previewImport(Long tenantId, Long operatorId, String fileName, List<Map<String, String>> rows) {
    String batchNo = generateBatchNo();
    int totalCount = rows.size();
    List<ContractImportError> errors = new ArrayList<>();

    Set<String> existingNos = loadExistingContractNos(tenantId);
    Set<String> seenNos = new HashSet<>();

    for (int i = 0; i < rows.size(); i++) {
      Map<String, String> row = rows.get(i);
      int rowNo = i + 1;
      String contractNo = row.get("contractNo");

      if (!StringUtils.hasText(contractNo)) {
        errors.add(buildError(tenantId, null, rowNo, contractNo, "MISSING_CONTRACT_NO", "合同编号不能为空"));
        continue;
      }
      if (existingNos.contains(contractNo) || seenNos.contains(contractNo)) {
        errors.add(buildError(tenantId, null, rowNo, contractNo, "DUPLICATE_CONTRACT_NO", "合同编号重复"));
        continue;
      }
      seenNos.add(contractNo);

      if (StringUtils.hasText(row.get("contractAmount"))) {
        try {
          new BigDecimal(row.get("contractAmount"));
        } catch (NumberFormatException e) {
          errors.add(buildError(tenantId, null, rowNo, contractNo, "INVALID_AMOUNT", "合同金额格式错误"));
          continue;
        }
      }
      if (StringUtils.hasText(row.get("agreedVolume"))) {
        try {
          new BigDecimal(row.get("agreedVolume"));
        } catch (NumberFormatException e) {
          errors.add(buildError(tenantId, null, rowNo, contractNo, "INVALID_VOLUME", "约定量格式错误"));
        }
      }
    }

    ContractImportBatch batch = new ContractImportBatch();
    batch.setTenantId(tenantId);
    batch.setBatchNo(batchNo);
    batch.setFileName(fileName);
    batch.setTotalCount(totalCount);
    batch.setSuccessCount(totalCount - errors.size());
    batch.setFailCount(errors.size());
    batch.setStatus("PREVIEWED");
    batch.setOperatorId(operatorId);
    batchMapper.insert(batch);

    for (ContractImportError error : errors) {
      error.setBatchId(batch.getId());
      errorMapper.insert(error);
    }

    return batch;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void commitImport(Long batchId, Long tenantId) {
    ContractImportBatch batch = getBatchDetail(batchId, tenantId);
    if (!"PREVIEWED".equals(batch.getStatus())) {
      throw new ContractServiceException(409, "仅预览状态的导入批次可以提交");
    }

    List<ContractImportError> errors = listBatchErrors(batchId, tenantId);
    Set<String> errorContractNos = new HashSet<>();
    for (ContractImportError error : errors) {
      if (StringUtils.hasText(error.getContractNo())) {
        errorContractNos.add(error.getContractNo());
      }
    }

    // NOTE: In a real implementation, we'd re-read the raw rows from storage.
    // For now, the batch commit marks the batch as COMMITTED.
    int successCount = batch.getSuccessCount() != null ? batch.getSuccessCount() : 0;
    int failCount = batch.getFailCount() != null ? batch.getFailCount() : 0;

    ContractImportBatch update = new ContractImportBatch();
    update.setId(batchId);
    update.setStatus("COMMITTED");
    update.setSuccessCount(successCount);
    update.setFailCount(failCount);
    batchMapper.updateById(update);
  }

  @Override
  public IPage<ContractImportBatch> pageBatches(Long tenantId, String status, int pageNo, int pageSize) {
    LambdaQueryWrapper<ContractImportBatch> query = new LambdaQueryWrapper<>();
    query.eq(ContractImportBatch::getTenantId, tenantId);
    if (StringUtils.hasText(status)) {
      query.eq(ContractImportBatch::getStatus, status.trim());
    }
    query.orderByDesc(ContractImportBatch::getCreateTime);
    return batchMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public ContractImportBatch getBatchDetail(Long batchId, Long tenantId) {
    ContractImportBatch batch = batchMapper.selectById(batchId);
    if (batch == null || !tenantId.equals(batch.getTenantId())) {
      throw new ContractServiceException(404, "导入批次不存在");
    }
    return batch;
  }

  @Override
  public List<ContractImportError> listBatchErrors(Long batchId, Long tenantId) {
    LambdaQueryWrapper<ContractImportError> query = new LambdaQueryWrapper<>();
    query.eq(ContractImportError::getBatchId, batchId);
    query.eq(ContractImportError::getTenantId, tenantId);
    query.orderByAsc(ContractImportError::getRowNo);
    return errorMapper.selectList(query);
  }

  private Set<String> loadExistingContractNos(Long tenantId) {
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    query.eq(Contract::getTenantId, tenantId);
    query.select(Contract::getContractNo);
    List<Contract> contracts = contractMapper.selectList(query);
    Set<String> nos = new HashSet<>();
    for (Contract c : contracts) {
      if (StringUtils.hasText(c.getContractNo())) {
        nos.add(c.getContractNo());
      }
    }
    return nos;
  }

  private ContractImportError buildError(Long tenantId, Long batchId, int rowNo, String contractNo, String code, String message) {
    ContractImportError error = new ContractImportError();
    error.setTenantId(tenantId);
    error.setBatchId(batchId);
    error.setRowNo(rowNo);
    error.setContractNo(contractNo);
    error.setErrorCode(code);
    error.setErrorMessage(message);
    return error;
  }

  private String generateBatchNo() {
    String timePart = LocalDateTime.now().format(BATCH_NO_TIME);
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "IM" + timePart + random;
  }
}
