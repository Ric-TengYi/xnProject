package com.xngl.manager.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.project.ProjectPaymentRecord;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectPaymentRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectPaymentServiceImpl implements ProjectPaymentService {

  private static final String STATUS_NORMAL = "NORMAL";
  private static final String STATUS_CANCELLED = "CANCELLED";
  private static final String DEFAULT_PAYMENT_TYPE = "MANUAL";
  private static final String DEFAULT_SOURCE_TYPE = "MANUAL";
  private static final DateTimeFormatter PAYMENT_NO_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final ProjectPaymentRecordMapper projectPaymentRecordMapper;
  private final ProjectMapper projectMapper;
  private final ContractMapper contractMapper;

  public ProjectPaymentServiceImpl(
      ProjectPaymentRecordMapper projectPaymentRecordMapper,
      ProjectMapper projectMapper,
      ContractMapper contractMapper) {
    this.projectPaymentRecordMapper = projectPaymentRecordMapper;
    this.projectMapper = projectMapper;
    this.contractMapper = contractMapper;
  }

  @Override
  public IPage<ProjectPaymentRecordVo> pageRecords(
      Long tenantId,
      Long projectId,
      String keyword,
      String paymentType,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      int pageNo,
      int pageSize) {
    List<Long> projectIds = resolveProjectIds(projectId, keyword);
    if (projectIds != null && projectIds.isEmpty()) {
      return new Page<>(pageNo, pageSize, 0L);
    }

    LambdaQueryWrapper<ProjectPaymentRecord> query = new LambdaQueryWrapper<>();
    query.eq(ProjectPaymentRecord::getTenantId, tenantId);
    if (projectIds != null) {
      query.in(ProjectPaymentRecord::getProjectId, projectIds);
    }
    if (StringUtils.hasText(paymentType)) {
      query.eq(ProjectPaymentRecord::getPaymentType, paymentType);
    }
    if (StringUtils.hasText(status)) {
      query.eq(ProjectPaymentRecord::getStatus, status);
    }
    if (startDate != null) {
      query.ge(ProjectPaymentRecord::getPaymentDate, startDate);
    }
    if (endDate != null) {
      query.le(ProjectPaymentRecord::getPaymentDate, endDate);
    }
    query.orderByDesc(ProjectPaymentRecord::getPaymentDate)
        .orderByDesc(ProjectPaymentRecord::getId);

    IPage<ProjectPaymentRecord> page =
        projectPaymentRecordMapper.selectPage(new Page<>(pageNo, pageSize), query);
    List<Long> pageProjectIds =
        page.getRecords().stream().map(ProjectPaymentRecord::getProjectId).distinct().toList();
    Map<Long, Project> projectMap = fetchProjectMap(pageProjectIds);

    Page<ProjectPaymentRecordVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    result.setRecords(
        page.getRecords().stream()
            .map(record -> toRecordVo(record, projectMap.get(record.getProjectId())))
            .toList());
    return result;
  }

  @Override
  public ProjectPaymentSummaryVo getSummary(Long tenantId, Long projectId) {
    Project project = requireProject(projectId);
    return buildSummary(tenantId, project);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ProjectPaymentChangeResultVo createPayment(
      Long tenantId, Long operatorId, Long projectId, ProjectPaymentCreateCommand command) {
    Project project = requireProject(projectId);
    validateCreateCommand(command);

    ProjectPaymentRecord record = new ProjectPaymentRecord();
    record.setTenantId(tenantId);
    record.setProjectId(projectId);
    record.setPaymentNo(resolvePaymentNo(tenantId, command.getPaymentNo()));
    record.setPaymentType(defaultText(command.getPaymentType(), DEFAULT_PAYMENT_TYPE));
    record.setAmount(command.getAmount());
    record.setPaymentDate(command.getPaymentDate());
    record.setVoucherNo(trimToNull(command.getVoucherNo()));
    record.setStatus(STATUS_NORMAL);
    record.setSourceType(defaultText(command.getSourceType(), DEFAULT_SOURCE_TYPE));
    record.setSourceId(command.getSourceId());
    record.setRemark(trimToNull(command.getRemark()));
    record.setOperatorId(operatorId);
    projectPaymentRecordMapper.insert(record);

    return new ProjectPaymentChangeResultVo(
        record.getId(), record.getPaymentNo(), buildSummary(tenantId, project));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ProjectPaymentChangeResultVo cancelPayment(
      Long tenantId, Long operatorId, Long paymentId, String cancelReason) {
    ProjectPaymentRecord current = projectPaymentRecordMapper.selectById(paymentId);
    if (current == null || !tenantId.equals(current.getTenantId())) {
      throw new ProjectPaymentException(404, "交款记录不存在");
    }
    if (STATUS_CANCELLED.equalsIgnoreCase(current.getStatus())) {
      throw new ProjectPaymentException(400, "交款记录已冲销");
    }

    ProjectPaymentRecord update = new ProjectPaymentRecord();
    update.setId(paymentId);
    update.setStatus(STATUS_CANCELLED);
    update.setCancelOperatorId(operatorId);
    update.setCancelTime(LocalDateTime.now());
    update.setCancelReason(trimToNull(cancelReason));
    projectPaymentRecordMapper.updateById(update);

    Project project = requireProject(current.getProjectId());
    return new ProjectPaymentChangeResultVo(
        current.getId(), current.getPaymentNo(), buildSummary(tenantId, project));
  }

  private Project requireProject(Long projectId) {
    Project project = projectMapper.selectById(projectId);
    if (project == null) {
      throw new ProjectPaymentException(404, "项目不存在");
    }
    return project;
  }

  private void validateCreateCommand(ProjectPaymentCreateCommand command) {
    if (command == null) {
      throw new ProjectPaymentException(400, "交款参数不能为空");
    }
    if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ProjectPaymentException(400, "交款金额必须大于 0");
    }
    if (command.getPaymentDate() == null) {
      throw new ProjectPaymentException(400, "交款日期不能为空");
    }
  }

  private List<Long> resolveProjectIds(Long projectId, String keyword) {
    if (projectId == null && !StringUtils.hasText(keyword)) {
      return null;
    }
    LambdaQueryWrapper<Project> query = new LambdaQueryWrapper<>();
    query.select(Project::getId);
    if (projectId != null) {
      query.eq(Project::getId, projectId);
    }
    if (StringUtils.hasText(keyword)) {
      query.and(
          wrapper ->
              wrapper.like(Project::getName, keyword).or().like(Project::getCode, keyword));
    }
    return projectMapper.selectList(query).stream().map(Project::getId).toList();
  }

  private Map<Long, Project> fetchProjectMap(List<Long> projectIds) {
    if (projectIds == null || projectIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return projectMapper.selectBatchIds(projectIds).stream()
        .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));
  }

  private ProjectPaymentRecordVo toRecordVo(ProjectPaymentRecord record, Project project) {
    return new ProjectPaymentRecordVo(
        record.getId(),
        record.getProjectId(),
        project != null ? project.getName() : null,
        project != null ? project.getCode() : null,
        record.getPaymentNo(),
        record.getPaymentType(),
        record.getAmount(),
        record.getPaymentDate(),
        record.getVoucherNo(),
        record.getStatus(),
        record.getSourceType(),
        record.getSourceId(),
        record.getRemark(),
        record.getOperatorId(),
        record.getCancelOperatorId(),
        record.getCancelTime(),
        record.getCancelReason(),
        record.getCreateTime(),
        record.getUpdateTime());
  }

  private ProjectPaymentSummaryVo buildSummary(Long tenantId, Project project) {
    BigDecimal totalAmount = calculateContractAmount(tenantId, project.getId());
    Map<String, Object> aggregate = selectPaymentAggregate(tenantId, project.getId());
    BigDecimal paidAmount = getBigDecimal(getMapValue(aggregate, "paidAmount"));
    LocalDate lastPaymentDate = getLocalDate(getMapValue(aggregate, "lastPaymentDate"));
    BigDecimal debtAmount = totalAmount.subtract(paidAmount);
    if (debtAmount.compareTo(BigDecimal.ZERO) < 0) {
      debtAmount = BigDecimal.ZERO;
    }
    String status = debtAmount.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "ARREARS";
    return new ProjectPaymentSummaryVo(
        project.getId(),
        project.getName(),
        project.getCode(),
        totalAmount,
        paidAmount,
        debtAmount,
        lastPaymentDate,
        status);
  }

  private Map<String, Object> selectPaymentAggregate(Long tenantId, Long projectId) {
    Map<String, Object> aggregate = projectPaymentRecordMapper.selectPaymentAggregate(tenantId, projectId);
    return aggregate != null ? aggregate : Collections.emptyMap();
  }

  private BigDecimal calculateContractAmount(Long tenantId, Long projectId) {
    return contractMapper
        .selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .eq(Contract::getProjectId, projectId))
        .stream()
        .map(this::resolveContractAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal resolveContractAmount(Contract contract) {
    if (contract == null) {
      return BigDecimal.ZERO;
    }
    if (contract.getContractAmount() != null) {
      return contract.getContractAmount();
    }
    if (contract.getAmount() != null) {
      return contract.getAmount();
    }
    return BigDecimal.ZERO;
  }

  private String resolvePaymentNo(Long tenantId, String providedPaymentNo) {
    if (StringUtils.hasText(providedPaymentNo)) {
      String normalized = providedPaymentNo.trim();
      ensurePaymentNoUnique(tenantId, normalized);
      return normalized;
    }
    String generated;
    do {
      generated =
          "PAY"
              + LocalDateTime.now().format(PAYMENT_NO_FORMAT)
              + ThreadLocalRandom.current().nextInt(100, 1000);
    } while (existsPaymentNo(tenantId, generated));
    return generated;
  }

  private void ensurePaymentNoUnique(Long tenantId, String paymentNo) {
    if (existsPaymentNo(tenantId, paymentNo)) {
      throw new ProjectPaymentException(400, "交款单号已存在");
    }
  }

  private boolean existsPaymentNo(Long tenantId, String paymentNo) {
    return projectPaymentRecordMapper.selectCount(
            new LambdaQueryWrapper<ProjectPaymentRecord>()
                .eq(ProjectPaymentRecord::getTenantId, tenantId)
                .eq(ProjectPaymentRecord::getPaymentNo, paymentNo))
        > 0;
  }

  private String defaultText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private BigDecimal getBigDecimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    return new BigDecimal(String.valueOf(value));
  }

  private LocalDate getLocalDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.toLocalDate();
    }
    return LocalDate.parse(String.valueOf(value));
  }

  private Object getMapValue(Map<String, Object> source, String key) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    if (source.containsKey(key)) {
      return source.get(key);
    }
    String lowered = key.toLowerCase();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      if (entry.getKey() != null && entry.getKey().toLowerCase().equals(lowered)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
