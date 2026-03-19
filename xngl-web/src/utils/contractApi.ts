import http from './request';

export interface ContractRecord {
  id: number | string;
  contractNo: string;
  contractType: string;
  name: string;
  projectId: number | string;
  projectName?: string;
  siteId: number | string;
  siteName?: string;
  constructionOrgId: number | string;
  constructionOrgName?: string;
  transportOrgId: number | string;
  transportOrgName?: string;
  contractAmount: number;
  receivedAmount: number;
  settledAmount: number;
  agreedVolume: number;
  unitPrice: number;
  unitPriceInside?: number;
  unitPriceOutside?: number;
  contractStatus: string;
  approvalStatus: string;
  signDate: string;
  effectiveDate: string;
  expireDate: string;
  isThreeParty: boolean;
  sourceType: string;
  createTime: string;
  rejectReason?: string;
  changeVersion?: number;
  applicantId?: string;
  remark?: string;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface ContractListParams {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  contractType?: string;
  contractStatus?: string;
  projectId?: number | string;
  siteId?: number | string;
  startDate?: string;
  endDate?: string;
}

export interface ContractStats {
  totalContracts: number;
  effectiveContracts: number;
  monthlyReceiptAmount: number;
  monthlyReceiptCount: number;
  pendingReceiptAmount: number;
  totalSettlementOrders: number;
  pendingSettlementOrders: number;
}

export interface ContractDetail extends ContractRecord {
  pendingAmount: number;
  code?: string;
  amount?: number;
  partyId?: string;
  siteOperatorOrgId?: string;
  siteOperatorOrgName?: string;
}

export interface ApprovalRecord {
  id: number | string;
  contractId: number | string;
  actionType?: string;
  actionName?: string;
  operatorId?: number | string;
  operatorName?: string;
  fromStatus?: string;
  toStatus?: string;
  remark?: string;
  operateTime?: string;
}

export interface ContractMaterial {
  id: number | string;
  contractId: number | string;
  materialName: string;
  materialType?: string;
  fileUrl?: string;
  fileType?: string;
  required?: boolean;
  status?: string;
  uploadTime?: string;
  remark?: string;
}

export interface ContractInvoice {
  id: number | string;
  contractId: number | string;
  invoiceNo: string;
  invoiceCode?: string;
  invoiceNumber?: string;
  amount: number;
  invoiceType?: string;
  invoiceDate?: string;
  status?: string;
}

export interface ContractTicket {
  id: number | string;
  contractId: number | string;
  ticketNo: string;
  ticketType?: string;
  volume?: number;
  amount?: number;
  vehiclePlate?: string;
  siteName?: string;
  weighTime?: string;
  ticketDate?: string;
  status?: string;
  remark?: string;
}

export interface ContractCreateDto {
  name: string;
  contractNo: string;
  contractType: string;
  projectId: number;
  partyId?: number;
  siteId: number;
  constructionOrgId: number;
  transportOrgId: number;
  signDate: string;
  effectiveDate: string;
  expireDate: string;
  agreedVolume: number;
  unitPrice: number;
  contractAmount: number;
  remark?: string;
}

export interface SettlementRecord {
  id: number | string;
  settlementNo: string;
  settlementType: string;
  targetName: string;
  period: string;
  startDate?: string;
  endDate?: string;
  volume: number;
  amount: number;
  status: string;
  createTime: string;
}

export interface MonthlySummary {
  month: string;
  contractCount: number;
  newContractCount: number;
  contractAmount: number;
  receiptAmount: number;
  settlementAmount: number;
  agreedVolume: number;
  actualVolume: number;
}

export interface MonthlyTrendItem {
  month: string;
  volume: number;
  amount: number;
  receiptAmount: number;
}

export interface MonthlyTypeItem {
  contractType: string;
  count: number;
  amount: number;
  volume: number;
}

export async function fetchContractList(params: ContractListParams = {}) {
  const res = await http.get<PageResult<ContractRecord>>('/contracts', { params });
  return res.data;
}

export async function fetchContractStats() {
  const res = await http.get<ContractStats>('/contracts/stats');
  return res.data;
}

export async function fetchContractDetail(id: string | number) {
  const res = await http.get<ContractDetail>(`/contracts/${id}/detail`);
  return res.data;
}

export async function fetchApprovalRecords(id: string | number) {
  const res = await http.get<ApprovalRecord[]>(`/contracts/${id}/approval-records`);
  return res.data;
}

export async function fetchContractMaterials(id: string | number) {
  const res = await http.get<ContractMaterial[]>(`/contracts/${id}/materials`);
  return res.data;
}

export async function fetchContractInvoices(id: string | number) {
  const res = await http.get<ContractInvoice[]>(`/contracts/${id}/invoices`);
  return res.data;
}

export async function fetchContractTickets(id: string | number) {
  const res = await http.get<ContractTicket[]>(`/contracts/${id}/tickets`);
  return res.data;
}

export async function createContract(dto: ContractCreateDto) {
  const res = await http.post<ContractRecord>('/contracts', dto);
  return res.data;
}

export async function submitContract(id: string | number) {
  const res = await http.post<void>(`/contracts/${id}/submit`);
  return res.data;
}

export async function approveContract(id: string | number) {
  const res = await http.post<void>(`/contracts/${id}/approve`);
  return res.data;
}

export async function rejectContract(id: string | number) {
  const res = await http.post<void>(`/contracts/${id}/reject`);
  return res.data;
}

export async function fetchChangeApplications(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<any>>('/contracts/change-applications', { params });
  return res.data;
}

export async function fetchExtensions(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<any>>('/contracts/extensions', { params });
  return res.data;
}

export async function fetchTransfers(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<any>>('/contracts/transfers', { params });
  return res.data;
}

export async function fetchSettlements(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<SettlementRecord>>('/settlements', { params });
  return res.data;
}

export async function fetchMonthlySummary(month?: string) {
  const res = await http.get<MonthlySummary>('/reports/contracts/monthly/summary', {
    params: { month },
  });
  return res.data;
}

export async function fetchMonthlyTrend(months = 6) {
  const res = await http.get<MonthlyTrendItem[]>('/reports/contracts/monthly/trend', {
    params: { months },
  });
  return res.data;
}

export async function fetchMonthlyTypes(month?: string) {
  const res = await http.get<MonthlyTypeItem[]>('/reports/contracts/monthly/types', {
    params: { month },
  });
  return res.data;
}
