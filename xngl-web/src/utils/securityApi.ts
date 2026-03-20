import http from './request';

export interface SecurityInspectionRecord {
  id: string;
  inspectionNo?: string | null;
  objectType: string;
  objectId?: string | null;
  title: string;
  checkScene?: string | null;
  checkType?: string | null;
  hazardCategory?: string | null;
  resultLevel?: string | null;
  dangerLevel?: string | null;
  issueCount?: number | null;
  status?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  vehicleId?: string | null;
  vehicleNo?: string | null;
  userId?: string | null;
  inspectorId?: string | null;
  inspectorName?: string | null;
  rectifyOwner?: string | null;
  rectifyOwnerPhone?: string | null;
  description?: string | null;
  attachmentUrls?: string | null;
  estimatedCost?: number | null;
  rectifyDeadline?: string | null;
  rectifyRemark?: string | null;
  rectifyTime?: string | null;
  checkTime?: string | null;
  nextCheckTime?: string | null;
  isOverdue?: boolean;
}

export interface SecuritySummaryRecord {
  monthInspectionCount: number;
  issueCount: number;
  closedIssueCount: number;
  openInspectionCount: number;
  failCount: number;
  passCount: number;
  rectifyingCount: number;
  overdueRectifyCount: number;
  objectTypeBuckets: Record<string, number>;
  dangerLevelBuckets: Array<{ code: string; count: number }>;
  hazardCategoryBuckets: Array<{ code: string; count: number }>;
}

export interface SecurityInspectionPayload {
  objectType: string;
  objectId?: number;
  title: string;
  checkScene?: string;
  checkType?: string;
  hazardCategory?: string;
  resultLevel?: string;
  dangerLevel?: string;
  issueCount?: number;
  status?: string;
  projectId?: number;
  siteId?: number;
  vehicleId?: number;
  userId?: number;
  rectifyOwner?: string;
  rectifyOwnerPhone?: string;
  description?: string;
  attachmentUrls?: string;
  estimatedCost?: number;
  rectifyDeadline?: string;
  checkTime?: string;
  nextCheckTime?: string;
}

const mapRecord = (item: any): SecurityInspectionRecord => ({
  id: String(item.id || ''),
  inspectionNo: item.inspectionNo || null,
  objectType: item.objectType || '',
  objectId: item.objectId != null ? String(item.objectId) : null,
  title: item.title || '',
  checkScene: item.checkScene || null,
  checkType: item.checkType || null,
  hazardCategory: item.hazardCategory || null,
  resultLevel: item.resultLevel || null,
  dangerLevel: item.dangerLevel || null,
  issueCount: Number(item.issueCount || 0),
  status: item.status || null,
  projectId: item.projectId != null ? String(item.projectId) : null,
  projectName: item.projectName || null,
  siteId: item.siteId != null ? String(item.siteId) : null,
  siteName: item.siteName || null,
  vehicleId: item.vehicleId != null ? String(item.vehicleId) : null,
  vehicleNo: item.vehicleNo || null,
  userId: item.userId != null ? String(item.userId) : null,
  inspectorId: item.inspectorId != null ? String(item.inspectorId) : null,
  inspectorName: item.inspectorName || null,
  rectifyOwner: item.rectifyOwner || null,
  rectifyOwnerPhone: item.rectifyOwnerPhone || null,
  description: item.description || null,
  attachmentUrls: item.attachmentUrls || null,
  estimatedCost: item.estimatedCost != null ? Number(item.estimatedCost) : null,
  rectifyDeadline: item.rectifyDeadline || null,
  rectifyRemark: item.rectifyRemark || null,
  rectifyTime: item.rectifyTime || null,
  checkTime: item.checkTime || null,
  nextCheckTime: item.nextCheckTime || null,
  isOverdue: Boolean(item.isOverdue),
});

export async function fetchSecurityInspections(params: Record<string, any> = {}) {
  const res = await http.get<SecurityInspectionRecord[]>('/security/inspections', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchSecurityInspectionDetail(id: string) {
  const res = await http.get<SecurityInspectionRecord>(`/security/inspections/${id}`);
  return mapRecord(res.data || {});
}

export async function fetchSecuritySummary() {
  const res = await http.get<Partial<SecuritySummaryRecord>>('/security/inspections/summary');
  return {
    monthInspectionCount: Number(res.data.monthInspectionCount || 0),
    issueCount: Number(res.data.issueCount || 0),
    closedIssueCount: Number(res.data.closedIssueCount || 0),
    openInspectionCount: Number(res.data.openInspectionCount || 0),
    failCount: Number(res.data.failCount || 0),
    passCount: Number(res.data.passCount || 0),
    rectifyingCount: Number(res.data.rectifyingCount || 0),
    overdueRectifyCount: Number(res.data.overdueRectifyCount || 0),
    objectTypeBuckets: res.data.objectTypeBuckets || {},
    dangerLevelBuckets: Array.isArray(res.data.dangerLevelBuckets) ? res.data.dangerLevelBuckets : [],
    hazardCategoryBuckets: Array.isArray(res.data.hazardCategoryBuckets) ? res.data.hazardCategoryBuckets : [],
  };
}

export async function createSecurityInspection(payload: SecurityInspectionPayload) {
  const res = await http.post<SecurityInspectionRecord>('/security/inspections', payload);
  return mapRecord(res.data || {});
}

export async function rectifySecurityInspection(
  id: string,
  payload: { status?: string; resultLevel?: string; rectifyRemark?: string; nextCheckTime?: string },
) {
  await http.post(`/security/inspections/${id}/rectify`, payload);
}
