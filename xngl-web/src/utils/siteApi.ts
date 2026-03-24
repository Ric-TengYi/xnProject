import http from './request';

export interface SiteRecord {
  id: string;
  name: string;
  code?: string | null;
  address?: string | null;
  projectId?: number | null;
  status?: number | string | null;
  orgId?: number | null;
  siteType?: string | null;
  capacity?: number | null;
  settlementMode?: string | null;
  disposalUnitPrice?: number | null;
  disposalFeeRate?: number | null;
  serviceFeeUnitPrice?: number | null;
  siteLevel?: string | null;
  parentSiteId?: string | null;
  parentSiteName?: string | null;
  managementArea?: string | null;
  weighbridgeSiteId?: string | null;
  weighbridgeSiteName?: string | null;
  lng?: number | null;
  lat?: number | null;
  boundaryGeoJson?: string | null;
  devices?: SiteDeviceRecord[];
  operationConfig?: SiteOperationConfigRecord | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SitePersonnelRecord {
  id: string;
  userId?: string | null;
  username?: string | null;
  userName?: string | null;
  mobile?: string | null;
  userType?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  roleType?: string | null;
  dutyScope?: string | null;
  shiftGroup?: string | null;
  accountEnabled?: boolean;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SiteDocumentRecord {
  id: string;
  siteId?: string | null;
  siteName?: string | null;
  stageCode?: string | null;
  approvalType?: string | null;
  documentType?: string | null;
  fileName?: string | null;
  fileUrl?: string | null;
  fileSize?: number | null;
  mimeType?: string | null;
  formatRequirement?: string | null;
  uploaderId?: string | null;
  uploaderName?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SiteDocumentSummaryRecord {
  siteId?: string | null;
  siteName?: string | null;
  stageCode?: string | null;
  approvalType?: string | null;
  documentType?: string | null;
  documentCount?: number | null;
  lastUpdateTime?: string | null;
  uploaderName?: string | null;
}

export interface SiteSurveyRecord {
  id: string;
  siteId?: string | null;
  siteName?: string | null;
  surveyNo?: string | null;
  surveyDate?: string | null;
  measuredVolume?: number | null;
  deductionVolume?: number | null;
  settlementVolume?: number | null;
  surveyCompany?: string | null;
  surveyorName?: string | null;
  status?: string | null;
  reportUrl?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SitePersonnelCandidateRecord {
  userId: string;
  username?: string | null;
  userName?: string | null;
  mobile?: string | null;
  userType?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  status?: string | null;
}

export interface SiteDeviceRecord {
  id: string;
  deviceCode?: string | null;
  deviceName?: string | null;
  deviceType?: string | null;
  provider?: string | null;
  ipAddress?: string | null;
  status?: string | null;
  lng?: number | null;
  lat?: number | null;
  remark?: string | null;
}

export interface SiteMapLayerRecord {
  id: string;
  name: string;
  code?: string | null;
  siteType?: string | null;
  status?: number | string | null;
  lng?: number | null;
  lat?: number | null;
  boundaryGeoJson?: string | null;
  devices?: SiteDeviceRecord[];
}

export interface SiteOperationConfigRecord {
  queueEnabled?: boolean;
  maxQueueCount?: number | null;
  manualDisposalEnabled?: boolean;
  rangeCheckRadius?: number | null;
  durationLimitMinutes?: number | null;
  remark?: string | null;
}

export interface DisposalRecord {
  id: string;
  siteId?: string | null;
  site?: string | null;
  time?: string | null;
  plate?: string | null;
  project?: string | null;
  source?: string | null;
  volume?: number | null;
  status?: string | null;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface SiteDisposalParams {
  siteId?: string;
  keyword?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface SiteDeviceUpsertPayload {
  deviceCode: string;
  deviceName: string;
  deviceType?: string;
  provider?: string;
  ipAddress?: string;
  status?: string;
  lng?: number;
  lat?: number;
  remark?: string;
}

export interface SiteOperationConfigPayload {
  queueEnabled?: boolean;
  maxQueueCount?: number;
  manualDisposalEnabled?: boolean;
  rangeCheckRadius?: number;
  durationLimitMinutes?: number;
  remark?: string;
}

export interface SitePersonnelUpsertPayload {
  userId: string;
  roleType?: string;
  dutyScope?: string;
  shiftGroup?: string;
  accountEnabled?: boolean;
  remark?: string;
}

export interface SiteDocumentUpsertPayload {
  stageCode: string;
  approvalType?: string;
  documentType: string;
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  mimeType?: string;
  remark?: string;
}

export interface SiteCreatePayload {
  name: string;
  code?: string;
  address?: string;
  projectId?: number;
  status?: number;
  orgId?: number;
  siteType?: string;
  capacity?: number;
  settlementMode?: string;
  disposalUnitPrice?: number;
  disposalFeeRate?: number;
  serviceFeeUnitPrice?: number;
  siteLevel?: string;
  parentSiteId?: number;
  managementArea?: string;
  weighbridgeSiteId?: number;
  lng?: number;
  lat?: number;
  boundaryGeoJson?: string;
}

export interface SiteSurveyUpsertPayload {
  surveyNo: string;
  surveyDate: string;
  measuredVolume?: number;
  deductionVolume?: number;
  surveyCompany?: string;
  surveyorName?: string;
  status?: string;
  reportUrl?: string;
  remark?: string;
}

export async function fetchSites() {
  const res = await http.get<SiteRecord[]>('/sites');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchSiteDetail(id: string) {
  const res = await http.get<SiteRecord>(`/sites/${id}`);
  return res.data;
}

export async function createSite(payload: SiteCreatePayload) {
  const res = await http.post<SiteRecord>('/sites', payload);
  return res.data;
}

export async function fetchSiteMapLayers() {
  const res = await http.get<SiteMapLayerRecord[]>('/sites/map-layers');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchSiteDisposals(params: SiteDisposalParams = {}) {
  const res = await http.get<PageResult<DisposalRecord>>('/sites/disposals', { params });
  return res.data;
}

export async function createSiteDevice(siteId: string, payload: SiteDeviceUpsertPayload) {
  const res = await http.post<SiteDeviceRecord>(`/sites/${siteId}/devices`, payload);
  return res.data;
}

export async function updateSiteDevice(siteId: string, deviceId: string, payload: SiteDeviceUpsertPayload) {
  const res = await http.put<SiteDeviceRecord>(`/sites/${siteId}/devices/${deviceId}`, payload);
  return res.data;
}

export async function updateSiteOperationConfig(siteId: string, payload: SiteOperationConfigPayload) {
  const res = await http.put<SiteOperationConfigRecord>(`/sites/${siteId}/operation-config`, payload);
  return res.data;
}

export async function fetchSitePersonnel(siteId: string) {
  const res = await http.get<SitePersonnelRecord[]>(`/sites/${siteId}/personnel`);
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchSitePersonnelCandidates(siteId: string) {
  const res = await http.get<SitePersonnelCandidateRecord[]>(`/sites/${siteId}/personnel/candidates`);
  return Array.isArray(res.data) ? res.data : [];
}

export async function createSitePersonnel(siteId: string, payload: SitePersonnelUpsertPayload) {
  const res = await http.post<SitePersonnelRecord>(`/sites/${siteId}/personnel`, {
    ...payload,
    userId: Number(payload.userId),
  });
  return res.data;
}

export async function updateSitePersonnel(siteId: string, personnelId: string, payload: SitePersonnelUpsertPayload) {
  const res = await http.put<SitePersonnelRecord>(`/sites/${siteId}/personnel/${personnelId}`, {
    ...payload,
    userId: Number(payload.userId),
  });
  return res.data;
}

export async function deleteSitePersonnel(siteId: string, personnelId: string) {
  await http.delete(`/sites/${siteId}/personnel/${personnelId}`);
}

export async function fetchSiteDocuments(siteId: string, stageCode?: string) {
  const res = await http.get<SiteDocumentRecord[]>(`/sites/${siteId}/documents`, { params: { stageCode } });
  return Array.isArray(res.data) ? res.data : [];
}

export async function createSiteDocument(siteId: string, payload: SiteDocumentUpsertPayload) {
  const res = await http.post<SiteDocumentRecord>(`/sites/${siteId}/documents`, payload);
  return res.data;
}

export async function updateSiteDocument(siteId: string, documentId: string, payload: SiteDocumentUpsertPayload) {
  const res = await http.put<SiteDocumentRecord>(`/sites/${siteId}/documents/${documentId}`, payload);
  return res.data;
}

export async function deleteSiteDocument(siteId: string, documentId: string) {
  await http.delete(`/sites/${siteId}/documents/${documentId}`);
}

export async function fetchSiteDocumentSummary(
  params: { siteId?: string; stageCode?: string; approvalType?: string; keyword?: string } = {},
) {
  const res = await http.get<SiteDocumentSummaryRecord[]>('/site-documents/summary', { params });
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchSiteSurveys(siteId: string) {
  const res = await http.get<SiteSurveyRecord[]>(`/sites/${siteId}/surveys`);
  return Array.isArray(res.data) ? res.data : [];
}

export async function createSiteSurvey(siteId: string, payload: SiteSurveyUpsertPayload) {
  const res = await http.post<SiteSurveyRecord>(`/sites/${siteId}/surveys`, payload);
  return res.data;
}

export async function updateSiteSurvey(siteId: string, surveyId: string, payload: SiteSurveyUpsertPayload) {
  const res = await http.put<SiteSurveyRecord>(`/sites/${siteId}/surveys/${surveyId}`, payload);
  return res.data;
}

export async function deleteSiteSurvey(siteId: string, surveyId: string) {
  await http.delete(`/sites/${siteId}/surveys/${surveyId}`);
}
