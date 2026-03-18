package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;

public interface ContractReportService {

  Map<String, Object> getMonthlySummary(Long tenantId, String month);

  List<Map<String, Object>> getMonthlyTrend(Long tenantId, int months);

  List<Map<String, Object>> getMonthlyTypes(Long tenantId, String month);

  IPage<Map<String, Object>> getUnitStats(Long tenantId, String unitType, String month, String keyword, int pageNo, int pageSize);

  List<Map<String, Object>> getUnitTrend(Long tenantId, Long orgId, int months);
}
