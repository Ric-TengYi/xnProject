import http from './request';

// ── Types ──────────────────────────────────────────────

export interface OrgTreeNode {
  id: string;
  orgCode?: string | null;
  orgName: string;
  parentId?: string | null;
  orgType?: string | null;
  orgTypeLabel?: string | null;
  leaderUserId?: string | null;
  leaderName?: string | null;
  status?: string | null;
  childrenCount: number;
  children?: OrgTreeNode[];
}

export interface OrgDetail {
  id: string;
  orgCode?: string | null;
  orgName: string;
  parentId?: string | null;
  orgType?: string | null;
  orgTypeLabel?: string | null;
  orgPath?: string | null;
  leaderUserId?: string | null;
  leaderName?: string | null;
  contactPerson?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  unifiedSocialCode?: string | null;
  remark?: string | null;
  sortOrder?: number | null;
  status?: string | null;
  projectCount?: number;
  contractCount?: number;
  vehicleCount?: number;
  activeVehicleCount?: number;
  userCount?: number;
}

export interface OrgCreatePayload {
  tenantId?: string;
  orgCode?: string;
  orgName: string;
  parentId?: string;
  orgType: string;
  leaderUserId?: string;
  contactPerson?: string;
  contactPhone?: string;
  address?: string;
  unifiedSocialCode?: string;
  remark?: string;
  sortOrder?: number;
  status?: string;
}

export interface OrgCreateResponse {
  orgId: string;
  adminUserId: string;
  adminUsername: string;
  adminPassword: string;
}

export interface OrgProjectStat {
  projectId: string;
  projectName: string;
  projectCode?: string | null;
  contractCount: number;
  contractAmount: number;
  agreedVolume: number;
}

export interface OrgContractItem {
  id: string;
  contractNo?: string | null;
  name?: string | null;
  contractType?: string | null;
  contractStatus?: string | null;
  sourceType?: string | null;
  siteId?: string | null;
  siteName?: string | null;
  contractAmount?: number | null;
  receivedAmount?: number | null;
  agreedVolume?: number | null;
}

export interface OrgSiteContractGroup {
  siteId?: string | null;
  siteName: string;
  contractCount: number;
  contractAmount: number;
  agreedVolume: number;
  receivedAmount: number;
  contracts: OrgContractItem[];
}

export interface OrgUserRecord {
  id: string;
  username?: string;
  name?: string;
  mainOrgName?: string;
  roleNames?: string[];
  status?: string;
  lastLoginTime?: string;
}

export interface OrgSummary {
  [orgType: string]: number;
}

// ── API ────────────────────────────────────────────────

export async function fetchOrgTree(orgType?: string) {
  const res = await http.get<OrgTreeNode[]>('/orgs/tree', {
    params: orgType ? { orgType } : undefined,
  });
  return res.data || [];
}

export async function fetchOrgDetail(id: string) {
  const res = await http.get<OrgDetail>('/orgs/' + id);
  return res.data;
}

export async function createOrg(payload: OrgCreatePayload) {
  const res = await http.post<OrgCreateResponse>('/orgs', payload);
  return res.data;
}

export async function updateOrg(id: string, payload: OrgCreatePayload) {
  const res = await http.put<OrgDetail>('/orgs/' + id, payload);
  return res.data;
}

export async function updateOrgStatus(id: string, status: string) {
  const res = await http.put<void>('/orgs/' + id + '/status', { status });
  return res.data;
}

export async function deleteOrg(id: string) {
  const res = await http.delete<void>('/orgs/' + id);
  return res.data;
}

export async function fetchOrgProjects(id: string) {
  const res = await http.get<OrgProjectStat[]>('/orgs/' + id + '/projects');
  return res.data || [];
}

export async function fetchOrgContractGroups(id: string, projectId?: string) {
  const res = await http.get<OrgSiteContractGroup[]>('/orgs/' + id + '/contract-groups', {
    params: projectId ? { projectId } : undefined,
  });
  return res.data || [];
}

export async function fetchOrgUsers(id: string) {
  const res = await http.get<OrgUserRecord[]>('/orgs/' + id + '/users');
  return res.data || [];
}

export async function fetchOrgSummary() {
  const res = await http.get<OrgSummary>('/orgs/summary');
  return res.data || {};
}
