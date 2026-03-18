package com.xngl.manager.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xngl.infrastructure.persistence.entity.contract.SettlementItem;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SettlementService {

  long generateProjectSettlement(
      Long tenantId,
      Long creatorId,
      Long projectId,
      LocalDate periodStart,
      LocalDate periodEnd,
      String remark);

  long generateSiteSettlement(
      Long tenantId,
      Long creatorId,
      Long siteId,
      LocalDate periodStart,
      LocalDate periodEnd,
      String remark);

  IPage<SettlementOrder> pageSettlements(
      Long tenantId,
      String settlementType,
      String status,
      Long projectId,
      Long siteId,
      int pageNo,
      int pageSize);

  SettlementOrder getSettlement(Long id, Long tenantId);

  List<SettlementItem> listSettlementItems(Long orderId, Long tenantId);

  void submitSettlement(Long id, Long tenantId);

  void approveSettlement(Long id, Long tenantId);

  void rejectSettlement(Long id, Long tenantId, String reason);

  Map<String, Object> getSettlementStats(Long tenantId);
}
