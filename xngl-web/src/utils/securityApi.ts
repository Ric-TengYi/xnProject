import http, { request } from './request';

export interface SecurityInspectionRecord {
  id: string;
  inspectionNo?: string | null;
  objectType: string;
  objectId?: string | null;
  objectName?: string | null;
  objectLabel?: string | null;
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
  userName?: string | null;
  userMobile?: string | null;
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
  relatedProfile?: SecurityRelatedProfile | null;
  relatedProfileSummary?: string | null;
  actions?: SecurityInspectionActionRecord[];
}

export interface SecurityRelatedProfile {
  profileType?: string | null;
  certificateCount?: number | null;
  expiringCertificateCount?: number | null;
  overdueFeeCount?: number | null;
  learningCount?: number | null;
  completedLearningCount?: number | null;
  studyMinutes?: number | null;
  lastStudyTime?: string | null;
  insuranceCount?: number | null;
  activeInsuranceCount?: number | null;
  expiringInsuranceCount?: number | null;
  expiredInsuranceCount?: number | null;
  maintenanceCount?: number | null;
  latestMaintenanceDate?: string | null;
  maintenanceCostTotal?: number | null;
  documentCount?: number | null;
  approvalDocumentCount?: number | null;
  operationDocumentCount?: number | null;
  deviceCount?: number | null;
  onlineDeviceCount?: number | null;
  offlineDeviceCount?: number | null;
  latestDocumentTime?: string | null;
  openAlertCount?: number | null;
  highRiskAlertCount?: number | null;
  certificateOwners?: string[];
}

export interface SecurityInspectionActionRecord {
  id: string;
  actionType?: string | null;
  actionLabel?: string | null;
  beforeStatus?: string | null;
  afterStatus?: string | null;
  beforeResultLevel?: string | null;
  afterResultLevel?: string | null;
  actionRemark?: string | null;
  nextCheckTime?: string | null;
  actorId?: string | null;
  actorName?: string | null;
  actionTime?: string | null;
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

export interface SecurityUserOption {
  id: string;
  username?: string | null;
  name?: string | null;
  mobile?: string | null;
  status?: string | null;
}

const mapAction = (item: any): SecurityInspectionActionRecord => ({
  id: String(item.id || ''),
  actionType: item.actionType || null,
  actionLabel: item.actionLabel || null,
  beforeStatus: item.beforeStatus || null,
  afterStatus: item.afterStatus || null,
  beforeResultLevel: item.beforeResultLevel || null,
  afterResultLevel: item.afterResultLevel || null,
  actionRemark: item.actionRemark || null,
  nextCheckTime: item.nextCheckTime || null,
  actorId: item.actorId != null ? String(item.actorId) : null,
  actorName: item.actorName || null,
  actionTime: item.actionTime || null,
});

const mapRecord = (item: any): SecurityInspectionRecord => ({
  id: String(item.id || ''),
  inspectionNo: item.inspectionNo || null,
  objectType: item.objectType || '',
  objectId: item.objectId != null ? String(item.objectId) : null,
  objectName: item.objectName || null,
  objectLabel: item.objectLabel || null,
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
  userName: item.userName || null,
  userMobile: item.userMobile || null,
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
  relatedProfile: item.relatedProfile
    ? {
        profileType: item.relatedProfile.profileType || null,
        certificateCount:
          item.relatedProfile.certificateCount != null ? Number(item.relatedProfile.certificateCount) : null,
        expiringCertificateCount:
          item.relatedProfile.expiringCertificateCount != null
            ? Number(item.relatedProfile.expiringCertificateCount)
            : null,
        overdueFeeCount:
          item.relatedProfile.overdueFeeCount != null ? Number(item.relatedProfile.overdueFeeCount) : null,
        learningCount: item.relatedProfile.learningCount != null ? Number(item.relatedProfile.learningCount) : null,
        completedLearningCount:
          item.relatedProfile.completedLearningCount != null
            ? Number(item.relatedProfile.completedLearningCount)
            : null,
        studyMinutes: item.relatedProfile.studyMinutes != null ? Number(item.relatedProfile.studyMinutes) : null,
        lastStudyTime: item.relatedProfile.lastStudyTime || null,
        insuranceCount:
          item.relatedProfile.insuranceCount != null ? Number(item.relatedProfile.insuranceCount) : null,
        activeInsuranceCount:
          item.relatedProfile.activeInsuranceCount != null ? Number(item.relatedProfile.activeInsuranceCount) : null,
        expiringInsuranceCount:
          item.relatedProfile.expiringInsuranceCount != null
            ? Number(item.relatedProfile.expiringInsuranceCount)
            : null,
        expiredInsuranceCount:
          item.relatedProfile.expiredInsuranceCount != null ? Number(item.relatedProfile.expiredInsuranceCount) : null,
        maintenanceCount:
          item.relatedProfile.maintenanceCount != null ? Number(item.relatedProfile.maintenanceCount) : null,
        latestMaintenanceDate: item.relatedProfile.latestMaintenanceDate || null,
        maintenanceCostTotal:
          item.relatedProfile.maintenanceCostTotal != null
            ? Number(item.relatedProfile.maintenanceCostTotal)
            : null,
        documentCount: item.relatedProfile.documentCount != null ? Number(item.relatedProfile.documentCount) : null,
        approvalDocumentCount:
          item.relatedProfile.approvalDocumentCount != null
            ? Number(item.relatedProfile.approvalDocumentCount)
            : null,
        operationDocumentCount:
          item.relatedProfile.operationDocumentCount != null
            ? Number(item.relatedProfile.operationDocumentCount)
            : null,
        deviceCount: item.relatedProfile.deviceCount != null ? Number(item.relatedProfile.deviceCount) : null,
        onlineDeviceCount:
          item.relatedProfile.onlineDeviceCount != null ? Number(item.relatedProfile.onlineDeviceCount) : null,
        offlineDeviceCount:
          item.relatedProfile.offlineDeviceCount != null ? Number(item.relatedProfile.offlineDeviceCount) : null,
        latestDocumentTime: item.relatedProfile.latestDocumentTime || null,
        openAlertCount: item.relatedProfile.openAlertCount != null ? Number(item.relatedProfile.openAlertCount) : null,
        highRiskAlertCount:
          item.relatedProfile.highRiskAlertCount != null ? Number(item.relatedProfile.highRiskAlertCount) : null,
        certificateOwners: Array.isArray(item.relatedProfile.certificateOwners)
          ? item.relatedProfile.certificateOwners.map((entry: any) => String(entry))
          : [],
      }
    : null,
  relatedProfileSummary: item.relatedProfileSummary || null,
  actions: Array.isArray(item.actions) ? item.actions.map(mapAction) : [],
});

export async function fetchSecurityInspections(params: Record<string, any> = {}) {
  const res = await http.get<SecurityInspectionRecord[]>('/security/inspections', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchSecurityInspectionDetail(id: string) {
  const res = await http.get<SecurityInspectionRecord>(`/security/inspections/${id}`);
  return mapRecord(res.data || {});
}

export async function fetchSecuritySummary(params: Record<string, any> = {}) {
  const res = await http.get<Partial<SecuritySummaryRecord>>('/security/inspections/summary', { params });
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

export async function deleteSecurityInspection(id: string) {
  await http.delete(`/security/inspections/${id}`);
}

export async function exportSecurityInspections(params: Record<string, any> = {}) {
  const response = await request.get('/security/inspections/export', {
    params,
    responseType: 'blob',
  });
  return response.data as Blob;
}

export async function fetchSecurityUsers(params: Record<string, any> = {}) {
  const res = await http.get<any>('/users', { params });
  const records = Array.isArray(res.data?.records) ? res.data.records : [];
  return records.map(
    (item: any): SecurityUserOption => ({
      id: String(item.id || ''),
      username: item.username || null,
      name: item.name || null,
      mobile: item.mobile || null,
      status: item.status || null,
    }),
  );
}
