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
  public Map<String, Object> getMonthlySummary(Long tenantId, String month) {
    ContractStatSnapshot snapshot = findSnapshot(tenantId, month, "MONTHLY", "TOTAL");
    if (snapshot != null) {
      return snapshotToSummaryMap(snapshot);
    }
    return aggregateMonthlySummary(tenantId, month);
  }

  @Override
  public List<Map<String, Object>> getMonthlyTrend(Long tenantId, int months) {
    List<Map<String, Object>> result = new ArrayList<>();
    YearMonth current = YearMonth.now();
    for (int i = months - 1; i >= 0; i--) {
      String month = current.minusMonths(i).format(MONTH_FMT);
      result.add(aggregateMonthlyTrend(tenantId, month));
    }
    return result;
  }

  @Override
  public List<Map<String, Object>> getMonthlyTypes(Long tenantId, String month) {
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
  public IPage<Map<String, Object>> getUnitStats(Long tenantId, String unitType, String month, String keyword, int pageNo, int pageSize) {
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
  public List<Map<String, Object>> getUnitTrend(Long tenantId, Long orgId, int months) {
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

  private Map<String, Object> aggregateMonthlySummary(Long tenantId, String month) {
    LocalDate start = parseMonthStart(month);
    LocalDate end = parseMonthEnd(month);

    long contractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .ge(Contract::getSignDate, start)
            .le(Contract::getSignDate, end));

    long newContractCount = contractMapper.selectCount(
        new LambdaQueryWrapper<Contract>()
            .eq(Contract::getTenantId, tenantId)
            .ge(Contract::getSignDate, start)
            .le(Contract::getSignDate, end));

    QueryWrapper<Contract> amountQuery = new QueryWrapper<>();
    amountQuery.select("COALESCE(sum(contract_amount), 0) as contractAmount",
            "COALESCE(sum(agreed_volume), 0) as agreedVolume")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end);
    List<Map<String, Object>> amountMaps = contractMapper.selectMaps(amountQuery);
    BigDecimal contractAmount = ZERO;
    BigDecimal agreedVolume = ZERO;
    if (!amountMaps.isEmpty() && amountMaps.get(0) != null) {
      contractAmount = toBigDecimal(amountMaps.get(0).get("contractAmount"));
      agreedVolume = toBigDecimal(amountMaps.get(0).get("agreedVolume"));
    }

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, start, end);
    BigDecimal settlementAmount = aggregateSettlementAmount(tenantId, start, end);

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

  private Map<String, Object> aggregateMonthlyTrend(Long tenantId, String month) {
    LocalDate start = parseMonthStart(month);
    LocalDate end = parseMonthEnd(month);

    QueryWrapper<Contract> query = new QueryWrapper<>();
    query.select("COALESCE(sum(agreed_volume), 0) as volume",
            "COALESCE(sum(contract_amount), 0) as amount")
        .eq("tenant_id", tenantId)
        .ge("sign_date", start)
        .le("sign_date", end);
    List<Map<String, Object>> maps = contractMapper.selectMaps(query);

    BigDecimal receiptAmount = aggregateReceiptAmount(tenantId, start, end);

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

  private BigDecimal aggregateReceiptAmount(Long tenantId, LocalDate start, LocalDate end) {
    QueryWrapper<ContractReceipt> rq = new QueryWrapper<>();
    rq.select("COALESCE(sum(amount), 0) as total")
        .eq("tenant_id", tenantId)
        .eq("status", "NORMAL")
        .ge("receipt_date", start)
        .le("receipt_date", end);
    List<Map<String, Object>> maps = receiptMapper.selectMaps(rq);
    if (!maps.isEmpty() && maps.get(0) != null) {
      return toBigDecimal(maps.get(0).get("total"));
    }
    return ZERO;
  }

  private BigDecimal aggregateSettlementAmount(Long tenantId, LocalDate start, LocalDate end) {
    QueryWrapper<SettlementOrder> sq = new QueryWrapper<>();
    sq.select("COALESCE(sum(payable_amount), 0) as total")
        .eq("tenant_id", tenantId)
        .ge("settlement_date", start)
        .le("settlement_date", end);
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
}
