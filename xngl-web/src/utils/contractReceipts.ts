import axios from 'axios';

const contractReceiptRequest = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

contractReceiptRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

interface WrappedResponse<T> {
  code?: number;
  message?: string;
  data?: T;
}

export interface ContractReceipt {
  id: number | string;
  contractId?: number | string;
  amount?: number;
  receiptDate?: string;
  voucherNo?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface CreateContractReceiptPayload {
  contractId: number;
  amount: number;
  receiptDate: string;
  voucherNo?: string;
  remark?: string;
  status?: string;
}

function unwrapResponse<T>(payload: WrappedResponse<T> | T): T {
  if (
    payload &&
    typeof payload === 'object' &&
    'code' in payload &&
    'data' in payload
  ) {
    const wrapped = payload as WrappedResponse<T>;
    if (wrapped.code !== undefined && wrapped.code !== 0 && wrapped.code !== 200) {
      throw new Error(wrapped.message || '请求失败');
    }
    return wrapped.data as T;
  }
  return payload as T;
}

export async function listContractReceipts(): Promise<ContractReceipt[]> {
  const response = await contractReceiptRequest.get('/contract-receipts');
  const data = unwrapResponse<ContractReceipt[]>(response.data);
  return Array.isArray(data) ? data : [];
}

export async function createContractReceipt(payload: CreateContractReceiptPayload): Promise<void> {
  const response = await contractReceiptRequest.post('/contract-receipts', payload);
  unwrapResponse(response.data);
}
