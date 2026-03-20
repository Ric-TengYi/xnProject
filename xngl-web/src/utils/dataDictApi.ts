import http from './request';

export interface DataDictRecord {
  id: string;
  tenantId?: string | null;
  dictType: string;
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  sort: number;
  status: string;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DataDictPayload {
  dictType: string;
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  sort?: number;
  status?: string;
  remark?: string;
}

const mapRecord = (item: any): DataDictRecord => ({
  id: String(item.id || ''),
  tenantId: item.tenantId ? String(item.tenantId) : null,
  dictType: item.dictType || '',
  dictCode: item.dictCode || '',
  dictLabel: item.dictLabel || '',
  dictValue: item.dictValue || '',
  sort: Number(item.sort || 0),
  status: item.status || 'ENABLED',
  remark: item.remark || null,
  createTime: item.createTime || null,
  updateTime: item.updateTime || null,
});

export async function fetchDataDicts(params: Record<string, any> = {}) {
  const res = await http.get<DataDictRecord[]>('/data-dicts', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function createDataDict(payload: DataDictPayload) {
  const res = await http.post<DataDictRecord>('/data-dicts', payload);
  return mapRecord(res.data || {});
}

export async function updateDataDict(id: string, payload: DataDictPayload) {
  const res = await http.put<DataDictRecord>(`/data-dicts/${id}`, payload);
  return mapRecord(res.data || {});
}

export async function updateDataDictStatus(id: string, status: string) {
  await http.put(`/data-dicts/${id}/status`, { status });
}

export async function deleteDataDict(id: string) {
  await http.delete(`/data-dicts/${id}`);
}
