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

export interface VehicleTrackPointRecord {
  id?: string | null;
  lng: number;
  lat: number;
  speed?: number | null;
  direction?: number | null;
  locateTime?: string | null;
  sourceType?: string | null;
  remark?: string | null;
}

export interface VehicleTrackHistoryRecord {
  vehicleId?: string | null;
  plateNo?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  pointCount?: number | null;
  points: VehicleTrackPointRecord[];
}

export interface VehicleViolationRecord {
  id: string;
  vehicleId?: string | null;
  plateNo: string;
  orgId?: string | null;
  orgName?: string | null;
  violationType?: string | null;
  triggerTime?: string | null;
  triggerLocation?: string | null;
  actionStatus?: string | null;
  actionStatusLabel?: string | null;
  penaltyResult?: string | null;
  banStartTime?: string | null;
  banEndTime?: string | null;
  releaseTime?: string | null;
  releaseReason?: string | null;
  operatorName?: string | null;
  remark?: string | null;
}

export interface VehicleViolationSummaryRecord {
  totalCount: number;
  pendingCount: number;
  processedCount: number;
  disabledCount: number;
  releasedCount: number;
  vehicleCount: number;
}

export interface VehicleViolationDetailRecord extends VehicleViolationRecord {
  vehicleType?: string | null;
  brand?: string | null;
  model?: string | null;
  driverName?: string | null;
  driverPhone?: string | null;
  fleetName?: string | null;
  useStatus?: string | null;
  status?: number | null;
  currentSpeed?: number | null;
  currentMileage?: number | null;
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
  vehicleType?: string;
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

export interface VehicleViolationQueryParams {
  keyword?: string;
  violationType?: string;
  actionStatus?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleDisablePayload {
  vehicleId: number;
  violationType: string;
  triggerTime?: string;
  triggerLocation?: string;
  penaltyResult?: string;
  banDays?: number;
  banStartTime?: string;
  banEndTime?: string;
  remark?: string;
}

export interface VehicleBatchStatusPayload {
  ids: number[];
  status: number;
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

export async function fetchVehicleTrackHistory(
  id: string,
  params?: { startTime?: string; endTime?: string }
) {
  const res = await http.get<VehicleTrackHistoryRecord>(`/vehicles/${id}/track-history`, { params });
  return {
    vehicleId: res.data.vehicleId || null,
    plateNo: res.data.plateNo || null,
    startTime: res.data.startTime || null,
    endTime: res.data.endTime || null,
    pointCount: toNumber(res.data.pointCount),
    points: Array.isArray(res.data.points)
      ? res.data.points.map((item) => ({
          id: item.id || null,
          lng: Number(item.lng || 0),
          lat: Number(item.lat || 0),
          speed: item.speed != null ? Number(item.speed) : null,
          direction: item.direction != null ? Number(item.direction) : null,
          locateTime: item.locateTime || null,
          sourceType: item.sourceType || null,
          remark: item.remark || null,
        }))
      : [],
  };
}

export async function fetchVehicleViolations(params: VehicleViolationQueryParams = {}) {
  const res = await http.get<PageResult<VehicleViolationRecord>>('/vehicles/violations', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map((item) => ({
      id: String(item.id || ''),
      vehicleId: item.vehicleId || null,
      plateNo: item.plateNo || '',
      orgId: item.orgId || null,
      orgName: item.orgName || null,
      violationType: item.violationType || null,
      triggerTime: item.triggerTime || null,
      triggerLocation: item.triggerLocation || null,
      actionStatus: item.actionStatus || null,
      actionStatusLabel: item.actionStatusLabel || null,
      penaltyResult: item.penaltyResult || null,
      banStartTime: item.banStartTime || null,
      banEndTime: item.banEndTime || null,
      releaseTime: item.releaseTime || null,
      releaseReason: item.releaseReason || null,
      operatorName: item.operatorName || null,
      remark: item.remark || null,
    })),
  };
}

export async function fetchVehicleViolationSummary(params: VehicleViolationQueryParams = {}) {
  const res = await http.get<Partial<VehicleViolationSummaryRecord>>('/vehicles/violations/summary', { params });
  return {
    totalCount: toNumber(res.data.totalCount),
    pendingCount: toNumber(res.data.pendingCount),
    processedCount: toNumber(res.data.processedCount),
    disabledCount: toNumber(res.data.disabledCount),
    releasedCount: toNumber(res.data.releasedCount),
    vehicleCount: toNumber(res.data.vehicleCount),
  };
}

export async function fetchVehicleViolationDetail(id: string) {
  const res = await http.get<VehicleViolationDetailRecord>(`/vehicles/violations/${id}`);
  return {
    id: String(res.data.id || ''),
    vehicleId: res.data.vehicleId || null,
    plateNo: res.data.plateNo || '',
    orgId: res.data.orgId || null,
    orgName: res.data.orgName || null,
    violationType: res.data.violationType || null,
    triggerTime: res.data.triggerTime || null,
    triggerLocation: res.data.triggerLocation || null,
    actionStatus: res.data.actionStatus || null,
    actionStatusLabel: res.data.actionStatusLabel || null,
    penaltyResult: res.data.penaltyResult || null,
    banStartTime: res.data.banStartTime || null,
    banEndTime: res.data.banEndTime || null,
    releaseTime: res.data.releaseTime || null,
    releaseReason: res.data.releaseReason || null,
    operatorName: res.data.operatorName || null,
    remark: res.data.remark || null,
    vehicleType: res.data.vehicleType || null,
    brand: res.data.brand || null,
    model: res.data.model || null,
    driverName: res.data.driverName || null,
    driverPhone: res.data.driverPhone || null,
    fleetName: res.data.fleetName || null,
    useStatus: res.data.useStatus || null,
    status: res.data.status ?? null,
    currentSpeed: res.data.currentSpeed != null ? Number(res.data.currentSpeed) : null,
    currentMileage: res.data.currentMileage != null ? Number(res.data.currentMileage) : null,
  };
}

export async function disableVehicle(payload: VehicleDisablePayload) {
  const res = await http.post<VehicleViolationRecord>('/vehicles/violations/disable', payload);
  return res.data;
}

export async function releaseVehicleViolation(id: string, releaseReason: string) {
  const res = await http.put<VehicleViolationRecord>(`/vehicles/violations/${id}/release`, {
    releaseReason,
  });
  return res.data;
}

export async function createVehicle(payload: VehicleUpsertPayload) {
  const res = await http.post<VehicleDetailRecord>('/vehicles', payload);
  return fetchVehicleDetail(String(res.data.id));
}

export async function updateVehicle(id: string, payload: VehicleUpsertPayload) {
  const res = await http.put<VehicleDetailRecord>('/vehicles/' + id, payload);
  return fetchVehicleDetail(String(res.data.id || id));
}

export async function deleteVehicle(id: string) {
  await http.delete('/vehicles/' + id);
}

export async function exportVehicles(params: VehicleQueryParams = {}) {
  const res = await http.get<Blob>('/vehicles/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}

export async function batchUpdateVehicleStatus(payload: VehicleBatchStatusPayload) {
  const res = await http.put<{ updated: number; status: number }>('/vehicles/batch-status', payload);
  return {
    updated: Number(res.data.updated || 0),
    status: Number(res.data.status || 0),
  };
}

export async function batchDeleteVehicles(ids: number[]) {
  const res = await http.post<{ deleted: number }>('/vehicles/batch-delete', { ids });
  return {
    deleted: Number(res.data.deleted || 0),
  };
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
