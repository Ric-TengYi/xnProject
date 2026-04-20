import http, { request } from './request';
import type { PageResult } from './vehicleApi';

export interface VehicleInsuranceRecord {
  id: string;
  vehicleId?: string | null;
  plateNo?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  policyNo: string;
  insuranceType?: string | null;
  insurerName?: string | null;
  coverageAmount: number;
  premiumAmount: number;
  claimAmount: number;
  startDate?: string | null;
  endDate?: string | null;
  status: string;
  statusLabel: string;
  remainingDays?: number | null;
  remark?: string | null;
}

export interface VehicleInsuranceSummaryRecord {
  totalPolicies: number;
  activePolicies: number;
  expiringPolicies: number;
  expiredPolicies: number;
  totalCoverageAmount: number;
  totalPremiumAmount: number;
  totalClaimAmount: number;
}

export interface VehicleInsuranceQueryParams {
  keyword?: string;
  status?: string;
  orgId?: string;
  vehicleId?: string;
  startDateFrom?: string;
  startDateTo?: string;
  endDateFrom?: string;
  endDateTo?: string;
  expiringWithinDays?: number;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleInsuranceUpsertPayload {
  vehicleId: number;
  policyNo: string;
  insuranceType?: string;
  insurerName?: string;
  coverageAmount?: number;
  premiumAmount?: number;
  claimAmount?: number;
  startDate: string;
  endDate: string;
  status?: string;
  remark?: string;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapRecord = (record: Partial<VehicleInsuranceRecord>): VehicleInsuranceRecord => ({
  id: String(record.id || ''),
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  policyNo: record.policyNo || '',
  insuranceType: record.insuranceType || null,
  insurerName: record.insurerName || null,
  coverageAmount: toNumber(record.coverageAmount),
  premiumAmount: toNumber(record.premiumAmount),
  claimAmount: toNumber(record.claimAmount),
  startDate: record.startDate || null,
  endDate: record.endDate || null,
  status: record.status || 'ACTIVE',
  statusLabel: record.statusLabel || '未知',
  remainingDays: record.remainingDays ?? null,
  remark: record.remark || null,
});

export async function fetchVehicleInsurances(params: VehicleInsuranceQueryParams = {}) {
  const res = await http.get<PageResult<VehicleInsuranceRecord>>('/vehicle-insurances', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function fetchVehicleInsuranceSummary(
  params: Omit<VehicleInsuranceQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<VehicleInsuranceSummaryRecord>>('/vehicle-insurances/summary', { params });
  return {
    totalPolicies: toNumber(res.data.totalPolicies),
    activePolicies: toNumber(res.data.activePolicies),
    expiringPolicies: toNumber(res.data.expiringPolicies),
    expiredPolicies: toNumber(res.data.expiredPolicies),
    totalCoverageAmount: toNumber(res.data.totalCoverageAmount),
    totalPremiumAmount: toNumber(res.data.totalPremiumAmount),
    totalClaimAmount: toNumber(res.data.totalClaimAmount),
  };
}

export async function createVehicleInsurance(payload: VehicleInsuranceUpsertPayload) {
  const res = await http.post<VehicleInsuranceRecord>('/vehicle-insurances', payload);
  return mapRecord(res.data);
}

export async function updateVehicleInsurance(id: string, payload: VehicleInsuranceUpsertPayload) {
  const res = await http.put<VehicleInsuranceRecord>('/vehicle-insurances/' + id, payload);
  return mapRecord(res.data);
}

export async function deleteVehicleInsurance(id: string) {
  await http.delete('/vehicle-insurances/' + id);
}

export async function exportVehicleInsurances(params: VehicleInsuranceQueryParams = {}) {
  const response = await request.get('/vehicle-insurances/export', {
    params,
    responseType: 'blob',
  });
  return response.data as Blob;
}
