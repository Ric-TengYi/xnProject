import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface CheckinRecord {
  id: string;
  ticketNo?: string | null;
  punchTime?: string | null;
  status?: string | null;
  statusLabel?: string | null;
  exceptionType?: string | null;
  voidReason?: string | null;
  volume?: number | null;
  sourceType?: string | null;
  contractId?: string | null;
  contractNo?: string | null;
  contractName?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  plateNo?: string | null;
  driverName?: string | null;
  transportOrgName?: string | null;
}

export interface CheckinQueryParams {
  projectId?: string;
  siteId?: string;
  keyword?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  pageNo?: number;
  pageSize?: number;
}

const toNumber = (value: any) => (value != null ? Number(value) : null);

const mapRecord = (item: any): CheckinRecord => ({
  id: String(item.id || ''),
  ticketNo: item.ticketNo || null,
  punchTime: item.punchTime || null,
  status: item.status || null,
  statusLabel: item.statusLabel || null,
  exceptionType: item.exceptionType || null,
  voidReason: item.voidReason || null,
  volume: toNumber(item.volume),
  sourceType: item.sourceType || null,
  contractId: item.contractId || null,
  contractNo: item.contractNo || null,
  contractName: item.contractName || null,
  projectId: item.projectId || null,
  projectName: item.projectName || null,
  siteId: item.siteId || null,
  siteName: item.siteName || null,
  plateNo: item.plateNo || null,
  driverName: item.driverName || null,
  transportOrgName: item.transportOrgName || null,
});

export async function fetchCheckins(params: CheckinQueryParams = {}) {
  const res = await http.get<PageResult<CheckinRecord>>('/checkins', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function voidCheckin(id: string, reason: string) {
  const res = await http.put<CheckinRecord>(`/checkins/${id}/void`, { reason });
  return mapRecord(res.data || {});
}
