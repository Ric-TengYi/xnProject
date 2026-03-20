import http from './request';

export interface ApprovalRuleRecord {
  id: string;
  tenantId?: string;
  processKey: string;
  ruleName: string;
  ruleType?: string | null;
  ruleExpression?: string | null;
  priority?: number | null;
  status?: string | null;
}

export interface ApprovalRulePayload {
  tenantId?: string;
  processKey: string;
  ruleName: string;
  ruleType?: string;
  ruleExpression?: string;
  priority?: number;
}

export interface ApprovalMaterialConfigRecord {
  id: string;
  processKey: string;
  materialCode: string;
  materialName: string;
  materialType?: string | null;
  required: boolean;
  sortOrder: number;
  status: string;
  remark?: string | null;
}

export interface ApprovalMaterialConfigPayload {
  processKey: string;
  materialCode: string;
  materialName: string;
  materialType?: string;
  required?: boolean;
  sortOrder?: number;
  status?: string;
  remark?: string;
}

const mapRecord = (item: any): ApprovalRuleRecord => ({
  id: String(item.id || ''),
  tenantId: item.tenantId || '1',
  processKey: item.processKey || '',
  ruleName: item.ruleName || '',
  ruleType: item.ruleType || null,
  ruleExpression: item.ruleExpression || null,
  priority: Number(item.priority || 0),
  status: item.status || 'ENABLED',
});

const mapMaterialRecord = (item: any): ApprovalMaterialConfigRecord => ({
  id: String(item.id || ''),
  processKey: item.processKey || '',
  materialCode: item.materialCode || '',
  materialName: item.materialName || '',
  materialType: item.materialType || null,
  required: Boolean(item.required),
  sortOrder: Number(item.sortOrder || 0),
  status: item.status || 'ENABLED',
  remark: item.remark || null,
});

export async function fetchApprovalRules(params: Record<string, any> = {}) {
  const res = await http.get<{ records: ApprovalRuleRecord[] }>('/approval-actor-rules', { params: { pageSize: 200, ...params } });
  return (res.data.records || []).map(mapRecord);
}

export async function fetchApprovalRuleDetail(id: string) {
  const res = await http.get<ApprovalRuleRecord>(`/approval-actor-rules/${id}`);
  return mapRecord(res.data || {});
}

export async function createApprovalRule(payload: ApprovalRulePayload) {
  const res = await http.post<string>('/approval-actor-rules', payload);
  return String(res.data || '');
}

export async function updateApprovalRule(id: string, payload: ApprovalRulePayload) {
  await http.put(`/approval-actor-rules/${id}`, payload);
}

export async function updateApprovalRuleStatus(id: string, status: string) {
  await http.put(`/approval-actor-rules/${id}/status`, { status });
}

export async function deleteApprovalRule(id: string) {
  await http.delete(`/approval-actor-rules/${id}`);
}

export async function fetchApprovalMaterialConfigs(params: Record<string, any> = {}) {
  const res = await http.get<ApprovalMaterialConfigRecord[]>('/approval-material-configs', {
    params,
  });
  return (Array.isArray(res.data) ? res.data : []).map(mapMaterialRecord);
}

export async function fetchApprovalMaterialConfigDetail(id: string) {
  const res = await http.get<ApprovalMaterialConfigRecord>(
    `/approval-material-configs/${id}`
  );
  return mapMaterialRecord(res.data || {});
}

export async function createApprovalMaterialConfig(
  payload: ApprovalMaterialConfigPayload
) {
  const res = await http.post<string>('/approval-material-configs', payload);
  return String(res.data || '');
}

export async function updateApprovalMaterialConfig(
  id: string,
  payload: ApprovalMaterialConfigPayload
) {
  await http.put(`/approval-material-configs/${id}`, payload);
}

export async function updateApprovalMaterialConfigStatus(
  id: string,
  status: string
) {
  await http.put(`/approval-material-configs/${id}/status`, { status });
}

export async function deleteApprovalMaterialConfig(id: string) {
  await http.delete(`/approval-material-configs/${id}`);
}
