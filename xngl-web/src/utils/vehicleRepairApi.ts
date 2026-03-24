import http, { request } from './request';
import type { PageResult } from './vehicleApi';

export interface VehicleRepairOrderRecord {
  id: string;
  orderNo: string;
  vehicleId?: string | null;
  plateNo?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  urgencyLevel?: string | null;
  urgencyLabel?: string | null;
  repairReason?: string | null;
  repairContent?: string | null;
  diagnosisResult?: string | null;
  safetyImpact?: string | null;
  budgetAmount: number;
  applyDate?: string | null;
  applicantName?: string | null;
  status: string;
  statusLabel: string;
  approvedBy?: string | null;
  approvedTime?: string | null;
  completedDate?: string | null;
  vendorName?: string | null;
  repairManager?: string | null;
  technicianName?: string | null;
  acceptanceResult?: string | null;
  signoffStatus?: string | null;
  signoffStatusLabel?: string | null;
  attachmentUrls?: string | null;
  actualAmount: number;
  partsCost?: number | null;
  laborCost?: number | null;
  otherCost?: number | null;
  costVariance?: number | null;
  auditRemark?: string | null;
  remark?: string | null;
}

export interface VehicleRepairSummaryRecord {
  totalOrders: number;
  pendingOrders: number;
  approvedOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  totalBudgetAmount: number;
  totalActualAmount: number;
}

export interface VehicleRepairQueryParams {
  keyword?: string;
  status?: string;
  urgencyLevel?: string;
  orgId?: string;
  vehicleId?: string;
  applyDateFrom?: string;
  applyDateTo?: string;
  completedDateFrom?: string;
  completedDateTo?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleRepairUpsertPayload {
  vehicleId: number;
  urgencyLevel?: string;
  repairReason: string;
  repairContent?: string;
  diagnosisResult?: string;
  safetyImpact?: string;
  budgetAmount?: number;
  applyDate?: string;
  applicantName?: string;
  status?: string;
  remark?: string;
}

export interface VehicleRepairAuditPayload {
  comment?: string;
}

export interface VehicleRepairCompletePayload {
  completedDate?: string;
  vendorName?: string;
  repairManager?: string;
  technicianName?: string;
  acceptanceResult?: string;
  signoffStatus?: string;
  attachmentUrls?: string;
  actualAmount?: number;
  partsCost?: number;
  laborCost?: number;
  otherCost?: number;
  remark?: string;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapRecord = (record: Partial<VehicleRepairOrderRecord>): VehicleRepairOrderRecord => ({
  id: String(record.id || ''),
  orderNo: record.orderNo || '',
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  urgencyLevel: record.urgencyLevel || null,
  urgencyLabel: record.urgencyLabel || null,
  repairReason: record.repairReason || null,
  repairContent: record.repairContent || null,
  diagnosisResult: record.diagnosisResult || null,
  safetyImpact: record.safetyImpact || null,
  budgetAmount: toNumber(record.budgetAmount),
  applyDate: record.applyDate || null,
  applicantName: record.applicantName || null,
  status: record.status || 'PENDING_APPROVAL',
  statusLabel: record.statusLabel || '未知',
  approvedBy: record.approvedBy || null,
  approvedTime: record.approvedTime || null,
  completedDate: record.completedDate || null,
  vendorName: record.vendorName || null,
  repairManager: record.repairManager || null,
  technicianName: record.technicianName || null,
  acceptanceResult: record.acceptanceResult || null,
  signoffStatus: record.signoffStatus || null,
  signoffStatusLabel: record.signoffStatusLabel || null,
  attachmentUrls: record.attachmentUrls || null,
  actualAmount: toNumber(record.actualAmount),
  partsCost: record.partsCost == null ? null : toNumber(record.partsCost),
  laborCost: record.laborCost == null ? null : toNumber(record.laborCost),
  otherCost: record.otherCost == null ? null : toNumber(record.otherCost),
  costVariance: record.costVariance == null ? null : toNumber(record.costVariance),
  auditRemark: record.auditRemark || null,
  remark: record.remark || null,
});

export async function fetchVehicleRepairs(params: VehicleRepairQueryParams = {}) {
  const res = await http.get<PageResult<VehicleRepairOrderRecord>>('/vehicle-repairs', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function fetchVehicleRepairSummary(
  params: Omit<VehicleRepairQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<VehicleRepairSummaryRecord>>('/vehicle-repairs/summary', { params });
  return {
    totalOrders: toNumber(res.data.totalOrders),
    pendingOrders: toNumber(res.data.pendingOrders),
    approvedOrders: toNumber(res.data.approvedOrders),
    inProgressOrders: toNumber(res.data.inProgressOrders),
    completedOrders: toNumber(res.data.completedOrders),
    totalBudgetAmount: toNumber(res.data.totalBudgetAmount),
    totalActualAmount: toNumber(res.data.totalActualAmount),
  };
}

export async function fetchVehicleRepairDetail(id: string) {
  const res = await http.get<VehicleRepairOrderRecord>('/vehicle-repairs/' + id);
  return mapRecord(res.data);
}

export async function createVehicleRepair(payload: VehicleRepairUpsertPayload) {
  const res = await http.post<VehicleRepairOrderRecord>('/vehicle-repairs', payload);
  return mapRecord(res.data);
}

export async function updateVehicleRepair(id: string, payload: VehicleRepairUpsertPayload) {
  const res = await http.put<VehicleRepairOrderRecord>('/vehicle-repairs/' + id, payload);
  return mapRecord(res.data);
}

export async function deleteVehicleRepair(id: string) {
  await http.delete('/vehicle-repairs/' + id);
}

export async function approveVehicleRepair(id: string, payload: VehicleRepairAuditPayload) {
  const res = await http.post<VehicleRepairOrderRecord>('/vehicle-repairs/' + id + '/approve', payload);
  return mapRecord(res.data);
}

export async function rejectVehicleRepair(id: string, payload: VehicleRepairAuditPayload) {
  const res = await http.post<VehicleRepairOrderRecord>('/vehicle-repairs/' + id + '/reject', payload);
  return mapRecord(res.data);
}

export async function completeVehicleRepair(id: string, payload: VehicleRepairCompletePayload) {
  const res = await http.post<VehicleRepairOrderRecord>('/vehicle-repairs/' + id + '/complete', payload);
  return mapRecord(res.data);
}

export async function exportVehicleRepairs(params: VehicleRepairQueryParams = {}) {
  const response = await request.get('/vehicle-repairs/export', {
    params,
    responseType: 'blob',
  });
  return response.data as Blob;
}
