import http from './request';

export interface DisposalPermitRecord {
  id: number | string;
  tenantId?: number | string | null;
  permitNo: string;
  permitType?: string | null;
  projectId?: number | string | null;
  contractId?: number | string | null;
  siteId?: number | string | null;
  vehicleNo?: string | null;
  issueDate?: string | null;
  expireDate?: string | null;
  approvedVolume?: number | null;
  usedVolume?: number | null;
  status?: string | null;
  bindStatus?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DisposalPermitUpsertPayload {
  permitNo: string;
  permitType?: string;
  projectId?: number;
  contractId?: number;
  siteId?: number;
  vehicleNo?: string;
  issueDate?: string;
  expireDate?: string;
  approvedVolume?: number;
  usedVolume?: number;
  status?: string;
  remark?: string;
}

const mapRecord = (record: Partial<DisposalPermitRecord>): DisposalPermitRecord => ({
  id: record.id || '',
  tenantId: record.tenantId ?? null,
  permitNo: record.permitNo || '',
  permitType: record.permitType || 'DISPOSAL',
  projectId: record.projectId ?? null,
  contractId: record.contractId ?? null,
  siteId: record.siteId ?? null,
  vehicleNo: record.vehicleNo || null,
  issueDate: record.issueDate || null,
  expireDate: record.expireDate || null,
  approvedVolume: Number(record.approvedVolume || 0),
  usedVolume: Number(record.usedVolume || 0),
  status: record.status || 'ACTIVE',
  bindStatus: record.bindStatus || 'UNBOUND',
  remark: record.remark || null,
  createTime: record.createTime || null,
  updateTime: record.updateTime || null,
});

export async function fetchDisposalPermits(params: Record<string, any> = {}) {
  const res = await http.get<DisposalPermitRecord[]>('/disposal-permits', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchDisposalPermitDetail(id: string | number) {
  const res = await http.get<DisposalPermitRecord>(`/disposal-permits/${id}`);
  return mapRecord(res.data || {});
}

export async function createDisposalPermit(payload: DisposalPermitUpsertPayload) {
  const res = await http.post<DisposalPermitRecord>('/disposal-permits', payload);
  return mapRecord(res.data || {});
}

export async function updateDisposalPermit(id: string | number, payload: DisposalPermitUpsertPayload) {
  const res = await http.put<DisposalPermitRecord>(`/disposal-permits/${id}`, payload);
  return mapRecord(res.data || {});
}
