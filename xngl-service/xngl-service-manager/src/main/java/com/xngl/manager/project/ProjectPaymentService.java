package com.xngl.manager.project;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.time.LocalDate;

public interface ProjectPaymentService {

  IPage<ProjectPaymentRecordVo> pageRecords(
      Long tenantId,
      Long projectId,
      String keyword,
      String paymentType,
      String status,
      LocalDate startDate,
      LocalDate endDate,
      int pageNo,
      int pageSize);

  ProjectPaymentSummaryVo getSummary(Long tenantId, Long projectId);

  ProjectPaymentChangeResultVo createPayment(
      Long tenantId, Long operatorId, Long projectId, ProjectPaymentCreateCommand command);

  ProjectPaymentChangeResultVo cancelPayment(
      Long tenantId, Long operatorId, Long paymentId, String cancelReason);
}
