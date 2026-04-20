export interface MiniAuthCodeResponse {
  mobile?: string | null;
  expiresAt?: string | null;
  sendStatus?: string | null;
  mockCode?: string | null;
}

export interface MiniAccessibleSite {
  siteId?: string | number | null;
  siteName?: string | null;
  siteType?: string | null;
  accountRole?: string | null;
  manualDisposalEnabled?: boolean | null;
  queueEnabled?: boolean | null;
}

export interface MiniUserProfile {
  userId?: string | null;
  username?: string | null;
  name?: string | null;
  mobile?: string | null;
  orgName?: string | null;
  userType?: string | null;
  bindStatus?: string | null;
  openId?: string | null;
}

export interface MiniLoginResponse {
  token: string;
  tokenType?: string | null;
  expiresIn?: number | null;
  user?: MiniUserProfile | null;
  accessibleSites?: MiniAccessibleSite[] | null;
}

export interface MiniExcavationOrg {
  orgId?: string | number | null;
  orgCode?: string | null;
  orgName?: string | null;
  orgType?: string | null;
  contactPerson?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  projectCount?: number | null;
}

export interface MiniProjectContract {
  contractId?: string | number | null;
  contractNo?: string | null;
  contractName?: string | null;
  siteId?: string | number | null;
  siteName?: string | null;
}

export interface MiniExcavationProject {
  projectId?: string | number | null;
  projectName?: string | null;
  statusLabel?: string | null;
  contractCount?: number | null;
  siteCount?: number | null;
}

export interface MiniProjectDetail {
  id?: string | number | null;
  name?: string | null;
  contractDetails?: MiniProjectContract[] | null;
}

export interface MiniPhotoRecord {
  id?: string | number | null;
  plateNo?: string | null;
  recognitionSource?: string | null;
  photoType?: string | null;
  fileUrl?: string | null;
  shootTime?: string | null;
  siteName?: string | null;
  projectName?: string | null;
  auditStatus?: string | null;
}

export interface MiniManualDisposalRecord {
  id?: string | number | null;
  siteName?: string | null;
  projectName?: string | null;
  contractNo?: string | null;
  plateNo?: string | null;
  disposalTime?: string | null;
  volume?: number | null;
  amount?: number | null;
  status?: string | null;
  ticketNo?: string | null;
}

export interface MiniEventRecord {
  id?: string | number | null;
  eventNo?: string | null;
  eventType?: string | null;
  title?: string | null;
  content?: string | null;
  siteName?: string | null;
  priority?: string | null;
  status?: string | null;
  reportTime?: string | null;
}

export interface MiniFeedbackRecord {
  id?: string | number | null;
  feedbackType?: string | null;
  title?: string | null;
  content?: string | null;
  siteName?: string | null;
  status?: string | null;
  createTime?: string | null;
  linkedEventId?: string | number | null;
}

export interface MiniDelayApplyRecord {
  id?: string | number | null;
  bizType?: string | null;
  bizId?: string | number | null;
  projectId?: string | number | null;
  projectName?: string | null;
  siteId?: string | number | null;
  siteName?: string | null;
  requestedEndTime?: string | null;
  reason?: string | null;
  attachmentUrls?: string | null;
  status?: string | null;
  linkedEventId?: string | number | null;
  createTime?: string | null;
}

export interface MiniVehicleRecord {
  vehicleId?: string | number | null;
  plateNo?: string | null;
  driverName?: string | null;
  fleetName?: string | null;
  runningStatus?: string | null;
  gpsTime?: string | null;
  lng?: number | null;
  lat?: number | null;
  speed?: number | null;
}

export interface MiniVehicleTrackPoint {
  id?: string | number | null;
  lng: number;
  lat: number;
  locateTime?: string | null;
  speed?: number | null;
}

export interface MiniVehicleTrackHistory {
  vehicleId?: string | number | null;
  plateNo?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  pointCount?: number | null;
  points?: MiniVehicleTrackPoint[] | null;
}

type MiniRequestOptions = {
  method?: 'GET' | 'POST' | 'PUT';
  token?: string;
  body?: unknown;
  params?: Record<string, string | number | boolean | undefined | null>;
};

const MINI_BASE = '/api/mini';

function buildMiniUrl(path: string, params?: MiniRequestOptions['params']) {
  const search = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    search.set(key, String(value));
  });
  const query = search.toString();
  return `${MINI_BASE}${path}${query ? `?${query}` : ''}`;
}

async function miniRequest<T>(path: string, options: MiniRequestOptions = {}): Promise<T> {
  const response = await fetch(buildMiniUrl(path, options.params), {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || '移动端接口调用失败');
  }
  return payload.data as T;
}

export function sendMiniSmsCode(payload: {
  tenantId: string;
  username: string;
  password: string;
  mobile: string;
  openId?: string;
}) {
  return miniRequest<MiniAuthCodeResponse>('/auth/send-sms-code', {
    method: 'POST',
    body: payload,
  });
}

export function loginMini(payload: {
  tenantId: string;
  username: string;
  password: string;
  mobile: string;
  smsCode: string;
  openId?: string;
  deviceName?: string;
}) {
  return miniRequest<MiniLoginResponse>('/auth/login', {
    method: 'POST',
    body: payload,
  });
}

export function openIdLoginMini(payload: { openId: string; unionId?: string; deviceName?: string }) {
  return miniRequest<MiniLoginResponse>('/auth/openid-login', {
    method: 'POST',
    body: payload,
  });
}

export function fetchMiniProfile(token: string) {
  return miniRequest<MiniUserProfile>('/me', { token });
}

export function fetchMiniSites(token: string) {
  return miniRequest<MiniAccessibleSite[]>('/sites', { token });
}

export function fetchMiniExcavationOrg(token: string) {
  return miniRequest<MiniExcavationOrg>('/excavation-orgs/current', { token });
}

export function fetchMiniExcavationProjects(token: string) {
  return miniRequest<MiniExcavationProject[]>('/excavation-orgs/projects', { token });
}

export function fetchMiniExcavationProjectDetail(token: string, projectId: string | number) {
  return miniRequest<MiniProjectDetail>(`/excavation-orgs/projects/${projectId}`, { token });
}

export function bindMiniAccount(
  token: string,
  payload: { openId: string; unionId?: string; deviceName?: string },
) {
  return miniRequest<MiniUserProfile>('/account/bind', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function createMiniPhoto(
  token: string,
  payload: {
    projectId: number;
    siteId?: number;
    plateNo?: string;
    fileUrl: string;
    photoType?: string;
    shootTime?: string;
    remark?: string;
  },
) {
  return miniRequest<MiniPhotoRecord>('/photos', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function listMiniPhotos(token: string, params?: { projectId?: number; siteId?: number }) {
  return miniRequest<MiniPhotoRecord[]>('/photos', { token, params });
}

export function createMiniManualDisposal(
  token: string,
  payload: {
    siteId: number;
    contractId: number;
    vehicleId?: number;
    plateNo?: string;
    volume: number;
    amount?: number;
    weightTons?: number;
    photoUrls?: string;
    disposalTime?: string;
    remark?: string;
  },
) {
  return miniRequest<MiniManualDisposalRecord>('/manual-disposals', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function listMiniManualDisposals(token: string, params?: { siteId?: number }) {
  return miniRequest<MiniManualDisposalRecord[]>('/manual-disposals', { token, params });
}

export function createMiniEvent(
  token: string,
  payload: {
    title: string;
    content?: string;
    eventType?: string;
    projectId?: number;
    siteId?: number;
    priority?: string;
  },
) {
  return miniRequest<MiniEventRecord>('/events', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function listMiniEvents(token: string) {
  return miniRequest<MiniEventRecord[]>('/events', { token });
}

export function createMiniFeedback(
  token: string,
  payload: {
    feedbackType: string;
    title: string;
    content: string;
    projectId?: number;
    siteId?: number;
  },
) {
  return miniRequest<MiniFeedbackRecord>('/feedbacks', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function listMiniFeedbacks(token: string) {
  return miniRequest<MiniFeedbackRecord[]>('/feedbacks', { token });
}

export function createMiniDelayApply(
  token: string,
  payload: {
    bizType?: string;
    bizId?: number;
    projectId?: number;
    siteId?: number;
    requestedEndTime: string;
    reason: string;
    attachmentUrls?: string;
  },
) {
  return miniRequest<MiniDelayApplyRecord>('/delay-applies', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function listMiniDelayApplies(token: string) {
  return miniRequest<MiniDelayApplyRecord[]>('/delay-applies', { token });
}

export async function fetchMiniVehicles(
  token: string,
  params: { pageNo?: number; pageSize?: number } = {},
) {
  const page = await miniRequest<{
    records?: MiniVehicleRecord[] | null;
  }>('/vehicles/realtime', { token, params });
  return Array.isArray(page.records) ? page.records : [];
}

export function fetchMiniVehicleTrackHistory(
  token: string,
  vehicleId: string | number,
  params?: { startTime?: string; endTime?: string },
) {
  return miniRequest<MiniVehicleTrackHistory>(`/vehicles/${vehicleId}/track-history`, {
    token,
    params,
  });
}
