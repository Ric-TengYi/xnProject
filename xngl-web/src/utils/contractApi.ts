import http, { request } from './request';

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

export interface ContractExportTask {
  id: string;
  bizType: string;
  exportType?: string;
  fileName?: string;
  fileUrl?: string;
  status: string;
  failReason?: string;
  creatorId?: string;
  createTime?: string;
  expireTime?: string;
}

export interface ImportErrorRecord {
  id?: string;
  rowNo: number;
  contractNo?: string;
  errorCode?: string;
  errorMessage: string;
}

export interface ImportPreviewResult {
  batchId: string;
  totalCount: number;
  validCount: number;
  errorCount: number;
  errors: ImportErrorRecord[];
}

export interface ImportCommitResult {
  batchId: string;
  successCount: number;
  failCount: number;
  status: string;
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
  contractNo?: string;
  contractType: string;
  projectId: number | string;
  partyId?: number | string;
  siteId: number | string;
  constructionOrgId: number | string;
  transportOrgId: number | string;
  siteOperatorOrgId?: number | string;
  signDate: string;
  effectiveDate: string;
  expireDate: string;
  agreedVolume: number;
  unitPrice: number;
  contractAmount: number;
  isThreeParty?: boolean;
  unitPriceInside?: number;
  unitPriceOutside?: number;
  sourceType?: string;
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

export interface ContractTransferRecord {
  id: string;
  transferNo: string;
  sourceContractId?: string | null;
  sourceContractNo?: string | null;
  targetContractId?: string | null;
  targetContractNo?: string | null;
  transferAmount?: number | null;
  transferVolume?: number | null;
  reason?: string | null;
  approvalStatus?: string | null;
  applicantId?: string | null;
  createTime?: string | null;
}

export interface ContractChangeRecord {
  id: string;
  changeNo?: string;
  contractId?: string;
  contractNo?: string;
  changeType?: string;
  reason?: string;
  approvalStatus?: string;
  applicantId?: string;
  createTime?: string;
}

export interface ContractExtensionRecord {
  id: string;
  applyNo?: string;
  contractId?: string;
  contractNo?: string;
  originalExpireDate?: string;
  requestedExpireDate?: string;
  requestedVolumeDelta?: number | null;
  reason?: string;
  approvalStatus?: string;
  applicantId?: string;
  createTime?: string;
}

export interface ContractChangeCreatePayload {
  contractId: number;
  changeType: string;
  afterSnapshotJson?: string;
  reason?: string;
  newSiteId?: number;
  newSiteName?: string;
  newAgreedVolume?: number;
  volumeDelta?: number;
  newContractAmount?: number;
  newUnitPrice?: number;
  newExpireDate?: string;
}

export interface ContractExtensionCreatePayload {
  contractId: number;
  requestedExpireDate: string;
  requestedVolumeDelta?: number;
  reason?: string;
}

export interface ContractTransferCreatePayload {
  sourceContractId: number | string;
  targetContractId: number | string;
  transferAmount?: number;
  transferVolume?: number;
  reason?: string;
}

export async function fetchContractList(params: ContractListParams = {}) {
  const res = await http.get<PageResult<ContractRecord>>('/contracts', { params });
  return res.data;
}

export async function fetchContractStats() {
  const res = await http.get<ContractStats>('/contracts/stats');
  return res.data;
}

export async function exportContracts(params: ContractListParams & { exportType?: string }) {
  const res = await http.post<{ taskId: string }>('/contracts/export', params);
  return res.data;
}

export async function fetchContractExportTask(taskId: string | number) {
  const res = await http.get<ContractExportTask>(`/export-tasks/${taskId}`);
  return res.data;
}

export async function downloadContractExport(taskId: string | number) {
  const res = await request.get(`/export-tasks/${taskId}/download`, {
    responseType: 'blob',
  });
  return res as unknown as Blob;
}

export async function previewContractImport(fileName: string, rows: Record<string, string>[]) {
  const res = await http.post<ImportPreviewResult>('/contracts/import-preview', {
    fileName,
    rows,
  });
  return res.data;
}

export async function commitContractImport(batchId: string | number) {
  const res = await http.post<ImportCommitResult>('/contracts/import-commit', {
    batchId: Number(batchId),
  });
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

export async function rejectContract(id: string | number, reason?: string) {
  const res = await http.post<void>(`/contracts/${id}/reject`, reason ? { reason } : {});
  return res.data;
}

export async function downloadContractMaterial(
  contractId: string | number,
  materialId: string | number
) {
  const res = await request.get(`/contracts/${contractId}/materials/${materialId}/download`, {
    responseType: 'blob',
  });
  return res as unknown as Blob;
}

export async function fetchChangeApplications(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<ContractChangeRecord>>('/contracts/change-applications', { params });
  return res.data;
}

export async function fetchExtensions(params?: { pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<ContractExtensionRecord>>('/contracts/extensions', { params });
  return res.data;
}

export async function createOnlineContract(dto: ContractCreateDto) {
  const res = await http.post<string>('/contracts', dto);
  return res.data;
}

export async function createContractChangeApplication(contractId: string | number, payload: ContractChangeCreatePayload) {
  const res = await http.post<string>(`/contracts/${contractId}/change-applications`, payload);
  return res.data;
}

export async function fetchContractChangeDetail(id: string | number) {
  const res = await http.get<ContractChangeRecord>(`/contracts/change-applications/${id}`);
  return res.data;
}

export async function submitContractChange(id: string | number) {
  const res = await http.post<void>(`/contracts/change-applications/${id}/submit`);
  return res.data;
}

export async function approveContractChange(id: string | number) {
  const res = await http.post<void>(`/contracts/change-applications/${id}/approve`);
  return res.data;
}

export async function rejectContractChange(id: string | number, reason?: string) {
  const res = await http.post<void>(`/contracts/change-applications/${id}/reject`, reason ? { reason } : {});
  return res.data;
}

export async function createContractExtension(contractId: string | number, payload: ContractExtensionCreatePayload) {
  const res = await http.post<string>(`/contracts/${contractId}/extensions`, payload);
  return res.data;
}

export async function fetchContractExtensionDetail(id: string | number) {
  const res = await http.get<ContractExtensionRecord>(`/contracts/extensions/${id}`);
  return res.data;
}

export async function submitContractExtension(id: string | number) {
  const res = await http.post<void>(`/contracts/extensions/${id}/submit`);
  return res.data;
}

export async function approveContractExtension(id: string | number) {
  const res = await http.post<void>(`/contracts/extensions/${id}/approve`);
  return res.data;
}

export async function rejectContractExtension(id: string | number, reason?: string) {
  const res = await http.post<void>(`/contracts/extensions/${id}/reject`, reason ? { reason } : {});
  return res.data;
}

export async function fetchTransfers(params?: { approvalStatus?: string; pageNo?: number; pageSize?: number }) {
  const res = await http.get<PageResult<ContractTransferRecord>>('/contracts/transfers', { params });
  return res.data;
}

export async function createContractTransfer(payload: ContractTransferCreatePayload) {
  const res = await http.post<string>('/contracts/transfers', payload);
  return res.data;
}

export async function fetchContractTransferDetail(id: string | number) {
  const res = await http.get<ContractTransferRecord>(`/contracts/transfers/${id}`);
  return res.data;
}

export async function submitContractTransfer(id: string | number) {
  const res = await http.post<void>(`/contracts/transfers/${id}/submit`);
  return res.data;
}

export async function approveContractTransfer(id: string | number) {
  const res = await http.post<void>(`/contracts/transfers/${id}/approve`);
  return res.data;
}

export async function rejectContractTransfer(id: string | number, reason?: string) {
  const res = await http.post<void>(`/contracts/transfers/${id}/reject`, reason ? { reason } : {});
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
