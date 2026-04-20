import http, { request } from './request';

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

export interface ProjectReportSummary {
  periodType: string;
  reportPeriod: string;
  projectCount: number;
  activeProjectCount: number;
  totalTrips: number;
  periodVolume: number;
  periodAmount: number;
  projectTotal: number;
  accumulatedVolume: number;
  progressPercent: number;
}

export interface ProjectReportItem {
  projectId: string;
  projectCode?: string;
  projectName: string;
  orgName?: string;
  reportPeriod: string;
  periodVolume: number;
  periodAmount: number;
  periodTrips: number;
  accumulatedVolume: number;
  projectTotal: number;
  remainingVolume: number;
  progressPercent: number;
  status: string;
}

export interface ProjectViolationSummary {
  reportPeriod: string;
  totalViolations: number;
  handledCount: number;
  pendingCount: number;
  vehicleCount: number;
  fleetCount: number;
  teamCount: number;
}

export interface ProjectViolationStatItem {
  name: string;
  violationCount: number;
  handledCount: number;
  pendingCount: number;
  latestTriggerTime?: string | null;
}

export interface ProjectViolationAnalysis {
  summary: ProjectViolationSummary;
  byFleet: ProjectViolationStatItem[];
  byPlate: ProjectViolationStatItem[];
  byTeam: ProjectViolationStatItem[];
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

export interface SiteReportSummary {
  periodType: string;
  reportPeriod: string;
  siteCount: number;
  activeSiteCount: number;
  totalTrips: number;
  periodVolume: number;
  periodAmount: number;
  totalCapacity: number;
  accumulatedVolume: number;
  utilizationRate: number;
}

export interface SiteReportItem {
  siteId: string;
  siteName: string;
  siteCode?: string;
  siteType?: string;
  reportPeriod: string;
  periodVolume: number;
  periodAmount: number;
  periodTrips: number;
  accumulatedVolume: number;
  capacity: number;
  remainingCapacity: number;
  utilizationRate: number;
  status: string;
}

export interface ReportTrendItem {
  periodLabel: string;
  volume: number;
  amount: number;
  trips: number;
  activeCount: number;
}

export interface ExportTaskRecord {
  id: string;
  bizType?: string;
  exportType?: string;
  fileName?: string;
  fileUrl?: string;
  status: string;
  failReason?: string;
  createTime?: string;
  expireTime?: string;
}

export interface DashboardOverview {
  reportDate: string;
  totalSites: number;
  activeSites: number;
  totalProjects: number;
  activeProjects: number;
  totalOrgs: number;
  activeOrgs: number;
  totalVehicles: number;
  movingVehicles: number;
  dailyVolume: number;
  monthlyVolume: number;
  warningCount: number;
}

export interface DashboardOrgItem {
  orgId: string;
  orgName: string;
  activeProjectCount: number;
  totalVehicles: number;
  movingVehicles: number;
  volume: number;
  warningCount: number;
  rank: number;
}

export interface ProjectAlertItem {
  projectId: string;
  projectName: string;
  siteName: string;
  progressPercent: number;
  status: string;
  warningLevel: number;
}

export interface VehicleCapacitySummary {
  periodType: string;
  reportPeriod: string;
  totalVehicles: number;
  activeVehicles: number;
  averageVolume: number;
  loadedMileage: number;
  emptyMileage: number;
  energyConsumption: number;
}

export interface VehicleCapacityItem {
  vehicleId: string;
  plateNo: string;
  orgName: string;
  fleetName: string;
  energyType: string;
  statusLabel: string;
  averageVolume: number;
  loadedMileage: number;
  emptyMileage: number;
  energyConsumption: number;
  loadWeight: number;
  currentMileage: number;
}

export interface VehicleCapacityAnalysis {
  summary: VehicleCapacitySummary;
  trend: ReportTrendItem[];
  records: VehicleCapacityItem[];
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

interface ProjectReportSummaryResponse {
  periodType?: string;
  reportPeriod?: string;
  projectCount?: number;
  activeProjectCount?: number;
  totalTrips?: number;
  periodVolume?: number;
  periodAmount?: number;
  projectTotal?: number;
  accumulatedVolume?: number;
  progressPercent?: number;
}

interface ProjectReportItemResponse {
  projectId?: string;
  projectCode?: string;
  projectName?: string;
  orgName?: string;
  reportPeriod?: string;
  periodVolume?: number;
  periodAmount?: number;
  periodTrips?: number;
  accumulatedVolume?: number;
  projectTotal?: number;
  remainingVolume?: number;
  progressPercent?: number;
  status?: string;
}

interface ProjectViolationSummaryResponse {
  reportPeriod?: string;
  totalViolations?: number;
  handledCount?: number;
  pendingCount?: number;
  vehicleCount?: number;
  fleetCount?: number;
  teamCount?: number;
}

interface ProjectViolationStatItemResponse {
  name?: string;
  violationCount?: number;
  handledCount?: number;
  pendingCount?: number;
  latestTriggerTime?: string;
}

interface ProjectViolationAnalysisResponse {
  summary?: ProjectViolationSummaryResponse;
  byFleet?: ProjectViolationStatItemResponse[];
  byPlate?: ProjectViolationStatItemResponse[];
  byTeam?: ProjectViolationStatItemResponse[];
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

function toNumber(value?: number | null) {
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

function mapTrend(item: Partial<ReportTrendItem>): ReportTrendItem {
  return {
    periodLabel: item.periodLabel || '',
    volume: toNumber(item.volume),
    amount: toNumber(item.amount),
    trips: toNumber(item.trips),
    activeCount: toNumber(item.activeCount),
  };
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

export async function fetchProjectReportSummary(params: {
  periodType?: string;
  date?: string;
  projectId?: string;
  keyword?: string;
}) {
  const res = await http.get<ProjectReportSummaryResponse>('/reports/projects/summary', { params });
  return {
    periodType: res.data.periodType || 'MONTH',
    reportPeriod: res.data.reportPeriod || '',
    projectCount: toNumber(res.data.projectCount),
    activeProjectCount: toNumber(res.data.activeProjectCount),
    totalTrips: toNumber(res.data.totalTrips),
    periodVolume: toNumber(res.data.periodVolume),
    periodAmount: toNumber(res.data.periodAmount),
    projectTotal: toNumber(res.data.projectTotal),
    accumulatedVolume: toNumber(res.data.accumulatedVolume),
    progressPercent: toNumber(res.data.progressPercent),
  };
}

export async function fetchProjectReportList(params: {
  periodType?: string;
  date?: string;
  projectId?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const res = await http.get<PageResult<ProjectReportItemResponse>>('/reports/projects/list', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map((item) => ({
      projectId: item.projectId || '',
      projectCode: item.projectCode || '',
      projectName: item.projectName || '',
      orgName: item.orgName || '',
      reportPeriod: item.reportPeriod || '',
      periodVolume: toNumber(item.periodVolume),
      periodAmount: toNumber(item.periodAmount),
      periodTrips: toNumber(item.periodTrips),
      accumulatedVolume: toNumber(item.accumulatedVolume),
      projectTotal: toNumber(item.projectTotal),
      remainingVolume: toNumber(item.remainingVolume),
      progressPercent: toNumber(item.progressPercent),
      status: item.status || '-',
    })),
  };
}

export async function fetchProjectReportTrend(params: {
  periodType?: string;
  date?: string;
  projectId?: string;
  keyword?: string;
  limit?: number;
}) {
  const res = await http.get<Partial<ReportTrendItem>[]>('/reports/projects/trend', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapTrend);
}

export async function exportProjectReport(payload: {
  periodType?: string;
  date?: string;
  projectId?: string;
  keyword?: string;
}) {
  const res = await http.post<{ taskId: string }>('/reports/projects/export', payload);
  return res.data;
}

export async function fetchProjectViolationAnalysis(params: {
  periodType?: string;
  date?: string;
  keyword?: string;
  violationType?: string;
}) {
  const res = await http.get<ProjectViolationAnalysisResponse>('/reports/projects/violations', { params });
  const mapItem = (item: ProjectViolationStatItemResponse): ProjectViolationStatItem => ({
    name: item.name || '-',
    violationCount: toNumber(item.violationCount),
    handledCount: toNumber(item.handledCount),
    pendingCount: toNumber(item.pendingCount),
    latestTriggerTime: item.latestTriggerTime || null,
  });
  return {
    summary: {
      reportPeriod: res.data.summary?.reportPeriod || '',
      totalViolations: toNumber(res.data.summary?.totalViolations),
      handledCount: toNumber(res.data.summary?.handledCount),
      pendingCount: toNumber(res.data.summary?.pendingCount),
      vehicleCount: toNumber(res.data.summary?.vehicleCount),
      fleetCount: toNumber(res.data.summary?.fleetCount),
      teamCount: toNumber(res.data.summary?.teamCount),
    },
    byFleet: (res.data.byFleet || []).map(mapItem),
    byPlate: (res.data.byPlate || []).map(mapItem),
    byTeam: (res.data.byTeam || []).map(mapItem),
  };
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

export async function fetchSiteReportSummary(params: {
  periodType?: string;
  date?: string;
  startDate?: string;
  endDate?: string;
  siteId?: string;
  keyword?: string;
}) {
  const res = await http.get<Partial<SiteReportSummary>>('/reports/sites/summary', { params });
  return {
    periodType: res.data.periodType || 'MONTH',
    reportPeriod: res.data.reportPeriod || '',
    siteCount: toNumber(res.data.siteCount),
    activeSiteCount: toNumber(res.data.activeSiteCount),
    totalTrips: toNumber(res.data.totalTrips),
    periodVolume: toNumber(res.data.periodVolume),
    periodAmount: toNumber(res.data.periodAmount),
    totalCapacity: toNumber(res.data.totalCapacity),
    accumulatedVolume: toNumber(res.data.accumulatedVolume),
    utilizationRate: toNumber(res.data.utilizationRate),
  };
}

export async function fetchSiteReportList(params: {
  periodType?: string;
  date?: string;
  startDate?: string;
  endDate?: string;
  siteId?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const res = await http.get<PageResult<Partial<SiteReportItem>>>('/reports/sites/list', { params });
  return {
    ...res.data,
    records: (res.data.records || []).map((item) => ({
      siteId: item.siteId || '',
      siteName: item.siteName || '',
      siteCode: item.siteCode || '',
      siteType: item.siteType || '',
      reportPeriod: item.reportPeriod || '',
      periodVolume: toNumber(item.periodVolume),
      periodAmount: toNumber(item.periodAmount),
      periodTrips: toNumber(item.periodTrips),
      accumulatedVolume: toNumber(item.accumulatedVolume),
      capacity: toNumber(item.capacity),
      remainingCapacity: toNumber(item.remainingCapacity),
      utilizationRate: toNumber(item.utilizationRate),
      status: item.status || '-',
    })),
  };
}

export async function fetchSiteReportTrend(params: {
  periodType?: string;
  date?: string;
  startDate?: string;
  endDate?: string;
  siteId?: string;
  keyword?: string;
  limit?: number;
}) {
  const res = await http.get<Partial<ReportTrendItem>[]>('/reports/sites/trend', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapTrend);
}

export async function exportSiteReport(payload: {
  periodType?: string;
  date?: string;
  startDate?: string;
  endDate?: string;
  siteId?: string;
  keyword?: string;
}) {
  const res = await http.post<{ taskId: string }>('/reports/sites/export', payload);
  return res.data;
}

export async function fetchExportTask(taskId: string | number) {
  const res = await http.get<ExportTaskRecord>(`/export-tasks/${taskId}`);
  return res.data;
}

export async function downloadExportTask(taskId: string | number) {
  const res = await request.get(`/export-tasks/${taskId}/download`, {
    responseType: 'blob',
  });
  return res as unknown as Blob;
}

export async function fetchDashboardOverview(params?: { date?: string }) {
  const res = await http.get<Partial<DashboardOverview>>('/reports/dashboard/overview', { params });
  return {
    reportDate: res.data.reportDate || '',
    totalSites: toNumber(res.data.totalSites),
    activeSites: toNumber(res.data.activeSites),
    totalProjects: toNumber(res.data.totalProjects),
    activeProjects: toNumber(res.data.activeProjects),
    totalOrgs: toNumber(res.data.totalOrgs),
    activeOrgs: toNumber(res.data.activeOrgs),
    totalVehicles: toNumber(res.data.totalVehicles),
    movingVehicles: toNumber(res.data.movingVehicles),
    dailyVolume: toNumber(res.data.dailyVolume),
    monthlyVolume: toNumber(res.data.monthlyVolume),
    warningCount: toNumber(res.data.warningCount),
  };
}

export async function fetchDashboardTrend(params?: { date?: string; days?: number }) {
  const res = await http.get<Partial<ReportTrendItem>[]>('/reports/dashboard/trend', { params });
  return (Array.isArray(res.data) ? res.data : []).map(mapTrend);
}

export async function fetchProjectAlerts(params?: { date?: string; limit?: number }) {
  const res = await http.get<Partial<ProjectAlertItem>[]>('/reports/dashboard/project-alerts', { params });
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    projectId: item.projectId || '',
    projectName: item.projectName || '',
    siteName: item.siteName || '-',
    progressPercent: toNumber(item.progressPercent),
    status: item.status || '-',
    warningLevel: toNumber(item.warningLevel),
  }));
}

export async function fetchDashboardOrgAnalysis(params?: { date?: string; limit?: number }) {
  const res = await http.get<Partial<DashboardOrgItem>[]>('/reports/dashboard/org-analysis', { params });
  return (Array.isArray(res.data) ? res.data : []).map((item) => ({
    orgId: item.orgId || '',
    orgName: item.orgName || '未归属单位',
    activeProjectCount: toNumber(item.activeProjectCount),
    totalVehicles: toNumber(item.totalVehicles),
    movingVehicles: toNumber(item.movingVehicles),
    volume: toNumber(item.volume),
    warningCount: toNumber(item.warningCount),
    rank: toNumber(item.rank),
  }));
}

export async function fetchVehicleCapacityAnalysis(params?: {
  periodType?: string;
  date?: string;
  keyword?: string;
}) {
  const res = await http.get<Partial<VehicleCapacityAnalysis>>('/reports/vehicles/capacity-analysis', { params });
  const summary = (res.data.summary || {}) as Partial<VehicleCapacitySummary>;
  return {
    summary: {
      periodType: summary.periodType || 'MONTH',
      reportPeriod: summary.reportPeriod || '',
      totalVehicles: toNumber(summary.totalVehicles),
      activeVehicles: toNumber(summary.activeVehicles),
      averageVolume: toNumber(summary.averageVolume),
      loadedMileage: toNumber(summary.loadedMileage),
      emptyMileage: toNumber(summary.emptyMileage),
      energyConsumption: toNumber(summary.energyConsumption),
    },
    trend: (Array.isArray(res.data.trend) ? res.data.trend : []).map(mapTrend),
    records: (Array.isArray(res.data.records) ? res.data.records : []).map((item) => ({
      vehicleId: item.vehicleId || '',
      plateNo: item.plateNo || '',
      orgName: item.orgName || '未归属单位',
      fleetName: item.fleetName || '未编组车队',
      energyType: item.energyType || '未配置',
      statusLabel: item.statusLabel || '-',
      averageVolume: toNumber(item.averageVolume),
      loadedMileage: toNumber(item.loadedMileage),
      emptyMileage: toNumber(item.emptyMileage),
      energyConsumption: toNumber(item.energyConsumption),
      loadWeight: toNumber(item.loadWeight),
      currentMileage: toNumber(item.currentMileage),
    })),
  };
}

export const fetchProjectRankings = fetchProjectRanking;
export const fetchSiteRankings = fetchSiteRanking;
