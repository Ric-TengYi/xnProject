package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.entity.contract.ContractStatSnapshot;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractReceiptMapper;
import com.xngl.infrastructure.persistence.mapper.ContractStatSnapshotMapper;
import com.xngl.infrastructure.persistence.mapper.SettlementOrderMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContractReportServiceImpl implements ContractReportService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

  private final ContractMapper contractMapper;
  private final ContractReceiptMapper receiptMapper;
  private final SettlementOrderMapper settlementOrderMapper;
  private final ContractStatSnapshotMapper snapshotMapper;

  public ContractReportServiceImpl(
      ContractMapper contractMapper,
      ContractReceiptMapper receiptMapper,
      SettlementOrderMapper settlementOrderMapper,
      ContractStatSnapshotMapper snapshotMapper) {
    this.contractMapper = contractMapper;
    this.receiptMapper = receiptMapper;
    this.settlementOrderMapper = settlementOrderMapper;
    this.snapshotMapper = snapshotMapper;
  }

  @Override
  public Map<String, Object> getMonthlySummary(
      Long tenantId, String month, ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return zeroSummary(month);
    }
    ContractStatSnapshot snapshot = findSnapshot(tenantId, month, "MONTHLY", "TOTAL");
    if (snapshot != null) {
      return filterSummarySnapshot(snapshot, accessScope);
    }
    return aggregateMonthlySummary(tenantId, month, accessScope);
  }

  @Override
  public List<Map<String, Object>> getMonthlyTrend(
      Long tenantId, int months, ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return List.of();
    }
    List<Map<String, Object>> result = new ArrayList<>();
    YearMonth current = YearMonth.now();
    for (int i = months - 1; i >= 0; i--) {
      String month = current.minusMonths(i).format(MONTH_FMT);
      result.add(aggregateMonthlyTrend(tenantId, month, accessScope));
    }
    return result;
  }

  @Override
  public List<Map<String, Object>> getMonthlyTypes(
      Long tenantId, String month, ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return List.of();
    }
    LocalDate start = parseMonthStart(month);
    LocalDate end = parseMonthEnd(month);

    QueryWrapper<Contract> query = new QueryWrapper<>();
    query.select("contract_type as contractType", "count(*) as count",
            "COALESCE(sum(contract_amount), 0) as amount",
            "COALESCE(sum(agreed_volume), 0) as volume")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end)
        .groupBy("contract_type");
    applyContractScope(query, accessScope);

    List<Map<String, Object>> maps = contractMapper.selectMaps(query);
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> m : maps) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("contractType", m.get("contractType"));
      item.put("count", toInt(m.get("count")));
      item.put("amount", toBigDecimal(m.get("amount")));
      item.put("volume", toBigDecimal(m.get("volume")));
      result.add(item);
    }
    return result;
  }

  @Override
  public IPage<Map<String, Object>> getUnitStats(
      Long tenantId,
      String unitType,
      String month,
      String keyword,
      int pageNo,
      int pageSize,
      ContractAccessScope accessScope) {
    if (accessScope != null && !accessScope.isTenantWideAccess() && !accessScope.hasAnyAccess()) {
      return emptyPage(pageNo, pageSize);
    }
    String orgColumn = "transport".equalsIgnoreCase(unitType)
        ? "transport_org_id" : "construction_org_id";

    QueryWrapper<Contract> query = new QueryWrapper<>();
    query.select(orgColumn + " as orgId",
            "count(*) as contractCount",
            "COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(received_amount), 0) as receivedAmount",
            "COALESCE(sum(contract_amount) - sum(received_amount), 0) as pendingAmount",
            "COALESCE(sum(settled_amount), 0) as settledAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .isNotNull(orgColumn);
    applyUnitOrgScope(query, orgColumn, accessScope);

    if (StringUtils.hasText(month)) {
      LocalDate start = parseMonthStart(month);
      LocalDate end = parseMonthEnd(month);
      query.ge("sign_date", start).le("sign_date", end);
    }

    query.groupBy(orgColumn);
    query.orderByDesc("contractAmount");

    Page<Map<String, Object>> page = new Page<>(pageNo, pageSize);
    return contractMapper.selectMapsPage(page, query);
  }

  @Override
  public List<Map<String, Object>> getUnitTrend(
      Long tenantId, Long orgId, int months, ContractAccessScope accessScope) {
    if (accessScope != null
        && !accessScope.isTenantWideAccess()
        && (accessScope.getOrgIds().isEmpty() || !accessScope.getOrgIds().contains(orgId))) {
      return List.of();
    }
    List<Map<String, Object>> result = new ArrayList<>();
    YearMonth current = YearMonth.now();
    for (int i = months - 1; i >= 0; i--) {
      YearMonth ym = current.minusMonths(i);
      String month = ym.format(MONTH_FMT);
      LocalDate start = ym.atDay(1);
      LocalDate end = ym.atEndOfMonth();

      QueryWrapper<Contract> query = new QueryWrapper<>();
      query.select("COALESCE(sum(agreed_volume), 0) as volume",
              "COALESCE(sum(contract_amount), 0) as amount",
              "COALESCE(sum(received_amount), 0) as receiptAmount")
          .eq("tenant_id", tenantId)
          .ge("sign_date", start)
          .le("sign_date", end)
          .and(w -> w.eq("construction_org_id", orgId).or().eq("transport_org_id", orgId));
      applyContractScope(query, accessScope);

      List<Map<String, Object>> maps = contractMapper.selectMaps(query);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("month", month);
      if (!maps.isEmpty() && maps.get(0) != null) {
        item.put("volume", toBigDecimal(maps.get(0).get("volume")));
        item.put("amount", toBigDecimal(maps.get(0).get("amount")));
        item.put("receiptAmount", toBigDecimal(maps.get(0).get("receiptAmount")));
      } else {
        item.put("volume", ZERO);
        item.put("amount", ZERO);
        item.put("receiptAmount", ZERO);
      }
      result.add(item);
    }
    return result;
  }

  private Map<String, Object> aggregateMonthlySummary(
      Long tenantId, String month, ContractAccessScope accessScope) {
    LocalDate start = parseMonthStart(month);
    LocalDate end = parseMonthEnd(month);

    long contractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(wrapper -> applyContractScope(wrapper, accessScope))
            .ge(Contract::getSignDate, start)
            .le(Contract::getSignDate, end));

    long newContractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(wrapper -> applyContractScope(wrapper, accessScope))
            .ge(Contract::getSignDate, start)
            .le(Contract::getSignDate, end));

    QueryWrapper<Contract> amountQuery = new QueryWrapper<>();
    amountQuery.select("COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end);
    applyContractScope(amountQuery, accessScope);
    List<Map<String, Object>> amountMaps = contractMapper.selectMaps(amountQuery);
    BigDecimal contractAmount = ZERO;
    BigDecimal agreedVolume = ZERO;
    if (!amountMaps.isEmpty() && amountMaps.get(0) != null) {
      contractAmount = toBigDecimal(amountMaps.get(0).get("contractAmount"));
      agreedVolume = toBigDecimal(amountMaps.get(0).get("agreedVolume"));
    }

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, start, end, accessScope);
    BigDecimal settlementAmount = aggregateSettlementAmount(tenantId, start, end, accessScope);

    ContractStatSnapshot snapshot = new ContractStatSnapshot();
    snapshot.setTenantId(tenantId);
    snapshot.setStatDate(LocalDate.now());
    snapshot.setStatMonth(month);
    snapshot.setStatType("MONTHLY");
    snapshot.setDimensionType("TOTAL");
    snapshot.setContractCount((int) contractCount);
    snapshot.setNewContractCount((int) newContractCount);
    snapshot.setContractAmount(contractAmount);
    snapshot.setReceiptAmount(receiptAmount);
    snapshot.setSettlementAmount(settlementAmount);
    snapshot.setAgreedVolume(agreedVolume);
    snapshot.setActualVolume(ZERO);
    snapshotMapper.insert(snapshot);

    return snapshotToSummaryMap(snapshot);
  }

  private Map<String, Object> aggregateMonthlyTrend(
      Long tenantId, String month, ContractAccessScope accessScope) {
    LocalDate start = parseMonthStart(month);
    LocalDate end = parseMonthEnd(month);

    QueryWrapper<Contract> query = new QueryWrapper<>();
    query.select("COALESCE(sum(agreed_volume), 0) as volume",
            "COALESCE(sum(contract_amount), 0) as amount")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end);
    applyContractScope(query, accessScope);
    List<Map<String, Object>> maps = contractMapper.selectMaps(query);

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, start, end, accessScope);

    Map<String, Object> item = new LinkedHashMap<>();
    item.put("month", month);
    if (!maps.isEmpty() && maps.get(0) != null) {
      item.put("volume", toBigDecimal(maps.get(0).get("volume")));
      item.put("amount", toBigDecimal(maps.get(0).get("amount")));
    } else {
      item.put("volume", ZERO);
      item.put("amount", ZERO);
    }
    item.put("receiptAmount", receiptAmount);
    return item;
  }

  private BigDecimal aggregateReceiptAmount(
      Long tenantId, LocalDate start, LocalDate end, ContractAccessScope accessScope) {
    Set<Long> accessibleContractIds = resolveAccessibleContractIds(tenantId, accessScope);
    if (accessScope != null && !accessScope.isTenantWideAccess() && accessibleContractIds.isEmpty()) {
      return ZERO;
    }
    QueryWrapper<ContractReceipt> rq = new QueryWrapper<>();
    rq.select("COALESCE(sum(amount), 0) as total")
        .eq("tenant_id", tenantId)
        .eq("status", "NORMAL")
        .ge("receipt_date", start)
        .le("receipt_date", end);
    if (accessScope != null && !accessScope.isTenantWideAccess()) {
      rq.in("contract_id", accessibleContractIds);
    }
    List<Map<String, Object>> maps = receiptMapper.selectMaps(rq);
    if (!maps.isEmpty() && maps.get(0) != null) {
      return toBigDecimal(maps.get(0).get("total"));
    }
    return ZERO;
  }

  private BigDecimal aggregateSettlementAmount(
      Long tenantId, LocalDate start, LocalDate end, ContractAccessScope accessScope) {
    QueryWrapper<SettlementOrder> sq = new QueryWrapper<>();
    sq.select("COALESCE(sum(payable_amount), 0) as total")
        .eq("tenant_id", tenantId)
        .ge("settlement_date", start)
        .le("settlement_date", end);
    applySettlementScope(sq, accessScope);
    List<Map<String, Object>> maps = settlementOrderMapper.selectMaps(sq);
    if (!maps.isEmpty() && maps.get(0) != null) {
      return toBigDecimal(maps.get(0).get("total"));
    }
    return ZERO;
  }

  private ContractStatSnapshot findSnapshot(Long tenantId, String month, String statType, String dimensionType) {
    LambdaQueryWrapper<ContractStatSnapshot> query = new LambdaQueryWrapper<>();
    query.eq(ContractStatSnapshot::getTenantId, tenantId)
        .eq(ContractStatSnapshot::getStatMonth, month)
        .eq(ContractStatSnapshot::getStatType, statType)
        .eq(ContractStatSnapshot::getDimensionType, dimensionType)
        .orderByDesc(ContractStatSnapshot::getCreateTime)
        .last("LIMIT 1");
    return snapshotMapper.selectOne(query);
  }

  private Map<String, Object> snapshotToSummaryMap(ContractStatSnapshot s) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("month", s.getStatMonth());
    map.put("contractCount", s.getContractCount());
    map.put("newContractCount", s.getNewContractCount());
    map.put("contractAmount", s.getContractAmount());
    map.put("receiptAmount", s.getReceiptAmount());
    map.put("settlementAmount", s.getSettlementAmount());
    map.put("agreedVolume", s.getAgreedVolume());
    map.put("actualVolume", s.getActualVolume());
    return map;
  }

  private LocalDate parseMonthStart(String month) {
    YearMonth ym = YearMonth.parse(month, MONTH_FMT);
    return ym.atDay(1);
  }

  private LocalDate parseMonthEnd(String month) {
    YearMonth ym = YearMonth.parse(month, MONTH_FMT);
    return ym.atEndOfMonth();
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) return ZERO;
    if (value instanceof BigDecimal bd) return bd;
    return new BigDecimal(value.toString());
  }

  private int toInt(Object value) {
    if (value == null) return 0;
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(value.toString());
  }

  private Map<String, Object> filterSummarySnapshot(
      ContractStatSnapshot snapshot, ContractAccessScope accessScope) {
    if (accessScope == null || accessScope.isTenantWideAccess()) {
      return snapshotToSummaryMap(snapshot);
    }
    return aggregateMonthlySummary(snapshot.getTenantId(), snapshot.getStatMonth(), accessScope);
  }

  private Map<String, Object> zeroSummary(String month) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("month", month);
    result.put("contractCount", 0);
    result.put("newContractCount", 0);
    result.put("contractAmount", ZERO);
    result.put("receiptAmount", ZERO);
    result.put("settlementAmount", ZERO);
    result.put("agreedVolume", ZERO);
    result.put("actualVolume", ZERO);
    return result;
  }

  private void applyUnitOrgScope(
      QueryWrapper<Contract> query, String orgColumn, ContractAccessScope accessScope) {
    if (query == null || accessScope == null || accessScope.isTenantWideAccess()) {
      return;
    }
    if (accessScope.getOrgIds().isEmpty()) {
      query.apply("1 = 0");
      return;
    }
    query.in(orgColumn, accessScope.getOrgIds());
  }

  private void applyContractScope(QueryWrapper<Contract> query, ContractAccessScope accessScope) {
    if (query == null || accessScope == null || accessScope.isTenantWideAccess()) {
      return;
    }
    if (!accessScope.hasAnyAccess()) {
      query.apply("1 = 0");
      return;
    }
    query.and(
        wrapper -> {
          boolean hasClause = false;
          if (!accessScope.getProjectIds().isEmpty()) {
            wrapper.in("project_id", accessScope.getProjectIds());
            hasClause = true;
          }
          if (!accessScope.getSiteIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in("site_id", accessScope.getSiteIds());
            hasClause = true;
          }
          if (!accessScope.getOrgIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in("construction_org_id", accessScope.getOrgIds())
                .or()
                .in("transport_org_id", accessScope.getOrgIds())
                .or()
                .in("site_operator_org_id", accessScope.getOrgIds())
                .or()
                .in("party_id", accessScope.getOrgIds());
            hasClause = true;
          }
          if (!hasClause) {
            wrapper.apply("1 = 0");
          }
        });
  }

  private void applyContractScope(
      LambdaQueryWrapper<Contract> query, ContractAccessScope accessScope) {
    if (query == null || accessScope == null || accessScope.isTenantWideAccess()) {
      return;
    }
    if (!accessScope.hasAnyAccess()) {
      query.apply("1 = 0");
      return;
    }
    query.and(
        wrapper -> {
          boolean hasClause = false;
          if (!accessScope.getProjectIds().isEmpty()) {
            wrapper.in(Contract::getProjectId, accessScope.getProjectIds());
            hasClause = true;
          }
          if (!accessScope.getSiteIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in(Contract::getSiteId, accessScope.getSiteIds());
            hasClause = true;
          }
          if (!accessScope.getOrgIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in(Contract::getConstructionOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getTransportOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getSiteOperatorOrgId, accessScope.getOrgIds())
                .or()
                .in(Contract::getPartyId, accessScope.getOrgIds());
            hasClause = true;
          }
          if (!hasClause) {
            wrapper.apply("1 = 0");
          }
        });
  }

  private void applySettlementScope(
      QueryWrapper<SettlementOrder> query, ContractAccessScope accessScope) {
    if (query == null || accessScope == null || accessScope.isTenantWideAccess()) {
      return;
    }
    if (!accessScope.hasAnyAccess()) {
      query.apply("1 = 0");
      return;
    }
    query.and(
        wrapper -> {
          boolean hasClause = false;
          if (!accessScope.getProjectIds().isEmpty()) {
            wrapper.in("target_project_id", accessScope.getProjectIds());
            hasClause = true;
          }
          if (!accessScope.getSiteIds().isEmpty()) {
            if (hasClause) {
              wrapper.or();
            }
            wrapper.in("target_site_id", accessScope.getSiteIds());
            hasClause = true;
          }
          if (!hasClause) {
            wrapper.apply("1 = 0");
          }
        });
  }

  private Set<Long> resolveAccessibleContractIds(Long tenantId, ContractAccessScope accessScope) {
    if (accessScope == null || accessScope.isTenantWideAccess()) {
      return Set.of();
    }
    if (!accessScope.hasAnyAccess()) {
      return Set.of();
    }
    LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
    query.eq(Contract::getTenantId, tenantId);
    applyContractScope(query, accessScope);
    query.select(Contract::getId);
    return contractMapper.selectList(query).stream()
        .map(Contract::getId)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private IPage<Map<String, Object>> emptyPage(int pageNo, int pageSize) {
    Page<Map<String, Object>> page = new Page<>(pageNo, pageSize);
    page.setTotal(0L);
    page.setRecords(List.of());
    return page;
  }

  // ==================== Daily Report ====================

  @Override
  public Map<String, Object> getDailySummary(
      Long tenantId, LocalDate date, ContractAccessScope accessScope) {
    LocalDate effectiveDate = date != null ? date : LocalDate.now();

    long contractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(wrapper -> applyContractScope(wrapper, accessScope))
            .eq(Contract::getSignDate, effectiveDate));

    QueryWrapper<Contract> amountQuery = new QueryWrapper<>();
    amountQuery.select("COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .eq("sign_date", effectiveDate);
    applyContractScope(amountQuery, accessScope);
    List<Map<String, Object>> amountMaps = contractMapper.selectMaps(amountQuery);
    BigDecimal contractAmount = ZERO;
    BigDecimal agreedVolume = ZERO;
    if (!amountMaps.isEmpty() && amountMaps.get(0) != null) {
      contractAmount = toBigDecimal(amountMaps.get(0).get("contractAmount"));
      agreedVolume = toBigDecimal(amountMaps.get(0).get("agreedVolume"));
    }

    BigDecimal receiptAmount =
        aggregateReceiptAmount(tenantId, effectiveDate, effectiveDate, accessScope);
    BigDecimal settlementAmount =
        aggregateSettlementAmount(tenantId, effectiveDate, effectiveDate, accessScope);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("date", effectiveDate.toString());
    result.put("contractCount", contractCount);
    result.put("contractAmount", contractAmount);
    result.put("agreedVolume", agreedVolume);
    result.put("receiptAmount", receiptAmount);
    result.put("settlementAmount", settlementAmount);
    return result;
  }

  @Override
  public List<Map<String, Object>> getDailyTrend(
      Long tenantId, LocalDate startDate, LocalDate endDate, ContractAccessScope accessScope) {
    LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(6);
    LocalDate end = endDate != null ? endDate : LocalDate.now();
    List<Map<String, Object>> result = new ArrayList<>();

    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("date", date.toString());

      QueryWrapper<Contract> query = new QueryWrapper<>();
      query.select("COALESCE(sum(contract_amount), 0) as amount",
              "COALESCE(sum(agreed_volume), 0) as volume")
          .eq("tenant_id", tenantId)
          .eq("sign_date", date);
      applyContractScope(query, accessScope);
      List<Map<String, Object>> maps = contractMapper.selectMaps(query);
      if (!maps.isEmpty() && maps.get(0) != null) {
        item.put("amount", toBigDecimal(maps.get(0).get("amount")));
        item.put("volume", toBigDecimal(maps.get(0).get("volume")));
      } else {
        item.put("amount", ZERO);
        item.put("volume", ZERO);
      }
      item.put("receiptAmount", aggregateReceiptAmount(tenantId, date, date, accessScope));
      result.add(item);
    }
    return result;
  }

  // ==================== Yearly Report ====================

  @Override
  public Map<String, Object> getYearlySummary(
      Long tenantId, int year, ContractAccessScope accessScope) {
    int effectiveYear = year > 0 ? year : LocalDate.now().getYear();
    LocalDate start = LocalDate.of(effectiveYear, 1, 1);
    LocalDate end = LocalDate.of(effectiveYear, 12, 31);

    long contractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(wrapper -> applyContractScope(wrapper, accessScope))
            .ge(Contract::getSignDate, start)
            .le(Contract::getSignDate, end));

    QueryWrapper<Contract> amountQuery = new QueryWrapper<>();
    amountQuery.select("COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end);
    applyContractScope(amountQuery, accessScope);
    List<Map<String, Object>> amountMaps = contractMapper.selectMaps(amountQuery);
    BigDecimal contractAmount = ZERO;
    BigDecimal agreedVolume = ZERO;
    if (!amountMaps.isEmpty() && amountMaps.get(0) != null) {
      contractAmount = toBigDecimal(amountMaps.get(0).get("contractAmount"));
      agreedVolume = toBigDecimal(amountMaps.get(0).get("agreedVolume"));
    }

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, start, end, accessScope);
    BigDecimal settlementAmount = aggregateSettlementAmount(tenantId, start, end, accessScope);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("year", effectiveYear);
    result.put("contractCount", contractCount);
    result.put("contractAmount", contractAmount);
    result.put("agreedVolume", agreedVolume);
    result.put("receiptAmount", receiptAmount);
    result.put("settlementAmount", settlementAmount);
    return result;
  }

  @Override
  public List<Map<String, Object>> getYearlyTrend(
      Long tenantId, int years, ContractAccessScope accessScope) {
    int effectiveYears = years > 0 ? years : 5;
    int currentYear = LocalDate.now().getYear();
    List<Map<String, Object>> result = new ArrayList<>();

    for (int i = effectiveYears - 1; i >= 0; i--) {
      int year = currentYear - i;
      LocalDate start = LocalDate.of(year, 1, 1);
      LocalDate end = LocalDate.of(year, 12, 31);

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("year", year);

      QueryWrapper<Contract> query = new QueryWrapper<>();
      query.select("COALESCE(sum(contract_amount), 0) as amount",
              "COALESCE(sum(agreed_volume), 0) as volume")
          .eq("tenant_id", tenantId)
          .ge("sign_date", start)
          .le("sign_date", end);
      applyContractScope(query, accessScope);
      List<Map<String, Object>> maps = contractMapper.selectMaps(query);
      if (!maps.isEmpty() && maps.get(0) != null) {
        item.put("amount", toBigDecimal(maps.get(0).get("amount")));
        item.put("volume", toBigDecimal(maps.get(0).get("volume")));
      } else {
        item.put("amount", ZERO);
        item.put("volume", ZERO);
      }
      item.put("receiptAmount", aggregateReceiptAmount(tenantId, start, end, accessScope));
      result.add(item);
    }
    return result;
  }

  // ==================== Custom Period Report ====================

  @Override
  public Map<String, Object> getCustomPeriodSummary(
      Long tenantId, LocalDate startDate, LocalDate endDate, ContractAccessScope accessScope) {
    if (startDate == null || endDate == null) {
      throw new ContractServiceException(400, "自定义周期必须提供开始和结束日期");
    }
    if (startDate.isAfter(endDate)) {
      throw new ContractServiceException(400, "开始日期不能晚于结束日期");
    }

    long contractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .and(wrapper -> applyContractScope(wrapper, accessScope))
            .ge(Contract::getSignDate, startDate)
            .le(Contract::getSignDate, endDate));

    QueryWrapper<Contract> amountQuery = new QueryWrapper<>();
    amountQuery.select("COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .ge("sign_date", startDate)
        .le("sign_date", endDate);
    applyContractScope(amountQuery, accessScope);
    List<Map<String, Object>> amountMaps = contractMapper.selectMaps(amountQuery);
    BigDecimal contractAmount = ZERO;
    BigDecimal agreedVolume = ZERO;
    if (!amountMaps.isEmpty() && amountMaps.get(0) != null) {
      contractAmount = toBigDecimal(amountMaps.get(0).get("contractAmount"));
      agreedVolume = toBigDecimal(amountMaps.get(0).get("agreedVolume"));
    }

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, startDate, endDate, accessScope);
    BigDecimal settlementAmount =
        aggregateSettlementAmount(tenantId, startDate, endDate, accessScope);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("startDate", startDate.toString());
    result.put("endDate", endDate.toString());
    result.put("contractCount", contractCount);
    result.put("contractAmount", contractAmount);
    result.put("agreedVolume", agreedVolume);
    result.put("receiptAmount", receiptAmount);
    result.put("settlementAmount", settlementAmount);
    return result;
  }

  @Override
  public List<Map<String, Object>> getCustomPeriodTrend(
      Long tenantId,
      LocalDate startDate,
      LocalDate endDate,
      String groupBy,
      ContractAccessScope accessScope) {
    if (startDate == null || endDate == null) {
      throw new ContractServiceException(400, "自定义周期必须提供开始和结束日期");
    }
    if (startDate.isAfter(endDate)) {
      throw new ContractServiceException(400, "开始日期不能晚于结束日期");
    }

    String effectiveGroupBy = StringUtils.hasText(groupBy) ? groupBy.toLowerCase() : "day";
    return switch (effectiveGroupBy) {
      case "month" -> getCustomPeriodTrendByMonth(tenantId, startDate, endDate, accessScope);
      case "year" -> getCustomPeriodTrendByYear(tenantId, startDate, endDate, accessScope);
      default -> getCustomPeriodTrendByDay(tenantId, startDate, endDate, accessScope);
    };
  }

  private List<Map<String, Object>> getCustomPeriodTrendByDay(
      Long tenantId, LocalDate startDate, LocalDate endDate, ContractAccessScope accessScope) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("date", date.toString());
      item.put("receiptAmount", aggregateReceiptAmount(tenantId, date, date, accessScope));
      item.put(
          "settlementAmount",
          aggregateSettlementAmount(tenantId, date, date, accessScope));
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> getCustomPeriodTrendByMonth(
      Long tenantId, LocalDate startDate, LocalDate endDate, ContractAccessScope accessScope) {
    List<Map<String, Object>> result = new ArrayList<>();
    YearMonth startMonth = YearMonth.from(startDate);
    YearMonth endMonth = YearMonth.from(endDate);

    for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      LocalDate monthStart = ym.atDay(1);
      LocalDate monthEnd = ym.atEndOfMonth();

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("month", ym.format(MONTH_FMT));
      item.put(
          "receiptAmount",
          aggregateReceiptAmount(tenantId, monthStart, monthEnd, accessScope));
      item.put(
          "settlementAmount",
          aggregateSettlementAmount(tenantId, monthStart, monthEnd, accessScope));
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> getCustomPeriodTrendByYear(
      Long tenantId, LocalDate startDate, LocalDate endDate, ContractAccessScope accessScope) {
    List<Map<String, Object>> result = new ArrayList<>();
    int startYear = startDate.getYear();
    int endYear = endDate.getYear();

    for (int year = startYear; year <= endYear; year++) {
      LocalDate yearStart = LocalDate.of(year, 1, 1);
      LocalDate yearEnd = LocalDate.of(year, 12, 31);

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("year", year);
      item.put(
          "receiptAmount",
          aggregateReceiptAmount(tenantId, yearStart, yearEnd, accessScope));
      item.put(
          "settlementAmount",
          aggregateSettlementAmount(tenantId, yearStart, yearEnd, accessScope));
      result.add(item);
    }
    return result;
  }
}
