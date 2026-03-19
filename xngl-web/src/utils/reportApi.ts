import http from './request';

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface ProjectDailyReportItem {
  projectId: string;
  projectCode?: string;
  projectName: string;
  reportDate: string;
  orgName?: string;
  vehicles: number;
  trips: number;
  todayVolume: number;
  totalVolume: number;
  projectTotal: number;
  progressPercent: number;
  statusLabel?: string;
}

export interface ProjectRankingItem {
  projectId: string;
  projectCode?: string;
  projectName: string;
  orgName?: string;
  total: number;
  used: number;
  today: number;
  rank: number;
  status: string;
  progressPercent: number;
}

export interface SiteRankingItem {
  siteId: string;
  siteName: string;
  siteType: string;
  capacity: number;
  used: number;
  today: number;
  rank: number;
  status: string;
}

export type ProjectRankingRecord = ProjectRankingItem;
export type SiteRankingRecord = SiteRankingItem;

interface ProjectDailyReportResponse {
  projectId: string;
  projectCode?: string;
  projectName: string;
  reportDate?: string;
  date?: string;
  orgName?: string;
  vehicles?: number;
  trips?: number;
  todayVolume?: number;
  totalVolume?: number;
  projectTotal?: number;
  progressPercent?: number;
  statusLabel?: string;
  status?: string;
}

interface ProjectRankingResponse {
  projectId: string;
  projectCode?: string;
  projectName: string;
  orgName?: string;
  builder?: string;
  total?: number;
  used?: number;
  today?: number;
  rank: number;
  status: string;
  progressPercent?: number;
}

interface SiteRankingResponse {
  siteId: string;
  siteName?: string;
  name?: string;
  siteType?: string;
  type?: string;
  capacity?: number;
  used?: number;
  today?: number;
  todayVolume?: number;
  rank: number;
  status: string;
}

function toNumber(value?: number) {
  return Number(value || 0);
}

function toProgressPercent(used?: number, total?: number) {
  const safeUsed = toNumber(used);
  const safeTotal = toNumber(total);
  if (safeTotal <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((safeUsed / safeTotal) * 100));
}

export async function fetchProjectDailyReport(params: {
  date?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const res = await http.get<PageResult<ProjectDailyReportResponse>>('/reports/projects/daily', {
    params,
  });
  return {
    ...res.data,
    records: (res.data.records || []).map((item) => ({
      projectId: item.projectId,
      projectCode: item.projectCode,
      projectName: item.projectName,
      reportDate: item.reportDate || item.date || '',
      orgName: item.orgName,
      vehicles: toNumber(item.vehicles),
      trips: toNumber(item.trips),
      todayVolume: toNumber(item.todayVolume),
      totalVolume: toNumber(item.totalVolume),
      projectTotal: toNumber(item.projectTotal),
      progressPercent: item.progressPercent ?? toProgressPercent(item.totalVolume, item.projectTotal),
      statusLabel: item.statusLabel || item.status,
    })),
  };
}

export async function exportProjectDailyReport(payload: {
  date?: string;
  keyword?: string;
}) {
  const res = await http.post<{ taskId: string }>('/reports/projects/daily/export', payload);
  return res.data;
}

export async function fetchProjectRanking(params?: { date?: string; limit?: number }) {
  const res = await http.get<ProjectRankingResponse[]>('/reports/projects/ranking', { params });
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    projectId: item.projectId,
    projectCode: item.projectCode,
    projectName: item.projectName,
    orgName: item.orgName || item.builder,
    total: toNumber(item.total),
    used: toNumber(item.used),
    today: toNumber(item.today),
    rank: item.rank,
    status: item.status,
    progressPercent: item.progressPercent ?? toProgressPercent(item.used, item.total),
  }));
}

export async function fetchSiteRanking(params?: { date?: string; limit?: number }) {
  const res = await http.get<SiteRankingResponse[]>('/reports/sites/ranking', { params });
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    siteId: item.siteId,
    siteName: item.siteName || item.name || '',
    siteType: item.siteType || item.type || '',
    capacity: toNumber(item.capacity),
    used: toNumber(item.used),
    today: toNumber(item.today ?? item.todayVolume),
    rank: item.rank,
    status: item.status,
  }));
}

export const fetchProjectRankings = fetchProjectRanking;
export const fetchSiteRankings = fetchSiteRanking;
