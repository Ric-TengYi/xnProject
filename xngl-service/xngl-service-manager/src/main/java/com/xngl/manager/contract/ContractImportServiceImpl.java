package com.xngl.manager.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private static final TypeReference<List<Map<String, String>>> ROW_LIST_TYPE = new TypeReference<>() {};
  private static final String IMPORT_DIR = "xngl-imports/contracts";

  private final ContractImportBatchMapper batchMapper;
  private final ContractImportErrorMapper errorMapper;
  private final ContractMapper contractMapper;
  private final ObjectMapper objectMapper;

  public ContractImportServiceImpl(
      ContractImportBatchMapper batchMapper,
      ContractImportErrorMapper errorMapper,
      ContractMapper contractMapper,
      ObjectMapper objectMapper) {
    this.batchMapper = batchMapper;
    this.errorMapper = errorMapper;
    this.contractMapper = contractMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ContractImportBatch previewImport(Long tenantId, Long operatorId, String fileName, List<Map<String, String>> rows) {
    String batchNo = generateBatchNo();
    int totalCount = rows.size();
    List<ContractImportError> errors = new ArrayList<>();
    Set<Integer> errorRows = new HashSet<>();

    Set<String> existingNos = loadExistingContractNos(tenantId);
    Set<String> seenNos = new HashSet<>();

    for (int i = 0; i < rows.size(); i++) {
      Map<String, String> row = sanitizeRow(rows.get(i));
      int rowNo = i + 1;
      String contractNo = row.get("contractNo");
      String rawJson = toRawJson(row);

      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "contractNo", "MISSING_CONTRACT_NO", "合同编号不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "name", "MISSING_NAME", "合同名称不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "contractType", "MISSING_CONTRACT_TYPE", "合同类型不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "projectId", "MISSING_PROJECT_ID", "项目ID不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "siteId", "MISSING_SITE_ID", "场地ID不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "constructionOrgId", "MISSING_CONSTRUCTION_ORG_ID", "建设单位ID不能为空");
      validateRequired(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row, "transportOrgId", "MISSING_TRANSPORT_ORG_ID", "运输单位ID不能为空");

      if (StringUtils.hasText(contractNo) && (existingNos.contains(contractNo) || seenNos.contains(contractNo))) {
        errors.add(buildError(tenantId, null, rowNo, contractNo, "DUPLICATE_CONTRACT_NO", "合同编号重复", rawJson));
        errorRows.add(rowNo);
      }
      if (StringUtils.hasText(contractNo)) {
        seenNos.add(contractNo);
      }

      validateLong(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("projectId"), "INVALID_PROJECT_ID", "项目ID格式错误");
      validateLong(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("siteId"), "INVALID_SITE_ID", "场地ID格式错误");
      validateLong(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("constructionOrgId"), "INVALID_CONSTRUCTION_ORG_ID", "建设单位ID格式错误");
      validateLong(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("transportOrgId"), "INVALID_TRANSPORT_ORG_ID", "运输单位ID格式错误");
      validateLong(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("partyId"), "INVALID_PARTY_ID", "三方单位ID格式错误");
      validateDecimal(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("contractAmount"), "INVALID_AMOUNT", "合同金额格式错误");
      validateDecimal(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("agreedVolume"), "INVALID_VOLUME", "约定方量格式错误");
      validateDecimal(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("unitPrice"), "INVALID_UNIT_PRICE", "合同单价格式错误");
      validateDecimal(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("unitPriceInside"), "INVALID_UNIT_PRICE_INSIDE", "区内单价格式错误");
      validateDecimal(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("unitPriceOutside"), "INVALID_UNIT_PRICE_OUTSIDE", "区外单价格式错误");
      validateDate(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("signDate"), "INVALID_SIGN_DATE", "签订日期格式错误，应为 YYYY-MM-DD");
      validateDate(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("effectiveDate"), "INVALID_EFFECTIVE_DATE", "生效日期格式错误，应为 YYYY-MM-DD");
      validateDate(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("expireDate"), "INVALID_EXPIRE_DATE", "到期日期格式错误，应为 YYYY-MM-DD");
      validateBoolean(errors, errorRows, tenantId, rowNo, contractNo, rawJson, row.get("isThreeParty"), "INVALID_THREE_PARTY", "三方合同标记格式错误，应为 true/false/是/否/1/0");
    }

    ContractImportBatch batch = new ContractImportBatch();
    batch.setTenantId(tenantId);
    batch.setBatchNo(batchNo);
    batch.setFileName(StringUtils.hasText(fileName) ? fileName : batchNo + ".csv");
    batch.setTotalCount(totalCount);
    batch.setSuccessCount(totalCount - errorRows.size());
    batch.setFailCount(errorRows.size());
    batch.setStatus("PREVIEWED");
    batch.setOperatorId(operatorId);
    batchMapper.insert(batch);

    batch.setFileUrl(writeRawRows(batchNo, batch.getId(), rows));
    batchMapper.updateById(batch);

    for (ContractImportError error : errors) {
      error.setBatchId(batch.getId());
      errorMapper.insert(error);
    }

    return batch;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ContractImportCommitResult commitImport(Long batchId, Long tenantId) {
    ContractImportBatch batch = getBatchDetail(batchId, tenantId);
    if (!"PREVIEWED".equals(batch.getStatus())) {
      throw new ContractServiceException(409, "仅预览状态的导入批次可以提交");
    }
    List<ContractImportError> errors = listBatchErrors(batchId, tenantId);
    Set<Integer> errorRows = new HashSet<>();
    for (ContractImportError error : errors) {
      errorRows.add(error.getRowNo());
    }

    List<Map<String, String>> rows = loadRawRows(batch);
    Set<String> existingNos = loadExistingContractNos(tenantId);
    int successCount = 0;
    Set<Integer> failedRowSet = new HashSet<>(errorRows);
    for (int i = 0; i < rows.size(); i++) {
      int rowNo = i + 1;
      if (failedRowSet.contains(rowNo)) {
        continue;
      }
      Map<String, String> row = sanitizeRow(rows.get(i));
      String contractNo = row.get("contractNo");
      String rawJson = toRawJson(row);
      if (StringUtils.hasText(contractNo) && existingNos.contains(contractNo)) {
        errorMapper.insert(buildError(tenantId, batchId, rowNo, contractNo, "DUPLICATE_CONTRACT_NO", "提交时合同编号已存在", rawJson));
        failedRowSet.add(rowNo);
        continue;
      }
      Contract contract = toContract(tenantId, batch.getOperatorId(), row);
      contractMapper.insert(contract);
      if (StringUtils.hasText(contractNo)) {
        existingNos.add(contractNo);
      }
      successCount++;
    }
    int failCount = failedRowSet.size();

    ContractImportBatch update = new ContractImportBatch();
    update.setId(batchId);
    update.setStatus("COMMITTED");
    update.setSuccessCount(successCount);
    update.setFailCount(failCount);
    batchMapper.updateById(update);

    ContractImportCommitResult result = new ContractImportCommitResult();
    result.setBatchId(batchId);
    result.setSuccessCount(successCount);
    result.setFailCount(failCount);
    result.setStatus("COMMITTED");
    return result;
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

  private void validateRequired(
      List<ContractImportError> errors,
      Set<Integer> errorRows,
      Long tenantId,
      int rowNo,
      String contractNo,
      String rawJson,
      Map<String, String> row,
      String field,
      String code,
      String message) {
    if (!StringUtils.hasText(row.get(field))) {
      errors.add(buildError(tenantId, null, rowNo, contractNo, code, message, rawJson));
      errorRows.add(rowNo);
    }
  }

  private void validateLong(
      List<ContractImportError> errors,
      Set<Integer> errorRows,
      Long tenantId,
      int rowNo,
      String contractNo,
      String rawJson,
      String value,
      String code,
      String message) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    try {
      Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      errors.add(buildError(tenantId, null, rowNo, contractNo, code, message, rawJson));
      errorRows.add(rowNo);
    }
  }

  private void validateDecimal(
      List<ContractImportError> errors,
      Set<Integer> errorRows,
      Long tenantId,
      int rowNo,
      String contractNo,
      String rawJson,
      String value,
      String code,
      String message) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    try {
      new BigDecimal(value.trim());
    } catch (NumberFormatException ex) {
      errors.add(buildError(tenantId, null, rowNo, contractNo, code, message, rawJson));
      errorRows.add(rowNo);
    }
  }

  private void validateDate(
      List<ContractImportError> errors,
      Set<Integer> errorRows,
      Long tenantId,
      int rowNo,
      String contractNo,
      String rawJson,
      String value,
      String code,
      String message) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    try {
      LocalDate.parse(value.trim());
    } catch (Exception ex) {
      errors.add(buildError(tenantId, null, rowNo, contractNo, code, message, rawJson));
      errorRows.add(rowNo);
    }
  }

  private void validateBoolean(
      List<ContractImportError> errors,
      Set<Integer> errorRows,
      Long tenantId,
      int rowNo,
      String contractNo,
      String rawJson,
      String value,
      String code,
      String message) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    try {
      parseBoolean(value);
    } catch (ContractServiceException ex) {
      errors.add(buildError(tenantId, null, rowNo, contractNo, code, message, rawJson));
      errorRows.add(rowNo);
    }
  }

  private Map<String, String> sanitizeRow(Map<String, String> source) {
    java.util.LinkedHashMap<String, String> target = new java.util.LinkedHashMap<>();
    for (Map.Entry<String, String> entry : source.entrySet()) {
      target.put(entry.getKey(), entry.getValue() != null ? entry.getValue().trim() : null);
    }
    return target;
  }

  private String writeRawRows(String batchNo, Long batchId, List<Map<String, String>> rows) {
    try {
      Path dir = Paths.get(System.getProperty("java.io.tmpdir"), IMPORT_DIR);
      Files.createDirectories(dir);
      Path path = dir.resolve(batchNo + "-" + batchId + ".json");
      objectMapper.writeValue(path.toFile(), rows);
      return path.toString();
    } catch (Exception ex) {
      throw new ContractServiceException(500, "导入原始数据落盘失败");
    }
  }

  private List<Map<String, String>> loadRawRows(ContractImportBatch batch) {
    if (!StringUtils.hasText(batch.getFileUrl())) {
      throw new ContractServiceException(500, "导入批次原始数据不存在");
    }
    try {
      return objectMapper.readValue(Path.of(batch.getFileUrl()).toFile(), ROW_LIST_TYPE);
    } catch (Exception ex) {
      throw new ContractServiceException(500, "读取导入原始数据失败");
    }
  }

  private String toRawJson(Map<String, String> row) {
    try {
      return objectMapper.writeValueAsString(row);
    } catch (Exception ex) {
      return "{}";
    }
  }

  private Contract toContract(Long tenantId, Long operatorId, Map<String, String> row) {
    Contract contract = new Contract();
    contract.setTenantId(tenantId);
    contract.setApplicantId(operatorId);
    contract.setContractNo(row.get("contractNo"));
    contract.setName(row.get("name"));
    contract.setContractType(row.get("contractType"));
    contract.setProjectId(parseLongOrNull(row.get("projectId")));
    contract.setSiteId(parseLongOrNull(row.get("siteId")));
    contract.setConstructionOrgId(parseLongOrNull(row.get("constructionOrgId")));
    contract.setTransportOrgId(parseLongOrNull(row.get("transportOrgId")));
    contract.setPartyId(parseLongOrNull(row.get("partyId")));
    contract.setSignDate(parseDateOrNull(row.get("signDate")));
    contract.setEffectiveDate(parseDateOrNull(row.get("effectiveDate")));
    contract.setExpireDate(parseDateOrNull(row.get("expireDate")));
    contract.setAgreedVolume(parseDecimalOrNull(row.get("agreedVolume")));
    contract.setUnitPrice(parseDecimalOrDefault(row.get("unitPrice"), ZERO));
    contract.setUnitPriceInside(parseDecimalOrNull(row.get("unitPriceInside")));
    contract.setUnitPriceOutside(parseDecimalOrNull(row.get("unitPriceOutside")));
    contract.setContractAmount(resolveContractAmount(row, contract));
    contract.setReceivedAmount(ZERO);
    contract.setSettledAmount(ZERO);
    contract.setChangeVersion(0);
    contract.setApprovalStatus(defaultValue(row.get("approvalStatus"), "APPROVED"));
    contract.setContractStatus(defaultValue(row.get("contractStatus"), "EFFECTIVE"));
    contract.setSourceType(defaultValue(row.get("sourceType"), "IMPORT"));
    contract.setIsThreeParty(parseBooleanOrDefault(row.get("isThreeParty"), false));
    contract.setRemark(row.get("remark"));
    contract.setRejectReason(row.get("rejectReason"));
    return contract;
  }

  private BigDecimal resolveContractAmount(Map<String, String> row, Contract contract) {
    BigDecimal explicitAmount = parseDecimalOrNull(row.get("contractAmount"));
    if (explicitAmount != null) {
      return explicitAmount;
    }
    if (contract.getAgreedVolume() != null && contract.getUnitPrice() != null) {
      return contract.getAgreedVolume().multiply(contract.getUnitPrice());
    }
    return ZERO;
  }

  private Long parseLongOrNull(String value) {
    return StringUtils.hasText(value) ? Long.parseLong(value.trim()) : null;
  }

  private LocalDate parseDateOrNull(String value) {
    return StringUtils.hasText(value) ? LocalDate.parse(value.trim()) : null;
  }

  private BigDecimal parseDecimalOrNull(String value) {
    return StringUtils.hasText(value) ? new BigDecimal(value.trim()) : null;
  }

  private BigDecimal parseDecimalOrDefault(String value, BigDecimal defaultValue) {
    return StringUtils.hasText(value) ? new BigDecimal(value.trim()) : defaultValue;
  }

  private Boolean parseBooleanOrDefault(String value, boolean defaultValue) {
    return StringUtils.hasText(value) ? parseBoolean(value) : defaultValue;
  }

  private Boolean parseBoolean(String value) {
    String normalized = value.trim().toLowerCase();
    if ("true".equals(normalized) || "1".equals(normalized) || "是".equals(value.trim()) || "y".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized) || "0".equals(normalized) || "否".equals(value.trim()) || "n".equals(normalized)) {
      return false;
    }
    throw new ContractServiceException(400, "布尔值格式错误");
  }

  private String defaultValue(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  private ContractImportError buildError(Long tenantId, Long batchId, int rowNo, String contractNo, String code, String message, String rawJson) {
    ContractImportError error = new ContractImportError();
    error.setTenantId(tenantId);
    error.setBatchId(batchId);
    error.setRowNo(rowNo);
    error.setContractNo(contractNo);
    error.setErrorCode(code);
    error.setErrorMessage(message);
    error.setRawJson(rawJson);
    return error;
  }

  private String generateBatchNo() {
    String timePart = LocalDateTime.now().format(BATCH_NO_TIME);
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "IM" + timePart + random;
  }
}
