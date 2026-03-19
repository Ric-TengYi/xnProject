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
  createTime?: string | null;
  updateTime?: string | null;
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
