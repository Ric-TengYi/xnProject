import http, { request } from './request';

export interface ManualEventRecord {
  id: string;
  eventNo?: string | null;
  eventType: string;
  title: string;
  content?: string | null;
  reportAddress?: string | null;
  contactPhone?: string | null;
  priority?: string | null;
  status?: string | null;
  reporterId?: string | null;
  reporterName?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  vehicleId?: string | null;
  vehicleNo?: string | null;
  sourceChannel?: string | null;
  currentAuditNode?: string | null;
  occurTime?: string | null;
  deadlineTime?: string | null;
  reportTime?: string | null;
  closeTime?: string | null;
  closeRemark?: string | null;
  attachmentUrls?: string | null;
  assigneeName?: string | null;
  assigneePhone?: string | null;
  dispatchRemark?: string | null;
  isOverdue?: boolean;
  auditCount?: number | null;
  lastAuditTime?: string | null;
  lastAuditAction?: string | null;
}

export interface ManualEventSummaryRecord {
  total: number;
  draftCount: number;
  pendingAuditCount: number;
  processingCount: number;
  rejectedCount: number;
  closedCount: number;
  highPriorityCount: number;
  overdueCount: number;
  todayCount: number;
  typeBuckets: Array<{ code: string; count: number }>;
  sourceBuckets: Array<{ code: string; count: number }>;
}

export interface ManualEventAuditLogRecord {
  id: string;
  nodeCode?: string | null;
  action?: string | null;
  resultStatus?: string | null;
  auditorName?: string | null;
  comment?: string | null;
  auditTime?: string | null;
}

export interface ManualEventDetailRecord {
  record: ManualEventRecord;
  auditLogs: ManualEventAuditLogRecord[];
}

export interface ManualEventPayload {
  eventType: string;
  title: string;
  content?: string;
  sourceChannel?: string;
  reportAddress?: string;
  projectId?: number;
  siteId?: number;
  vehicleId?: number;
  contactPhone?: string;
  priority?: string;
  status?: string;
  currentAuditNode?: string;
  occurTime?: string;
  deadlineTime?: string;
  attachmentUrls?: string;
  assigneeName?: string;
  assigneePhone?: string;
  dispatchRemark?: string;
}

const mapRecord = (item: any): ManualEventRecord => ({
  id: String(item.id || ''),
  eventNo: item.eventNo || null,
  eventType: item.eventType || '',
  title: item.title || '',
  content: item.content || null,
  reportAddress: item.reportAddress || null,
  contactPhone: item.contactPhone || null,
  priority: item.priority || null,
  status: item.status || null,
  reporterId: item.reporterId != null ? String(item.reporterId) : null,
  reporterName: item.reporterName || null,
  projectId: item.projectId != null ? String(item.projectId) : null,
  projectName: item.projectName || null,
  siteId: item.siteId != null ? String(item.siteId) : null,
  siteName: item.siteName || null,
  vehicleId: item.vehicleId != null ? String(item.vehicleId) : null,
  vehicleNo: item.vehicleNo || null,
  sourceChannel: item.sourceChannel || null,
  currentAuditNode: item.currentAuditNode || null,
  occurTime: item.occurTime || null,
  deadlineTime: item.deadlineTime || null,
  reportTime: item.reportTime || null,
  closeTime: item.closeTime || null,
  closeRemark: item.closeRemark || null,
  attachmentUrls: item.attachmentUrls || null,
  assigneeName: item.assigneeName || null,
  assigneePhone: item.assigneePhone || null,
  dispatchRemark: item.dispatchRemark || null,
  isOverdue: Boolean(item.isOverdue),
  auditCount: item.auditCount != null ? Number(item.auditCount) : null,
  lastAuditTime: item.lastAuditTime || null,
  lastAuditAction: item.lastAuditAction || null,
});

const mapLog = (item: any): ManualEventAuditLogRecord => ({
  id: String(item.id || ''),
  nodeCode: item.nodeCode || null,
  action: item.action || null,
  resultStatus: item.resultStatus || null,
  auditorName: item.auditorName || null,
  comment: item.comment || null,
  auditTime: item.auditTime || null,
});

export async function fetchManualEvents(params: Record<string, any> = {}) {
  const res = await http.get<ManualEventRecord[]>('/events', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchPendingAuditEvents() {
  const res = await http.get<ManualEventRecord[]>('/events/pending-audits');
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchManualEventSummary(params: Record<string, any> = {}): Promise<ManualEventSummaryRecord> {
  const res = await http.get<any>('/events/summary', { params });
  return {
    total: Number(res.data.total || 0),
    draftCount: Number(res.data.draftCount || 0),
    pendingAuditCount: Number(res.data.pendingAuditCount || 0),
    processingCount: Number(res.data.processingCount || 0),
    rejectedCount: Number(res.data.rejectedCount || 0),
    closedCount: Number(res.data.closedCount || 0),
    highPriorityCount: Number(res.data.highPriorityCount || 0),
    overdueCount: Number(res.data.overdueCount || 0),
    todayCount: Number(res.data.todayCount || 0),
    typeBuckets: Array.isArray(res.data.typeBuckets) ? res.data.typeBuckets : [],
    sourceBuckets: Array.isArray(res.data.sourceBuckets) ? res.data.sourceBuckets : [],
  };
}

export async function fetchManualEventDetail(id: string) {
  const res = await http.get<any>(`/events/${id}`);
  return {
    record: mapRecord(res.data.record || {}),
    auditLogs: (res.data.auditLogs || []).map(mapLog),
  } as ManualEventDetailRecord;
}

export async function createManualEvent(payload: ManualEventPayload) {
  const res = await http.post<any>('/events', payload);
  return {
    record: mapRecord(res.data.record || {}),
    auditLogs: (res.data.auditLogs || []).map(mapLog),
  } as ManualEventDetailRecord;
}

export async function updateManualEvent(id: string, payload: ManualEventPayload) {
  const res = await http.put<any>(`/events/${id}`, payload);
  return {
    record: mapRecord(res.data.record || {}),
    auditLogs: (res.data.auditLogs || []).map(mapLog),
  } as ManualEventDetailRecord;
}

export async function submitManualEvent(id: string) {
  await http.post(`/events/${id}/submit`);
}

export async function approveManualEvent(id: string, comment?: string) {
  await http.post(`/events/${id}/approve`, { comment });
}

export async function rejectManualEvent(id: string, comment?: string) {
  await http.post(`/events/${id}/reject`, { comment });
}

export async function closeManualEvent(id: string, comment?: string) {
  await http.post(`/events/${id}/close`, { comment });
}

export async function exportManualEvents(params: Record<string, any> = {}) {
  const response = await request.get('/events/export', {
    params,
    responseType: 'blob',
  });
  return response.data as Blob;
}
