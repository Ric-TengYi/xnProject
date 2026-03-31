package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContractExportFileService {

  private static final int EXPORT_PAGE_SIZE = 500;
  private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final String EXPORT_DIR = "xngl-exports/contracts";

  private final ContractService contractService;
  private final ExportTaskService exportTaskService;
  private final ProjectMapper projectMapper;
  private final SiteMapper siteMapper;
  private final OrgMapper orgMapper;

  public ContractExportFileService(
      ContractService contractService,
      ExportTaskService exportTaskService,
      ProjectMapper projectMapper,
      SiteMapper siteMapper,
      OrgMapper orgMapper) {
    this.contractService = contractService;
    this.exportTaskService = exportTaskService;
    this.projectMapper = projectMapper;
    this.siteMapper = siteMapper;
    this.orgMapper = orgMapper;
  }

  public void generateContractCsv(
      Long taskId, Long tenantId, ContractQueryParams baseParams, ContractAccessScope accessScope) {
    exportTaskService.markProcessing(taskId, tenantId);
    try {
      List<Contract> contracts = loadContracts(tenantId, baseParams, accessScope);
      Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), EXPORT_DIR);
      Files.createDirectories(exportDir);
      String fileName = "contracts_" + LocalDateTime.now().format(FILE_TIME) + ".csv";
      Path filePath = exportDir.resolve(fileName);
      writeCsv(filePath, contracts, tenantId);
      exportTaskService.completeExportTask(taskId, tenantId, fileName, filePath.toString());
    } catch (Exception ex) {
      exportTaskService.failExportTask(taskId, tenantId, truncateFailReason(ex.getMessage()));
      throw ex instanceof RuntimeException runtimeException
          ? runtimeException
          : new ContractServiceException(500, "合同导出失败");
    }
  }

  private List<Contract> loadContracts(
      Long tenantId, ContractQueryParams baseParams, ContractAccessScope accessScope) {
    List<Contract> contracts = new ArrayList<>();
    long total = Long.MAX_VALUE;
    int current = 1;
    while ((long) (current - 1) * EXPORT_PAGE_SIZE < total) {
      ContractQueryParams params = copyParams(baseParams);
      params.setPageNo(current);
      params.setPageSize(EXPORT_PAGE_SIZE);
      IPage<Contract> page = contractService.pageContractsAdvanced(tenantId, params, accessScope);
      if (page.getRecords() == null || page.getRecords().isEmpty()) {
        break;
      }
      contracts.addAll(page.getRecords());
      total = page.getTotal();
      current++;
    }
    return contracts;
  }

  private ContractQueryParams copyParams(ContractQueryParams source) {
    ContractQueryParams target = new ContractQueryParams();
    target.setContractType(source.getContractType());
    target.setContractStatus(source.getContractStatus());
    target.setApprovalStatus(source.getApprovalStatus());
    target.setKeyword(source.getKeyword());
    target.setProjectId(source.getProjectId());
    target.setSiteId(source.getSiteId());
    target.setConstructionOrgId(source.getConstructionOrgId());
    target.setTransportOrgId(source.getTransportOrgId());
    target.setIsThreeParty(source.getIsThreeParty());
    target.setSourceType(source.getSourceType());
    target.setStartDate(source.getStartDate());
    target.setEndDate(source.getEndDate());
    target.setEffectiveStartDate(source.getEffectiveStartDate());
    target.setEffectiveEndDate(source.getEffectiveEndDate());
    target.setExpireStartDate(source.getExpireStartDate());
    target.setExpireEndDate(source.getExpireEndDate());
    return target;
  }

  private void writeCsv(Path filePath, List<Contract> contracts, Long tenantId) throws IOException {
    Map<Long, String> projectNames = loadProjectNames(contracts);
    Map<Long, String> siteNames = loadSiteNames(contracts);
    Map<Long, String> orgNames = loadOrgNames(contracts, tenantId);
    try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
      writer.write('\uFEFF');
      writer.write(
          "合同编号,合同名称,合同类型,项目名称,消纳场地,建设单位,运输单位,合同金额,已入账金额,已结算金额,约定方量,合同单价,区内单价,区外单价,签订日期,生效日期,到期日期,审批状态,合同状态,来源类型,三方合同,退回原因,备注");
      writer.newLine();
      for (Contract contract : contracts) {
        List<String> row = List.of(
            valueOf(contract.getContractNo(), fallbackId(contract.getId())),
            valueOf(contract.getName(), ""),
            valueOf(contract.getContractType(), ""),
            valueOf(projectNames.get(contract.getProjectId()), fallbackId(contract.getProjectId())),
            valueOf(siteNames.get(contract.getSiteId()), fallbackId(contract.getSiteId())),
            valueOf(orgNames.get(contract.getConstructionOrgId()), fallbackId(contract.getConstructionOrgId())),
            valueOf(orgNames.get(contract.getTransportOrgId()), fallbackId(contract.getTransportOrgId())),
            decimalOf(contract.getContractAmount()),
            decimalOf(contract.getReceivedAmount()),
            decimalOf(contract.getSettledAmount()),
            decimalOf(contract.getAgreedVolume()),
            decimalOf(contract.getUnitPrice()),
            decimalOf(contract.getUnitPriceInside()),
            decimalOf(contract.getUnitPriceOutside()),
            dateOf(contract.getSignDate()),
            dateOf(contract.getEffectiveDate()),
            dateOf(contract.getExpireDate()),
            valueOf(contract.getApprovalStatus(), ""),
            valueOf(contract.getContractStatus(), ""),
            valueOf(contract.getSourceType(), ""),
            Boolean.TRUE.equals(contract.getIsThreeParty()) ? "是" : "否",
            valueOf(contract.getRejectReason(), ""),
            valueOf(contract.getRemark(), ""));
        writer.write(String.join(",", row.stream().map(this::escapeCsv).toList()));
        writer.newLine();
      }
    }
  }

  private Map<Long, String> loadProjectNames(List<Contract> contracts) {
    Set<Long> ids = new LinkedHashSet<>();
    for (Contract contract : contracts) {
      if (contract.getProjectId() != null) {
        ids.add(contract.getProjectId());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> result = new LinkedHashMap<>();
    for (Project project : projectMapper.selectBatchIds(ids)) {
      result.put(project.getId(), project.getName());
    }
    return result;
  }

  private Map<Long, String> loadSiteNames(List<Contract> contracts) {
    Set<Long> ids = new LinkedHashSet<>();
    for (Contract contract : contracts) {
      if (contract.getSiteId() != null) {
        ids.add(contract.getSiteId());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> result = new LinkedHashMap<>();
    for (Site site : siteMapper.selectBatchIds(ids)) {
      result.put(site.getId(), site.getName());
    }
    return result;
  }

  private Map<Long, String> loadOrgNames(List<Contract> contracts, Long tenantId) {
    Set<Long> ids = new LinkedHashSet<>();
    for (Contract contract : contracts) {
      if (contract.getConstructionOrgId() != null) {
        ids.add(contract.getConstructionOrgId());
      }
      if (contract.getTransportOrgId() != null) {
        ids.add(contract.getTransportOrgId());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    LambdaQueryWrapper<Org> query = new LambdaQueryWrapper<>();
    query.eq(Org::getTenantId, tenantId);
    query.in(Org::getId, ids);
    Map<Long, String> result = new LinkedHashMap<>();
    for (Org org : orgMapper.selectList(query)) {
      result.put(org.getId(), org.getOrgName());
    }
    return result;
  }

  private String escapeCsv(String value) {
    String sanitized = value == null ? "" : value;
    boolean needQuote =
        sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r");
    if (!needQuote) {
      return sanitized;
    }
    return "\"" + sanitized.replace("\"", "\"\"") + "\"";
  }

  private String decimalOf(BigDecimal value) {
    if (value == null) {
      return "";
    }
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() < 0) {
      normalized = normalized.setScale(0);
    }
    return normalized.toPlainString();
  }

  private String dateOf(LocalDate value) {
    return value != null ? value.toString() : "";
  }

  private String valueOf(String value, String fallback) {
    return StringUtils.hasText(value) ? value : fallback;
  }

  private String fallbackId(Long id) {
    return id != null ? "#" + id : "";
  }

  private String truncateFailReason(String message) {
    String value = StringUtils.hasText(message) ? message.trim() : "导出文件生成失败";
    return value.length() > 450 ? value.substring(0, 450) : value;
  }
}
