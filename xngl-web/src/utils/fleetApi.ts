import http from './request';
import type { PageResult } from './vehicleApi';

export interface FleetSummaryRecord {
  totalFleets: number;
  activeFleets: number;
  totalPlans: number;
  pendingDispatchOrders: number;
  totalRevenueAmount: number;
  totalProfitAmount: number;
}

export interface FleetTrackingSummaryRecord {
  totalVehicles: number;
  movingVehicles: number;
  stoppedVehicles: number;
  offlineVehicles: number;
  deliveringVehicles: number;
  warningVehicles: number;
}

export interface FleetProfileRecord {
  id: string;
  orgId?: string | null;
  orgName?: string | null;
  fleetName: string;
  captainName?: string | null;
  captainPhone?: string | null;
  driverCountPlan: number;
  vehicleCountPlan: number;
  status: string;
  statusLabel: string;
  attendanceMode?: string | null;
  remark?: string | null;
}

export interface FleetTransportPlanRecord {
  id: string;
  fleetId?: string | null;
  fleetName: string;
  orgId?: string | null;
  orgName?: string | null;
  planNo: string;
  planDate?: string | null;
  sourcePoint?: string | null;
  destinationPoint?: string | null;
  cargoType?: string | null;
  plannedTrips: number;
  plannedVolume: number;
  status: string;
  statusLabel: string;
  remark?: string | null;
}

export interface FleetDispatchOrderRecord {
  id: string;
  fleetId?: string | null;
  fleetName: string;
  orgId?: string | null;
  orgName?: string | null;
  orderNo: string;
  relatedPlanNo?: string | null;
  applyDate?: string | null;
  requestedVehicleCount: number;
  requestedDriverCount: number;
  urgencyLevel: string;
  urgencyLabel: string;
  status: string;
  statusLabel: string;
  applicantName?: string | null;
  approvedBy?: string | null;
  approvedTime?: string | null;
  auditRemark?: string | null;
  remark?: string | null;
}

export interface FleetFinanceRecord {
  id: string;
  fleetId?: string | null;
  fleetName: string;
  orgId?: string | null;
  orgName?: string | null;
  recordNo: string;
  contractNo?: string | null;
  statementMonth?: string | null;
  revenueAmount: number;
  costAmount: number;
  otherAmount: number;
  settledAmount: number;
  profitAmount: number;
  outstandingAmount: number;
  status: string;
  statusLabel: string;
  remark?: string | null;
}

export interface FleetFinanceSummaryRecord {
  totalRecords: number;
  settledRecords: number;
  totalRevenueAmount: number;
  totalCostAmount: number;
  totalProfitAmount: number;
  totalOutstandingAmount: number;
}

export interface FleetReportItemRecord {
  fleetId?: string | null;
  fleetName: string;
  orgName?: string | null;
  totalPlans: number;
  totalDispatchOrders: number;
  approvedDispatchOrders: number;
  plannedVolume: number;
  revenueAmount: number;
  costAmount: number;
  profitAmount: number;
}

export interface FleetTrackingRecord {
  vehicleId: string;
  plateNo: string;
  orgId?: string | null;
  orgName?: string | null;
  fleetId?: string | null;
  fleetName?: string | null;
  driverName?: string | null;
  driverPhone?: string | null;
  trackingStatus?: string | null;
  trackingStatusLabel?: string | null;
  runningStatus?: string | null;
  runningStatusLabel?: string | null;
  vehicleStatusLabel?: string | null;
  warningLabel?: string | null;
  currentSpeed: number;
  currentMileage: number;
  lng?: number | null;
  lat?: number | null;
  gpsTime?: string | null;
  dispatchOrderNo?: string | null;
  dispatchStatus?: string | null;
  dispatchStatusLabel?: string | null;
  relatedPlanNo?: string | null;
  sourcePoint?: string | null;
  destinationPoint?: string | null;
  cargoType?: string | null;
}

export interface FleetTrackingStopRecord {
  startTime?: string | null;
  endTime?: string | null;
  durationMinutes: number;
  lng?: number | null;
  lat?: number | null;
  remark?: string | null;
}

export interface FleetTrackingHistoryRecord {
  vehicleId?: string | null;
  plateNo?: string | null;
  fleetId?: string | null;
  fleetName?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  dispatchOrderNo?: string | null;
  relatedPlanNo?: string | null;
  sourcePoint?: string | null;
  destinationPoint?: string | null;
  cargoType?: string | null;
  pointCount: number;
  totalDistanceKm: number;
  maxSpeed: number;
  averageSpeed: number;
  points: Array<{
    id?: string | null;
    lng: number;
    lat: number;
    speed?: number | null;
    direction?: number | null;
    locateTime?: string | null;
    sourceType?: string | null;
    remark?: string | null;
  }>;
  stops: FleetTrackingStopRecord[];
}

export interface FleetProfileQueryParams {
  keyword?: string;
  status?: string;
  orgId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FleetTransportPlanQueryParams {
  keyword?: string;
  status?: string;
  fleetId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FleetDispatchOrderQueryParams {
  keyword?: string;
  status?: string;
  fleetId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FleetFinanceRecordQueryParams {
  keyword?: string;
  status?: string;
  fleetId?: string;
  contractNo?: string;
  statementMonthFrom?: string;
  statementMonthTo?: string;
  unsettledOnly?: boolean;
  pageNo?: number;
  pageSize?: number;
}

export interface FleetTrackingQueryParams {
  keyword?: string;
  fleetId?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FleetProfileUpsertPayload {
  orgId?: number;
  fleetName: string;
  captainName?: string;
  captainPhone?: string;
  driverCountPlan?: number;
  vehicleCountPlan?: number;
  status?: string;
  attendanceMode?: string;
  remark?: string;
}

export interface FleetTransportPlanUpsertPayload {
  fleetId: number;
  planNo?: string;
  planDate?: string;
  sourcePoint?: string;
  destinationPoint?: string;
  cargoType?: string;
  plannedTrips?: number;
  plannedVolume?: number;
  status?: string;
  remark?: string;
}

export interface FleetDispatchOrderUpsertPayload {
  fleetId: number;
  relatedPlanNo?: string;
  applyDate?: string;
  requestedVehicleCount?: number;
  requestedDriverCount?: number;
  urgencyLevel?: string;
  status?: string;
  applicantName?: string;
  remark?: string;
}

export interface FleetDispatchAuditPayload {
  comment?: string;
}

export interface FleetFinanceRecordUpsertPayload {
  fleetId: number;
  contractNo?: string;
  statementMonth?: string;
  revenueAmount?: number;
  costAmount?: number;
  otherAmount?: number;
  settledAmount?: number;
  status?: string;
  remark?: string;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapProfile = (record: Partial<FleetProfileRecord>): FleetProfileRecord => ({
  id: String(record.id || ''),
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  fleetName: record.fleetName || '',
  captainName: record.captainName || null,
  captainPhone: record.captainPhone || null,
  driverCountPlan: toNumber(record.driverCountPlan),
  vehicleCountPlan: toNumber(record.vehicleCountPlan),
  status: record.status || 'ENABLED',
  statusLabel: record.statusLabel || '未知',
  attendanceMode: record.attendanceMode || null,
  remark: record.remark || null,
});

const mapTransportPlan = (record: Partial<FleetTransportPlanRecord>): FleetTransportPlanRecord => ({
  id: String(record.id || ''),
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || '未关联车队',
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  planNo: record.planNo || '',
  planDate: record.planDate || null,
  sourcePoint: record.sourcePoint || null,
  destinationPoint: record.destinationPoint || null,
  cargoType: record.cargoType || null,
  plannedTrips: toNumber(record.plannedTrips),
  plannedVolume: toNumber(record.plannedVolume),
  status: record.status || 'ACTIVE',
  statusLabel: record.statusLabel || '未知',
  remark: record.remark || null,
});

const mapDispatchOrder = (record: Partial<FleetDispatchOrderRecord>): FleetDispatchOrderRecord => ({
  id: String(record.id || ''),
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || '未关联车队',
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  orderNo: record.orderNo || '',
  relatedPlanNo: record.relatedPlanNo || null,
  applyDate: record.applyDate || null,
  requestedVehicleCount: toNumber(record.requestedVehicleCount),
  requestedDriverCount: toNumber(record.requestedDriverCount),
  urgencyLevel: record.urgencyLevel || 'MEDIUM',
  urgencyLabel: record.urgencyLabel || '中',
  status: record.status || 'PENDING_APPROVAL',
  statusLabel: record.statusLabel || '未知',
  applicantName: record.applicantName || null,
  approvedBy: record.approvedBy || null,
  approvedTime: record.approvedTime || null,
  auditRemark: record.auditRemark || null,
  remark: record.remark || null,
});

const mapFinanceRecord = (record: Partial<FleetFinanceRecord>): FleetFinanceRecord => ({
  id: String(record.id || ''),
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || '未关联车队',
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  recordNo: record.recordNo || '',
  contractNo: record.contractNo || null,
  statementMonth: record.statementMonth || null,
  revenueAmount: toNumber(record.revenueAmount),
  costAmount: toNumber(record.costAmount),
  otherAmount: toNumber(record.otherAmount),
  settledAmount: toNumber(record.settledAmount),
  profitAmount: toNumber(record.profitAmount),
  outstandingAmount: toNumber(record.outstandingAmount),
  status: record.status || 'CONFIRMED',
  statusLabel: record.statusLabel || '未知',
  remark: record.remark || null,
});

const mapReportItem = (record: Partial<FleetReportItemRecord>): FleetReportItemRecord => ({
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || '未命名车队',
  orgName: record.orgName || null,
  totalPlans: toNumber(record.totalPlans),
  totalDispatchOrders: toNumber(record.totalDispatchOrders),
  approvedDispatchOrders: toNumber(record.approvedDispatchOrders),
  plannedVolume: toNumber(record.plannedVolume),
  revenueAmount: toNumber(record.revenueAmount),
  costAmount: toNumber(record.costAmount),
  profitAmount: toNumber(record.profitAmount),
});

const mapTrackingSummary = (
  record: Partial<FleetTrackingSummaryRecord>
): FleetTrackingSummaryRecord => ({
  totalVehicles: toNumber(record.totalVehicles),
  movingVehicles: toNumber(record.movingVehicles),
  stoppedVehicles: toNumber(record.stoppedVehicles),
  offlineVehicles: toNumber(record.offlineVehicles),
  deliveringVehicles: toNumber(record.deliveringVehicles),
  warningVehicles: toNumber(record.warningVehicles),
});

const mapTrackingRecord = (record: Partial<FleetTrackingRecord>): FleetTrackingRecord => ({
  vehicleId: String(record.vehicleId || ''),
  plateNo: record.plateNo || '',
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || null,
  driverName: record.driverName || null,
  driverPhone: record.driverPhone || null,
  trackingStatus: record.trackingStatus || null,
  trackingStatusLabel: record.trackingStatusLabel || null,
  runningStatus: record.runningStatus || null,
  runningStatusLabel: record.runningStatusLabel || null,
  vehicleStatusLabel: record.vehicleStatusLabel || null,
  warningLabel: record.warningLabel || null,
  currentSpeed: toNumber(record.currentSpeed),
  currentMileage: toNumber(record.currentMileage),
  lng: record.lng ?? null,
  lat: record.lat ?? null,
  gpsTime: record.gpsTime || null,
  dispatchOrderNo: record.dispatchOrderNo || null,
  dispatchStatus: record.dispatchStatus || null,
  dispatchStatusLabel: record.dispatchStatusLabel || null,
  relatedPlanNo: record.relatedPlanNo || null,
  sourcePoint: record.sourcePoint || null,
  destinationPoint: record.destinationPoint || null,
  cargoType: record.cargoType || null,
});

const mapTrackingHistory = (
  record: Partial<FleetTrackingHistoryRecord>
): FleetTrackingHistoryRecord => ({
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  fleetId: record.fleetId || null,
  fleetName: record.fleetName || null,
  startTime: record.startTime || null,
  endTime: record.endTime || null,
  dispatchOrderNo: record.dispatchOrderNo || null,
  relatedPlanNo: record.relatedPlanNo || null,
  sourcePoint: record.sourcePoint || null,
  destinationPoint: record.destinationPoint || null,
  cargoType: record.cargoType || null,
  pointCount: toNumber(record.pointCount),
  totalDistanceKm: toNumber(record.totalDistanceKm),
  maxSpeed: toNumber(record.maxSpeed),
  averageSpeed: toNumber(record.averageSpeed),
  points: Array.isArray(record.points)
    ? record.points.map((item) => ({
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
  stops: Array.isArray(record.stops)
    ? record.stops.map((item) => ({
        startTime: item.startTime || null,
        endTime: item.endTime || null,
        durationMinutes: toNumber(item.durationMinutes),
        lng: item.lng ?? null,
        lat: item.lat ?? null,
        remark: item.remark || null,
      }))
    : [],
});

export async function fetchFleetSummary() {
  const res = await http.get<Partial<FleetSummaryRecord>>('/fleet-management/summary');
  return {
    totalFleets: toNumber(res.data.totalFleets),
    activeFleets: toNumber(res.data.activeFleets),
    totalPlans: toNumber(res.data.totalPlans),
    pendingDispatchOrders: toNumber(res.data.pendingDispatchOrders),
    totalRevenueAmount: toNumber(res.data.totalRevenueAmount),
    totalProfitAmount: toNumber(res.data.totalProfitAmount),
  };
}

export async function fetchFleetProfiles(params: FleetProfileQueryParams = {}) {
  const res = await http.get<PageResult<FleetProfileRecord>>('/fleet-management/profiles', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapProfile),
  };
}

export async function createFleetProfile(payload: FleetProfileUpsertPayload) {
  const res = await http.post<FleetProfileRecord>('/fleet-management/profiles', payload);
  return mapProfile(res.data);
}

export async function updateFleetProfile(id: string, payload: FleetProfileUpsertPayload) {
  const res = await http.put<FleetProfileRecord>('/fleet-management/profiles/' + id, payload);
  return mapProfile(res.data);
}

export async function fetchFleetTransportPlans(params: FleetTransportPlanQueryParams = {}) {
  const res = await http.get<PageResult<FleetTransportPlanRecord>>('/fleet-management/transport-plans', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapTransportPlan),
  };
}

export async function createFleetTransportPlan(payload: FleetTransportPlanUpsertPayload) {
  const res = await http.post<FleetTransportPlanRecord>('/fleet-management/transport-plans', payload);
  return mapTransportPlan(res.data);
}

export async function updateFleetTransportPlan(id: string, payload: FleetTransportPlanUpsertPayload) {
  const res = await http.put<FleetTransportPlanRecord>('/fleet-management/transport-plans/' + id, payload);
  return mapTransportPlan(res.data);
}

export async function fetchFleetDispatchOrders(params: FleetDispatchOrderQueryParams = {}) {
  const res = await http.get<PageResult<FleetDispatchOrderRecord>>('/fleet-management/dispatch-orders', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapDispatchOrder),
  };
}

export async function createFleetDispatchOrder(payload: FleetDispatchOrderUpsertPayload) {
  const res = await http.post<FleetDispatchOrderRecord>('/fleet-management/dispatch-orders', payload);
  return mapDispatchOrder(res.data);
}

export async function updateFleetDispatchOrder(id: string, payload: FleetDispatchOrderUpsertPayload) {
  const res = await http.put<FleetDispatchOrderRecord>('/fleet-management/dispatch-orders/' + id, payload);
  return mapDispatchOrder(res.data);
}

export async function approveFleetDispatchOrder(id: string, payload: FleetDispatchAuditPayload = {}) {
  const res = await http.post<FleetDispatchOrderRecord>(
    '/fleet-management/dispatch-orders/' + id + '/approve',
    payload
  );
  return mapDispatchOrder(res.data);
}

export async function rejectFleetDispatchOrder(id: string, payload: FleetDispatchAuditPayload = {}) {
  const res = await http.post<FleetDispatchOrderRecord>(
    '/fleet-management/dispatch-orders/' + id + '/reject',
    payload
  );
  return mapDispatchOrder(res.data);
}

export async function fetchFleetFinanceRecords(params: FleetFinanceRecordQueryParams = {}) {
  const res = await http.get<PageResult<FleetFinanceRecord>>('/fleet-management/finance-records', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapFinanceRecord),
  };
}

export async function fetchFleetFinanceSummary(
  params: Omit<FleetFinanceRecordQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<FleetFinanceSummaryRecord>>('/fleet-management/finance-records/summary', {
    params,
  });
  return {
    totalRecords: toNumber(res.data.totalRecords),
    settledRecords: toNumber(res.data.settledRecords),
    totalRevenueAmount: toNumber(res.data.totalRevenueAmount),
    totalCostAmount: toNumber(res.data.totalCostAmount),
    totalProfitAmount: toNumber(res.data.totalProfitAmount),
    totalOutstandingAmount: toNumber(res.data.totalOutstandingAmount),
  };
}

export async function exportFleetFinanceRecords(
  params: Omit<FleetFinanceRecordQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Blob>('/fleet-management/finance-records/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}

export async function createFleetFinanceRecord(payload: FleetFinanceRecordUpsertPayload) {
  const res = await http.post<FleetFinanceRecord>('/fleet-management/finance-records', payload);
  return mapFinanceRecord(res.data);
}

export async function updateFleetFinanceRecord(id: string, payload: FleetFinanceRecordUpsertPayload) {
  const res = await http.put<FleetFinanceRecord>('/fleet-management/finance-records/' + id, payload);
  return mapFinanceRecord(res.data);
}

export async function fetchFleetReport(params?: {
  keyword?: string;
  orgId?: string;
  statementMonthFrom?: string;
  statementMonthTo?: string;
}) {
  const res = await http.get<FleetReportItemRecord[]>('/fleet-management/report', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapReportItem);
}

export async function exportFleetReport(params?: {
  keyword?: string;
  orgId?: string;
  statementMonthFrom?: string;
  statementMonthTo?: string;
}) {
  const res = await http.get<Blob>('/fleet-management/report/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}

export async function fetchFleetTrackingSummary(params: FleetTrackingQueryParams = {}) {
  const res = await http.get<FleetTrackingSummaryRecord>('/fleet-management/tracking/summary', {
    params,
  });
  return mapTrackingSummary(res.data);
}

export async function fetchFleetTracking(params: FleetTrackingQueryParams = {}) {
  const res = await http.get<PageResult<FleetTrackingRecord>>('/fleet-management/tracking', {
    params,
  });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapTrackingRecord),
  };
}

export async function fetchFleetTrackingHistory(
  vehicleId: string,
  params?: {
    fleetId?: string;
    startTime?: string;
    endTime?: string;
    minStopMinutes?: number;
  }
) {
  const res = await http.get<FleetTrackingHistoryRecord>(
    '/fleet-management/tracking/' + vehicleId + '/history',
    { params }
  );
  return mapTrackingHistory(res.data);
}
