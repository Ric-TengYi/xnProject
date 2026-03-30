import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface UnitRecord {
  id: string;
  orgCode?: string | null;
  orgName: string;
  orgType?: string | null;
  orgTypeLabel?: string | null;
  contactPerson?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  unifiedSocialCode?: string | null;
  status?: string | null;
  statusLabel?: string | null;
  projectCount?: number | null;
  contractCount?: number | null;
  vehicleCount?: number | null;
  activeVehicleCount?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface UnitDetailRecord extends UnitRecord {
  remark?: string | null;
}

export interface UnitSummaryRecord {
  totalUnits: number;
  constructionUnits: number;
  builderUnits: number;
  transportUnits: number;
  totalVehicles: number;
}

export interface UnitQueryParams {
  keyword?: string;
  unitType?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface UnitUpsertPayload {
  orgName: string;
  orgType: string;
  orgCode?: string;
  contactPerson?: string;
  contactPhone?: string;
  address?: string;
  unifiedSocialCode?: string;
  remark?: string;
  status?: string;
}

function toNumber(value?: number | null) {
  return Number(value || 0);
}

function mapUnit(record: Partial<UnitRecord>): UnitRecord {
  return {
    id: String(record.id || ''),
    orgCode: record.orgCode || null,
    orgName: record.orgName || '',
    orgType: record.orgType || null,
    orgTypeLabel: record.orgTypeLabel || null,
    contactPerson: record.contactPerson || null,
    contactPhone: record.contactPhone || null,
    address: record.address || null,
    unifiedSocialCode: record.unifiedSocialCode || null,
    status: record.status || null,
    statusLabel: record.statusLabel || null,
    projectCount: toNumber(record.projectCount),
    contractCount: toNumber(record.contractCount),
    vehicleCount: toNumber(record.vehicleCount),
    activeVehicleCount: toNumber(record.activeVehicleCount),
    createTime: record.createTime || null,
    updateTime: record.updateTime || null,
  };
}

export async function fetchUnits(params: UnitQueryParams = {}) {
  const res = await http.get<PageResult<UnitRecord>>('/units', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map(mapUnit),
  };
}

export async function fetchUnitSummary() {
  const res = await http.get<Partial<UnitSummaryRecord>>('/units/summary');
  return {
    totalUnits: toNumber(res.data.totalUnits),
    constructionUnits: toNumber(res.data.constructionUnits),
    builderUnits: toNumber(res.data.builderUnits),
    transportUnits: toNumber(res.data.transportUnits),
    totalVehicles: toNumber(res.data.totalVehicles),
  };
}

export async function fetchUnitDetail(id: string) {
  const res = await http.get<UnitDetailRecord>('/units/' + id);
  return {
    ...mapUnit(res.data),
    remark: res.data.remark || null,
  };
}

export async function createUnit(payload: UnitUpsertPayload) {
  const res = await http.post<UnitDetailRecord>('/units', payload);
  return res.data;
}

export async function updateUnit(id: string, payload: UnitUpsertPayload) {
  const res = await http.put<UnitDetailRecord>('/units/' + id, payload);
  return res.data;
}
