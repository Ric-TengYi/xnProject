import http from './request';

// ── Types ──────────────────────────────────────────────

export interface MenuTreeNode {
  id: string;
  menuCode: string;
  menuName: string;
  parentId: string;
  menuType: string; // DIR | MENU | BUTTON
  platform: string; // PC | MINI | SCREEN
  path?: string;
  icon?: string;
  sortOrder?: number;
  visible?: string;
  status?: string;
  childCount: number;
  children?: MenuTreeNode[];
}

export interface MenuDetail {
  id: string;
  tenantId?: string;
  parentId?: string;
  menuCode: string;
  menuName: string;
  menuType: string;
  platform: string;
  path?: string;
  component?: string;
  icon?: string;
  sortOrder?: number;
  visible?: string;
  status?: string;
}

export interface MenuPayload {
  tenantId?: string;
  parentId?: string;
  menuCode: string;
  menuName: string;
  menuType: string;
  platform?: string;
  path?: string;
  component?: string;
  icon?: string;
  sortOrder?: number;
  visible?: string;
}

export interface PermissionItem {
  id: string;
  permissionCode: string;
  permissionName: string;
  menuId?: string;
  resourceType?: string;
  status?: string;
}

export interface PermissionPayload {
  tenantId?: string;
  menuId?: string;
  permissionCode: string;
  permissionName: string;
  resourceType?: string;
}

// ── API ────────────────────────────────────────────────

export async function fetchMenuTree(platform?: string) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const tenantId = userInfo.tenantId || '1';
  const params: Record<string, string> = { tenantId };
  if (platform) params.platform = platform;
  const res = await http.get<MenuTreeNode[]>('/menus/tree', { params });
  return res.data || [];
}

export async function fetchMenuDetail(id: string) {
  const res = await http.get<MenuDetail>('/menus/' + id);
  return res.data;
}

export async function createMenu(payload: MenuPayload) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const body = { ...payload, tenantId: payload.tenantId || userInfo.tenantId || '1' };
  const res = await http.post<string>('/menus', body);
  return res.data;
}

export async function updateMenu(id: string, payload: MenuPayload) {
  const res = await http.put<void>('/menus/' + id, payload);
  return res.data;
}

export async function deleteMenu(id: string) {
  const res = await http.delete<void>('/menus/' + id);
  return res.data;
}

export async function fetchPermissions(menuId?: string) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const params: Record<string, string> = { tenantId: userInfo.tenantId || '1', pageSize: '200' };
  if (menuId) params.menuId = menuId;
  const res = await http.get<{ records: PermissionItem[] }>('/permissions', { params });
  return res.data?.records || [];
}

export async function createPermission(payload: PermissionPayload) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const body = { ...payload, tenantId: payload.tenantId || userInfo.tenantId || '1' };
  const res = await http.post<string>('/permissions', body);
  return res.data;
}

export async function updatePermission(id: string, payload: PermissionPayload) {
  const res = await http.put<void>('/permissions/' + id, payload);
  return res.data;
}

export async function deletePermission(id: string) {
  const res = await http.delete<void>('/permissions/' + id);
  return res.data;
}
