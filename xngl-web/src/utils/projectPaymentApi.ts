import http from './request';
import type { PageResult, ProjectPaymentSummary } from './projectApi';

export interface ProjectPaymentRecord {
  id: string;
  projectId: string;
  projectName?: string;
  projectCode?: string;
  paymentNo?: string;
  paymentType?: string;
  amount?: number;
  paymentDate?: string;
  voucherNo?: string;
  status?: string;
  sourceType?: string;
  sourceId?: string;
  remark?: string;
  operatorId?: string;
  cancelOperatorId?: string;
  cancelTime?: string;
  cancelReason?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ProjectPaymentQueryParams {
  projectId?: string;
  keyword?: string;
  paymentType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface CreateProjectPaymentPayload {
  paymentNo?: string;
  paymentType?: string;
  amount: number;
  paymentDate: string;
  voucherNo?: string;
  sourceType?: string;
  sourceId?: string;
  remark?: string;
}

export interface ProjectPaymentChangeResult {
  paymentId?: string;
  paymentNo?: string;
  summary?: ProjectPaymentSummary;
}

export async function listProjectPayments(params: ProjectPaymentQueryParams = {}) {
  const res = await http.get<PageResult<ProjectPaymentRecord>>('/project-payments/payments', {
    params,
  });
  return res.data;
}

export async function createProjectPayment(
  projectId: string,
  payload: CreateProjectPaymentPayload
) {
  const res = await http.post<ProjectPaymentChangeResult>(
    '/project-payments/' + projectId + '/payments',
    payload
  );
  return res.data;
}

export async function cancelProjectPayment(paymentId: string, reason?: string) {
  const res = await http.post<ProjectPaymentChangeResult>(
    '/project-payments/payments/' + paymentId + '/cancel',
    reason ? { reason } : {}
  );
  return res.data;
}
