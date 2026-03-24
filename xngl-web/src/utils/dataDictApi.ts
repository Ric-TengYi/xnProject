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
  dictCode?: string;
  dictLabel: string;
  dictValue?: string;
  sort?: number;
  status?: string;
  remark?: string;
}

export interface DictTypeItem {
  typeCode: string;
  typeLabel: string;
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

export async function exportDataDicts(params: Record<string, any> = {}) {
  const res = await http.get<Blob>('/data-dicts/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}

/**
 * 平台预置的字典类型列表。
 * 字典类型由平台开发阶段确定，不开放用户新增，因此直接在前端维护映射。
 */
export const PREDEFINED_DICT_TYPES: DictTypeItem[] = [
  { typeCode: 'ORG_TYPE', typeLabel: '组织类型' },
  { typeCode: 'alert_level', typeLabel: '预警级别' },
  { typeCode: 'event_type', typeLabel: '事件类型' },
  { typeCode: 'VEHICLE_MODEL', typeLabel: '车型分类' },
  { typeCode: 'contract_type', typeLabel: '合同类型' },
  { typeCode: 'settlement_type', typeLabel: '结算方式' },
];

export function fetchDictTypes(): DictTypeItem[] {
  return PREDEFINED_DICT_TYPES;
}
