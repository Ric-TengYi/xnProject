package com.xngl.manager.contract;

import com.xngl.infrastructure.persistence.entity.contract.ReportExportTask;
import com.xngl.infrastructure.persistence.mapper.ReportExportTaskMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportTaskServiceImpl implements ExportTaskService {

  private final ReportExportTaskMapper taskMapper;

  public ExportTaskServiceImpl(ReportExportTaskMapper taskMapper) {
    this.taskMapper = taskMapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long createExportTask(Long tenantId, Long creatorId, String bizType, String exportType, String queryJson) {
    ReportExportTask task = new ReportExportTask();
    task.setTenantId(tenantId);
    task.setCreatorId(creatorId);
    task.setBizType(bizType);
    task.setExportType(exportType);
    task.setQueryJson(queryJson);
    task.setStatus("PENDING");
    task.setExpireTime(LocalDateTime.now().plusDays(7));
    taskMapper.insert(task);
    return task.getId();
  }

  @Override
  public ReportExportTask getExportTask(Long taskId, Long tenantId) {
    ReportExportTask task = taskMapper.selectById(taskId);
    if (task == null || !tenantId.equals(task.getTenantId())) {
      throw new ContractServiceException(404, "导出任务不存在");
    }
    return task;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void markProcessing(Long taskId, Long tenantId) {
    ReportExportTask task = getExportTask(taskId, tenantId);
    ReportExportTask update = new ReportExportTask();
    update.setId(task.getId());
    update.setStatus("PROCESSING");
    update.setFailReason(null);
    taskMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void completeExportTask(Long taskId, Long tenantId, String fileName, String fileUrl) {
    ReportExportTask task = getExportTask(taskId, tenantId);
    ReportExportTask update = new ReportExportTask();
    update.setId(task.getId());
    update.setStatus("COMPLETED");
    update.setFileName(fileName);
    update.setFileUrl(fileUrl);
    update.setFailReason(null);
    taskMapper.updateById(update);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void failExportTask(Long taskId, Long tenantId, String failReason) {
    ReportExportTask task = getExportTask(taskId, tenantId);
    ReportExportTask update = new ReportExportTask();
    update.setId(task.getId());
    update.setStatus("FAILED");
    update.setFailReason(failReason);
    taskMapper.updateById(update);
  }
}
