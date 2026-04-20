import http, { request } from './request';
import type { PageResult } from './vehicleApi';

export interface VehicleCardRecord {
  id: string;
  cardNo: string;
  cardType: string;
  cardTypeLabel: string;
  providerName?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  vehicleId?: string | null;
  plateNo?: string | null;
  balance: number;
  totalRecharge: number;
  totalConsume: number;
  status: string;
  statusLabel: string;
  remark?: string | null;
  updateTime?: string | null;
}

export interface VehicleCardSummaryRecord {
  totalCards: number;
  fuelCards: number;
  electricCards: number;
  boundCards: number;
  lowBalanceCards: number;
  totalBalance: number;
  fuelBalance: number;
  electricBalance: number;
}

export interface VehicleCardTransactionRecord {
  id: string;
  cardId?: string | null;
  cardNo?: string | null;
  cardType?: string | null;
  cardTypeLabel?: string | null;
  txnType: string;
  txnTypeLabel?: string | null;
  orgId?: string | null;
  orgName?: string | null;
  vehicleId?: string | null;
  plateNo?: string | null;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  occurredAt?: string | null;
  operatorName?: string | null;
  remark?: string | null;
}

export interface VehicleCardTransactionSummaryRecord {
  totalTransactions: number;
  rechargeTransactions: number;
  consumeTransactions: number;
  totalRechargeAmount: number;
  totalConsumeAmount: number;
}

export interface VehicleCardQueryParams {
  keyword?: string;
  cardType?: string;
  status?: string;
  orgId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleCardTransactionQueryParams {
  keyword?: string;
  cardType?: string;
  txnType?: string;
  orgId?: string;
  vehicleId?: string;
  cardId?: string;
  dateFrom?: string;
  dateTo?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface VehicleCardUpsertPayload {
  cardNo: string;
  cardType: string;
  providerName?: string;
  orgId?: number;
  vehicleId?: number;
  balance?: number;
  totalRecharge?: number;
  totalConsume?: number;
  status?: string;
  remark?: string;
}

export interface VehicleCardRechargePayload {
  amount: number;
  remark?: string;
}

export interface VehicleCardConsumePayload {
  amount: number;
  remark?: string;
}

export interface VehicleCardBindPayload {
  vehicleId: number;
}

const toNumber = (value?: number | null) => Number(value || 0);

const mapRecord = (record: Partial<VehicleCardRecord>): VehicleCardRecord => ({
  id: String(record.id || ''),
  cardNo: record.cardNo || '',
  cardType: record.cardType || 'FUEL',
  cardTypeLabel: record.cardTypeLabel || '未知',
  providerName: record.providerName || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  balance: toNumber(record.balance),
  totalRecharge: toNumber(record.totalRecharge),
  totalConsume: toNumber(record.totalConsume),
  status: record.status || 'UNBOUND',
  statusLabel: record.statusLabel || '未知',
  remark: record.remark || null,
  updateTime: record.updateTime || null,
});

const mapTransactionRecord = (
  record: Partial<VehicleCardTransactionRecord>
): VehicleCardTransactionRecord => ({
  id: String(record.id || ''),
  cardId: record.cardId || null,
  cardNo: record.cardNo || null,
  cardType: record.cardType || null,
  cardTypeLabel: record.cardTypeLabel || null,
  txnType: record.txnType || 'RECHARGE',
  txnTypeLabel: record.txnTypeLabel || null,
  orgId: record.orgId || null,
  orgName: record.orgName || null,
  vehicleId: record.vehicleId || null,
  plateNo: record.plateNo || null,
  amount: toNumber(record.amount),
  balanceBefore: toNumber(record.balanceBefore),
  balanceAfter: toNumber(record.balanceAfter),
  occurredAt: record.occurredAt || null,
  operatorName: record.operatorName || null,
  remark: record.remark || null,
});

export async function fetchVehicleCards(params: VehicleCardQueryParams = {}) {
  const res = await http.get<PageResult<VehicleCardRecord>>('/vehicle-cards', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapRecord),
  };
}

export async function fetchVehicleCardSummary(params: Omit<VehicleCardQueryParams, 'pageNo' | 'pageSize'> = {}) {
  const res = await http.get<Partial<VehicleCardSummaryRecord>>('/vehicle-cards/summary', { params });
  return {
    totalCards: toNumber(res.data.totalCards),
    fuelCards: toNumber(res.data.fuelCards),
    electricCards: toNumber(res.data.electricCards),
    boundCards: toNumber(res.data.boundCards),
    lowBalanceCards: toNumber(res.data.lowBalanceCards),
    totalBalance: toNumber(res.data.totalBalance),
    fuelBalance: toNumber(res.data.fuelBalance),
    electricBalance: toNumber(res.data.electricBalance),
  };
}

export async function exportVehicleCards(params: Omit<VehicleCardQueryParams, 'pageNo' | 'pageSize'> = {}) {
  const res = await request.get('/vehicle-cards/export', {
    params,
    responseType: 'blob',
  });
  return res as unknown as Blob;
}

export async function fetchVehicleCardTransactions(params: VehicleCardTransactionQueryParams = {}) {
  const res = await http.get<PageResult<VehicleCardTransactionRecord>>('/vehicle-cards/transactions', {
    params,
  });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapTransactionRecord),
  };
}

export async function fetchVehicleCardTransactionSummary(
  params: Omit<VehicleCardTransactionQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await http.get<Partial<VehicleCardTransactionSummaryRecord>>(
    '/vehicle-cards/transactions/summary',
    { params }
  );
  return {
    totalTransactions: toNumber(res.data.totalTransactions),
    rechargeTransactions: toNumber(res.data.rechargeTransactions),
    consumeTransactions: toNumber(res.data.consumeTransactions),
    totalRechargeAmount: toNumber(res.data.totalRechargeAmount),
    totalConsumeAmount: toNumber(res.data.totalConsumeAmount),
  };
}

export async function exportVehicleCardTransactions(
  params: Omit<VehicleCardTransactionQueryParams, 'pageNo' | 'pageSize'> = {}
) {
  const res = await request.get('/vehicle-cards/transactions/export', {
    params,
    responseType: 'blob',
  });
  return res as unknown as Blob;
}

export async function createVehicleCard(payload: VehicleCardUpsertPayload) {
  const res = await http.post<VehicleCardRecord>('/vehicle-cards', payload);
  return mapRecord(res.data);
}

export async function updateVehicleCard(id: string, payload: VehicleCardUpsertPayload) {
  const res = await http.put<VehicleCardRecord>('/vehicle-cards/' + id, payload);
  return mapRecord(res.data);
}

export async function rechargeVehicleCard(id: string, payload: VehicleCardRechargePayload) {
  const res = await http.post<VehicleCardRecord>('/vehicle-cards/' + id + '/recharge', payload);
  return mapRecord(res.data);
}

export async function consumeVehicleCard(id: string, payload: VehicleCardConsumePayload) {
  const res = await http.post<VehicleCardRecord>('/vehicle-cards/' + id + '/consume', payload);
  return mapRecord(res.data);
}

export async function bindVehicleCard(id: string, payload: VehicleCardBindPayload) {
  const res = await http.post<VehicleCardRecord>('/vehicle-cards/' + id + '/bind', payload);
  return mapRecord(res.data);
}

export async function unbindVehicleCard(id: string) {
  const res = await http.post<VehicleCardRecord>('/vehicle-cards/' + id + '/unbind');
  return mapRecord(res.data);
}
