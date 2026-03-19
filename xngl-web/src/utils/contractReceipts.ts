import http from './request';

export interface WrappedPageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface ContractReceipt {
  id: number | string;
  contractId?: number | string;
  contractNo?: string;
  contractName?: string;
  receiptNo?: string;
  receiptDate?: string;
  amount?: number;
  receiptType?: string;
  voucherNo?: string;
  bankFlowNo?: string;
  remark?: string;
  status?: string;
  operatorId?: string;
  createTime?: string;
}

export interface ContractReceiptQueryParams {
  contractId?: number | string;
  keyword?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface CreateContractReceiptPayload {
  contractId: number | string;
  amount: number;
  receiptDate: string;
  receiptType?: string;
  voucherNo?: string;
  bankFlowNo?: string;
  remark?: string;
}

export async function listContractReceipts(params: ContractReceiptQueryParams = {}) {
  const res = await http.get<WrappedPageResult<ContractReceipt>>('/contracts/receipts', { params });
  return res.data;
}

export async function listContractReceiptsByContract(contractId: number | string) {
  const res = await http.get<ContractReceipt[]>(`/contracts/${contractId}/receipts`);
  return Array.isArray(res.data) ? res.data : [];
}

export async function createContractReceipt(payload: CreateContractReceiptPayload) {
  const { contractId, ...body } = payload;
  const res = await http.post<string>(`/contracts/${contractId}/receipts`, body);
  return res.data;
}

export async function cancelContractReceipt(receiptId: number | string, remark?: string) {
  const res = await http.put<string>(`/contracts/receipts/${receiptId}/cancel`, { remark });
  return res.data;
}
