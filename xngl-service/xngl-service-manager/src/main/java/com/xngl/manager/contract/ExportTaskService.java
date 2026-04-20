package com.xngl.manager.contract;

import com.xngl.infrastructure.persistence.entity.contract.ReportExportTask;

public interface ExportTaskService {

  long createExportTask(Long tenantId, Long creatorId, String bizType, String exportType, String queryJson);

  ReportExportTask getExportTask(Long taskId, Long tenantId);

  void markProcessing(Long taskId, Long tenantId);

  void completeExportTask(Long taskId, Long tenantId, String fileName, String fileUrl);

  void failExportTask(Long taskId, Long tenantId, String failReason);
}
