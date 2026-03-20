import http from './request';

export interface SysParamRecord {
  id: string;
  tenantId?: string | null;
  paramKey: string;
  paramName: string;
  paramValue?: string | null;
  paramType: string;
  status: string;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SysParamPayload {
  paramKey: string;
  paramName: string;
  paramValue?: string;
  paramType?: string;
  status?: string;
  remark?: string;
}

const mapRecord = (item: any): SysParamRecord => ({
  id: String(item.id || ''),
  tenantId: item.tenantId ? String(item.tenantId) : null,
  paramKey: item.paramKey || '',
  paramName: item.paramName || '',
  paramValue: item.paramValue || null,
  paramType: item.paramType || 'STRING',
  status: item.status || 'ENABLED',
  remark: item.remark || null,
  createTime: item.createTime || null,
  updateTime: item.updateTime || null,
});

export async function fetchSysParams(params: Record<string, any> = {}) {
  const res = await http.get<SysParamRecord[]>('/sys-params', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function createSysParam(payload: SysParamPayload) {
  const res = await http.post<SysParamRecord>('/sys-params', payload);
  return mapRecord(res.data || {});
}

export async function updateSysParam(id: string, payload: SysParamPayload) {
  const res = await http.put<SysParamRecord>(`/sys-params/${id}`, payload);
  return mapRecord(res.data || {});
}

export async function updateSysParamStatus(id: string, status: string) {
  await http.put(`/sys-params/${id}/status`, { status });
}

export async function deleteSysParam(id: string) {
  await http.delete(`/sys-params/${id}`);
}
