package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.SettlementItem;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.mapper.SettlementItemMapper;
import com.xngl.infrastructure.persistence.mapper.SettlementOrderMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SettlementServiceImpl implements SettlementService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final DateTimeFormatter NO_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private static final String TYPE_PROJECT = "PROJECT";
  private static final String TYPE_SITE = "SITE";
  private static final String STATUS_DRAFT = "DRAFT";
  private static final String STATUS_APPROVING = "APPROVING";
  private static final String STATUS_APPROVED = "APPROVED";
  private static final String STATUS_SETTLED = "SETTLED";
  private static final String STATUS_REJECTED = "REJECTED";
  private static final String APPROVAL_NOT_SUBMITTED = "NOT_SUBMITTED";

  private final SettlementOrderMapper settlementOrderMapper;
  private final SettlementItemMapper settlementItemMapper;

  public SettlementServiceImpl(
      SettlementOrderMapper settlementOrderMapper, SettlementItemMapper settlementItemMapper) {
    this.settlementOrderMapper = settlementOrderMapper;
    this.settlementItemMapper = settlementItemMapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long generateProjectSettlement(
      Long tenantId,
      Long creatorId,
      Long projectId,
      LocalDate periodStart,
      LocalDate periodEnd,
      String remark) {
    validatePeriod(periodStart, periodEnd);
    checkDuplicate(tenantId, TYPE_PROJECT, projectId, null, periodStart, periodEnd);

    SettlementOrder order = new SettlementOrder();
    order.setTenantId(tenantId);
    order.setSettlementNo(generateSettlementNo());
    order.setSettlementType(TYPE_PROJECT);
    order.setTargetProjectId(projectId);
    order.setPeriodStart(periodStart);
    order.setPeriodEnd(periodEnd);
    order.setTotalVolume(ZERO);
    order.setUnitPrice(ZERO);
    order.setTotalAmount(ZERO);
    order.setAdjustAmount(ZERO);
    order.setPayableAmount(ZERO);
    order.setApprovalStatus(APPROVAL_NOT_SUBMITTED);
    order.setSettlementStatus(STATUS_DRAFT);
    order.setCreatorId(creatorId);
    order.setRemark(trimToNull(remark));
    settlementOrderMapper.insert(order);
    return order.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long generateSiteSettlement(
      Long tenantId,
      Long creatorId,
      Long siteId,
      LocalDate periodStart,
      LocalDate periodEnd,
      String remark) {
    validatePeriod(periodStart, periodEnd);
    checkDuplicate(tenantId, TYPE_SITE, null, siteId, periodStart, periodEnd);

    SettlementOrder order = new SettlementOrder();
    order.setTenantId(tenantId);
    order.setSettlementNo(generateSettlementNo());
    order.setSettlementType(TYPE_SITE);
    order.setTargetSiteId(siteId);
    order.setPeriodStart(periodStart);
    order.setPeriodEnd(periodEnd);
    order.setTotalVolume(ZERO);
    order.setUnitPrice(ZERO);
    order.setTotalAmount(ZERO);
    order.setAdjustAmount(ZERO);
    order.setPayableAmount(ZERO);
    order.setApprovalStatus(APPROVAL_NOT_SUBMITTED);
    order.setSettlementStatus(STATUS_DRAFT);
    order.setCreatorId(creatorId);
    order.setRemark(trimToNull(remark));
    settlementOrderMapper.insert(order);
    return order.getId();
  }

  @Override
  public IPage<SettlementOrder> pageSettlements(
      Long tenantId,
      String settlementType,
      String status,
      Long projectId,
      Long siteId,
      int pageNo,
      int pageSize) {
    LambdaQueryWrapper<SettlementOrder> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(SettlementOrder::getTenantId, tenantId);
    }
    if (StringUtils.hasText(settlementType)) {
      query.eq(SettlementOrder::getSettlementType, settlementType.trim().toUpperCase());
    }
    if (StringUtils.hasText(status)) {
      String trimmed = status.trim().toUpperCase();
      query.and(
          w ->
              w.eq(SettlementOrder::getApprovalStatus, trimmed)
                  .or()
                  .eq(SettlementOrder::getSettlementStatus, trimmed));
    }
    if (projectId != null) {
      query.eq(SettlementOrder::getTargetProjectId, projectId);
    }
    if (siteId != null) {
      query.eq(SettlementOrder::getTargetSiteId, siteId);
    }
    query.orderByDesc(SettlementOrder::getCreateTime).orderByDesc(SettlementOrder::getId);
    return settlementOrderMapper.selectPage(new Page<>(pageNo, pageSize), query);
  }

  @Override
  public SettlementOrder getSettlement(Long id, Long tenantId) {
    SettlementOrder order = settlementOrderMapper.selectById(id);
    if (order == null || !isTenantAccessible(tenantId, order.getTenantId())) {
      throw new ContractServiceException(404, "结算单不存在");
    }
    return order;
  }

  @Override
  public List<SettlementItem> listSettlementItems(Long orderId, Long tenantId) {
    LambdaQueryWrapper<SettlementItem> query =
        new LambdaQueryWrapper<SettlementItem>()
            .eq(SettlementItem::getSettlementOrderId, orderId);
    if (tenantId != null) {
      query.eq(SettlementItem::getTenantId, tenantId);
    }
    query.orderByAsc(SettlementItem::getBizDate).orderByAsc(SettlementItem::getId);
    return settlementItemMapper.selectList(query);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void submitSettlement(Long id, Long tenantId) {
    SettlementOrder order = getSettlement(id, tenantId);
    if (!STATUS_DRAFT.equals(order.getSettlementStatus())) {
      throw new ContractServiceException(409, "仅草稿状态的结算单可以提交");
    }
    SettlementOrder update = new SettlementOrder();
    update.setId(id);
    update.setApprovalStatus(STATUS_APPROVING);
    update.setSettlementStatus(STATUS_APPROVING);
    settlementOrderMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void approveSettlement(Long id, Long tenantId) {
    SettlementOrder order = getSettlement(id, tenantId);
    if (!STATUS_APPROVING.equals(order.getApprovalStatus())) {
      throw new ContractServiceException(409, "仅审批中的结算单可以审批通过");
    }
    SettlementOrder update = new SettlementOrder();
    update.setId(id);
    update.setApprovalStatus(STATUS_APPROVED);
    update.setSettlementStatus(STATUS_SETTLED);
    update.setSettlementDate(LocalDate.now());
    settlementOrderMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void rejectSettlement(Long id, Long tenantId, String reason) {
    SettlementOrder order = getSettlement(id, tenantId);
    if (!STATUS_APPROVING.equals(order.getApprovalStatus())) {
      throw new ContractServiceException(409, "仅审批中的结算单可以驳回");
    }
    SettlementOrder update = new SettlementOrder();
    update.setId(id);
    update.setApprovalStatus(STATUS_REJECTED);
    update.setSettlementStatus(STATUS_REJECTED);
    if (StringUtils.hasText(reason)) {
      String existingRemark = order.getRemark();
      String rejectNote = "驳回原因：" + reason.trim();
      update.setRemark(
          StringUtils.hasText(existingRemark) ? existingRemark + " | " + rejectNote : rejectNote);
    }
    settlementOrderMapper.updateById(update);
  }

  @Override
  public Map<String, Object> getSettlementStats(Long tenantId) {
    LambdaQueryWrapper<SettlementOrder> baseQuery = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      baseQuery.eq(SettlementOrder::getTenantId, tenantId);
    }
    List<SettlementOrder> allOrders = settlementOrderMapper.selectList(baseQuery);

    long totalOrders = allOrders.size();
    long draftOrders =
        allOrders.stream()
            .filter(o -> STATUS_DRAFT.equals(o.getSettlementStatus()))
            .count();
    long pendingOrders =
        allOrders.stream()
            .filter(o -> STATUS_APPROVING.equals(o.getApprovalStatus()))
            .count();
    long settledOrders =
        allOrders.stream()
            .filter(o -> STATUS_SETTLED.equals(o.getSettlementStatus()))
            .count();

    BigDecimal pendingAmount =
        allOrders.stream()
            .filter(o -> STATUS_APPROVING.equals(o.getApprovalStatus()))
            .map(o -> o.getPayableAmount() != null ? o.getPayableAmount() : ZERO)
            .reduce(ZERO, BigDecimal::add);

    BigDecimal settledAmount =
        allOrders.stream()
            .filter(o -> STATUS_SETTLED.equals(o.getSettlementStatus()))
            .map(o -> o.getPayableAmount() != null ? o.getPayableAmount() : ZERO)
            .reduce(ZERO, BigDecimal::add);

    Map<String, Object> stats = new HashMap<>();
    stats.put("pendingAmount", pendingAmount);
    stats.put("settledAmount", settledAmount);
    stats.put("totalOrders", totalOrders);
    stats.put("draftOrders", draftOrders);
    stats.put("pendingOrders", pendingOrders);
    stats.put("settledOrders", settledOrders);
    return stats;
  }

  private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
    if (periodStart == null || periodEnd == null) {
      throw new ContractServiceException(400, "结算周期不能为空");
    }
    if (periodStart.isAfter(periodEnd)) {
      throw new ContractServiceException(400, "结算开始日期不能晚于结束日期");
    }
  }

  private void checkDuplicate(
      Long tenantId,
      String settlementType,
      Long projectId,
      Long siteId,
      LocalDate periodStart,
      LocalDate periodEnd) {
    LambdaQueryWrapper<SettlementOrder> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(SettlementOrder::getTenantId, tenantId);
    }
    query.eq(SettlementOrder::getSettlementType, settlementType);
    if (TYPE_PROJECT.equals(settlementType) && projectId != null) {
      query.eq(SettlementOrder::getTargetProjectId, projectId);
    }
    if (TYPE_SITE.equals(settlementType) && siteId != null) {
      query.eq(SettlementOrder::getTargetSiteId, siteId);
    }
    query.le(SettlementOrder::getPeriodStart, periodEnd);
    query.ge(SettlementOrder::getPeriodEnd, periodStart);
    query.ne(SettlementOrder::getSettlementStatus, STATUS_REJECTED);

    if (settlementOrderMapper.selectCount(query) > 0) {
      throw new ContractServiceException(409, "该对象在此结算周期内已存在结算单");
    }
  }

  private boolean isTenantAccessible(Long expectedTenantId, Long actualTenantId) {
    return expectedTenantId == null
        || actualTenantId == null
        || expectedTenantId.equals(actualTenantId);
  }

  private String generateSettlementNo() {
    String timePart = LocalDateTime.now().format(NO_TIME_FORMAT);
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "JS" + timePart + random;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
