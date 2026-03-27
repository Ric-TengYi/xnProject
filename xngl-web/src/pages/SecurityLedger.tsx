import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import {
  createSecurityInspection,
  deleteSecurityInspection,
  exportSecurityInspections,
  fetchSecurityInspectionDetail,
  fetchSecurityInspections,
  fetchSecuritySummary,
  fetchSecurityUsers,
  rectifySecurityInspection,
  type SecurityInspectionPayload,
  type SecurityInspectionRecord,
  type SecuritySummaryRecord,
  type SecurityUserOption,
} from '../utils/securityApi';
import { fetchProjects } from '../utils/projectApi';
import { fetchSites } from '../utils/siteApi';
import { fetchVehicles } from '../utils/vehicleApi';
import dayjs, { type Dayjs } from 'dayjs';
import { useSearchParams } from 'react-router-dom';

const { RangePicker } = DatePicker;

const objectTypeOptions = [
  { label: '全部对象', value: 'ALL' },
  { label: '场地', value: 'SITE' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '人员', value: 'PERSON' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '待整改', value: 'OPEN' },
  { label: '整改中', value: 'RECTIFYING' },
  { label: '已关闭', value: 'CLOSED' },
];

const resultOptions = [
  { label: '全部结果', value: 'ALL' },
  { label: '合格', value: 'PASS' },
  { label: '不合格', value: 'FAIL' },
  { label: '需复查', value: 'RECTIFYING' },
];

const overdueOptions = [
  { label: '全部时效', value: 'ALL' },
  { label: '仅超期', value: 'Y' },
];

const dangerLevelOptions = [
  { label: '全部等级', value: 'ALL' },
  { label: '高风险', value: 'HIGH' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '低风险', value: 'LOW' },
];

const emptySummary: SecuritySummaryRecord = {
  monthInspectionCount: 0,
  issueCount: 0,
  closedIssueCount: 0,
  openInspectionCount: 0,
  failCount: 0,
  passCount: 0,
  rectifyingCount: 0,
  overdueRectifyCount: 0,
  objectTypeBuckets: {},
  dangerLevelBuckets: [],
  hazardCategoryBuckets: [],
};

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
};

const formatRangeParams = (
  params: Record<string, string>,
  keyFrom: string,
  keyTo: string,
  range: [Dayjs, Dayjs] | null,
) => {
  if (!range) {
    return;
  }
  params[keyFrom] = range[0].format('YYYY-MM-DDTHH:mm:ss');
  params[keyTo] = range[1].format('YYYY-MM-DDTHH:mm:ss');
};

const parseAttachmentUrls = (value?: string | null) =>
  (value || '')
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean);

const formatDateTimeValue = (value?: Dayjs | string | null) => {
  if (!value) {
    return undefined;
  }
  if (typeof value === 'string') {
    return value;
  }
  return value.format('YYYY-MM-DDTHH:mm:ss');
};

const toDayjs = (value?: string | null) => (value ? dayjs(value) : undefined);

const buildObjectOptionLabel = (type: string, item: { name?: string | null; plateNo?: string | null; username?: string | null; mobile?: string | null }) => {
  if (type === 'SITE') {
    return item.name || '-';
  }
  if (type === 'VEHICLE') {
    return item.plateNo || '-';
  }
  return [item.name || item.username || '-', item.mobile || ''].filter(Boolean).join(' / ');
};

const renderRelatedProfilePanel = (detail?: SecurityInspectionRecord | null) => {
  const profile = detail?.relatedProfile;
  if (!profile || !profile.profileType) {
    return null;
  }
  if (profile.profileType === 'PERSON') {
    return (
      <Card className="glass-panel g-border-panel border mt-4" title="人员安全档案">
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="证照记录">
            {profile.certificateCount ?? 0} 条
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              临期 {profile.expiringCertificateCount ?? 0} / 欠费 {profile.overdueFeeCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="安全学习">
            {profile.learningCount ?? 0} 次
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              已完成 {profile.completedLearningCount ?? 0} / 学习时长 {profile.studyMinutes ?? 0} 分钟
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="关联预警">
            {profile.openAlertCount ?? 0} 条未闭环
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              高风险 {profile.highRiskAlertCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="证照人员">{profile.certificateOwners?.length ? profile.certificateOwners.join(' / ') : '-'}</Descriptions.Item>
          <Descriptions.Item label="最近学习">{profile.lastStudyTime || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    );
  }
  if (profile.profileType === 'VEHICLE') {
    return (
      <Card className="glass-panel g-border-panel border mt-4" title="车辆安全档案">
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="保险台账">
            {profile.insuranceCount ?? 0} 条
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              有效 {profile.activeInsuranceCount ?? 0} / 临期 {profile.expiringInsuranceCount ?? 0} / 过期 {profile.expiredInsuranceCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="维保台账">
            {profile.maintenanceCount ?? 0} 条
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              最近维保 {profile.latestMaintenanceDate || '-'} / 累计费用 {profile.maintenanceCostTotal ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="关联预警">
            {profile.openAlertCount ?? 0} 条未闭环
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              高风险 {profile.highRiskAlertCount ?? 0}
            </span>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    );
  }
  if (profile.profileType === 'SITE') {
    return (
      <Card className="glass-panel g-border-panel border mt-4" title="场地安全档案">
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="资料台账">
            {profile.documentCount ?? 0} 份
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              审批 {profile.approvalDocumentCount ?? 0} / 运营 {profile.operationDocumentCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="设备台账">
            {profile.deviceCount ?? 0} 台
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              在线 {profile.onlineDeviceCount ?? 0} / 离线 {profile.offlineDeviceCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="关联预警">
            {profile.openAlertCount ?? 0} 条未闭环
            <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>
              高风险 {profile.highRiskAlertCount ?? 0}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="最近资料更新时间">{profile.latestDocumentTime || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    );
  }
  return null;
};

const SecurityLedger: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [rectifyLoading, setRectifyLoading] = useState(false);
  const [summary, setSummary] = useState<SecuritySummaryRecord>(emptySummary);
  const [records, setRecords] = useState<SecurityInspectionRecord[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<SecurityInspectionRecord | null>(null);
  const [queryHandledId, setQueryHandledId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [rectifyOpen, setRectifyOpen] = useState(false);
  const [rectifyRecord, setRectifyRecord] = useState<SecurityInspectionRecord | null>(null);
  const [projects, setProjects] = useState<any[]>([]);
  const [sites, setSites] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [users, setUsers] = useState<SecurityUserOption[]>([]);
  const [filters, setFilters] = useState({
    keyword: '',
    objectType: 'ALL',
    status: 'ALL',
    resultLevel: 'ALL',
    checkScene: '',
    dangerLevel: 'ALL',
    hazardCategory: '',
    projectId: undefined as string | undefined,
    siteId: undefined as string | undefined,
    vehicleId: undefined as string | undefined,
    userId: undefined as string | undefined,
    overdueOnly: 'ALL',
  });
  const [checkRange, setCheckRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [rectifyDeadlineRange, setRectifyDeadlineRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [nextCheckRange, setNextCheckRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [createForm] = Form.useForm<SecurityInspectionPayload>();
  const [rectifyForm] = Form.useForm();
  const createObjectType = Form.useWatch('objectType', createForm) || 'SITE';

  const loadMasters = async () => {
    setUsers(userList || []);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (filters.keyword.trim()) params.keyword = filters.keyword.trim();
      if (filters.objectType !== 'ALL') params.objectType = filters.objectType;
      if (filters.status !== 'ALL') params.status = filters.status;
      if (filters.resultLevel !== 'ALL') params.resultLevel = filters.resultLevel;
      if (filters.checkScene.trim()) params.checkScene = filters.checkScene.trim();
      if (filters.dangerLevel !== 'ALL') params.dangerLevel = filters.dangerLevel;
      if (filters.hazardCategory.trim()) params.hazardCategory = filters.hazardCategory.trim();
      if (filters.projectId) params.projectId = filters.projectId;
      if (filters.siteId) params.siteId = filters.siteId;
      if (filters.vehicleId) params.vehicleId = filters.vehicleId;
      if (filters.userId) params.userId = filters.userId;
      if (filters.overdueOnly === 'Y') params.overdueOnly = 'true';
      formatRangeParams(params, 'checkTimeFrom', 'checkTimeTo', checkRange);
      formatRangeParams(params, 'rectifyDeadlineFrom', 'rectifyDeadlineTo', rectifyDeadlineRange);
      formatRangeParams(params, 'nextCheckTimeFrom', 'nextCheckTimeTo', nextCheckRange);
      const [summaryData, inspectionList] = await Promise.all([
        fetchSecuritySummary(params),
        fetchSecurityInspections(params),
      ]);
      setSummary(summaryData);
      setRecords(inspectionList);
    } catch (error) {
      console.error(error);
      message.error('获取安全台账失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadMasters().catch((error) => {
      console.error(error);
      message.error('加载基础数据失败');
    });
  }, []);

  useEffect(() => {
    void loadData();
  }, [
    filters.keyword,
    filters.objectType,
    filters.status,
    filters.resultLevel,
    filters.checkScene,
    filters.dangerLevel,
    filters.hazardCategory,
    filters.projectId,
    filters.siteId,
    filters.vehicleId,
    filters.userId,
    filters.overdueOnly,
    checkRange,
    rectifyDeadlineRange,
    nextCheckRange,
  ]);

  useEffect(() => {
    const inspectionId = searchParams.get('inspectionId');
    if (!inspectionId || queryHandledId === inspectionId) {
      return;
    }
    setQueryHandledId(inspectionId);
    setDetailOpen(true);
    void fetchSecurityInspectionDetail(inspectionId)
      .then((data) => setDetail(data))
      .catch((error) => {
        console.error(error);
        message.error('获取检查详情失败');
      });
  }, [queryHandledId, searchParams]);

  const openDetail = async (record: SecurityInspectionRecord) => {
    setDetailOpen(true);
    try {
      setDetail(await fetchSecurityInspectionDetail(record.id));
    } catch (error) {
      console.error(error);
      message.error('获取检查详情失败');
    }
  };

  const refreshDetail = async (id: string) => {
    const [detailData] = await Promise.all([fetchSecurityInspectionDetail(id), loadData()]);
    setDetail(detailData);
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      setSubmitLoading(true);
      const payload: SecurityInspectionPayload = {
        ...values,
        objectId: values.objectId ? Number(values.objectId) : undefined,
        projectId: values.projectId ? Number(values.projectId) : undefined,
        siteId: values.siteId ? Number(values.siteId) : undefined,
        vehicleId: values.vehicleId ? Number(values.vehicleId) : undefined,
        userId: values.userId ? Number(values.userId) : undefined,
        checkTime: formatDateTimeValue(values.checkTime as unknown as Dayjs | string | null),
        rectifyDeadline: formatDateTimeValue(values.rectifyDeadline as unknown as Dayjs | string | null),
        nextCheckTime: formatDateTimeValue(values.nextCheckTime as unknown as Dayjs | string | null),
      };
      if (payload.objectType === 'PERSON' && payload.objectId) {
        payload.userId = payload.objectId;
      }
      if (payload.objectType === 'SITE' && payload.objectId && !payload.siteId) {
        payload.siteId = payload.objectId;
      }
      if (payload.objectType === 'VEHICLE' && payload.objectId && !payload.vehicleId) {
        payload.vehicleId = payload.objectId;
      }
      await createSecurityInspection(payload);
      message.success('安全检查记录已新增');
      setCreateOpen(false);
      createForm.resetFields();
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存安全检查失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const openRectifyModal = (record: SecurityInspectionRecord) => {
    setRectifyRecord(record);
    rectifyForm.setFieldsValue({
      status: record.status === 'OPEN' ? 'RECTIFYING' : record.status || 'CLOSED',
      resultLevel: record.resultLevel || 'PASS',
      rectifyRemark: record.rectifyRemark || undefined,
      nextCheckTime: toDayjs(record.nextCheckTime),
    });
    setRectifyOpen(true);
  };

  const handleRectify = async () => {
    if (!rectifyRecord) {
      return;
    }
    try {
      const values = await rectifyForm.validateFields();
      setRectifyLoading(true);
      await rectifySecurityInspection(rectifyRecord.id, {
        ...values,
        nextCheckTime: formatDateTimeValue(values.nextCheckTime as Dayjs | string | null),
      });
      message.success('整改信息已更新');
      setRectifyOpen(false);
      setRectifyRecord(null);
      rectifyForm.resetFields();
      if (detailOpen && detail?.id === rectifyRecord.id) {
        await refreshDetail(rectifyRecord.id);
      } else {
        await loadData();
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('更新整改失败');
    } finally {
      setRectifyLoading(false);
    }
  };

  const buildParams = () => {
    const params: Record<string, string> = {};
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim();
    if (filters.objectType !== 'ALL') params.objectType = filters.objectType;
    if (filters.status !== 'ALL') params.status = filters.status;
    if (filters.resultLevel !== 'ALL') params.resultLevel = filters.resultLevel;
    if (filters.checkScene.trim()) params.checkScene = filters.checkScene.trim();
    if (filters.dangerLevel !== 'ALL') params.dangerLevel = filters.dangerLevel;
    if (filters.hazardCategory.trim()) params.hazardCategory = filters.hazardCategory.trim();
    if (filters.projectId) params.projectId = filters.projectId;
    if (filters.siteId) params.siteId = filters.siteId;
    if (filters.vehicleId) params.vehicleId = filters.vehicleId;
    if (filters.userId) params.userId = filters.userId;
    if (filters.overdueOnly === 'Y') params.overdueOnly = 'true';
    formatRangeParams(params, 'checkTimeFrom', 'checkTimeTo', checkRange);
    formatRangeParams(params, 'rectifyDeadlineFrom', 'rectifyDeadlineTo', rectifyDeadlineRange);
    formatRangeParams(params, 'nextCheckTimeFrom', 'nextCheckTimeTo', nextCheckRange);
    return params;
  };

  const handleExport = async () => {
    try {
      downloadBlob(await exportSecurityInspections(buildParams()), 'security_inspections.csv');
      message.success('安全台账导出成功');
    } catch (error) {
      console.error(error);
      message.error('安全台账导出失败');
    }
  };

  const handleDelete = (record: SecurityInspectionRecord) => {
    Modal.confirm({
      title: `确认删除 ${record.inspectionNo || '该检查记录'} 吗？`,
      content: '删除后该台账记录将不再出现在列表中。',
      okButtonProps: { danger: true },
      okText: '删除',
      cancelText: '取消',
      onOk: async () => {
        await deleteSecurityInspection(record.id);
        message.success('安全检查记录已删除');
        if (detail?.id === record.id) {
          setDetail(null);
          setDetailOpen(false);
        }
        await loadData();
      },
    });
  };

  const columns: ColumnsType<SecurityInspectionRecord> = [
    {
      title: '检查编号',
      dataIndex: 'inspectionNo',
      key: 'inspectionNo',
      render: (value) => <span className="font-mono">{value || '-'}</span>,
    },
    {
      title: '检查信息',
      key: 'info',
      render: (_, record) => (
        <div className="flex flex-col gap-1">
          <span>{record.title}</span>
          <Space size={6} wrap>
            <Tag color="blue">{record.objectType}</Tag>
            {record.dangerLevel ? <Tag color={record.dangerLevel === 'HIGH' ? 'red' : record.dangerLevel === 'MEDIUM' ? 'orange' : 'green'}>{record.dangerLevel}</Tag> : null}
            {record.hazardCategory ? <Tag color="geekblue">{record.hazardCategory}</Tag> : null}
            <span style={{ color: 'var(--text-secondary)' }}>{record.checkScene || '-'}</span>
            {record.isOverdue ? <Tag color="red">已超期</Tag> : null}
          </Space>
        </div>
      ),
    },
    {
      title: '关联对象',
      key: 'relation',
      render: (_, record) => (
        <div className="flex flex-col gap-1">
          <span>{record.objectLabel || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.projectName || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>
            {[record.siteName, record.vehicleNo, record.userName].filter(Boolean).join(' / ') || '-'}
          </span>
          {record.relatedProfileSummary ? (
            <span style={{ color: 'var(--text-secondary)' }}>{record.relatedProfileSummary}</span>
          ) : null}
        </div>
      ),
    },
    { title: '问题数', dataIndex: 'issueCount', key: 'issueCount' },
    {
      title: '结果',
      dataIndex: 'resultLevel',
      key: 'resultLevel',
      render: (value) => <Tag color={value === 'FAIL' ? 'red' : value === 'RECTIFYING' ? 'orange' : 'green'}>{value || '-'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value) => <Tag color={value === 'CLOSED' ? 'green' : value === 'RECTIFYING' ? 'orange' : 'red'}>{value || '-'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => void openDetail(record)}>
            详情
          </Button>
          {record.status !== 'CLOSED' ? (
            <Button type="link" onClick={() => openRectifyModal(record)}>
              整改
            </Button>
          ) : null}
          <Button type="link" danger onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const objectOptions =
    createObjectType === 'SITE'
      ? sites.map((item) => ({ label: buildObjectOptionLabel('SITE', item), value: Number(item.id) }))
      : createObjectType === 'VEHICLE'
      ? vehicles.map((item) => ({ label: buildObjectOptionLabel('VEHICLE', item), value: Number(item.id) }))
      : users.map((item) => ({ label: buildObjectOptionLabel('PERSON', item), value: Number(item.id) }));

  const bucketEntries = Object.entries(summary.objectTypeBuckets || {});

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">安全台账管理</h1>
          <p className="g-text-secondary mt-1">扩展场地、车辆、人员安全检查字段与台账明细闭环能力</p>
        </div>
        <Space>
          <Button onClick={() => void handleExport()}>
            导出
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              createForm.setFieldsValue({
                objectType: 'SITE',
                resultLevel: 'PASS',
                dangerLevel: 'LOW',
                issueCount: 0,
                status: 'CLOSED',
              });
              setCreateOpen(true);
            }}
          >
            新增检查记录
          </Button>
        </Space>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4 xl:grid-cols-12">
          <Input
            allowClear
            placeholder="搜索编号 / 标题 / 检查人 / 描述"
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <Select value={filters.objectType} options={objectTypeOptions} onChange={(value) => setFilters((prev) => ({ ...prev, objectType: value }))} />
          <Select value={filters.status} options={statusOptions} onChange={(value) => setFilters((prev) => ({ ...prev, status: value }))} />
          <Select value={filters.resultLevel} options={resultOptions} onChange={(value) => setFilters((prev) => ({ ...prev, resultLevel: value }))} />
          <Input placeholder="检查场景，如 FIRE" value={filters.checkScene} onChange={(event) => setFilters((prev) => ({ ...prev, checkScene: event.target.value }))} />
          <Select value={filters.dangerLevel} options={dangerLevelOptions} onChange={(value) => setFilters((prev) => ({ ...prev, dangerLevel: value }))} />
          <Input placeholder="隐患类别，如 PERSONNEL" value={filters.hazardCategory} onChange={(event) => setFilters((prev) => ({ ...prev, hazardCategory: event.target.value }))} />
          <Select allowClear placeholder="关联项目" value={filters.projectId} options={projects.map((item) => ({ label: item.name, value: String(item.id) }))} onChange={(value) => setFilters((prev) => ({ ...prev, projectId: value }))} />
          <Select allowClear placeholder="关联场地" value={filters.siteId} options={sites.map((item) => ({ label: item.name, value: String(item.id) }))} onChange={(value) => setFilters((prev) => ({ ...prev, siteId: value }))} />
          <Select allowClear placeholder="关联车辆" value={filters.vehicleId} options={vehicles.map((item) => ({ label: item.plateNo, value: String(item.id) }))} onChange={(value) => setFilters((prev) => ({ ...prev, vehicleId: value }))} />
          <Select allowClear placeholder="关联人员" value={filters.userId} options={users.map((item) => ({ label: buildObjectOptionLabel('PERSON', item), value: String(item.id) }))} onChange={(value) => setFilters((prev) => ({ ...prev, userId: value }))} />
          <Select value={filters.overdueOnly} options={overdueOptions} onChange={(value) => setFilters((prev) => ({ ...prev, overdueOnly: value }))} />
          <RangePicker
            showTime
            value={checkRange}
            onChange={(value) => setCheckRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['检查开始', '检查结束']}
          />
          <RangePicker
            showTime
            value={rectifyDeadlineRange}
            onChange={(value) => setRectifyDeadlineRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['整改截止开始', '整改截止结束']}
          />
          <RangePicker
            showTime
            value={nextCheckRange}
            onChange={(value) => setNextCheckRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['复查开始', '复查结束']}
          />
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card className="glass-panel g-border-panel border"><Statistic title="本月检查" value={summary.monthInspectionCount} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="隐患问题" value={summary.issueCount} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已整改问题" value={summary.closedIssueCount} valueStyle={{ color: '#16a34a' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="待闭环" value={summary.openInspectionCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="不合格" value={summary.failCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="合格" value={summary.passCount} valueStyle={{ color: '#16a34a' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="整改中" value={summary.rectifyingCount} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="超期整改" value={summary.overdueRectifyCount} valueStyle={{ color: '#ef4444' }} /></Card>
      </div>

      <Card
        className="glass-panel g-border-panel border"
        title="对象分布"
        extra={
          <Space wrap>
            {bucketEntries.map(([key, value]) => <Tag key={key}>{key}: {value}</Tag>)}
            {summary.dangerLevelBuckets.map((item) => <Tag key={`danger-${item.code}`} color={item.code === 'HIGH' ? 'red' : item.code === 'MEDIUM' ? 'orange' : 'green'}>{item.code}: {item.count}</Tag>)}
            {summary.hazardCategoryBuckets.map((item) => <Tag key={`hazard-${item.code}`} color="blue">{item.code}: {item.count}</Tag>)}
          </Space>
        }
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={{ pageSize: 10 }} />
      </Card>

      <Drawer
        title="检查详情"
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={640}
        extra={
          <Space>
            {detail && detail.status !== 'CLOSED' ? <Button type="primary" onClick={() => openRectifyModal(detail)}>整改处理</Button> : null}
            {detail ? (
              <Button danger onClick={() => handleDelete(detail)}>
                删除记录
              </Button>
            ) : null}
          </Space>
        }
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="检查编号">{detail?.inspectionNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="检查标题">{detail?.title || '-'}</Descriptions.Item>
          <Descriptions.Item label="对象类型">{detail?.objectType || '-'}</Descriptions.Item>
          <Descriptions.Item label="检查对象">{detail?.objectLabel || detail?.objectName || '-'}</Descriptions.Item>
          <Descriptions.Item label="关联对象ID">{detail?.objectId || '-'}</Descriptions.Item>
          <Descriptions.Item label="项目 / 场地 / 车辆">
            {detail?.projectName || '-'} / {detail?.siteName || '-'} / {detail?.vehicleNo || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="关联人员">
            {detail?.userName || '-'} / {detail?.userMobile || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查场景 / 类型">{detail?.checkScene || '-'} / {detail?.checkType || '-'}</Descriptions.Item>
          <Descriptions.Item label="隐患类别 / 等级">
            {detail?.hazardCategory || '-'} / {detail?.dangerLevel || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查人">{detail?.inspectorName || '-'} ({detail?.inspectorId || '-'})</Descriptions.Item>
          <Descriptions.Item label="整改责任人">
            {detail?.rectifyOwner || '-'} / {detail?.rectifyOwnerPhone || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查结果">
            <Space>
              <Tag color={detail?.resultLevel === 'FAIL' ? 'red' : detail?.resultLevel === 'RECTIFYING' ? 'orange' : 'green'}>{detail?.resultLevel || '-'}</Tag>
              <Tag color={detail?.status === 'CLOSED' ? 'green' : detail?.status === 'RECTIFYING' ? 'orange' : 'red'}>{detail?.status || '-'}</Tag>
              {detail?.isOverdue ? <Tag color="red">已超期</Tag> : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="问题数量">{detail?.issueCount ?? 0}</Descriptions.Item>
          <Descriptions.Item label="检查时间">{detail?.checkTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改截止">{detail?.rectifyDeadline || '-'}</Descriptions.Item>
          <Descriptions.Item label="下次复查">{detail?.nextCheckTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改时间">{detail?.rectifyTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改说明">{detail?.rectifyRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="预估整改费用">{detail?.estimatedCost ?? 0}</Descriptions.Item>
          <Descriptions.Item label="附件">
            <Space wrap>
              {parseAttachmentUrls(detail?.attachmentUrls).length
                ? parseAttachmentUrls(detail?.attachmentUrls).map((item) => (
                    <a key={item} href={item} target="_blank" rel="noreferrer">
                      {item}
                    </a>
                  ))
                : '-'}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="检查说明">{detail?.description || '-'}</Descriptions.Item>
        </Descriptions>
        <Card className="glass-panel g-border-panel border mt-4" title="处理时间线">
          <Timeline
            items={(detail?.actions || []).map((item) => ({
              color: item.actionType === 'DELETE' ? 'red' : item.actionType === 'RECTIFY' ? 'orange' : 'blue',
              children: (
                <div className="flex flex-col gap-1">
                  <Space wrap>
                    <strong>{item.actionLabel || item.actionType || '-'}</strong>
                    <Tag>{item.actorName || '-'}</Tag>
                    <span style={{ color: 'var(--text-secondary)' }}>{item.actionTime || '-'}</span>
                  </Space>
                  <span style={{ color: 'var(--text-secondary)' }}>
                    状态：{item.beforeStatus || '-'} → {item.afterStatus || '-'}，结果：{item.beforeResultLevel || '-'} → {item.afterResultLevel || '-'}
                  </span>
                  {item.nextCheckTime ? <span style={{ color: 'var(--text-secondary)' }}>下次复查：{item.nextCheckTime}</span> : null}
                  {item.actionRemark ? <span>{item.actionRemark}</span> : null}
                </div>
              ),
            }))}
          />
        </Card>
        {renderRelatedProfilePanel(detail)}
      </Drawer>

      <Modal title="新增安全检查" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => void handleCreate()} confirmLoading={submitLoading}>
        <Form form={createForm} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="objectType" label="对象类型" rules={[{ required: true, message: '请选择对象类型' }]}>
              <Select options={objectTypeOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="objectId" label="检查对象" rules={[{ required: true, message: '请选择检查对象' }]}>
              <Select showSearch optionFilterProp="label" options={objectOptions} />
            </Form.Item>
          </div>
          <Form.Item name="title" label="检查标题" rules={[{ required: true, message: '请输入检查标题' }]}>
            <Input />
          </Form.Item>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="checkScene" label="检查场景">
              <Input placeholder="如 FIRE / VEHICLE_OPERATION" />
            </Form.Item>
            <Form.Item name="checkType" label="检查类型">
              <Input placeholder="如 DAILY / SPECIAL" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="hazardCategory" label="隐患类别">
              <Input placeholder="如 FIRE / OPERATION / PERSONNEL" />
            </Form.Item>
            <Form.Item name="dangerLevel" label="隐患等级">
              <Select options={[{ label: '高', value: 'HIGH' }, { label: '中', value: 'MEDIUM' }, { label: '低', value: 'LOW' }]} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="resultLevel" label="检查结果">
              <Select options={resultOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="issueCount" label="问题数量">
              <InputNumber className="w-full" min={0} />
            </Form.Item>
            <Form.Item name="checkTime" label="检查时间">
              <DatePicker className="w-full" showTime />
            </Form.Item>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <Form.Item name="projectId" label="关联项目">
              <Select allowClear options={projects.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="siteId" label="关联场地">
              <Select allowClear options={sites.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="vehicleId" label="关联车辆">
              <Select allowClear options={vehicles.map((item) => ({ label: item.plateNo, value: Number(item.id) }))} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="rectifyOwner" label="整改责任人">
              <Input />
            </Form.Item>
            <Form.Item name="rectifyOwnerPhone" label="责任人电话">
              <Input />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="rectifyDeadline" label="整改截止">
              <DatePicker className="w-full" showTime />
            </Form.Item>
            <Form.Item name="nextCheckTime" label="下次复查">
              <DatePicker className="w-full" showTime />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="estimatedCost" label="预估整改费用">
              <InputNumber className="w-full" min={0} />
            </Form.Item>
            <Form.Item name="attachmentUrls" label="附件地址">
              <Input placeholder="https://..." />
            </Form.Item>
          </div>
          <Form.Item name="description" label="检查说明">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="整改处理" open={rectifyOpen} onCancel={() => setRectifyOpen(false)} onOk={() => void handleRectify()} confirmLoading={rectifyLoading}>
        <Form form={rectifyForm} layout="vertical">
          <Form.Item name="status" label="整改状态" rules={[{ required: true, message: '请选择整改状态' }]}>
            <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
          </Form.Item>
          <Form.Item name="resultLevel" label="整改结果">
            <Select options={resultOptions.filter((item) => item.value !== 'ALL')} />
          </Form.Item>
          <Form.Item name="nextCheckTime" label="下次复查时间">
            <DatePicker className="w-full" showTime />
          </Form.Item>
          <Form.Item name="rectifyRemark" label="整改说明">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default SecurityLedger;
