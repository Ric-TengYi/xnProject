package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.contract.SettlementItem;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.SettlementItemMapper;
import com.xngl.infrastructure.persistence.mapper.SettlementOrderMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private static final String SITE_TYPE_STATE_OWNED = "STATE_OWNED";
  private static final String SITE_TYPE_COLLECTIVE = "COLLECTIVE";
  private static final String SITE_TYPE_ENGINEERING = "ENGINEERING";
  private static final String SITE_TYPE_SHORT_BARGE = "SHORT_BARGE";
  private static final String SOURCE_RECORD_TYPE_CONTRACT_TICKET = "CONTRACT_TICKET";

  private final SettlementOrderMapper settlementOrderMapper;
  private final SettlementItemMapper settlementItemMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final SiteMapper siteMapper;

  public SettlementServiceImpl(
      SettlementOrderMapper settlementOrderMapper,
      SettlementItemMapper settlementItemMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      SiteMapper siteMapper) {
    this.settlementOrderMapper = settlementOrderMapper;
    this.settlementItemMapper = settlementItemMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.siteMapper = siteMapper;
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
    List<Contract> contracts = loadContracts(tenantId, projectId, null);
    List<SettlementItem> items = buildProjectSettlementItems(tenantId, contracts, periodStart, periodEnd);

    SettlementOrder order = new SettlementOrder();
    order.setTenantId(tenantId);
    order.setSettlementNo(generateSettlementNo());
    order.setSettlementType(TYPE_PROJECT);
    order.setTargetProjectId(projectId);
    order.setPeriodStart(periodStart);
    order.setPeriodEnd(periodEnd);
    applySummary(order, summarize(items));
    order.setAdjustAmount(ZERO);
    order.setApprovalStatus(APPROVAL_NOT_SUBMITTED);
    order.setSettlementStatus(STATUS_DRAFT);
    order.setCreatorId(creatorId);
    order.setRemark(trimToNull(remark));
    settlementOrderMapper.insert(order);
    insertSettlementItems(order, items);
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
    Site site = siteMapper.selectById(siteId);
    if (site == null) {
      throw new ContractServiceException(404, "场地不存在");
    }
    List<Contract> contracts = loadContracts(tenantId, null, siteId);
    List<SettlementItem> items = buildSiteSettlementItems(tenantId, site, contracts, periodStart, periodEnd);

    SettlementOrder order = new SettlementOrder();
    order.setTenantId(tenantId);
    order.setSettlementNo(generateSettlementNo());
    order.setSettlementType(TYPE_SITE);
    order.setTargetSiteId(siteId);
    order.setPeriodStart(periodStart);
    order.setPeriodEnd(periodEnd);
    applySummary(order, summarize(items));
    order.setAdjustAmount(ZERO);
    order.setApprovalStatus(APPROVAL_NOT_SUBMITTED);
    order.setSettlementStatus(STATUS_DRAFT);
    order.setCreatorId(creatorId);
    order.setRemark(mergeRemark(buildSiteSettlementRuleText(site), remark));
    settlementOrderMapper.insert(order);
    insertSettlementItems(order, items);
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

  private List<Contract> loadContracts(Long tenantId, Long projectId, Long siteId) {
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    if (tenantId != null) {
      query.eq(Contract::getTenantId, tenantId);
    }
    if (projectId != null) {
      query.eq(Contract::getProjectId, projectId);
    }
    if (siteId != null) {
      query.eq(Contract::getSiteId, siteId);
    }
    return contractMapper.selectList(query);
  }

  private List<SettlementItem> buildProjectSettlementItems(
      Long tenantId, List<Contract> contracts, LocalDate periodStart, LocalDate periodEnd) {
    List<SettlementItem> items = new ArrayList<>();
    for (Contract contract : contracts) {
      for (ContractTicket ticket : loadTickets(contract.getId(), periodStart, periodEnd)) {
        BigDecimal volume = safeAmount(ticket.getVolume());
        BigDecimal unitPrice = safeAmount(contract.getUnitPrice());
        SettlementItem item = new SettlementItem();
        item.setTenantId(tenantId);
        item.setSourceRecordType(SOURCE_RECORD_TYPE_CONTRACT_TICKET);
        item.setSourceRecordId(ticket.getId());
        item.setProjectId(contract.getProjectId());
        item.setSiteId(contract.getSiteId());
        item.setBizDate(resolveBizDate(ticket));
        item.setVolume(volume);
        item.setUnitPrice(unitPrice);
        item.setAmount(volume.multiply(unitPrice));
        item.setRemark(buildTicketRemark(ticket));
        items.add(item);
      }
    }
    return items;
  }

  private List<SettlementItem> buildSiteSettlementItems(
      Long tenantId,
      Site site,
      List<Contract> contracts,
      LocalDate periodStart,
      LocalDate periodEnd) {
    List<SettlementItem> items = new ArrayList<>();
    for (Contract contract : contracts) {
      for (ContractTicket ticket : loadTickets(contract.getId(), periodStart, periodEnd)) {
        BigDecimal volume = safeAmount(ticket.getVolume());
        BigDecimal unitPrice = resolveSiteSettlementUnitPrice(site, contract);
        SettlementItem item = new SettlementItem();
        item.setTenantId(tenantId);
        item.setSourceRecordType(SOURCE_RECORD_TYPE_CONTRACT_TICKET);
        item.setSourceRecordId(ticket.getId());
        item.setProjectId(contract.getProjectId());
        item.setSiteId(contract.getSiteId());
        item.setBizDate(resolveBizDate(ticket));
        item.setVolume(volume);
        item.setUnitPrice(unitPrice);
        item.setAmount(volume.multiply(unitPrice));
        item.setRemark(buildTicketRemark(ticket));
        items.add(item);
      }
    }
    return items;
  }

  private List<ContractTicket> loadTickets(Long contractId, LocalDate periodStart, LocalDate periodEnd) {
    if (contractId == null) {
      return Collections.emptyList();
    }
    LambdaQueryWrapper<ContractTicket> query =
        new LambdaQueryWrapper<ContractTicket>().eq(ContractTicket::getContractId, contractId);
    List<ContractTicket> tickets = contractTicketMapper.selectList(query);
    if (tickets.isEmpty()) {
      return Collections.emptyList();
    }
    return tickets.stream()
        .filter(this::isEffectiveTicket)
        .filter(ticket -> isWithinPeriod(resolveBizDate(ticket), periodStart, periodEnd))
        .toList();
  }

  private boolean isEffectiveTicket(ContractTicket ticket) {
    if (ticket == null) {
      return false;
    }
    if (!StringUtils.hasText(ticket.getStatus())) {
      return true;
    }
    String normalized = ticket.getStatus().trim().toUpperCase();
    return !"CANCELLED".equals(normalized) && !"VOID".equals(normalized);
  }

  private boolean isWithinPeriod(LocalDate bizDate, LocalDate periodStart, LocalDate periodEnd) {
    return bizDate != null && !bizDate.isBefore(periodStart) && !bizDate.isAfter(periodEnd);
  }

  private LocalDate resolveBizDate(ContractTicket ticket) {
    if (ticket == null) {
      return null;
    }
    if (ticket.getTicketDate() != null) {
      return ticket.getTicketDate();
    }
    return ticket.getCreateTime() != null ? ticket.getCreateTime().toLocalDate() : null;
  }

  private BigDecimal resolveSiteSettlementUnitPrice(Site site, Contract contract) {
    String siteType = normalizeSiteType(site);
    BigDecimal contractUnitPrice = safeAmount(contract != null ? contract.getUnitPrice() : null);
    BigDecimal disposalUnitPrice = safeAmount(site != null ? site.getDisposalUnitPrice() : null);
    BigDecimal disposalFeeRate = safeAmount(site != null ? site.getDisposalFeeRate() : null);
    BigDecimal serviceFeeUnitPrice = safeAmount(site != null ? site.getServiceFeeUnitPrice() : null);
    return switch (siteType) {
      case SITE_TYPE_COLLECTIVE ->
          disposalUnitPrice.compareTo(ZERO) > 0
              ? disposalUnitPrice.multiply(disposalFeeRate).add(serviceFeeUnitPrice)
              : contractUnitPrice.multiply(disposalFeeRate).add(serviceFeeUnitPrice);
      case SITE_TYPE_ENGINEERING ->
          disposalUnitPrice.compareTo(ZERO) > 0 ? disposalUnitPrice : contractUnitPrice;
      case SITE_TYPE_SHORT_BARGE -> serviceFeeUnitPrice;
      case SITE_TYPE_STATE_OWNED ->
          disposalUnitPrice.compareTo(ZERO) > 0 ? disposalUnitPrice : contractUnitPrice;
      default -> contractUnitPrice;
    };
  }

  private String normalizeSiteType(Site site) {
    if (site == null || !StringUtils.hasText(site.getSiteType())) {
      return SITE_TYPE_SHORT_BARGE;
    }
    return site.getSiteType().trim().toUpperCase();
  }

  private String buildSiteSettlementRuleText(Site site) {
    String siteType = normalizeSiteType(site);
    return switch (siteType) {
      case SITE_TYPE_STATE_OWNED -> "结算规则：国有场地按月结算申请";
      case SITE_TYPE_COLLECTIVE -> "结算规则：集体场地按消纳费比例+平台服务费结算";
      case SITE_TYPE_ENGINEERING -> "结算规则：工程场地按单价结算";
      case SITE_TYPE_SHORT_BARGE -> "结算规则：短驳场地按平台服务费结算";
      default -> "结算规则：按配置规则结算";
    };
  }

  private String mergeRemark(String generatedRemark, String customRemark) {
    String left = trimToNull(generatedRemark);
    String right = trimToNull(customRemark);
    if (!StringUtils.hasText(left)) {
      return right;
    }
    if (!StringUtils.hasText(right)) {
      return left;
    }
    return left + " | " + right;
  }

  private String buildTicketRemark(ContractTicket ticket) {
    if (ticket == null) {
      return null;
    }
    String ticketNo = trimToNull(ticket.getTicketNo());
    String remark = trimToNull(ticket.getRemark());
    if (!StringUtils.hasText(ticketNo)) {
      return remark;
    }
    return StringUtils.hasText(remark) ? "票据号：" + ticketNo + " | " + remark : "票据号：" + ticketNo;
  }

  private SettlementSummary summarize(List<SettlementItem> items) {
    BigDecimal totalVolume =
        items.stream().map(item -> safeAmount(item.getVolume())).reduce(ZERO, BigDecimal::add);
    BigDecimal totalAmount =
        items.stream().map(item -> safeAmount(item.getAmount())).reduce(ZERO, BigDecimal::add);
    BigDecimal unitPrice =
        totalVolume.compareTo(ZERO) > 0
            ? totalAmount.divide(totalVolume, 2, RoundingMode.HALF_UP)
            : ZERO;
    return new SettlementSummary(totalVolume, unitPrice, totalAmount);
  }

  private void applySummary(SettlementOrder order, SettlementSummary summary) {
    order.setTotalVolume(summary.totalVolume());
    order.setUnitPrice(summary.unitPrice());
    order.setTotalAmount(summary.totalAmount());
    order.setPayableAmount(summary.totalAmount());
  }

  private void insertSettlementItems(SettlementOrder order, List<SettlementItem> items) {
    if (order.getId() == null || items.isEmpty()) {
      return;
    }
    for (SettlementItem item : items) {
      item.setSettlementOrderId(order.getId());
      settlementItemMapper.insert(item);
    }
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value != null ? value : ZERO;
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

  private record SettlementSummary(BigDecimal totalVolume, BigDecimal unitPrice, BigDecimal totalAmount) {}
}
