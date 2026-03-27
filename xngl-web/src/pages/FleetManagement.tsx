import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Slider,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CarOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  FundOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import {
  approveFleetDispatchOrder,
  createFleetDispatchOrder,
  createFleetFinanceRecord,
  createFleetProfile,
  createFleetTransportPlan,
  exportFleetFinanceRecords,
  exportFleetReport,
  fetchFleetDispatchOrders,
  fetchFleetFinanceRecords,
  fetchFleetFinanceSummary,
  fetchFleetProfiles,
  fetchFleetReport,
  fetchFleetSummary,
  fetchFleetTracking,
  fetchFleetTrackingHistory,
  fetchFleetTrackingSummary,
  fetchFleetTransportPlans,
  rejectFleetDispatchOrder,
  updateFleetDispatchOrder,
  updateFleetFinanceRecord,
  updateFleetProfile,
  updateFleetTransportPlan,
  type FleetDispatchOrderRecord,
  type FleetFinanceRecord,
  type FleetFinanceSummaryRecord,
  type FleetProfileRecord,
  type FleetReportItemRecord,
  type FleetSummaryRecord,
  type FleetTrackingHistoryRecord,
  type FleetTrackingRecord,
  type FleetTrackingSummaryRecord,
  type FleetTransportPlanRecord,
} from '../utils/fleetApi';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker, MapPoint, MapPolyline } from '../components/TiandituMap';
import { fetchVehicleCompanyCapacity } from '../utils/vehicleApi';

type ProfileFormValues = {
  orgId?: string;
  fleetName: string;
  captainName?: string;
  captainPhone?: string;
  driverCountPlan?: number;
  vehicleCountPlan?: number;
  status?: string;
  attendanceMode?: string;
  remark?: string;
};

type PlanFormValues = {
  fleetId: string;
  planNo?: string;
  planDate?: Dayjs;
  sourcePoint?: string;
  destinationPoint?: string;
  cargoType?: string;
  plannedTrips?: number;
  plannedVolume?: number;
  status?: string;
  remark?: string;
};

type DispatchFormValues = {
  fleetId: string;
  relatedPlanNo?: string;
  applyDate?: Dayjs;
  requestedVehicleCount?: number;
  requestedDriverCount?: number;
  urgencyLevel?: string;
  status?: string;
  applicantName?: string;
  remark?: string;
};

type FinanceFormValues = {
  fleetId: string;
  contractNo?: string;
  statementMonth?: Dayjs;
  revenueAmount?: number;
  costAmount?: number;
  otherAmount?: number;
  settledAmount?: number;
  status?: string;
  remark?: string;
};

type AuditFormValues = {
  comment?: string;
};

type SelectOption = {
  label: string;
  value: string;
};

type RangeValue = [Dayjs | null, Dayjs | null] | null;

type TrackingPoint = {
  position: MapPoint;
  locateTime?: string | null;
  speed?: string;
};

const { RangePicker } = DatePicker;

const defaultTrackingCenter: MapPoint = [120.1551, 30.2741];

const buildTrackingRange = (): [Dayjs, Dayjs] => [dayjs().startOf('day'), dayjs().endOf('day')];

const interpolatePosition = (path: MapPoint[], progress: number): MapPoint => {
  if (path.length <= 1) {
    return path[0] || defaultTrackingCenter;
  }
  const normalized = Math.min(Math.max(progress, 0), 100) / 100;
  const scaled = normalized * (path.length - 1);
  const index = Math.min(Math.floor(scaled), path.length - 2);
  const ratio = scaled - index;
  const start = path[index];
  const end = path[index + 1];
  return [start[0] + (end[0] - start[0]) * ratio, start[1] + (end[1] - start[1]) * ratio];
};

const defaultSummary: FleetSummaryRecord = {
  totalFleets: 0,
  activeFleets: 0,
  totalPlans: 0,
  pendingDispatchOrders: 0,
  totalRevenueAmount: 0,
  totalProfitAmount: 0,
};

const defaultTrackingSummary: FleetTrackingSummaryRecord = {
  totalVehicles: 0,
  movingVehicles: 0,
  stoppedVehicles: 0,
  offlineVehicles: 0,
  deliveringVehicles: 0,
  warningVehicles: 0,
};

const defaultFinanceSummary: FleetFinanceSummaryRecord = {
  totalRecords: 0,
  settledRecords: 0,
  totalRevenueAmount: 0,
  totalCostAmount: 0,
  totalProfitAmount: 0,
  totalOutstandingAmount: 0,
};

const profileStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const attendanceModeOptions = [
  { label: '人工排班', value: 'MANUAL' },
  { label: '自动排班', value: 'AUTO' },
  { label: '混合模式', value: 'HYBRID' },
];

const planStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '执行中', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已完成', value: 'COMPLETED' },
];

const dispatchStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '执行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
];

const financeStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已结清', value: 'SETTLED' },
];

const trackingStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '行驶中', value: 'MOVING' },
  { label: '停留中', value: 'STOPPED' },
  { label: '离线', value: 'OFFLINE' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '异常预警', value: 'WARNING' },
];

const urgencyOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
];

const profileTagColor: Record<string, string> = {
  ENABLED: 'success',
  DISABLED: 'default',
};

const planTagColor: Record<string, string> = {
  ACTIVE: 'processing',
  DRAFT: 'default',
  COMPLETED: 'success',
};

const dispatchTagColor: Record<string, string> = {
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  IN_PROGRESS: 'processing',
  COMPLETED: 'default',
};

const financeTagColor: Record<string, string> = {
  CONFIRMED: 'processing',
  DRAFT: 'default',
  SETTLED: 'success',
};

const trackingTagColor: Record<string, string> = {
  MOVING: 'success',
  STOPPED: 'warning',
  OFFLINE: 'default',
};

const formatMoney = (value: number) =>
  value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

const FleetManagement: React.FC = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summary, setSummary] = useState<FleetSummaryRecord>(defaultSummary);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportRows, setReportRows] = useState<FleetReportItemRecord[]>([]);

  const [companyOptions, setCompanyOptions] = useState<SelectOption[]>([]);
  const [fleetOptions, setFleetOptions] = useState<SelectOption[]>([]);
  const [planOptions, setPlanOptions] = useState<SelectOption[]>([]);

  const [profilesLoading, setProfilesLoading] = useState(false);
  const [profileRows, setProfileRows] = useState<FleetProfileRecord[]>([]);
  const [profileKeyword, setProfileKeyword] = useState('');
  const [profileStatus, setProfileStatus] = useState('all');
  const [profileOrgId, setProfileOrgId] = useState<string | undefined>(undefined);
  const [profilePageNo, setProfilePageNo] = useState(1);
  const [profilePageSize, setProfilePageSize] = useState(10);
  const [profileTotal, setProfileTotal] = useState(0);
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [profileSubmitLoading, setProfileSubmitLoading] = useState(false);
  const [editingProfile, setEditingProfile] = useState<FleetProfileRecord | null>(null);
  const [profileForm] = Form.useForm<ProfileFormValues>();

  const [plansLoading, setPlansLoading] = useState(false);
  const [planRows, setPlanRows] = useState<FleetTransportPlanRecord[]>([]);
  const [planKeyword, setPlanKeyword] = useState('');
  const [planStatus, setPlanStatus] = useState('all');
  const [planFleetId, setPlanFleetId] = useState<string | undefined>(undefined);
  const [planPageNo, setPlanPageNo] = useState(1);
  const [planPageSize, setPlanPageSize] = useState(10);
  const [planTotal, setPlanTotal] = useState(0);
  const [planModalOpen, setPlanModalOpen] = useState(false);
  const [planSubmitLoading, setPlanSubmitLoading] = useState(false);
  const [editingPlan, setEditingPlan] = useState<FleetTransportPlanRecord | null>(null);
  const [planForm] = Form.useForm<PlanFormValues>();

  const [dispatchLoading, setDispatchLoading] = useState(false);
  const [dispatchRows, setDispatchRows] = useState<FleetDispatchOrderRecord[]>([]);
  const [dispatchKeyword, setDispatchKeyword] = useState('');
  const [dispatchStatus, setDispatchStatus] = useState('all');
  const [dispatchFleetId, setDispatchFleetId] = useState<string | undefined>(undefined);
  const [dispatchPageNo, setDispatchPageNo] = useState(1);
  const [dispatchPageSize, setDispatchPageSize] = useState(10);
  const [dispatchTotal, setDispatchTotal] = useState(0);
  const [dispatchModalOpen, setDispatchModalOpen] = useState(false);
  const [dispatchSubmitLoading, setDispatchSubmitLoading] = useState(false);
  const [editingDispatch, setEditingDispatch] = useState<FleetDispatchOrderRecord | null>(null);
  const [dispatchForm] = Form.useForm<DispatchFormValues>();
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [auditMode, setAuditMode] = useState<'approve' | 'reject'>('approve');
  const [auditTarget, setAuditTarget] = useState<FleetDispatchOrderRecord | null>(null);
  const [auditSubmitLoading, setAuditSubmitLoading] = useState(false);
  const [auditForm] = Form.useForm<AuditFormValues>();

  const [financeLoading, setFinanceLoading] = useState(false);
  const [financeSummaryLoading, setFinanceSummaryLoading] = useState(false);
  const [financeRows, setFinanceRows] = useState<FleetFinanceRecord[]>([]);
  const [financeSummary, setFinanceSummary] = useState<FleetFinanceSummaryRecord>(defaultFinanceSummary);
  const [financeKeyword, setFinanceKeyword] = useState('');
  const [financeStatus, setFinanceStatus] = useState('all');
  const [financeFleetId, setFinanceFleetId] = useState<string | undefined>(undefined);
  const [financeContractNo, setFinanceContractNo] = useState('');
  const [financeMonthRange, setFinanceMonthRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [financeUnsettledOnly, setFinanceUnsettledOnly] = useState(false);
  const [financePageNo, setFinancePageNo] = useState(1);
  const [financePageSize, setFinancePageSize] = useState(10);
  const [financeTotal, setFinanceTotal] = useState(0);
  const [financeModalOpen, setFinanceModalOpen] = useState(false);
  const [financeSubmitLoading, setFinanceSubmitLoading] = useState(false);
  const [editingFinance, setEditingFinance] = useState<FleetFinanceRecord | null>(null);
  const [financeForm] = Form.useForm<FinanceFormValues>();

  const [reportKeyword, setReportKeyword] = useState('');
  const [reportOrgId, setReportOrgId] = useState<string | undefined>(undefined);
  const [reportMonthRange, setReportMonthRange] = useState<[Dayjs | null, Dayjs | null] | null>(
    null
  );

  const [trackingSummaryLoading, setTrackingSummaryLoading] = useState(false);
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [trackingHistoryLoading, setTrackingHistoryLoading] = useState(false);
  const [trackingSummary, setTrackingSummary] = useState<FleetTrackingSummaryRecord>(
    defaultTrackingSummary
  );
  const [trackingRows, setTrackingRows] = useState<FleetTrackingRecord[]>([]);
  const [trackingKeyword, setTrackingKeyword] = useState('');
  const [trackingStatus, setTrackingStatus] = useState('all');
  const [trackingFleetId, setTrackingFleetId] = useState<string | undefined>(undefined);
  const [trackingPageNo, setTrackingPageNo] = useState(1);
  const [trackingPageSize, setTrackingPageSize] = useState(10);
  const [trackingTotal, setTrackingTotal] = useState(0);
  const [selectedTrackingVehicleId, setSelectedTrackingVehicleId] = useState('');
  const [trackingRange, setTrackingRange] = useState<RangeValue>(buildTrackingRange());
  const [trackingHistory, setTrackingHistory] = useState<FleetTrackingHistoryRecord | null>(null);
  const [trackingProgress, setTrackingProgress] = useState(0);
  const [trackingPlaying, setTrackingPlaying] = useState(false);

  const profileQuery = useMemo(
    () => ({
      keyword: profileKeyword.trim() || undefined,
      status: profileStatus === 'all' ? undefined : profileStatus,
      orgId: profileOrgId,
      pageNo: profilePageNo,
      pageSize: profilePageSize,
    }),
    [profileKeyword, profileStatus, profileOrgId, profilePageNo, profilePageSize]
  );

  const planQuery = useMemo(
    () => ({
      keyword: planKeyword.trim() || undefined,
      status: planStatus === 'all' ? undefined : planStatus,
      fleetId: planFleetId,
      pageNo: planPageNo,
      pageSize: planPageSize,
    }),
    [planKeyword, planStatus, planFleetId, planPageNo, planPageSize]
  );

  const dispatchQuery = useMemo(
    () => ({
      keyword: dispatchKeyword.trim() || undefined,
      status: dispatchStatus === 'all' ? undefined : dispatchStatus,
      fleetId: dispatchFleetId,
      pageNo: dispatchPageNo,
      pageSize: dispatchPageSize,
    }),
    [dispatchKeyword, dispatchStatus, dispatchFleetId, dispatchPageNo, dispatchPageSize]
  );

  const financeQuery = useMemo(
    () => ({
      keyword: financeKeyword.trim() || undefined,
      status: financeStatus === 'all' ? undefined : financeStatus,
      fleetId: financeFleetId,
      contractNo: financeContractNo.trim() || undefined,
      statementMonthFrom: financeMonthRange?.[0]?.format('YYYY-MM'),
      statementMonthTo: financeMonthRange?.[1]?.format('YYYY-MM'),
      unsettledOnly: financeUnsettledOnly || undefined,
      pageNo: financePageNo,
      pageSize: financePageSize,
    }),
    [
      financeContractNo,
      financeFleetId,
      financeKeyword,
      financeMonthRange,
      financePageNo,
      financePageSize,
      financeStatus,
      financeUnsettledOnly,
    ]
  );

  const financeSummaryQuery = useMemo(
    () => ({
      keyword: financeKeyword.trim() || undefined,
      status: financeStatus === 'all' ? undefined : financeStatus,
      fleetId: financeFleetId,
      contractNo: financeContractNo.trim() || undefined,
      statementMonthFrom: financeMonthRange?.[0]?.format('YYYY-MM'),
      statementMonthTo: financeMonthRange?.[1]?.format('YYYY-MM'),
      unsettledOnly: financeUnsettledOnly || undefined,
    }),
    [
      financeContractNo,
      financeFleetId,
      financeKeyword,
      financeMonthRange,
      financeStatus,
      financeUnsettledOnly,
    ]
  );

  const reportQuery = useMemo(
    () => ({
      keyword: reportKeyword.trim() || undefined,
      orgId: reportOrgId,
      statementMonthFrom: reportMonthRange?.[0]?.format('YYYY-MM'),
      statementMonthTo: reportMonthRange?.[1]?.format('YYYY-MM'),
    }),
    [reportKeyword, reportMonthRange, reportOrgId]
  );

  const trackingQuery = useMemo(
    () => ({
      keyword: trackingKeyword.trim() || undefined,
      status: trackingStatus === 'all' ? undefined : trackingStatus,
      fleetId: trackingFleetId,
      pageNo: trackingPageNo,
      pageSize: trackingPageSize,
    }),
    [trackingKeyword, trackingStatus, trackingFleetId, trackingPageNo, trackingPageSize]
  );

  const overviewTopRows = useMemo(() => reportRows.slice(0, 5), [reportRows]);
  const pendingDispatchRows = useMemo(
    () => dispatchRows.filter((item) => item.status === 'PENDING_APPROVAL').slice(0, 5),
    [dispatchRows]
  );
  const selectedTrackingRecord = useMemo(
    () =>
      trackingRows.find((item) => item.vehicleId === selectedTrackingVehicleId) ||
      trackingRows[0] ||
      null,
    [trackingRows, selectedTrackingVehicleId]
  );
  const trackingPoints = useMemo<TrackingPoint[]>(
    () =>
      (trackingHistory?.points || []).map((point) => ({
        position: [point.lng, point.lat],
        locateTime: point.locateTime || null,
        speed: point.speed != null ? `${point.speed} km/h` : undefined,
      })),
    [trackingHistory]
  );
  const trackingPath = useMemo<MapPoint[]>(
    () => trackingPoints.map((item) => item.position),
    [trackingPoints]
  );
  const activeTrackingPoint = useMemo(() => {
    if (!trackingPoints.length) {
      return null;
    }
    const index = Math.min(
      Math.round((Math.min(Math.max(trackingProgress, 0), 100) / 100) * Math.max(trackingPoints.length - 1, 0)),
      Math.max(trackingPoints.length - 1, 0)
    );
    return trackingPoints[index] || null;
  }, [trackingPoints, trackingProgress]);
  const trackingCenter = useMemo<MapPoint>(() => {
    if (trackingPath.length > 1) {
      return interpolatePosition(trackingPath, trackingProgress);
    }
    if (activeTrackingPoint) {
      return activeTrackingPoint.position;
    }
    if (selectedTrackingRecord?.lng != null && selectedTrackingRecord?.lat != null) {
      return [selectedTrackingRecord.lng, selectedTrackingRecord.lat];
    }
    return defaultTrackingCenter;
  }, [activeTrackingPoint, selectedTrackingRecord, trackingPath, trackingProgress]);
  const trackingMarkers = useMemo<MapMarker[]>(() => {
    const rows = trackingRows
      .filter((item) => item.lng != null && item.lat != null)
      .map((item) => ({
        id: item.vehicleId,
        position:
          selectedTrackingRecord && item.vehicleId === selectedTrackingRecord.vehicleId
            ? trackingCenter
            : ([item.lng!, item.lat!] as MapPoint),
        title: `${item.plateNo} / ${item.trackingStatusLabel || '未知状态'}`,
      }));
    if (activeTrackingPoint) {
      rows.push({
        id: 'active-tracking-point',
        position: activeTrackingPoint.position,
        title: activeTrackingPoint.locateTime || '轨迹播放点',
      });
    }
    return rows;
  }, [activeTrackingPoint, selectedTrackingRecord, trackingCenter, trackingRows]);
  const trackingPolylines = useMemo<MapPolyline[]>(
    () =>
      trackingPath.length > 1
        ? [{ id: 'delivery-tracking-path', path: trackingPath, color: '#1677ff', weight: 5 }]
        : [],
    [trackingPath]
  );

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchFleetSummary());
    } catch (error) {
      console.error(error);
      message.error('获取车队概览失败');
      setSummary(defaultSummary);
    } finally {
      setSummaryLoading(false);
    }
  };

  const loadSupportOptions = async () => {
    try {
      const [companies, profilesPage, plansPage] = await Promise.all([
        fetchVehicleCompanyCapacity(),
        fetchFleetProfiles({ pageNo: 1, pageSize: 200 }),
        fetchFleetTransportPlans({ pageNo: 1, pageSize: 200 }),
      ]);
      setCompanyOptions(
        companies
          .filter((item) => item.orgId)
          .map((item) => ({ label: item.orgName, value: item.orgId as string }))
      );
      setFleetOptions(
        (profilesPage.records || []).map((item) => ({
          label: `${item.fleetName}${item.orgName ? ` / ${item.orgName}` : ''}`,
          value: item.id,
        }))
      );
      setPlanOptions(
        (plansPage.records || []).map((item) => ({
          label: `${item.planNo} / ${item.fleetName}`,
          value: item.planNo,
        }))
      );
    } catch (error) {
      console.error(error);
    }
  };

  const loadProfiles = async () => {
    setProfilesLoading(true);
    try {
      const page = await fetchFleetProfiles(profileQuery);
      setProfileRows(page.records || []);
      setProfileTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取车队维护列表失败');
      setProfileRows([]);
      setProfileTotal(0);
    } finally {
      setProfilesLoading(false);
    }
  };

  const loadPlans = async () => {
    setPlansLoading(true);
    try {
      const page = await fetchFleetTransportPlans(planQuery);
      setPlanRows(page.records || []);
      setPlanTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取运输计划失败');
      setPlanRows([]);
      setPlanTotal(0);
    } finally {
      setPlansLoading(false);
    }
  };

  const loadDispatchOrders = async () => {
    setDispatchLoading(true);
    try {
      const page = await fetchFleetDispatchOrders(dispatchQuery);
      setDispatchRows(page.records || []);
      setDispatchTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取调度单失败');
      setDispatchRows([]);
      setDispatchTotal(0);
    } finally {
      setDispatchLoading(false);
    }
  };

  const loadFinanceRecords = async () => {
    setFinanceLoading(true);
    try {
      const page = await fetchFleetFinanceRecords(financeQuery);
      setFinanceRows(page.records || []);
      setFinanceTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取财务记录失败');
      setFinanceRows([]);
      setFinanceTotal(0);
    } finally {
      setFinanceLoading(false);
    }
  };

  const loadFinanceSummary = async () => {
    setFinanceSummaryLoading(true);
    try {
      setFinanceSummary(await fetchFleetFinanceSummary(financeSummaryQuery));
    } catch (error) {
      console.error(error);
      message.error('获取财务汇总失败');
      setFinanceSummary(defaultFinanceSummary);
    } finally {
      setFinanceSummaryLoading(false);
    }
  };

  const loadReport = async () => {
    setReportLoading(true);
    try {
      setReportRows(await fetchFleetReport(reportQuery));
    } catch (error) {
      console.error(error);
      message.error('获取车队报表失败');
      setReportRows([]);
    } finally {
      setReportLoading(false);
    }
  };

  const loadTrackingSummary = async () => {
    setTrackingSummaryLoading(true);
    try {
      setTrackingSummary(await fetchFleetTrackingSummary(trackingQuery));
    } catch (error) {
      console.error(error);
      message.error('获取送货跟踪汇总失败');
      setTrackingSummary(defaultTrackingSummary);
    } finally {
      setTrackingSummaryLoading(false);
    }
  };

  const loadTrackingRows = async () => {
    setTrackingLoading(true);
    try {
      const page = await fetchFleetTracking(trackingQuery);
      setTrackingRows(page.records || []);
      setTrackingTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取送货跟踪列表失败');
      setTrackingRows([]);
      setTrackingTotal(0);
    } finally {
      setTrackingLoading(false);
    }
  };

  const loadTrackingHistory = async (
    vehicleId = selectedTrackingVehicleId,
    range = trackingRange
  ) => {
    if (!vehicleId || !range?.[0] || !range?.[1]) {
      setTrackingHistory(null);
      setTrackingProgress(0);
      setTrackingPlaying(false);
      return;
    }
    setTrackingHistoryLoading(true);
    try {
      const result = await fetchFleetTrackingHistory(vehicleId, {
        fleetId: trackingFleetId,
        startTime: range[0].startOf('minute').format('YYYY-MM-DD HH:mm:ss'),
        endTime: range[1].endOf('minute').format('YYYY-MM-DD HH:mm:ss'),
        minStopMinutes: 10,
      });
      setTrackingHistory(result);
      setTrackingProgress(0);
      setTrackingPlaying(false);
    } catch (error) {
      console.error(error);
      message.error('获取送货轨迹失败');
      setTrackingHistory(null);
    } finally {
      setTrackingHistoryLoading(false);
    }
  };

  const reloadSharedData = async () => {
    await Promise.all([loadSummary(), loadSupportOptions()]);
  };

  useEffect(() => {
    void reloadSharedData();
  }, []);

  useEffect(() => {
    void loadProfiles();
  }, [profileQuery]);

  useEffect(() => {
    void loadPlans();
  }, [planQuery]);

  useEffect(() => {
    void loadDispatchOrders();
  }, [dispatchQuery]);

  useEffect(() => {
    void loadFinanceRecords();
  }, [financeQuery]);

  useEffect(() => {
    void loadFinanceSummary();
  }, [financeSummaryQuery]);

  useEffect(() => {
    void loadReport();
  }, [reportQuery]);

  useEffect(() => {
    void Promise.all([loadTrackingSummary(), loadTrackingRows()]);
  }, [trackingQuery]);

  useEffect(() => {
    if (!trackingRows.some((item) => item.vehicleId === selectedTrackingVehicleId)) {
      setSelectedTrackingVehicleId(trackingRows[0]?.vehicleId || '');
      setTrackingProgress(0);
      setTrackingPlaying(false);
    }
  }, [trackingRows, selectedTrackingVehicleId]);

  useEffect(() => {
    if (selectedTrackingVehicleId && trackingRange?.[0] && trackingRange?.[1]) {
      void loadTrackingHistory(selectedTrackingVehicleId, trackingRange);
    }
  }, [
    selectedTrackingVehicleId,
    trackingRange?.[0]?.valueOf(),
    trackingRange?.[1]?.valueOf(),
    trackingFleetId,
  ]);

  useEffect(() => {
    if (!trackingPlaying) {
      return;
    }
    const timer = window.setInterval(() => {
      setTrackingProgress((current) => {
        if (current >= 100) {
          window.clearInterval(timer);
          setTrackingPlaying(false);
          return 100;
        }
        return Math.min(current + 4, 100);
      });
    }, 700);
    return () => window.clearInterval(timer);
  }, [trackingPlaying]);

  const resetProfileFilters = () => {
    setProfileKeyword('');
    setProfileStatus('all');
    setProfileOrgId(undefined);
    setProfilePageNo(1);
  };

  const resetPlanFilters = () => {
    setPlanKeyword('');
    setPlanStatus('all');
    setPlanFleetId(undefined);
    setPlanPageNo(1);
  };

  const resetDispatchFilters = () => {
    setDispatchKeyword('');
    setDispatchStatus('all');
    setDispatchFleetId(undefined);
    setDispatchPageNo(1);
  };

  const resetFinanceFilters = () => {
    setFinanceKeyword('');
    setFinanceStatus('all');
    setFinanceFleetId(undefined);
    setFinanceContractNo('');
    setFinanceMonthRange(null);
    setFinanceUnsettledOnly(false);
    setFinancePageNo(1);
  };

  const resetReportFilters = () => {
    setReportKeyword('');
    setReportOrgId(undefined);
    setReportMonthRange(null);
  };

  const resetTrackingFilters = () => {
    setTrackingKeyword('');
    setTrackingStatus('all');
    setTrackingFleetId(undefined);
    setTrackingPageNo(1);
    setTrackingRange(buildTrackingRange());
  };

  const openCreateProfile = () => {
    setEditingProfile(null);
    profileForm.resetFields();
    profileForm.setFieldsValue({
      status: 'ENABLED',
      attendanceMode: 'MANUAL',
      driverCountPlan: 0,
      vehicleCountPlan: 0,
    });
    setProfileModalOpen(true);
  };

  const openEditProfile = (record: FleetProfileRecord) => {
    setEditingProfile(record);
    profileForm.setFieldsValue({
      orgId: record.orgId || undefined,
      fleetName: record.fleetName,
      captainName: record.captainName || undefined,
      captainPhone: record.captainPhone || undefined,
      driverCountPlan: record.driverCountPlan,
      vehicleCountPlan: record.vehicleCountPlan,
      status: record.status,
      attendanceMode: record.attendanceMode || 'MANUAL',
      remark: record.remark || undefined,
    });
    setProfileModalOpen(true);
  };

  const submitProfile = async () => {
    try {
      const values = await profileForm.validateFields();
      setProfileSubmitLoading(true);
      const payload = {
        orgId: values.orgId ? Number(values.orgId) : undefined,
        fleetName: values.fleetName,
        captainName: values.captainName,
        captainPhone: values.captainPhone,
        driverCountPlan: values.driverCountPlan,
        vehicleCountPlan: values.vehicleCountPlan,
        status: values.status,
        attendanceMode: values.attendanceMode,
        remark: values.remark,
      };
      if (editingProfile) {
        await updateFleetProfile(editingProfile.id, payload);
        message.success('车队信息已更新');
      } else {
        await createFleetProfile(payload);
        message.success('车队信息已新增');
      }
      setProfileModalOpen(false);
      profileForm.resetFields();
      await Promise.all([loadProfiles(), reloadSharedData()]);
    } catch (error) {
      console.error(error);
    } finally {
      setProfileSubmitLoading(false);
    }
  };

  const openCreatePlan = () => {
    setEditingPlan(null);
    planForm.resetFields();
    planForm.setFieldsValue({
      planDate: dayjs(),
      plannedTrips: 0,
      plannedVolume: 0,
      status: 'ACTIVE',
    });
    setPlanModalOpen(true);
  };

  const openEditPlan = (record: FleetTransportPlanRecord) => {
    setEditingPlan(record);
    planForm.setFieldsValue({
      fleetId: record.fleetId || undefined,
      planNo: record.planNo,
      planDate: record.planDate ? dayjs(record.planDate) : dayjs(),
      sourcePoint: record.sourcePoint || undefined,
      destinationPoint: record.destinationPoint || undefined,
      cargoType: record.cargoType || undefined,
      plannedTrips: record.plannedTrips,
      plannedVolume: record.plannedVolume,
      status: record.status,
      remark: record.remark || undefined,
    });
    setPlanModalOpen(true);
  };

  const submitPlan = async () => {
    try {
      const values = await planForm.validateFields();
      setPlanSubmitLoading(true);
      const payload = {
        fleetId: Number(values.fleetId),
        planNo: values.planNo,
        planDate: values.planDate?.format('YYYY-MM-DD'),
        sourcePoint: values.sourcePoint,
        destinationPoint: values.destinationPoint,
        cargoType: values.cargoType,
        plannedTrips: values.plannedTrips,
        plannedVolume: values.plannedVolume,
        status: values.status,
        remark: values.remark,
      };
      if (editingPlan) {
        await updateFleetTransportPlan(editingPlan.id, payload);
        message.success('运输计划已更新');
      } else {
        await createFleetTransportPlan(payload);
        message.success('运输计划已新增');
      }
      setPlanModalOpen(false);
      planForm.resetFields();
      await Promise.all([loadPlans(), loadDispatchOrders(), reloadSharedData()]);
    } catch (error) {
      console.error(error);
    } finally {
      setPlanSubmitLoading(false);
    }
  };

  const openCreateDispatch = () => {
    setEditingDispatch(null);
    dispatchForm.resetFields();
    dispatchForm.setFieldsValue({
      applyDate: dayjs(),
      requestedVehicleCount: 0,
      requestedDriverCount: 0,
      urgencyLevel: 'MEDIUM',
      status: 'PENDING_APPROVAL',
    });
    setDispatchModalOpen(true);
  };

  const openEditDispatch = (record: FleetDispatchOrderRecord) => {
    setEditingDispatch(record);
    dispatchForm.setFieldsValue({
      fleetId: record.fleetId || undefined,
      relatedPlanNo: record.relatedPlanNo || undefined,
      applyDate: record.applyDate ? dayjs(record.applyDate) : dayjs(),
      requestedVehicleCount: record.requestedVehicleCount,
      requestedDriverCount: record.requestedDriverCount,
      urgencyLevel: record.urgencyLevel,
      status: record.status,
      applicantName: record.applicantName || undefined,
      remark: record.remark || undefined,
    });
    setDispatchModalOpen(true);
  };

  const submitDispatch = async () => {
    try {
      const values = await dispatchForm.validateFields();
      setDispatchSubmitLoading(true);
      const payload = {
        fleetId: Number(values.fleetId),
        relatedPlanNo: values.relatedPlanNo,
        applyDate: values.applyDate?.format('YYYY-MM-DD'),
        requestedVehicleCount: values.requestedVehicleCount,
        requestedDriverCount: values.requestedDriverCount,
        urgencyLevel: values.urgencyLevel,
        status: values.status,
        applicantName: values.applicantName,
        remark: values.remark,
      };
      if (editingDispatch) {
        await updateFleetDispatchOrder(editingDispatch.id, payload);
        message.success('调度单已更新');
      } else {
        await createFleetDispatchOrder(payload);
        message.success('调度单已新增');
      }
      setDispatchModalOpen(false);
      dispatchForm.resetFields();
      await Promise.all([loadDispatchOrders(), reloadSharedData()]);
    } catch (error) {
      console.error(error);
    } finally {
      setDispatchSubmitLoading(false);
    }
  };

  const openAuditModal = (mode: 'approve' | 'reject', record: FleetDispatchOrderRecord) => {
    setAuditMode(mode);
    setAuditTarget(record);
    auditForm.resetFields();
    auditForm.setFieldsValue({
      comment: mode === 'approve' ? '同意按计划执行' : '请补充调度依据',
    });
    setAuditModalOpen(true);
  };

  const submitAudit = async () => {
    if (!auditTarget) {
      return;
    }
    try {
      const values = await auditForm.validateFields();
      setAuditSubmitLoading(true);
      if (auditMode === 'approve') {
        await approveFleetDispatchOrder(auditTarget.id, { comment: values.comment });
        message.success('调度单已审批通过');
      } else {
        await rejectFleetDispatchOrder(auditTarget.id, { comment: values.comment });
        message.success('调度单已驳回');
      }
      setAuditModalOpen(false);
      await Promise.all([loadDispatchOrders(), reloadSharedData()]);
    } catch (error) {
      console.error(error);
    } finally {
      setAuditSubmitLoading(false);
    }
  };

  const openCreateFinance = () => {
    setEditingFinance(null);
    financeForm.resetFields();
    financeForm.setFieldsValue({
      statementMonth: dayjs(),
      revenueAmount: 0,
      costAmount: 0,
      otherAmount: 0,
      settledAmount: 0,
      status: 'CONFIRMED',
    });
    setFinanceModalOpen(true);
  };

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleExportFinance = async () => {
    try {
      downloadBlob(await exportFleetFinanceRecords(financeSummaryQuery), 'fleet_finance_records.csv');
      message.success('财务台账导出成功');
    } catch (error) {
      console.error(error);
      message.error('财务台账导出失败');
    }
  };

  const handleExportReport = async () => {
    try {
      downloadBlob(await exportFleetReport(reportQuery), 'fleet_report.csv');
      message.success('车队报表导出成功');
    } catch (error) {
      console.error(error);
      message.error('车队报表导出失败');
    }
  };

  const openEditFinance = (record: FleetFinanceRecord) => {
    setEditingFinance(record);
    financeForm.setFieldsValue({
      fleetId: record.fleetId || undefined,
      contractNo: record.contractNo || undefined,
      statementMonth: record.statementMonth ? dayjs(record.statementMonth, 'YYYY-MM') : dayjs(),
      revenueAmount: record.revenueAmount,
      costAmount: record.costAmount,
      otherAmount: record.otherAmount,
      settledAmount: record.settledAmount,
      status: record.status,
      remark: record.remark || undefined,
    });
    setFinanceModalOpen(true);
  };

  const submitFinance = async () => {
    try {
      const values = await financeForm.validateFields();
      setFinanceSubmitLoading(true);
      const payload = {
        fleetId: Number(values.fleetId),
        contractNo: values.contractNo,
        statementMonth: values.statementMonth?.format('YYYY-MM'),
        revenueAmount: values.revenueAmount,
        costAmount: values.costAmount,
        otherAmount: values.otherAmount,
        settledAmount: values.settledAmount,
        status: values.status,
        remark: values.remark,
      };
      if (editingFinance) {
        await updateFleetFinanceRecord(editingFinance.id, payload);
        message.success('财务记录已更新');
      } else {
        await createFleetFinanceRecord(payload);
        message.success('财务记录已新增');
      }
      setFinanceModalOpen(false);
      financeForm.resetFields();
      await Promise.all([loadFinanceRecords(), reloadSharedData()]);
    } catch (error) {
      console.error(error);
    } finally {
      setFinanceSubmitLoading(false);
    }
  };

  const profileColumns: ColumnsType<FleetProfileRecord> = [
    {
      title: '车队名称',
      dataIndex: 'fleetName',
      key: 'fleetName',
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.fleetName}</span>
          <span className="text-xs g-text-secondary">{record.orgName || '未归属单位'}</span>
        </Space>
      ),
    },
    { title: '队长', dataIndex: 'captainName', key: 'captainName', width: 120 },
    { title: '联系电话', dataIndex: 'captainPhone', key: 'captainPhone', width: 140 },
    { title: '计划司机数', dataIndex: 'driverCountPlan', key: 'driverCountPlan', width: 120 },
    { title: '计划车辆数', dataIndex: 'vehicleCountPlan', key: 'vehicleCountPlan', width: 120 },
    {
      title: '考勤模式',
      dataIndex: 'attendanceMode',
      key: 'attendanceMode',
      width: 120,
      render: (value: string) =>
        attendanceModeOptions.find((item) => item.value === value)?.label || value || '-',
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      width: 100,
      render: (_, record) => <Tag color={profileTagColor[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" onClick={() => openEditProfile(record)}>
          编辑
        </Button>
      ),
    },
  ];

  const planColumns: ColumnsType<FleetTransportPlanRecord> = [
    {
      title: '计划编号',
      dataIndex: 'planNo',
      key: 'planNo',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.planNo}</span>
          <span className="text-xs g-text-secondary">{record.fleetName}</span>
        </Space>
      ),
    },
    { title: '计划日期', dataIndex: 'planDate', key: 'planDate', width: 120 },
    { title: '起点', dataIndex: 'sourcePoint', key: 'sourcePoint', width: 180 },
    { title: '终点', dataIndex: 'destinationPoint', key: 'destinationPoint', width: 180 },
    { title: '货类', dataIndex: 'cargoType', key: 'cargoType', width: 120 },
    { title: '计划趟次', dataIndex: 'plannedTrips', key: 'plannedTrips', width: 110 },
    {
      title: '计划方量',
      dataIndex: 'plannedVolume',
      key: 'plannedVolume',
      width: 120,
      render: (value: number) => `${value.toFixed(2)} m3`,
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      width: 100,
      render: (_, record) => <Tag color={planTagColor[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" onClick={() => openEditPlan(record)}>
          编辑
        </Button>
      ),
    },
  ];

  const dispatchColumns: ColumnsType<FleetDispatchOrderRecord> = [
    {
      title: '调度单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.orderNo}</span>
          <span className="text-xs g-text-secondary">{record.fleetName}</span>
        </Space>
      ),
    },
    { title: '关联计划', dataIndex: 'relatedPlanNo', key: 'relatedPlanNo', width: 160 },
    { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 120 },
    { title: '申请车辆数', dataIndex: 'requestedVehicleCount', key: 'requestedVehicleCount', width: 120 },
    { title: '申请司机数', dataIndex: 'requestedDriverCount', key: 'requestedDriverCount', width: 120 },
    { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 120 },
    {
      title: '紧急程度',
      dataIndex: 'urgencyLabel',
      key: 'urgencyLabel',
      width: 100,
      render: (_, record) => {
        const color = record.urgencyLevel === 'HIGH' ? 'error' : record.urgencyLevel === 'LOW' ? 'default' : 'warning';
        return <Tag color={color}>{record.urgencyLabel}</Tag>;
      },
    },
    {
      title: '审批状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      width: 110,
      render: (_, record) => <Tag color={dispatchTagColor[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '审批信息',
      key: 'audit',
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span>{record.approvedBy || '-'}</span>
          <span className="text-xs g-text-secondary">{record.approvedTime || record.auditRemark || '-'}</span>
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" onClick={() => openEditDispatch(record)} disabled={record.status === 'COMPLETED'}>
            编辑
          </Button>
          <Button
            type="link"
            onClick={() => openAuditModal('approve', record)}
            disabled={record.status !== 'PENDING_APPROVAL'}
          >
            通过
          </Button>
          <Button
            type="link"
            danger
            onClick={() => openAuditModal('reject', record)}
            disabled={record.status !== 'PENDING_APPROVAL'}
          >
            驳回
          </Button>
        </Space>
      ),
    },
  ];

  const financeColumns: ColumnsType<FleetFinanceRecord> = [
    {
      title: '结算单号',
      dataIndex: 'recordNo',
      key: 'recordNo',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.recordNo}</span>
          <span className="text-xs g-text-secondary">{record.fleetName}</span>
        </Space>
      ),
    },
    { title: '运输合同号', dataIndex: 'contractNo', key: 'contractNo', width: 160 },
    { title: '账期', dataIndex: 'statementMonth', key: 'statementMonth', width: 100 },
    {
      title: '收入',
      dataIndex: 'revenueAmount',
      key: 'revenueAmount',
      width: 130,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '成本',
      dataIndex: 'costAmount',
      key: 'costAmount',
      width: 130,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '其他费用',
      dataIndex: 'otherAmount',
      key: 'otherAmount',
      width: 130,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '已结算',
      dataIndex: 'settledAmount',
      key: 'settledAmount',
      width: 130,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '利润',
      dataIndex: 'profitAmount',
      key: 'profitAmount',
      width: 130,
      render: (value: number) => (
        <span className={value >= 0 ? 'g-text-success' : 'g-text-error'}>¥{formatMoney(value)}</span>
      ),
    },
    {
      title: '未结金额',
      dataIndex: 'outstandingAmount',
      key: 'outstandingAmount',
      width: 130,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      width: 100,
      render: (_, record) => <Tag color={financeTagColor[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" onClick={() => openEditFinance(record)}>
          编辑
        </Button>
      ),
    },
  ];

  const reportColumns: ColumnsType<FleetReportItemRecord> = [
    {
      title: '车队',
      dataIndex: 'fleetName',
      key: 'fleetName',
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.fleetName}</span>
          <span className="text-xs g-text-secondary">{record.orgName || '未归属单位'}</span>
        </Space>
      ),
    },
    { title: '运输计划数', dataIndex: 'totalPlans', key: 'totalPlans', width: 110 },
    { title: '调度申请数', dataIndex: 'totalDispatchOrders', key: 'totalDispatchOrders', width: 120 },
    { title: '已批准调度', dataIndex: 'approvedDispatchOrders', key: 'approvedDispatchOrders', width: 120 },
    {
      title: '计划方量',
      dataIndex: 'plannedVolume',
      key: 'plannedVolume',
      width: 120,
      render: (value: number) => `${value.toFixed(2)} m3`,
    },
    {
      title: '收入',
      dataIndex: 'revenueAmount',
      key: 'revenueAmount',
      width: 140,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '成本',
      dataIndex: 'costAmount',
      key: 'costAmount',
      width: 140,
      render: (value: number) => `¥${formatMoney(value)}`,
    },
    {
      title: '利润',
      dataIndex: 'profitAmount',
      key: 'profitAmount',
      width: 140,
      render: (value: number) => (
        <span className={value >= 0 ? 'g-text-success' : 'g-text-error'}>¥{formatMoney(value)}</span>
      ),
    },
  ];

  const trackingColumns: ColumnsType<FleetTrackingRecord> = [
    {
      title: '车辆',
      dataIndex: 'plateNo',
      key: 'plateNo',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.plateNo}</span>
          <span className="text-xs g-text-secondary">{record.fleetName || '未编组车队'}</span>
        </Space>
      ),
    },
    {
      title: '单位',
      dataIndex: 'orgName',
      key: 'orgName',
      width: 180,
      render: (value?: string | null) => value || '-',
    },
    {
      title: '跟踪状态',
      dataIndex: 'trackingStatusLabel',
      key: 'trackingStatusLabel',
      width: 110,
      render: (_, record) => (
        <Tag color={trackingTagColor[record.trackingStatus || ''] || 'default'}>
          {record.trackingStatusLabel || '未知'}
        </Tag>
      ),
    },
    {
      title: '调度状态',
      dataIndex: 'dispatchStatusLabel',
      key: 'dispatchStatusLabel',
      width: 120,
      render: (_, record) => (
        <Tag color={dispatchTagColor[record.dispatchStatus || ''] || 'default'}>
          {record.dispatchStatusLabel || '未关联'}
        </Tag>
      ),
    },
    { title: '目的地', dataIndex: 'destinationPoint', key: 'destinationPoint', width: 180 },
    {
      title: '当前速度',
      dataIndex: 'currentSpeed',
      key: 'currentSpeed',
      width: 110,
      render: (value: number) => `${value.toFixed(1)} km/h`,
    },
    { title: '定位时间', dataIndex: 'gpsTime', key: 'gpsTime', width: 170 },
    {
      title: '预警',
      dataIndex: 'warningLabel',
      key: 'warningLabel',
      width: 120,
      render: (value?: string | null) =>
        value ? <Tag color="error">{value}</Tag> : <Tag>正常</Tag>,
    },
  ];

  const trackingStopColumns: ColumnsType<NonNullable<FleetTrackingHistoryRecord['stops']>[number]> = [
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
    {
      title: '停留时长',
      dataIndex: 'durationMinutes',
      key: 'durationMinutes',
      width: 110,
      render: (value: number) => `${value} 分钟`,
    },
    { title: '位置说明', dataIndex: 'remark', key: 'remark', render: (value?: string | null) => value || '-' },
  ];

  const overviewContent = (
    <Space direction="vertical" size={24} className="w-full">
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="车队总数" value={summary.totalFleets} prefix={<TeamOutlined />} loading={summaryLoading} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="启用车队" value={summary.activeFleets} prefix={<CheckCircleOutlined />} loading={summaryLoading} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="运输计划" value={summary.totalPlans} prefix={<FileTextOutlined />} loading={summaryLoading} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="待审批调度" value={summary.pendingDispatchOrders} prefix={<CarOutlined />} loading={summaryLoading} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="累计收入" value={summary.totalRevenueAmount} precision={2} prefix="¥" loading={summaryLoading} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="累计利润" value={summary.totalProfitAmount} precision={2} prefix={<DollarOutlined />} loading={summaryLoading} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={11}>
          <Card
            className="glass-panel g-border-panel border"
            title="待审批调度"
            extra={
              <Button type="link" onClick={() => setActiveTab('dispatch')}>
                查看全部
              </Button>
            }
          >
            <Table<FleetDispatchOrderRecord>
              rowKey="id"
              size="small"
              loading={dispatchLoading}
              dataSource={pendingDispatchRows}
              pagination={false}
              locale={{ emptyText: '暂无待审批调度' }}
              columns={[
                { title: '调度单号', dataIndex: 'orderNo', key: 'orderNo' },
                { title: '车队', dataIndex: 'fleetName', key: 'fleetName' },
                { title: '申请人', dataIndex: 'applicantName', key: 'applicantName' },
                {
                  title: '紧急程度',
                  dataIndex: 'urgencyLabel',
                  key: 'urgencyLabel',
                  render: (_, record) => <Tag color={record.urgencyLevel === 'HIGH' ? 'error' : 'warning'}>{record.urgencyLabel}</Tag>,
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} xl={13}>
          <Card
            className="glass-panel g-border-panel border"
            title="利润贡献 Top5"
            extra={
              <Button type="link" onClick={() => setActiveTab('report')}>
                打开报表
              </Button>
            }
          >
            <Table<FleetReportItemRecord>
              rowKey={(record) => record.fleetId || record.fleetName}
              size="small"
              loading={reportLoading}
              dataSource={overviewTopRows}
              pagination={false}
              locale={{ emptyText: '暂无报表数据' }}
              columns={[
                { title: '车队', dataIndex: 'fleetName', key: 'fleetName' },
                { title: '计划数', dataIndex: 'totalPlans', key: 'totalPlans', width: 90 },
                { title: '收入', dataIndex: 'revenueAmount', key: 'revenueAmount', render: (value: number) => `¥${formatMoney(value)}` },
                {
                  title: '利润',
                  dataIndex: 'profitAmount',
                  key: 'profitAmount',
                  render: (value: number) => <span className={value >= 0 ? 'g-text-success' : 'g-text-error'}>¥{formatMoney(value)}</span>,
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );

  const profileContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={6}>
            <Input
              placeholder="搜索车队名称/队长/单位"
              prefix={<SearchOutlined />}
              value={profileKeyword}
              onChange={(e) => {
                setProfileKeyword(e.target.value);
                setProfilePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              className="w-full"
              value={profileStatus}
              options={profileStatusOptions}
              onChange={(value) => {
                setProfileStatus(value);
                setProfilePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              allowClear
              placeholder="选择归属单位"
              className="w-full"
              value={profileOrgId}
              options={companyOptions}
              onChange={(value) => {
                setProfileOrgId(value);
                setProfilePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} xl={8}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetProfileFilters}>重置</Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateProfile}>
                新增车队
              </Button>
            </div>
          </Col>
        </Row>
      </Card>
      <Card className="glass-panel g-border-panel border">
        <Table<FleetProfileRecord>
          rowKey="id"
          loading={profilesLoading}
          dataSource={profileRows}
          columns={profileColumns}
          scroll={{ x: 1100 }}
          pagination={{
            current: profilePageNo,
            pageSize: profilePageSize,
            total: profileTotal,
            showSizeChanger: true,
            onChange: (page, pageSize) => {
              setProfilePageNo(page);
              setProfilePageSize(pageSize);
            },
          }}
        />
      </Card>
    </Space>
  );

  const planContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={6}>
            <Input
              placeholder="搜索计划编号/车队/线路"
              prefix={<SearchOutlined />}
              value={planKeyword}
              onChange={(e) => {
                setPlanKeyword(e.target.value);
                setPlanPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              className="w-full"
              value={planStatus}
              options={planStatusOptions}
              onChange={(value) => {
                setPlanStatus(value);
                setPlanPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              allowClear
              placeholder="选择车队"
              className="w-full"
              value={planFleetId}
              options={fleetOptions}
              onChange={(value) => {
                setPlanFleetId(value);
                setPlanPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} xl={8}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetPlanFilters}>重置</Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreatePlan}>
                新增计划
              </Button>
            </div>
          </Col>
        </Row>
      </Card>
      <Card className="glass-panel g-border-panel border">
        <Table<FleetTransportPlanRecord>
          rowKey="id"
          loading={plansLoading}
          dataSource={planRows}
          columns={planColumns}
          scroll={{ x: 1400 }}
          pagination={{
            current: planPageNo,
            pageSize: planPageSize,
            total: planTotal,
            showSizeChanger: true,
            onChange: (page, pageSize) => {
              setPlanPageNo(page);
              setPlanPageSize(pageSize);
            },
          }}
        />
      </Card>
    </Space>
  );

  const dispatchContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={6}>
            <Input
              placeholder="搜索调度单号/申请人/计划号"
              prefix={<SearchOutlined />}
              value={dispatchKeyword}
              onChange={(e) => {
                setDispatchKeyword(e.target.value);
                setDispatchPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              className="w-full"
              value={dispatchStatus}
              options={dispatchStatusOptions}
              onChange={(value) => {
                setDispatchStatus(value);
                setDispatchPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              allowClear
              placeholder="选择车队"
              className="w-full"
              value={dispatchFleetId}
              options={fleetOptions}
              onChange={(value) => {
                setDispatchFleetId(value);
                setDispatchPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} xl={8}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetDispatchFilters}>重置</Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDispatch}>
                新增调度
              </Button>
            </div>
          </Col>
        </Row>
      </Card>
      <Card className="glass-panel g-border-panel border">
        <Table<FleetDispatchOrderRecord>
          rowKey="id"
          loading={dispatchLoading}
          dataSource={dispatchRows}
          columns={dispatchColumns}
          scroll={{ x: 1700 }}
          pagination={{
            current: dispatchPageNo,
            pageSize: dispatchPageSize,
            total: dispatchTotal,
            showSizeChanger: true,
            onChange: (page, pageSize) => {
              setDispatchPageNo(page);
              setDispatchPageSize(pageSize);
            },
          }}
        />
      </Card>
    </Space>
  );

  const financeContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="财务记录数"
              value={financeSummary.totalRecords}
              loading={financeSummaryLoading}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="总收入"
              value={financeSummary.totalRevenueAmount}
              precision={2}
              loading={financeSummaryLoading}
              prefix="¥"
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="总利润"
              value={financeSummary.totalProfitAmount}
              precision={2}
              loading={financeSummaryLoading}
              prefix={<FundOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="未结金额"
              value={financeSummary.totalOutstandingAmount}
              precision={2}
              loading={financeSummaryLoading}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
      </Row>
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={6}>
            <Input
              placeholder="搜索结算单号/合同号/账期"
              prefix={<SearchOutlined />}
              value={financeKeyword}
              onChange={(e) => {
                setFinanceKeyword(e.target.value);
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={4}>
            <Select
              className="w-full"
              value={financeStatus}
              options={financeStatusOptions}
              onChange={(value) => {
                setFinanceStatus(value);
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={4}>
            <Select
              allowClear
              placeholder="选择车队"
              className="w-full"
              value={financeFleetId}
              options={fleetOptions}
              onChange={(value) => {
                setFinanceFleetId(value);
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={4}>
            <Input
              placeholder="运输合同号"
              value={financeContractNo}
              onChange={(e) => {
                setFinanceContractNo(e.target.value);
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={4}>
            <RangePicker
              picker="month"
              className="w-full"
              value={financeMonthRange}
              onChange={(value) => {
                setFinanceMonthRange(value);
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={2}>
            <Select
              className="w-full"
              value={financeUnsettledOnly ? 'UNSETTLED' : 'all'}
              options={[
                { label: '全部', value: 'all' },
                { label: '仅未结', value: 'UNSETTLED' },
              ]}
              onChange={(value) => {
                setFinanceUnsettledOnly(value === 'UNSETTLED');
                setFinancePageNo(1);
              }}
            />
          </Col>
          <Col xs={24} xl={4}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetFinanceFilters}>重置</Button>
              <Button onClick={() => void handleExportFinance()}>导出财务</Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateFinance}>
                新增财务记录
              </Button>
            </div>
          </Col>
        </Row>
      </Card>
      <Card className="glass-panel g-border-panel border">
        <Table<FleetFinanceRecord>
          rowKey="id"
          loading={financeLoading}
          dataSource={financeRows}
          columns={financeColumns}
          scroll={{ x: 1800 }}
          pagination={{
            current: financePageNo,
            pageSize: financePageSize,
            total: financeTotal,
            showSizeChanger: true,
            onChange: (page, pageSize) => {
              setFinancePageNo(page);
              setFinancePageSize(pageSize);
            },
          }}
        />
      </Card>
    </Space>
  );

  const reportContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={7}>
            <Input
              placeholder="搜索车队/单位"
              prefix={<SearchOutlined />}
              value={reportKeyword}
              onChange={(e) => setReportKeyword(e.target.value)}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              allowClear
              placeholder="归属单位"
              className="w-full"
              value={reportOrgId}
              options={companyOptions}
              onChange={(value) => setReportOrgId(value)}
            />
          </Col>
          <Col xs={24} md={8} xl={6}>
            <RangePicker
              picker="month"
              className="w-full"
              value={reportMonthRange}
              onChange={(value) => setReportMonthRange(value)}
            />
          </Col>
          <Col xs={24} xl={6}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetReportFilters}>重置</Button>
              <Button onClick={() => void loadReport()}>刷新报表</Button>
              <Button onClick={() => void handleExportReport()}>导出报表</Button>
            </div>
          </Col>
        </Row>
      </Card>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="车队总利润"
              value={reportRows.reduce((sum, item) => sum + item.profitAmount, 0)}
              precision={2}
              prefix={<FundOutlined />}
              loading={reportLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="车队总收入"
              value={reportRows.reduce((sum, item) => sum + item.revenueAmount, 0)}
              precision={2}
              prefix="¥"
              loading={reportLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="计划总方量"
              value={reportRows.reduce((sum, item) => sum + item.plannedVolume, 0)}
              precision={2}
              suffix="m3"
              loading={reportLoading}
            />
          </Card>
        </Col>
      </Row>
      <Card className="glass-panel g-border-panel border">
        <Table<FleetReportItemRecord>
          rowKey={(record) => record.fleetId || record.fleetName}
          loading={reportLoading}
          dataSource={reportRows}
          columns={reportColumns}
          scroll={{ x: 1200 }}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>
    </Space>
  );

  const trackingContent = (
    <Space direction="vertical" size={16} className="w-full">
      <Card className="glass-panel g-border-panel border">
        <Row gutter={[12, 12]}>
          <Col xs={24} md={8} xl={6}>
            <Input
              placeholder="搜索车牌/车队/单位/目的地"
              prefix={<SearchOutlined />}
              value={trackingKeyword}
              onChange={(e) => {
                setTrackingKeyword(e.target.value);
                setTrackingPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              className="w-full"
              value={trackingStatus}
              options={trackingStatusOptions}
              onChange={(value) => {
                setTrackingStatus(value);
                setTrackingPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} md={8} xl={5}>
            <Select
              allowClear
              placeholder="选择车队"
              className="w-full"
              value={trackingFleetId}
              options={fleetOptions}
              onChange={(value) => {
                setTrackingFleetId(value);
                setTrackingPageNo(1);
              }}
            />
          </Col>
          <Col xs={24} xl={8}>
            <div className="flex gap-2 justify-end">
              <Button onClick={resetTrackingFilters}>重置</Button>
              <Button onClick={() => void Promise.all([loadTrackingSummary(), loadTrackingRows()])}>
                刷新列表
              </Button>
              <Button
                type="primary"
                icon={<EnvironmentOutlined />}
                disabled={!selectedTrackingVehicleId}
                onClick={() => void loadTrackingHistory()}
              >
                查询轨迹
              </Button>
            </div>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="跟踪车辆"
              value={trackingSummary.totalVehicles}
              prefix={<CarOutlined />}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="行驶中"
              value={trackingSummary.movingVehicles}
              prefix={<CheckCircleOutlined />}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="停留中"
              value={trackingSummary.stoppedVehicles}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="离线"
              value={trackingSummary.offlineVehicles}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="配送中"
              value={trackingSummary.deliveringVehicles}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title="异常预警"
              value={trackingSummary.warningVehicles}
              loading={trackingSummaryLoading}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={15}>
          <Card
            className="glass-panel g-border-panel border"
            title="送货轨迹地图"
            extra={
              <Space>
                <RangePicker
                  value={trackingRange}
                  onChange={(value) => setTrackingRange(value)}
                  showTime
                  allowClear={false}
                />
                <Button
                  icon={trackingPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  disabled={(trackingHistory?.points.length || 0) <= 1}
                  onClick={() => setTrackingPlaying((value) => !value)}
                >
                  {trackingPlaying ? '暂停回放' : '播放回放'}
                </Button>
              </Space>
            }
          >
            <div className="h-[420px] rounded-2xl overflow-hidden relative">
              <TiandituMap
                center={trackingCenter}
                zoom={11}
                markers={trackingMarkers}
                polylines={trackingPolylines}
                className="h-full w-full"
                loadingText="送货地图加载中..."
              />
            </div>
            <div className="mt-4">
              <Slider
                min={0}
                max={100}
                value={trackingProgress}
                disabled={(trackingHistory?.points.length || 0) <= 1}
                onChange={(value) => {
                  setTrackingProgress(Number(value));
                  setTrackingPlaying(false);
                }}
                tooltip={{ formatter: (value) => `${value || 0}%` }}
              />
              <div className="flex items-center justify-between text-xs g-text-secondary">
                <span>{trackingHistory?.startTime || '暂无开始时间'}</span>
                <span>{activeTrackingPoint?.locateTime || trackingHistory?.endTime || '暂无轨迹点'}</span>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Card
            className="glass-panel g-border-panel border"
            title="当前车辆详情"
            extra={
              selectedTrackingRecord ? (
                <Tag color={trackingTagColor[selectedTrackingRecord.trackingStatus || ''] || 'default'}>
                  {selectedTrackingRecord.trackingStatusLabel || '未知'}
                </Tag>
              ) : null
            }
          >
            {selectedTrackingRecord ? (
              <Space direction="vertical" size={16} className="w-full">
                <Descriptions size="small" column={1} bordered>
                  <Descriptions.Item label="车牌">{selectedTrackingRecord.plateNo}</Descriptions.Item>
                  <Descriptions.Item label="车队">
                    {selectedTrackingRecord.fleetName || '未编组车队'}
                  </Descriptions.Item>
                  <Descriptions.Item label="单位">
                    {selectedTrackingRecord.orgName || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="调度单">
                    {selectedTrackingRecord.dispatchOrderNo || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="运输计划">
                    {selectedTrackingRecord.relatedPlanNo || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="起止线路">
                    {selectedTrackingRecord.sourcePoint || '-'} 至{' '}
                    {selectedTrackingRecord.destinationPoint || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="货类">
                    {selectedTrackingRecord.cargoType || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="当前速度">
                    {selectedTrackingRecord.currentSpeed.toFixed(1)} km/h
                  </Descriptions.Item>
                  <Descriptions.Item label="定位时间">
                    {selectedTrackingRecord.gpsTime || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="预警">
                    {selectedTrackingRecord.warningLabel ? (
                      <Tag color="error">{selectedTrackingRecord.warningLabel}</Tag>
                    ) : (
                      <Tag>正常</Tag>
                    )}
                  </Descriptions.Item>
                </Descriptions>

                <Row gutter={[12, 12]}>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic
                        title="轨迹点"
                        value={trackingHistory?.pointCount || 0}
                        loading={trackingHistoryLoading}
                      />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic
                        title="距离(km)"
                        value={trackingHistory?.totalDistanceKm || 0}
                        precision={2}
                        loading={trackingHistoryLoading}
                      />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic
                        title="均速"
                        value={trackingHistory?.averageSpeed || 0}
                        precision={2}
                        suffix="km/h"
                        loading={trackingHistoryLoading}
                      />
                    </Card>
                  </Col>
                </Row>
              </Space>
            ) : (
              <Empty description="暂无车辆数据" />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={15}>
          <Card className="glass-panel g-border-panel border" title="跟踪车辆列表">
            <Table<FleetTrackingRecord>
              rowKey="vehicleId"
              loading={trackingLoading}
              dataSource={trackingRows}
              columns={trackingColumns}
              scroll={{ x: 1280 }}
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedTrackingVehicleId ? [selectedTrackingVehicleId] : [],
                onChange: (selectedRowKeys) =>
                  setSelectedTrackingVehicleId(String(selectedRowKeys[0] || '')),
              }}
              pagination={{
                current: trackingPageNo,
                pageSize: trackingPageSize,
                total: trackingTotal,
                showSizeChanger: true,
                onChange: (page, pageSize) => {
                  setTrackingPageNo(page);
                  setTrackingPageSize(pageSize);
                },
              }}
            />
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Card className="glass-panel g-border-panel border" title="异常停留明细">
            <Table
              rowKey={(record) => `${record.startTime}-${record.endTime}`}
              size="small"
              loading={trackingHistoryLoading}
              dataSource={trackingHistory?.stops || []}
              columns={trackingStopColumns}
              pagination={false}
              scroll={{ x: 560 }}
              locale={{ emptyText: '当前时间范围无异常停留' }}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">车队管理</h1>
          <p className="g-text-secondary mt-1 mb-0">
            打通车队维护、运输计划、调度审批、财务结算、利润报表与送货跟踪，支撑车队管理 7 大需求闭环。
          </p>
        </div>
        <Space wrap>
          <Button onClick={() => void reloadSharedData()}>刷新概览</Button>
          <Button type="primary" onClick={() => setActiveTab('dispatch')}>
            进入调度审批
          </Button>
        </Space>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          { key: 'overview', label: '概览', children: overviewContent },
          { key: 'profiles', label: '车队维护', children: profileContent },
          { key: 'plans', label: '运输计划', children: planContent },
          { key: 'dispatch', label: '调度审批', children: dispatchContent },
          { key: 'finance', label: '财务管理', children: financeContent },
          { key: 'report', label: '报表管理', children: reportContent },
          { key: 'tracking', label: '送货跟踪', children: trackingContent },
        ]}
      />

      <Modal
        title={editingProfile ? '编辑车队' : '新增车队'}
        open={profileModalOpen}
        onCancel={() => setProfileModalOpen(false)}
        onOk={() => void submitProfile()}
        confirmLoading={profileSubmitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={profileForm}>
          <Form.Item label="归属单位" name="orgId">
            <Select allowClear options={companyOptions} placeholder="选择归属单位" />
          </Form.Item>
          <Form.Item label="车队名称" name="fleetName" rules={[{ required: true, message: '请输入车队名称' }]}>
            <Input placeholder="请输入车队名称" maxLength={128} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="队长姓名" name="captainName">
                <Input placeholder="请输入队长姓名" maxLength={64} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="联系电话" name="captainPhone">
                <Input placeholder="请输入联系电话" maxLength={32} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="计划司机数" name="driverCountPlan">
                <InputNumber min={0} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="计划车辆数" name="vehicleCountPlan">
                <InputNumber min={0} className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="状态" name="status">
                <Select options={profileStatusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="考勤模式" name="attendanceMode">
                <Select options={attendanceModeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingPlan ? '编辑运输计划' : '新增运输计划'}
        open={planModalOpen}
        onCancel={() => setPlanModalOpen(false)}
        onOk={() => void submitPlan()}
        confirmLoading={planSubmitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={planForm}>
          <Form.Item label="所属车队" name="fleetId" rules={[{ required: true, message: '请选择车队' }]}>
            <Select options={fleetOptions} placeholder="请选择车队" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="计划编号" name="planNo">
                <Input placeholder="留空自动生成" maxLength={64} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="计划日期" name="planDate">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="起点" name="sourcePoint">
                <Input placeholder="请输入起点" maxLength={128} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="终点" name="destinationPoint">
                <Input placeholder="请输入终点" maxLength={128} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="货类" name="cargoType">
                <Input placeholder="例如渣土" maxLength={64} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="状态" name="status">
                <Select options={planStatusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="计划趟次" name="plannedTrips">
                <InputNumber min={0} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="计划方量(m3)" name="plannedVolume">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingDispatch ? '编辑调度申请' : '新增调度申请'}
        open={dispatchModalOpen}
        onCancel={() => setDispatchModalOpen(false)}
        onOk={() => void submitDispatch()}
        confirmLoading={dispatchSubmitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={dispatchForm}>
          <Form.Item label="所属车队" name="fleetId" rules={[{ required: true, message: '请选择车队' }]}>
            <Select options={fleetOptions} placeholder="请选择车队" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="关联运输计划" name="relatedPlanNo">
                <Select allowClear showSearch options={planOptions} placeholder="请选择计划" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="申请日期" name="applyDate">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="申请车辆数" name="requestedVehicleCount">
                <InputNumber min={0} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="申请司机数" name="requestedDriverCount">
                <InputNumber min={0} className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="紧急程度" name="urgencyLevel">
                <Select options={urgencyOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="状态" name="status">
                <Select options={dispatchStatusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="申请人" name="applicantName">
            <Input placeholder="请输入申请人" maxLength={64} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={auditMode === 'approve' ? '审批通过' : '审批驳回'}
        open={auditModalOpen}
        onCancel={() => setAuditModalOpen(false)}
        onOk={() => void submitAudit()}
        confirmLoading={auditSubmitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={auditForm}>
          <Form.Item label="审批意见" name="comment" rules={[{ required: true, message: '请输入审批意见' }]}>
            <Input.TextArea rows={4} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingFinance ? '编辑财务记录' : '新增财务记录'}
        open={financeModalOpen}
        onCancel={() => setFinanceModalOpen(false)}
        onOk={() => void submitFinance()}
        confirmLoading={financeSubmitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={financeForm}>
          <Form.Item label="所属车队" name="fleetId" rules={[{ required: true, message: '请选择车队' }]}>
            <Select options={fleetOptions} placeholder="请选择车队" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="运输合同号" name="contractNo">
                <Input placeholder="请输入运输合同号" maxLength={64} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="账期" name="statementMonth">
                <DatePicker picker="month" className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="收入金额" name="revenueAmount">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="成本金额" name="costAmount">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="其他费用" name="otherAmount">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="已结算金额" name="settledAmount">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="状态" name="status">
            <Select options={financeStatusOptions.filter((item) => item.value !== 'all')} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default FleetManagement;
