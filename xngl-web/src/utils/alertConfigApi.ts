import http from './request';

export interface AlertRuleRecord {
  id: string;
  ruleCode: string;
  ruleName: string;
  ruleScene: string;
  metricCode?: string | null;
  thresholdJson?: string | null;
  level?: string | null;
  status?: string | null;
  scopeType?: string | null;
  remark?: string | null;
}

export interface AlertFenceRecord {
  id: string;
  ruleCode?: string | null;
  fenceCode: string;
  fenceName: string;
  fenceType: string;
  geoJson?: string | null;
  bufferMeters?: number | null;
  bizScope?: string | null;
  activeTimeRange?: string | null;
  directionRule?: string | null;
  status?: string | null;
}

export interface AlertPushRuleRecord {
  id: string;
  ruleCode: string;
  level: string;
  channelTypes: string;
  receiverType?: string | null;
  receiverExpr: string;
  pushTimeRule?: string | null;
  escalationMinutes?: number | null;
  status?: string | null;
}

const mapRule = (item: any): AlertRuleRecord => ({
  id: String(item.id || ''),
  ruleCode: item.ruleCode || '',
  ruleName: item.ruleName || '',
  ruleScene: item.ruleScene || 'VEHICLE',
  metricCode: item.metricCode || null,
  thresholdJson: item.thresholdJson || null,
  level: item.level || 'L2',
  status: item.status || 'ENABLED',
  scopeType: item.scopeType || 'GLOBAL',
  remark: item.remark || null,
});

const mapFence = (item: any): AlertFenceRecord => ({
  id: String(item.id || ''),
  ruleCode: item.ruleCode || null,
  fenceCode: item.fenceCode || '',
  fenceName: item.fenceName || '',
  fenceType: item.fenceType || 'ENTRY',
  geoJson: item.geoJson || null,
  bufferMeters: Number(item.bufferMeters || 0),
  bizScope: item.bizScope || null,
  activeTimeRange: item.activeTimeRange || null,
  directionRule: item.directionRule || null,
  status: item.status || 'ENABLED',
});

const mapPushRule = (item: any): AlertPushRuleRecord => ({
  id: String(item.id || ''),
  ruleCode: item.ruleCode || '',
  level: item.level || 'L2',
  channelTypes: item.channelTypes || '',
  receiverType: item.receiverType || 'ROLE',
  receiverExpr: item.receiverExpr || '',
  pushTimeRule: item.pushTimeRule || null,
  escalationMinutes: Number(item.escalationMinutes || 0),
  status: item.status || 'ENABLED',
});

export async function fetchAlertRules() {
  const res = await http.get<AlertRuleRecord[]>('/alert-rules');
  return (Array.isArray(res.data) ? res.data : []).map(mapRule);
}

export async function createAlertRule(payload: Partial<AlertRuleRecord>) {
  const res = await http.post<AlertRuleRecord>('/alert-rules', payload);
  return mapRule(res.data || {});
}

export async function updateAlertRule(id: string, payload: Partial<AlertRuleRecord>) {
  const res = await http.put<AlertRuleRecord>(`/alert-rules/${id}`, payload);
  return mapRule(res.data || {});
}

export async function updateAlertRuleStatus(id: string, status: string) {
  await http.put(`/alert-rules/${id}/status`, { status });
}

export async function fetchAlertFences() {
  const res = await http.get<AlertFenceRecord[]>('/alert-fences');
  return (Array.isArray(res.data) ? res.data : []).map(mapFence);
}

export async function createAlertFence(payload: Partial<AlertFenceRecord>) {
  const res = await http.post<AlertFenceRecord>('/alert-fences', payload);
  return mapFence(res.data || {});
}

export async function updateAlertFence(id: string, payload: Partial<AlertFenceRecord>) {
  const res = await http.put<AlertFenceRecord>(`/alert-fences/${id}`, payload);
  return mapFence(res.data || {});
}

export async function updateAlertFenceStatus(id: string, status: string) {
  await http.put(`/alert-fences/${id}/status`, { status });
}

export async function fetchAlertPushRules() {
  const res = await http.get<AlertPushRuleRecord[]>('/alert-push-rules');
  return (Array.isArray(res.data) ? res.data : []).map(mapPushRule);
}

export async function createAlertPushRule(payload: Partial<AlertPushRuleRecord>) {
  const res = await http.post<AlertPushRuleRecord>('/alert-push-rules', payload);
  return mapPushRule(res.data || {});
}

export async function updateAlertPushRule(id: string, payload: Partial<AlertPushRuleRecord>) {
  const res = await http.put<AlertPushRuleRecord>(`/alert-push-rules/${id}`, payload);
  return mapPushRule(res.data || {});
}

export async function updateAlertPushRuleStatus(id: string, status: string) {
  await http.put(`/alert-push-rules/${id}/status`, { status });
}
