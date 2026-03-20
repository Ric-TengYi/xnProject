import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface VehicleRecord {
  id: string;
  plateNo: string;
  vin?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  vehicleType?: string | null;
  brand?: string | null;
  model?: string | null;
  energyType?: string | null;
  axleCount?: number | null;
  loadWeight?: number | null;
  driverName?: string | null;
  driverPhone?: string | null;
  fleetName?: string | null;
  captainName?: string | null;
  captainPhone?: string | null;
  status?: number | null;
  statusLabel?: string | null;
  useStatus?: string | null;
  runningStatus?: string | null;
  runningStatusLabel?: string | null;
  currentSpeed?: number | null;
  currentMileage?: number | null;
  nextMaintainDate?: string | null;
  annualInspectionExpireDate?: string | null;
  insuranceExpireDate?: string | null;
  warningLabel?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface VehicleDetailRecord extends VehicleRecord {
  deadWeight?: number | null;
  lng?: number | null;
  lat?: number | null;
  gpsTime?: string | null;
  remark?: string | null;
}

export interface VehicleStatsRecord {
  totalVehicles: number;
  activeVehicles: number;
  maintenanceVehicles: number;
  disabledVehicles: number;
  warningVehicles: number;
  activeRate: number;
  totalLoadTons: number;
}

export interface VehicleCompanyCapacityRecord {
  orgId?: string | null;
  orgName: string;
  totalVehicles: number;
  activeVehicles: number;
  movingVehicles: number;
  warningVehicles: number;
  disabledVehicles: number;
  totalLoadTons: number;
  activeRate: number;
  avgLoadTons: number;
  captainName?: string | null;
  captainPhone?: string | null;
}

export interface VehicleFleetRecord {
  id: string;
  fleetName: string;
  orgId?: string | null;
  orgName?: string | null;
  captainName?: string | null;
  captainPhone?: string | null;
  driverCount: number;
  totalVehicles: number;
  activeVehicles: number;
  movingVehicles: number;
  warningVehicles: number;
  totalLoadTons: number;
  avgLoadTons: number;
  statusLabel?: string | null;
}

export interface VehicleQueryParams {
  keyword?: string;
  status?: number;
  orgId?: string;
  useStatus?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleUpsertPayload {
  plateNo: string;
  vin?: string;
  orgId?: number;
  vehicleType?: string;
  brand?: string;
  model?: string;
  energyType?: string;
  axleCount?: number;
  deadWeight?: number;
  loadWeight?: number;
  driverName?: string;
  driverPhone?: string;
  fleetName?: string;
  captainName?: string;
  captainPhone?: string;
  status?: number;
  useStatus?: string;
  runningStatus?: string;
  currentSpeed?: number;
  currentMileage?: number;
  nextMaintainDate?: string;
  annualInspectionExpireDate?: string;
  insuranceExpireDate?: string;
  lng?: number;
  lat?: number;
  gpsTime?: string;
  remark?: string;
}

function toNumber(value?: number | null) {
  return Number(value || 0);
}

function mapVehicle(record: Partial<VehicleRecord>): VehicleRecord {
  return {
    id: String(record.id || ''),
    plateNo: record.plateNo || '',
    vin: record.vin || null,
    orgId: record.orgId || null,
    orgName: record.orgName || null,
    vehicleType: record.vehicleType || null,
    brand: record.brand || null,
    model: record.model || null,
    energyType: record.energyType || null,
    axleCount: record.axleCount ?? null,
    loadWeight: toNumber(record.loadWeight),
    driverName: record.driverName || null,
    driverPhone: record.driverPhone || null,
    fleetName: record.fleetName || null,
    captainName: record.captainName || null,
    captainPhone: record.captainPhone || null,
    status: record.status ?? null,
    statusLabel: record.statusLabel || null,
    useStatus: record.useStatus || null,
    runningStatus: record.runningStatus || null,
    runningStatusLabel: record.runningStatusLabel || null,
    currentSpeed: toNumber(record.currentSpeed),
    currentMileage: toNumber(record.currentMileage),
    nextMaintainDate: record.nextMaintainDate || null,
    annualInspectionExpireDate: record.annualInspectionExpireDate || null,
    insuranceExpireDate: record.insuranceExpireDate || null,
    warningLabel: record.warningLabel || null,
    createTime: record.createTime || null,
    updateTime: record.updateTime || null,
  };
}

export async function fetchVehicles(params: VehicleQueryParams = {}) {
  const res = await http.get<PageResult<VehicleRecord>>('/vehicles', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapVehicle),
  };
}

export async function fetchVehicleDetail(id: string) {
  const res = await http.get<VehicleDetailRecord>('/vehicles/' + id);
  const base = mapVehicle(res.data);
  return {
    ...base,
    deadWeight: toNumber(res.data.deadWeight),
    lng: res.data.lng ?? null,
    lat: res.data.lat ?? null,
    gpsTime: res.data.gpsTime || null,
    remark: res.data.remark || null,
  };
}

export async function createVehicle(payload: VehicleUpsertPayload) {
  const res = await http.post<VehicleDetailRecord>('/vehicles', payload);
  return fetchVehicleDetail(String(res.data.id));
}

export async function updateVehicle(id: string, payload: VehicleUpsertPayload) {
  const res = await http.put<VehicleDetailRecord>('/vehicles/' + id, payload);
  return fetchVehicleDetail(String(res.data.id || id));
}

export async function fetchVehicleStats() {
  const res = await http.get<Partial<VehicleStatsRecord>>('/vehicles/stats');
  return {
    totalVehicles: toNumber(res.data.totalVehicles),
    activeVehicles: toNumber(res.data.activeVehicles),
    maintenanceVehicles: toNumber(res.data.maintenanceVehicles),
    disabledVehicles: toNumber(res.data.disabledVehicles),
    warningVehicles: toNumber(res.data.warningVehicles),
    activeRate: toNumber(res.data.activeRate),
    totalLoadTons: toNumber(res.data.totalLoadTons),
  };
}

export async function fetchVehicleCompanyCapacity() {
  const res = await http.get<VehicleCompanyCapacityRecord[]>('/vehicles/company-capacity');
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    orgId: item.orgId || null,
    orgName: item.orgName || '未归属单位',
    totalVehicles: toNumber(item.totalVehicles),
    activeVehicles: toNumber(item.activeVehicles),
    movingVehicles: toNumber(item.movingVehicles),
    warningVehicles: toNumber(item.warningVehicles),
    disabledVehicles: toNumber(item.disabledVehicles),
    totalLoadTons: toNumber(item.totalLoadTons),
    activeRate: toNumber(item.activeRate),
    avgLoadTons: toNumber(item.avgLoadTons),
    captainName: item.captainName || null,
    captainPhone: item.captainPhone || null,
  }));
}

export async function fetchVehicleFleets() {
  const res = await http.get<VehicleFleetRecord[]>('/vehicles/fleets');
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    id: item.id,
    fleetName: item.fleetName || '未编组车队',
    orgId: item.orgId || null,
    orgName: item.orgName || null,
    captainName: item.captainName || null,
    captainPhone: item.captainPhone || null,
    driverCount: toNumber(item.driverCount),
    totalVehicles: toNumber(item.totalVehicles),
    activeVehicles: toNumber(item.activeVehicles),
    movingVehicles: toNumber(item.movingVehicles),
    warningVehicles: toNumber(item.warningVehicles),
    totalLoadTons: toNumber(item.totalLoadTons),
    avgLoadTons: toNumber(item.avgLoadTons),
    statusLabel: item.statusLabel || null,
  }));
}
