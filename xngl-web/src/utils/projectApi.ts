import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface ProjectRecord {
  id: string;
  code?: string | null;
  name: string;
  address?: string | null;
  status?: number | null;
  statusLabel?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  contractCount?: number | null;
  siteCount?: number | null;
  totalAmount?: number | null;
  paidAmount?: number | null;
  debtAmount?: number | null;
  lastPaymentDate?: string | null;
  paymentStatus?: string | null;
  paymentStatusLabel?: string | null;
  contractDetails?: ProjectContractSummary[];
  siteDetails?: ProjectSiteSummary[];
  config?: ProjectConfigRecord | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProjectContractSummary {
  contractId: string;
  contractNo?: string | null;
  contractName?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  siteType?: string | null;
  agreedVolume?: number | null;
  disposedVolume?: number | null;
  remainingVolume?: number | null;
  unitPrice?: number | null;
  contractAmount?: number | null;
  contractStatus?: string | null;
  approvalStatus?: string | null;
  expireDate?: string | null;
}

export interface ProjectSiteSummary {
  siteId?: string | null;
  siteName?: string | null;
  siteType?: string | null;
  capacity?: number | null;
  lng?: number | null;
  lat?: number | null;
  contractCount?: number | null;
  contractVolume?: number | null;
  disposedVolume?: number | null;
  remainingVolume?: number | null;
}

export interface ProjectConfigRecord {
  checkinEnabled?: boolean;
  checkinAccount?: string | null;
  checkinAuthScope?: string | null;
  locationCheckRequired?: boolean;
  locationRadiusMeters?: number | null;
  preloadVolume?: number | null;
  routeGeoJson?: string | null;
  violationRuleEnabled?: boolean;
  violationFenceCode?: string | null;
  violationFenceName?: string | null;
  violationFenceGeoJson?: string | null;
  remark?: string | null;
}

export interface ProjectQueryParams {
  keyword?: string;
  status?: number;
  pageNo?: number;
  pageSize?: number;
}

export interface ProjectPaymentSummary {
  projectId?: string;
  projectName?: string;
  projectCode?: string;
  totalAmount?: number;
  paidAmount?: number;
  debtAmount?: number;
  lastPaymentDate?: string;
  status?: string;
}

export async function fetchProjects(params: ProjectQueryParams = {}) {
  const res = await http.get<PageResult<ProjectRecord>>('/projects', { params });
  return res.data;
}

export async function fetchProjectDetail(id: string) {
  const res = await http.get<ProjectRecord>('/projects/' + id);
  return res.data;
}

export async function fetchProjectPaymentSummary(id: string) {
  const res = await http.get<ProjectPaymentSummary>(
    '/project-payments/' + id + '/payments/summary'
  );
  return res.data;
}

export async function updateProjectConfig(id: string, data: ProjectConfigRecord) {
  const res = await http.put<ProjectConfigRecord>('/projects/' + id + '/config', data);
  return res.data;
}
