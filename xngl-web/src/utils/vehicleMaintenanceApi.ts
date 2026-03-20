import http from './request';
import type { PageResult } from './vehicleApi';

export interface VehicleMaintenancePlanRecord {
  id: string;
  planNo: string;
  vehicleId?: string | null;
  plateNo?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  planType?: string | null;
  cycleType?: string | null;
  cycleValue?: number | null;
  lastMaintainDate?: string | null;
  nextMaintainDate?: string | null;
  lastOdometer: number;
  nextOdometer?: number | null;
  responsibleName?: string | null;
  status: string;
  statusLabel: string;
  overdue?: boolean | null;
  remark?: string | null;
}

export interface VehicleMaintenanceRecord {
  id: string;
  recordNo: string;
  planId?: string | null;
  planNo?: string | null;
  vehicleId?: string | null;
  plateNo?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  maintainType?: string | null;
  serviceDate?: string | null;
  odometer: number;
  vendorName?: string | null;
  costAmount: number;
  items?: string | null;
  operatorName?: string | null;
  status: string;
  statusLabel: string;
  remark?: string | null;
}

export interface VehicleMaintenanceSummaryRecord {
  totalPlans: number;
  activePlans: number;
  overduePlans: number;
  pausedPlans: number;
  recordCount: number;
  totalCostAmount: number;
}

export interface VehicleMaintenanceQueryParams {
  keyword?: string;
  status?: string;
  orgId?: string;
  vehicleId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleMaintenancePlanUpsertPayload {
  vehicleId: number;
  planType: string;
  cycleType: string;
  cycleValue: number;
  lastMaintainDate?: string;
  nextMaintainDate?: string;
  lastOdometer?: number;
  nextOdometer?: number;
  responsibleName?: string;
  status?: string;
  remark?: string;
}

export interface VehicleMaintenanceExecutePayload {
  serviceDate?: string;
  odometer?: number;
  vendorName?: string;
  costAmount?: number;
  items?: string;
  operatorName?: string;
  status?: string;
  remark?: string;
  nextMaintainDate?: string;
  nextOdometer?: number;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapPlan = (record: Partial<VehicleMaintenancePlanRecord>): VehicleMaintenancePlanRecord => ({
  id: String(record.id || ''),
  planNo: record.planNo || '',
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  planType: record.planType || null,
  cycleType: record.cycleType || null,
  cycleValue: record.cycleValue ?? null,
  lastMaintainDate: record.lastMaintainDate || null,
  nextMaintainDate: record.nextMaintainDate || null,
  lastOdometer: toNumber(record.lastOdometer),
  nextOdometer: record.nextOdometer == null ? null : toNumber(record.nextOdometer),
  responsibleName: record.responsibleName || null,
  status: record.status || 'ACTIVE',
  statusLabel: record.statusLabel || '未知',
  overdue: record.overdue ?? false,
  remark: record.remark || null,
});

const mapRecord = (record: Partial<VehicleMaintenanceRecord>): VehicleMaintenanceRecord => ({
  id: String(record.id || ''),
  recordNo: record.recordNo || '',
  planId: record.planId || null,
  planNo: record.planNo || null,
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  maintainType: record.maintainType || null,
  serviceDate: record.serviceDate || null,
  odometer: toNumber(record.odometer),
  vendorName: record.vendorName || null,
  costAmount: toNumber(record.costAmount),
  items: record.items || null,
  operatorName: record.operatorName || null,
  status: record.status || 'DONE',
  statusLabel: record.statusLabel || '未知',
  remark: record.remark || null,
});

export async function fetchVehicleMaintenancePlans(params: VehicleMaintenanceQueryParams = {}) {
  const res = await http.get<PageResult<VehicleMaintenancePlanRecord>>('/vehicle-maintenance-plans', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapPlan),
  };
}

export async function fetchVehicleMaintenanceSummary(
  params: Omit<VehicleMaintenanceQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<VehicleMaintenanceSummaryRecord>>('/vehicle-maintenance-plans/summary', {
    params,
  });
  return {
    totalPlans: toNumber(res.data.totalPlans),
    activePlans: toNumber(res.data.activePlans),
    overduePlans: toNumber(res.data.overduePlans),
    pausedPlans: toNumber(res.data.pausedPlans),
    recordCount: toNumber(res.data.recordCount),
    totalCostAmount: toNumber(res.data.totalCostAmount),
  };
}

export async function fetchVehicleMaintenanceRecords(params: VehicleMaintenanceQueryParams = {}) {
  const res = await http.get<PageResult<VehicleMaintenanceRecord>>('/vehicle-maintenance-plans/records', {
    params,
  });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function createVehicleMaintenancePlan(payload: VehicleMaintenancePlanUpsertPayload) {
  const res = await http.post<VehicleMaintenancePlanRecord>('/vehicle-maintenance-plans', payload);
  return mapPlan(res.data);
}

export async function updateVehicleMaintenancePlan(
  id: string,
  payload: VehicleMaintenancePlanUpsertPayload
) {
  const res = await http.put<VehicleMaintenancePlanRecord>('/vehicle-maintenance-plans/' + id, payload);
  return mapPlan(res.data);
}

export async function executeVehicleMaintenance(id: string, payload: VehicleMaintenanceExecutePayload) {
  const res = await http.post<VehicleMaintenanceRecord>('/vehicle-maintenance-plans/' + id + '/execute', payload);
  return mapRecord(res.data);
}
