import http from './request';

export interface PlatformIntegrationOverview {
  enabledCount: number;
  totalCount: number;
  videoChannelCount: number;
  onlineDamSiteCount: number;
  activeSsoTicketCount: number;
  govSyncCount: number;
  weighbridgeRecordCount: number;
}

export interface PlatformIntegrationConfig {
  integrationCode: string;
  integrationName: string;
  enabled: boolean;
  vendorName?: string | null;
  baseUrl?: string | null;
  apiVersion?: string | null;
  clientId?: string | null;
  clientSecret?: string | null;
  accessKey?: string | null;
  accessSecret?: string | null;
  callbackPath?: string | null;
  extJson?: string | null;
  remark?: string | null;
  updateTime?: string | null;
}

export interface PlatformIntegrationConfigPayload {
  enabled?: boolean;
  vendorName?: string;
  baseUrl?: string;
  apiVersion?: string;
  clientId?: string;
  clientSecret?: string;
  accessKey?: string;
  accessSecret?: string;
  callbackPath?: string;
  extJson?: string;
  remark?: string;
}

export interface SsoTicketPayload {
  targetPlatform?: string;
  redirectUri?: string;
}

export interface SsoTicketRecord {
  ticket: string;
  loginUrl: string;
  targetPlatform?: string | null;
  expiresAt?: string | null;
}

export interface PlatformVideoChannel {
  siteId?: string | null;
  siteName?: string | null;
  deviceId?: string | null;
  deviceCode?: string | null;
  deviceName?: string | null;
  deviceType?: string | null;
  status?: string | null;
  playUrl?: string | null;
}

export interface DamMonitorRecord {
  id: string;
  siteId?: string | null;
  siteName?: string | null;
  deviceName?: string | null;
  monitorTime?: string | null;
  onlineStatus?: string | null;
  safetyLevel?: string | null;
  displacementValue?: number | null;
  waterLevel?: number | null;
  rainfall?: number | null;
  alarmFlag?: boolean;
  remark?: string | null;
}

export interface DamMonitorRecordPayload {
  siteId: number;
  deviceName?: string;
  monitorTime?: string;
  onlineStatus?: string;
  safetyLevel?: string;
  displacementValue?: number;
  waterLevel?: number;
  rainfall?: number;
  alarmFlag?: boolean;
  remark?: string;
}

export interface PlatformSyncLogRecord {
  id: string;
  integrationCode?: string | null;
  syncMode?: string | null;
  bizType?: string | null;
  batchNo?: string | null;
  totalCount?: number | null;
  successCount?: number | null;
  failCount?: number | null;
  status?: string | null;
  operatorName?: string | null;
  syncTime?: string | null;
  requestPayload?: string | null;
  responsePayload?: string | null;
  remark?: string | null;
}

export interface GovPermitSyncPayload {
  syncMode?: string;
  includeTransportPermits?: boolean;
  contractId?: number;
  projectId?: number;
  siteId?: number;
  vehicleNo?: string;
  remark?: string;
}

export interface GovPermitSyncResult {
  batchNo: string;
  syncMode?: string | null;
  totalCount?: number | null;
  createdCount?: number | null;
  updatedCount?: number | null;
  successCount?: number | null;
  failCount?: number | null;
  syncTime?: string | null;
}

export interface WeighbridgeRecord {
  id: string;
  siteId?: string | null;
  siteName?: string | null;
  deviceId?: string | null;
  deviceName?: string | null;
  vehicleNo?: string | null;
  ticketNo?: string | null;
  grossWeight?: number | null;
  tareWeight?: number | null;
  netWeight?: number | null;
  estimatedVolume?: number | null;
  weighTime?: string | null;
  syncStatus?: string | null;
  controlCommand?: string | null;
  sourceType?: string | null;
  remark?: string | null;
}

export interface WeighbridgeRecordPayload {
  siteId: number;
  deviceId?: number;
  vehicleNo: string;
  ticketNo?: string;
  grossWeight?: number;
  tareWeight?: number;
  netWeight?: number;
  estimatedVolume?: number;
  weighTime?: string;
  syncStatus?: string;
  controlCommand?: string;
  sourceType?: string;
  remark?: string;
}

export interface WeighbridgeControlPayload {
  siteId: number;
  deviceId?: number;
  command: string;
  remark?: string;
}

export async function fetchPlatformIntegrationOverview() {
  const res = await http.get<PlatformIntegrationOverview>('/platform-integrations/overview');
  return res.data;
}

export async function fetchPlatformIntegrationConfigs() {
  const res = await http.get<PlatformIntegrationConfig[]>('/platform-integrations/configs');
  return Array.isArray(res.data) ? res.data : [];
}

export async function updatePlatformIntegrationConfig(
  integrationCode: string,
  payload: PlatformIntegrationConfigPayload,
) {
  const res = await http.put<PlatformIntegrationConfig>(
    `/platform-integrations/${integrationCode}`,
    payload,
  );
  return res.data;
}

export async function createSsoTicket(payload: SsoTicketPayload) {
  const res = await http.post<SsoTicketRecord>('/platform-integrations/sso/tickets', payload);
  return res.data;
}

export async function fetchPlatformVideoChannels() {
  const res = await http.get<PlatformVideoChannel[]>('/platform-integrations/video/channels');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchDamMonitorRecords(siteId?: string) {
  const res = await http.get<DamMonitorRecord[]>('/platform-integrations/dam/records', {
    params: siteId ? { siteId } : undefined,
  });
  return Array.isArray(res.data) ? res.data : [];
}

export async function syncDamMonitorRecord(payload: DamMonitorRecordPayload) {
  const res = await http.post<DamMonitorRecord>('/platform-integrations/dam/sync', payload);
  return res.data;
}

export async function syncGovPermits(payload: GovPermitSyncPayload = {}) {
  const res = await http.post<GovPermitSyncResult>('/platform-integrations/gov/sync', payload);
  return res.data;
}

export async function fetchPlatformSyncLogs(integrationCode?: string) {
  const res = await http.get<PlatformSyncLogRecord[]>('/platform-integrations/sync-logs', {
    params: integrationCode ? { integrationCode } : undefined,
  });
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchGovSyncLogs() {
  const res = await http.get<PlatformSyncLogRecord[]>('/platform-integrations/gov/sync-logs');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchWeighbridgeRecords(siteId?: string) {
  const res = await http.get<WeighbridgeRecord[]>('/platform-integrations/weighbridge/records', {
    params: siteId ? { siteId } : undefined,
  });
  return Array.isArray(res.data) ? res.data : [];
}

export async function syncWeighbridgeRecord(payload: WeighbridgeRecordPayload) {
  const res = await http.post<WeighbridgeRecord>('/platform-integrations/weighbridge/sync', payload);
  return res.data;
}

export async function issueWeighbridgeControlCommand(payload: WeighbridgeControlPayload) {
  const res = await http.post<PlatformSyncLogRecord>(
    '/platform-integrations/weighbridge/control-command',
    payload,
  );
  return res.data;
}
