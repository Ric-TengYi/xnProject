import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface DisposalRecord {
  id: string;
  ticketNo?: string | null;
  disposalTime?: string | null;
  status?: string | null;
  statusLabel?: string | null;
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

export interface DisposalQueryParams {
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

const mapRecord = (item: any): DisposalRecord => ({
  id: String(item.id || ''),
  ticketNo: item.ticketNo || null,
  disposalTime: item.disposalTime || null,
  status: item.status || null,
  statusLabel: item.statusLabel || null,
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

export async function fetchDisposals(params: DisposalQueryParams = {}) {
  const res = await http.get<PageResult<DisposalRecord>>('/disposals', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}
