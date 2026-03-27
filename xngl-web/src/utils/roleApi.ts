import http from './request';

// ── Types ──────────────────────────────────────────────

export interface RoleListItem {
  id: string;
  roleCode: string;
  roleName: string;
  roleScope: string;
  roleCategory: string;
  status: string;
  orgId?: string;
}

export interface RoleDetail extends RoleListItem {
  tenantId: string;
  description?: string;
  dataScopeTypeDefault?: string;
}

export interface RolePermissions {
  menuIds: string[];
  permissionIds: string[];
  buttonCodes: string[];
  apiCodes: string[];
}

export interface RoleCreatePayload {
  tenantId?: string;
  roleCode: string;
  roleName: string;
  roleScope?: string;
  roleCategory?: string;
  description?: string;
  dataScopeTypeDefault?: string;
}

export interface DataScopeRule {
  ruleType: string;
  ruleValue: string;
  resourceCode: string;
}

// ── API ────────────────────────────────────────────────

function getTenantId(): string {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  return userInfo.tenantId || '1';
}

export async function fetchRoles(pageSize = 100, orgId?: string) {
  const params: Record<string, any> = { pageSize, tenantId: getTenantId() };
  if (orgId) params.orgId = orgId;
  const res = await http.get<{ records: RoleListItem[] }>('/roles', { params });
  return res.data?.records || [];
}

export async function fetchRoleDetail(id: string) {
  const res = await http.get<RoleDetail>('/roles/' + id);
  return res.data;
}

export async function createRole(payload: RoleCreatePayload) {
  const body = { ...payload, tenantId: payload.tenantId || getTenantId() };
  const res = await http.post<string>('/roles', body);
  return res.data;
}

export async function updateRole(id: string, payload: RoleCreatePayload) {
  const res = await http.put<void>('/roles/' + id, payload);
  return res.data;
}

export async function deleteRole(id: string) {
  const res = await http.delete<void>('/roles/' + id);
  return res.data;
}

export async function fetchRolePermissions(id: string) {
  const res = await http.get<RolePermissions>('/roles/' + id + '/permissions');
  return res.data;
}

export async function updateRolePermissions(id: string, menuIds: number[], permissionIds: number[]) {
  const res = await http.put<void>('/roles/' + id + '/permissions', { menuIds, permissionIds });
  return res.data;
}

export async function fetchDataScopeRules(id: string) {
  const res = await http.get<DataScopeRule[]>('/roles/' + id + '/data-scope-rules');
  return res.data || [];
}

export async function updateDataScopeRules(id: string, rules: DataScopeRule[]) {
  const res = await http.put<void>('/roles/' + id + '/data-scope-rules', rules);
  return res.data;
}
