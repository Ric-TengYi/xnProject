import http from './request';

export interface SiteRecord {
  id: string;
  name: string;
  code?: string | null;
  address?: string | null;
  projectId?: number | null;
  status?: number | string | null;
  orgId?: number | null;
  siteType?: string | null;
  capacity?: number | null;
  settlementMode?: string | null;
  disposalUnitPrice?: number | null;
  disposalFeeRate?: number | null;
  serviceFeeUnitPrice?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DisposalRecord {
  id: string;
  siteId?: string | null;
  site?: string | null;
  time?: string | null;
  plate?: string | null;
  project?: string | null;
  source?: string | null;
  volume?: number | null;
  status?: string | null;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface SiteDisposalParams {
  siteId?: string;
  keyword?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export async function fetchSites() {
  const res = await http.get<SiteRecord[]>('/sites');
  return Array.isArray(res.data) ? res.data : [];
}

export async function fetchSiteDetail(id: string) {
  const res = await http.get<SiteRecord>(`/sites/${id}`);
  return res.data;
}

export async function fetchSiteDisposals(params: SiteDisposalParams = {}) {
  const res = await http.get<PageResult<DisposalRecord>>('/sites/disposals', { params });
  return res.data;
}
