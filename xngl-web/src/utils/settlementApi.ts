import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface SettlementRecord {
  id: string;
  settlementNo: string;
  settlementType: string;
  targetProjectId?: string | null;
  targetProjectName?: string | null;
  targetSiteId?: string | null;
  targetSiteName?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  totalVolume?: number | null;
  totalAmount?: number | null;
  adjustAmount?: number | null;
  payableAmount?: number | null;
  approvalStatus?: string | null;
  settlementStatus?: string | null;
  creatorId?: string | null;
  createTime?: string | null;
}

export interface SettlementLine {
  id: string;
  sourceRecordType?: string | null;
  sourceRecordId?: string | null;
  projectId?: string | null;
  siteId?: string | null;
  vehicleId?: string | null;
  bizDate?: string | null;
  volume?: number | null;
  unitPrice?: number | null;
  amount?: number | null;
  remark?: string | null;
}

export interface SettlementDetail extends SettlementRecord {
  unitPrice?: number | null;
  settlementDate?: string | null;
  processInstanceId?: string | null;
  remark?: string | null;
  items?: SettlementLine[];
}

export interface SettlementStats {
  pendingAmount?: number | null;
  settledAmount?: number | null;
  totalOrders?: number | null;
  draftOrders?: number | null;
  pendingOrders?: number | null;
  settledOrders?: number | null;
}

export interface SettlementQueryParams {
  settlementType?: string;
  status?: string;
  projectId?: string | number;
  siteId?: string | number;
  pageNo?: number;
  pageSize?: number;
}

export interface GenerateSettlementPayload {
  targetId: string | number;
  periodStart: string;
  periodEnd: string;
  remark?: string;
}

export async function fetchSettlementStats() {
  const res = await http.get<SettlementStats>('/settlements/stats');
  return res.data;
}

export async function fetchSettlementList(params: SettlementQueryParams = {}) {
  const res = await http.get<PageResult<SettlementRecord>>('/settlements', { params });
  return res.data;
}

export async function fetchSettlementDetail(id: string | number) {
  const res = await http.get<SettlementDetail>(`/settlements/${id}`);
  return res.data;
}

export async function submitSettlement(id: string | number) {
  const res = await http.post<void>(`/settlements/${id}/submit`);
  return res.data;
}

export async function approveSettlement(id: string | number) {
  const res = await http.post<void>(`/settlements/${id}/approve`);
  return res.data;
}

export async function rejectSettlement(id: string | number, reason?: string) {
  const res = await http.post<void>(`/settlements/${id}/reject`, reason ? { reason } : {});
  return res.data;
}

export async function generateProjectSettlement(payload: GenerateSettlementPayload) {
  const res = await http.post<string>('/settlements/project/generate', payload);
  return res.data;
}

export async function generateSiteSettlement(payload: GenerateSettlementPayload) {
  const res = await http.post<string>('/settlements/site/generate', payload);
  return res.data;
}
