import http from './request';
import type { PageResult } from './vehicleApi';

export interface VehiclePersonnelCertificateRecord {
  id: string;
  personName: string;
  mobile?: string | null;
  roleType?: string | null;
  roleTypeLabel?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  vehicleId?: string | null;
  plateNo?: string | null;
  idCardNo?: string | null;
  driverLicenseNo?: string | null;
  driverLicenseExpireDate?: string | null;
  transportLicenseNo?: string | null;
  transportLicenseExpireDate?: string | null;
  status: string;
  statusLabel: string;
  remainingDays?: number | null;
  feeAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  feeDueDate?: string | null;
  remark?: string | null;
}

export interface VehiclePersonnelCertificateSummaryRecord {
  totalPersons: number;
  activeCertificates: number;
  expiringCertificates: number;
  expiredCertificates: number;
  totalFeeAmount: number;
  paidAmount: number;
  unpaidAmount: number;
}

export interface VehiclePersonnelCertificateQueryParams {
  keyword?: string;
  roleType?: string;
  status?: string;
  orgId?: string;
  vehicleId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehiclePersonnelCertificateUpsertPayload {
  orgId?: number;
  vehicleId?: number;
  personName: string;
  mobile?: string;
  roleType?: string;
  idCardNo?: string;
  driverLicenseNo?: string;
  driverLicenseExpireDate?: string;
  transportLicenseNo?: string;
  transportLicenseExpireDate?: string;
  feeAmount?: number;
  paidAmount?: number;
  feeDueDate?: string;
  status?: string;
  remark?: string;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapRecord = (
  record: Partial<VehiclePersonnelCertificateRecord>
): VehiclePersonnelCertificateRecord => ({
  id: String(record.id || ''),
  personName: record.personName || '',
  mobile: record.mobile || null,
  roleType: record.roleType || null,
  roleTypeLabel: record.roleTypeLabel || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  idCardNo: record.idCardNo || null,
  driverLicenseNo: record.driverLicenseNo || null,
  driverLicenseExpireDate: record.driverLicenseExpireDate || null,
  transportLicenseNo: record.transportLicenseNo || null,
  transportLicenseExpireDate: record.transportLicenseExpireDate || null,
  status: record.status || 'ACTIVE',
  statusLabel: record.statusLabel || '未知',
  remainingDays: record.remainingDays ?? null,
  feeAmount: toNumber(record.feeAmount),
  paidAmount: toNumber(record.paidAmount),
  unpaidAmount: toNumber(record.unpaidAmount),
  feeDueDate: record.feeDueDate || null,
  remark: record.remark || null,
});

export async function fetchVehiclePersonnelCertificates(
  params: VehiclePersonnelCertificateQueryParams = {}
) {
  const res = await http.get<PageResult<VehiclePersonnelCertificateRecord>>(
    '/vehicle-personnel-certificates',
    { params }
  );
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function fetchVehiclePersonnelCertificateSummary(
  params: Omit<VehiclePersonnelCertificateQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<VehiclePersonnelCertificateSummaryRecord>>(
    '/vehicle-personnel-certificates/summary',
    { params }
  );
  return {
    totalPersons: toNumber(res.data.totalPersons),
    activeCertificates: toNumber(res.data.activeCertificates),
    expiringCertificates: toNumber(res.data.expiringCertificates),
    expiredCertificates: toNumber(res.data.expiredCertificates),
    totalFeeAmount: toNumber(res.data.totalFeeAmount),
    paidAmount: toNumber(res.data.paidAmount),
    unpaidAmount: toNumber(res.data.unpaidAmount),
  };
}

export async function createVehiclePersonnelCertificate(
  payload: VehiclePersonnelCertificateUpsertPayload
) {
  const res = await http.post<VehiclePersonnelCertificateRecord>(
    '/vehicle-personnel-certificates',
    payload
  );
  return mapRecord(res.data);
}

export async function updateVehiclePersonnelCertificate(
  id: string,
  payload: VehiclePersonnelCertificateUpsertPayload
) {
  const res = await http.put<VehiclePersonnelCertificateRecord>(
    '/vehicle-personnel-certificates/' + id,
    payload
  );
  return mapRecord(res.data);
}
