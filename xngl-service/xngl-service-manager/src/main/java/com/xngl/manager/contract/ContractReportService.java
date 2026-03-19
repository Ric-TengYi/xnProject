package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ContractReportService {

  Map<String, Object> getMonthlySummary(Long tenantId, String month);

  List<Map<String, Object>> getMonthlyTrend(Long tenantId, int months);

  List<Map<String, Object>> getMonthlyTypes(Long tenantId, String month);

  IPage<Map<String, Object>> getUnitStats(Long tenantId, String unitType, String month, String keyword, int pageNo, int pageSize);

  List<Map<String, Object>> getUnitTrend(Long tenantId, Long orgId, int months);

  // Daily report
  Map<String, Object> getDailySummary(Long tenantId, LocalDate date);

  List<Map<String, Object>> getDailyTrend(Long tenantId, LocalDate startDate, LocalDate endDate);

  // Yearly report
  Map<String, Object> getYearlySummary(Long tenantId, int year);

  List<Map<String, Object>> getYearlyTrend(Long tenantId, int years);

  // Custom settlement date report
  Map<String, Object> getCustomPeriodSummary(Long tenantId, LocalDate startDate, LocalDate endDate);

  List<Map<String, Object>> getCustomPeriodTrend(Long tenantId, LocalDate startDate, LocalDate endDate, String groupBy);
}
