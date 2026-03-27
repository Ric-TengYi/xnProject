import http from './request';

// ── Types ──────────────────────────────────────────────

export interface UserListItem {
  id: string;
  username: string;
  name: string;
  mobile?: string;
  email?: string;
  userType?: string;
  mainOrgId?: string;
  mainOrgName?: string;
  roleNames?: string[];
  status?: string;
  lastLoginTime?: string;
}

export interface UserDetail extends UserListItem {
  tenantId?: string;
  avatarUrl?: string;
  authSource?: string;
  needResetPassword?: number;
  lockStatus?: number;
}

export interface UserCreatePayload {
  tenantId?: string;
  username: string;
  name: string;
  password?: string;
  mobile?: string;
  email?: string;
  userType?: string;
  mainOrgId?: string;
  status?: string;
}

// ── API ────────────────────────────────────────────────

function getTenantId(): string {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  return userInfo.tenantId || '1';
}

export async function fetchUsers(params?: {
  keyword?: string;
  orgId?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const tenantId = getTenantId();
  const res = await http.get<{ records: UserListItem[]; total: number }>('/users', {
    params: { tenantId, pageNo: 1, pageSize: 20, ...params },
  });
  return res.data || { records: [], total: 0 };
}

export async function fetchUserDetail(id: string) {
  const res = await http.get<UserDetail>('/users/' + id);
  return res.data;
}

export async function createUser(payload: UserCreatePayload) {
  const body = { ...payload, tenantId: payload.tenantId || getTenantId() };
  const res = await http.post<string>('/users', body);
  return res.data;
}

export async function updateUser(id: string, payload: UserCreatePayload) {
  const res = await http.put<void>('/users/' + id, payload);
  return res.data;
}

export async function deleteUser(id: string) {
  const res = await http.delete<void>('/users/' + id);
  return res.data;
}

export async function fetchUserRoles(id: string) {
  const res = await http.get<{ id: string; roleName: string }[]>('/users/' + id + '/roles');
  return res.data || [];
}

export async function updateUserRoles(id: string, roleIds: string[]) {
  const res = await http.put<void>('/users/' + id + '/roles', { roleIds });
  return res.data;
}

export async function updateUserOrgs(id: string, mainOrgId: string, orgIds: string[]) {
  const res = await http.put<void>('/users/' + id + '/orgs', { mainOrgId, orgIds });
  return res.data;
}

export async function resetUserPassword(id: string, newPassword: string) {
  const res = await http.put<void>('/users/' + id + '/reset-password', { newPassword });
  return res.data;
}

export async function updateUserStatus(id: string, status: string) {
  const res = await http.put<void>('/users/' + id + '/status', { status });
  return res.data;
}
