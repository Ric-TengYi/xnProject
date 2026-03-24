import http from './request';

export interface VehicleModelRecord {
  id: string;
  modelCode: string;
  brand: string;
  modelName: string;
  vehicleType?: string | null;
  axleCount?: number | null;
  seatCount?: number | null;
  deadWeight?: number | null;
  loadWeight?: number | null;
  energyType?: string | null;
  status?: string | null;
  remark?: string | null;
}

export interface VehicleModelPayload {
  modelCode: string;
  brand: string;
  modelName: string;
  vehicleType?: string;
  axleCount?: number;
  seatCount?: number;
  deadWeight?: number;
  loadWeight?: number;
  energyType?: string;
  status?: string;
  remark?: string;
}

const toNumber = (value: any) => (value != null ? Number(value) : null);

const mapRecord = (item: any): VehicleModelRecord => ({
  id: String(item.id || ''),
  modelCode: item.modelCode || '',
  brand: item.brand || '',
  modelName: item.modelName || '',
  vehicleType: item.vehicleType || null,
  axleCount: toNumber(item.axleCount),
  seatCount: toNumber(item.seatCount),
  deadWeight: toNumber(item.deadWeight),
  loadWeight: toNumber(item.loadWeight),
  energyType: item.energyType || null,
  status: item.status || 'ENABLED',
  remark: item.remark || null,
});

export async function fetchVehicleModels(params: Record<string, any> = {}) {
  const res = await http.get<VehicleModelRecord[]>('/vehicle-models', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapRecord);
}

export async function fetchVehicleModelDetail(id: string) {
  const res = await http.get<VehicleModelRecord>(`/vehicle-models/${id}`);
  return mapRecord(res.data || {});
}

export async function exportVehicleModels(params: Record<string, any> = {}) {
  const res = await http.get<Blob>('/vehicle-models/export', {
    params,
    responseType: 'blob',
  });
  return res.data;
}

export async function createVehicleModel(payload: VehicleModelPayload) {
  const res = await http.post<VehicleModelRecord>('/vehicle-models', payload);
  return mapRecord(res.data || {});
}

export async function updateVehicleModel(id: string, payload: VehicleModelPayload) {
  const res = await http.put<VehicleModelRecord>(`/vehicle-models/${id}`, payload);
  return mapRecord(res.data || {});
}

export async function updateVehicleModelStatus(id: string, status: string) {
  await http.put(`/vehicle-models/${id}/status`, { status });
}

export async function deleteVehicleModel(id: string) {
  await http.delete(`/vehicle-models/${id}`);
}
