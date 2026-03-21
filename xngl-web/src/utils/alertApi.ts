import http from './request';

export interface AlertRecord {
  id: string;
  alertNo?: string | null;
  title: string;
  alertType?: string | null;
  ruleCode?: string | null;
  targetType?: string | null;
  targetId?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  vehicleId?: string | null;
  vehicleNo?: string | null;
  userId?: string | null;
  userName?: string | null;
  contractId?: string | null;
  contractNo?: string | null;
  targetName?: string | null;
  relatedId?: string | null;
  relatedType?: string | null;
  level?: string | null;
  status?: string | null;
  content?: string | null;
  sourceChannel?: string | null;
  snapshotJson?: string | null;
  latestPositionJson?: string | null;
  handleRemark?: string | null;
  occurTime?: string | null;
  resolveTime?: string | null;
  durationMinutes?: number | null;
}

export interface AlertSummaryRecord {
  total: number;
  pending: number;
  processing: number;
  closed: number;
  confirmed: number;
  vehicleCount: number;
  siteCount: number;
  projectCount: number;
  contractCount: number;
  userCount: number;
  highRiskCount: number;
  avgHandleMinutes: number;
  overdueCount: number;
  enabledRuleCount: number;
  enabledFenceCount: number;
  enabledPushCount: number;
}

export interface AlertBucketRecord {
  code: string;
  label: string;
  count: number;
}

export interface AlertTrendRecord {
  date: string;
  count: number;
}

export interface AlertAnalyticsRecord {
  levelBuckets: AlertBucketRecord[];
  targetBuckets: AlertBucketRecord[];
  sourceBuckets: AlertBucketRecord[];
  statusBuckets: AlertBucketRecord[];
  ruleBuckets: AlertBucketRecord[];
  trend7d: AlertTrendRecord[];
  modelCoverage: {
    totalRules: number;
    enabledRules: number;
    connectedFenceRules: number;
    connectedPushRules: number;
    sceneCoverage: Record<string, number>;
  };
}

export interface AlertTopRiskRecord {
  targetId: string;
  targetType: string;
  targetName: string;
  extraName?: string | null;
  count: number;
}

const mapRecord = (item: any): AlertRecord => ({
  id: String(item.id || ''),
  alertNo: item.alertNo || null,
  title: item.title || '',
  alertType: item.alertType || null,
  ruleCode: item.ruleCode || null,
  targetType: item.targetType || null,
  targetId: item.targetId != null ? String(item.targetId) : null,
  projectId: item.projectId != null ? String(item.projectId) : null,
  projectName: item.projectName || null,
  siteId: item.siteId != null ? String(item.siteId) : null,
  siteName: item.siteName || null,
  vehicleId: item.vehicleId != null ? String(item.vehicleId) : null,
  vehicleNo: item.vehicleNo || null,
  userId: item.userId != null ? String(item.userId) : null,
  userName: item.userName || null,
  contractId: item.contractId != null ? String(item.contractId) : null,
  contractNo: item.contractNo || null,
  targetName: item.targetName || null,
  relatedId: item.relatedId != null ? String(item.relatedId) : null,
  relatedType: item.relatedType || null,
  level: item.level || null,
  status: item.status || null,
  content: item.content || null,
  sourceChannel: item.sourceChannel || null,
  snapshotJson: item.snapshotJson || null,
  latestPositionJson: item.latestPositionJson || null,
  handleRemark: item.handleRemark || null,
  occurTime: item.occurTime || null,
  resolveTime: item.resolveTime || null,
  durationMinutes: item.durationMinutes != null ? Number(item.durationMinutes) : null,
});

const mapBucket = (item: any): AlertBucketRecord => ({
  code: item.code || '',
  label: item.label || item.code || '-',
  count: Number(item.count || 0),
});

export async function fetchAlerts(params: Record<string, any> = {}) {
  const res = await http.get<AlertRecord[]>('/alerts', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchAlertDetail(id: string) {
  const res = await http.get<AlertRecord>(`/alerts/${id}`);
  return mapRecord(res.data || {});
}

export async function fetchAlertSummary() {
  const res = await http.get<Partial<AlertSummaryRecord>>('/alerts/summary');
  return {
    total: Number(res.data.total || 0),
    pending: Number(res.data.pending || 0),
    processing: Number(res.data.processing || 0),
    closed: Number(res.data.closed || 0),
    confirmed: Number(res.data.confirmed || 0),
    vehicleCount: Number(res.data.vehicleCount || 0),
    siteCount: Number(res.data.siteCount || 0),
    projectCount: Number(res.data.projectCount || 0),
    contractCount: Number(res.data.contractCount || 0),
    userCount: Number(res.data.userCount || 0),
    highRiskCount: Number(res.data.highRiskCount || 0),
    avgHandleMinutes: Number(res.data.avgHandleMinutes || 0),
    overdueCount: Number(res.data.overdueCount || 0),
    enabledRuleCount: Number(res.data.enabledRuleCount || 0),
    enabledFenceCount: Number(res.data.enabledFenceCount || 0),
    enabledPushCount: Number(res.data.enabledPushCount || 0),
  };
}

export async function fetchAlertAnalytics(): Promise<AlertAnalyticsRecord> {
  const res = await http.get<any>('/alerts/analytics');
  return {
    levelBuckets: (res.data.levelBuckets || []).map(mapBucket),
    targetBuckets: (res.data.targetBuckets || []).map(mapBucket),
    sourceBuckets: (res.data.sourceBuckets || []).map(mapBucket),
    statusBuckets: (res.data.statusBuckets || []).map(mapBucket),
    ruleBuckets: (res.data.ruleBuckets || []).map(mapBucket),
    trend7d: (res.data.trend7d || []).map((item: any) => ({
      date: item.date || '',
      count: Number(item.count || 0),
    })),
    modelCoverage: {
      totalRules: Number(res.data.modelCoverage?.totalRules || 0),
      enabledRules: Number(res.data.modelCoverage?.enabledRules || 0),
      connectedFenceRules: Number(res.data.modelCoverage?.connectedFenceRules || 0),
      connectedPushRules: Number(res.data.modelCoverage?.connectedPushRules || 0),
      sceneCoverage: res.data.modelCoverage?.sceneCoverage || {},
    },
  };
}

export async function fetchTopRiskVehicles() {
  const res = await http.get<any[]>('/alerts/top-risk');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchTopRiskTargets(targetType: 'VEHICLE' | 'CONTRACT' | 'USER') {
  const res = await http.get<any[]>('/alerts/top-risk-targets', { params: { targetType } });
  return (Array.isArray(res.data) ? res.data : []).map(
    (item): AlertTopRiskRecord => ({
      targetId: String(item.targetId || ''),
      targetType: item.targetType || targetType,
      targetName: item.targetName || '-',
      extraName: item.extraName || null,
      count: Number(item.count || 0),
    }),
  );
}

export async function fetchFenceStatus() {
  const res = await http.get<any[]>('/alerts/fence-status');
  return Array.isArray(res.data) ? res.data : [];
}

export async function generateAlerts(targetTypes?: string[]) {
  const res = await http.post<any>('/alerts/generate', {
    targetTypes: targetTypes?.length ? targetTypes : undefined,
  });
  return {
    targetTypes: Array.isArray(res.data?.targetTypes) ? res.data.targetTypes : [],
    createdCount: Number(res.data?.createdCount || 0),
    updatedCount: Number(res.data?.updatedCount || 0),
    closedCount: Number(res.data?.closedCount || 0),
    activeCount: Number(res.data?.activeCount || 0),
  };
}

export async function handleAlert(id: string, payload: { status?: string; handleRemark?: string }) {
  await http.post(`/alerts/${id}/handle`, payload);
}

export async function closeAlert(id: string, payload: { handleRemark?: string }) {
  await http.post(`/alerts/${id}/close`, payload);
}
