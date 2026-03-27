import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  List,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  closeAlert,
  fetchAlertAnalyticsByParams,
  fetchAlertDetail,
  fetchAlerts,
  fetchAlertSummaryByParams,
  fetchFenceStatus,
  fetchTopRiskTargets,
  fetchTopRiskVehiclesByParams,
  exportAlerts,
  generateAlerts,
  handleAlert,
  type AlertAnalyticsRecord,
  type AlertRecord,
  type AlertSummaryRecord,
  type AlertTopRiskRecord,
} from '../utils/alertApi';
import type { Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

const levelOptions = [
  { label: '全部等级', value: 'ALL' },
  { label: 'L3 高风险', value: 'L3' },
  { label: 'L2 中风险', value: 'L2' },
  { label: 'L1 低风险', value: 'L1' },
];

const targetOptions = [
  { label: '全部对象', value: 'ALL' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '场地', value: 'SITE' },
  { label: '项目', value: 'PROJECT' },
  { label: '合同', value: 'CONTRACT' },
  { label: '人员', value: 'USER' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '待处置', value: 'PENDING' },
  { label: '处置中', value: 'PROCESSING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已关闭', value: 'CLOSED' },
];

const sourceOptions = [
  { label: '全部来源', value: 'ALL' },
  { label: '系统识别', value: 'SYSTEM' },
  { label: 'GPS', value: 'GPS' },
  { label: 'VIDEO', value: 'VIDEO' },
  { label: 'WEB', value: 'WEB' },
  { label: 'MANUAL', value: 'MANUAL' },
];

const overdueOptions = [
  { label: '全部时效', value: 'ALL' },
  { label: '仅超期', value: 'Y' },
];

const statusColorMap: Record<string, string> = {
  PENDING: 'red',
  PROCESSING: 'orange',
  CLOSED: 'default',
  CONFIRMED: 'green',

const emptySummary: AlertSummaryRecord = {
  total: 0,
  pending: 0,
  processing: 0,
  closed: 0,
  confirmed: 0,
  vehicleCount: 0,
  siteCount: 0,
  projectCount: 0,
  contractCount: 0,
  userCount: 0,
  highRiskCount: 0,
  avgHandleMinutes: 0,
  overdueCount: 0,
  enabledRuleCount: 0,
  enabledFenceCount: 0,
  enabledPushCount: 0,

const emptyAnalytics: AlertAnalyticsRecord = {
  levelBuckets: [],
  targetBuckets: [],
  sourceBuckets: [],
  statusBuckets: [],
  ruleBuckets: [],
  trend7d: [],
  modelCoverage: {
    totalRules: 0,
    enabledRules: 0,
    connectedFenceRules: 0,
    connectedPushRules: 0,
    sceneCoverage: {},
  },

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);

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

const formatJsonText = (value?: string | null) => {
  if (!value) {
    return '-';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }

const AlertsMonitor: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [generateLoading, setGenerateLoading] = useState(false);
  const [summary, setSummary] = useState<AlertSummaryRecord>(emptySummary);
  const [analytics, setAnalytics] = useState<AlertAnalyticsRecord>(emptyAnalytics);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [topRisk, setTopRisk] = useState<AlertTopRiskRecord[]>([]);
  const [topContractRisk, setTopContractRisk] = useState<AlertTopRiskRecord[]>([]);
  const [topUserRisk, setTopUserRisk] = useState<AlertTopRiskRecord[]>([]);
  const [fences, setFences] = useState<any[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<AlertRecord | null>(null);
  const [actionModalOpen, setActionModalOpen] = useState(false);
  const [actionRecord, setActionRecord] = useState<AlertRecord | null>(null);
  const [actionMode, setActionMode] = useState<'PROCESSING' | 'CONFIRMED' | 'CLOSED'>('PROCESSING');
  const [actionForm] = Form.useForm<{ handleRemark: string }>();
  const [filters, setFilters] = useState({
    keyword: '',
    level: 'ALL',
    targetType: 'ALL',
    status: 'ALL',
    sourceChannel: 'ALL',
    ruleCode: '',
    overdueOnly: 'ALL',
  });
  const [occurRange, setOccurRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [resolveRange, setResolveRange] = useState<[Dayjs, Dayjs] | null>(null);

  const buildParams = () => {
    const params: Record<string, string> = {};
    if (filters.keyword.trim()) {
      params.keyword = filters.keyword.trim();
    }
    if (filters.level !== 'ALL') {
      params.level = filters.level;
    }
    if (filters.targetType !== 'ALL') {
      params.targetType = filters.targetType;
    }
    if (filters.status !== 'ALL') {
      params.status = filters.status;
    }
    if (filters.sourceChannel !== 'ALL') {
      params.sourceChannel = filters.sourceChannel;
    }
    if (filters.ruleCode.trim()) {
      params.ruleCode = filters.ruleCode.trim().toUpperCase();
    }
    if (filters.overdueOnly === 'Y') {
      params.overdueOnly = 'true';
    }
    formatRangeParams(params, 'occurTimeFrom', 'occurTimeTo', occurRange);
    formatRangeParams(params, 'resolveTimeFrom', 'resolveTimeTo', resolveRange);
    return params;
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const params = buildParams();
      const [summaryData, analyticsData, alertList, topRiskList, topContractList, topUserList, fenceList] = await Promise.all([
        fetchAlertSummaryByParams(params),
        fetchAlertAnalyticsByParams(params),
        fetchAlerts(params),
        fetchTopRiskVehiclesByParams(params),
        fetchTopRiskTargets('CONTRACT', params),
        fetchTopRiskTargets('USER', params),
        fetchFenceStatus(),
      ]);
      setSummary(summaryData);
      setAnalytics(analyticsData);
      setRecords(alertList);
      setTopRisk(topRiskList);
      setTopContractRisk(topContractList);
      setTopUserRisk(topUserList);
      setFences(fenceList);
    } catch (error) {
      console.error(error);
      message.error('获取预警中心数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [
    filters.keyword,
    filters.level,
    filters.targetType,
    filters.status,
    filters.sourceChannel,
    filters.ruleCode,
    filters.overdueOnly,
    occurRange,
    resolveRange,
  ]);

  const openDetail = async (record: AlertRecord) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetail(await fetchAlertDetail(record.id));
    } catch (error) {
      console.error(error);
      message.error('获取预警详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshDetail = async (id: string) => {
    setDetailLoading(true);
    try {
      const [detailData] = await Promise.all([fetchAlertDetail(id), loadData()]);
      setDetail(detailData);
    } catch (error) {
      console.error(error);
      message.error('刷新预警详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const openActionModal = (record: AlertRecord, mode: 'PROCESSING' | 'CONFIRMED' | 'CLOSED') => {
    setActionRecord(record);
    setActionMode(mode);
    actionForm.setFieldsValue({
      handleRemark:
        mode === 'PROCESSING'
          ? '已开始研判处置'
          : mode === 'CONFIRMED'
            ? '研判属实，已确认并转后续跟进'
            : '已完成处置并关闭预警',
    });
    setActionModalOpen(true);
  };

  const submitAction = async () => {
    if (!actionRecord) return;
    try {
      const values = await actionForm.validateFields();
      setActionLoading(true);
      if (actionMode === 'CLOSED') {
        await closeAlert(actionRecord.id, { handleRemark: values.handleRemark });
      } else {
        await handleAlert(actionRecord.id, { status: actionMode, handleRemark: values.handleRemark });
      }
      message.success(actionMode === 'CLOSED' ? '预警已关闭' : actionMode === 'CONFIRMED' ? '预警已确认' : '预警已转入处置中');
      setActionModalOpen(false);
      setActionRecord(null);
      actionForm.resetFields();
      if (detail?.id === actionRecord.id) {
        await refreshDetail(actionRecord.id);
      } else {
        await loadData();
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('提交预警处置失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleGenerate = async () => {
    try {
      setGenerateLoading(true);
      const result = await generateAlerts();
      message.success(
        `预警已刷新：新增 ${result.createdCount}，更新 ${result.updatedCount}，关闭 ${result.closedCount}`,
      );
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('刷新自动预警失败');
    } finally {
      setGenerateLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      downloadBlob(await exportAlerts(buildParams()), 'alerts_monitor.csv');
      message.success('预警导出成功');
    } catch (error) {
      console.error(error);
      message.error('预警导出失败');
    }
  };

  const columns: ColumnsType<AlertRecord> = useMemo(
    () => [
      {
        title: '预警编号',
        dataIndex: 'alertNo',
        key: 'alertNo',
        render: (value) => <span className="font-mono">{value || '-'}</span>,
      },
      { title: '标题', dataIndex: 'title', key: 'title' },
      {
        title: '对象 / 关联信息',
        key: 'relation',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <Space size={6} wrap>
              <Tag color="blue">{record.targetType || '-'}</Tag>
              <span>{record.vehicleNo || record.siteName || record.projectName || record.contractNo || '-'}</span>
              {record.isOverdue ? <Tag color="red">超期</Tag> : null}
            </Space>
            <span style={{ color: 'var(--text-secondary)' }}>
              {record.projectName || '-'} / {record.siteName || '-'}
            </span>
          </div>
        ),
      },
      {
        title: '等级',
        dataIndex: 'level',
        key: 'level',
        render: (value) => <Tag color={value === 'L3' ? 'red' : value === 'L2' ? 'orange' : 'blue'}>{value || '-'}</Tag>,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (value) => <Tag color={statusColorMap[value || ''] || 'default'}>{value || '-'}</Tag>,
      },
      {
        title: '来源',
        dataIndex: 'sourceChannel',
        key: 'sourceChannel',
        render: (value) => value || '-',
      },
      {
        title: '持续时长',
        dataIndex: 'durationMinutes',
        key: 'durationMinutes',
        render: (value) => (value != null ? `${value} 分钟` : '-'),
      },
      {
        title: '操作',
        key: 'action',
        render: (_, record) => (
          <Space>
            <Button type="link" size="small" onClick={() => void openDetail(record)}>
              详情
            </Button>
            {record.status !== 'PROCESSING' && record.status !== 'CLOSED' ? (
              <Button
                type="link"
                size="small"
                onClick={() => openActionModal(record, 'PROCESSING')}
              >
                开始处置
              </Button>
            ) : null}
            {record.status !== 'CONFIRMED' && record.status !== 'CLOSED' ? (
              <Button type="link" size="small" onClick={() => openActionModal(record, 'CONFIRMED')}>
                确认
              </Button>
            ) : null}
            {record.status !== 'CLOSED' ? (
              <Button type="link" size="small" onClick={() => openActionModal(record, 'CLOSED')}>
                关闭
              </Button>
            ) : null}
          </Space>
        ),
      },
    ],
    [detail?.id],
  );

  const sceneCoverage = Object.entries(analytics.modelCoverage.sceneCoverage || {});

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">预警与监控中心</h1>
          <p className="g-text-secondary mt-1">补实预警规则、实例闭环和告警模型覆盖情况</p>
        </div>
        <Space>
          <Button onClick={() => void handleExport()}>
            导出
          </Button>
          <Button loading={loading} onClick={() => void loadData()}>
            刷新列表
          </Button>
          <Button type="primary" loading={generateLoading} onClick={() => void handleGenerate()}>
            刷新自动预警
          </Button>
        </Space>
      </div>
      <Card className="glass-panel g-border-panel border">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4 xl:grid-cols-8">
          <Input
            allowClear
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
            placeholder="搜索编号 / 标题 / 规则 / 内容"
          />
          <Select value={filters.targetType} options={targetOptions} onChange={(value) => setFilters((prev) => ({ ...prev, targetType: value }))} />
          <Select value={filters.level} options={levelOptions} onChange={(value) => setFilters((prev) => ({ ...prev, level: value }))} />
          <Select value={filters.status} options={statusOptions} onChange={(value) => setFilters((prev) => ({ ...prev, status: value }))} />
          <Select value={filters.sourceChannel} options={sourceOptions} onChange={(value) => setFilters((prev) => ({ ...prev, sourceChannel: value }))} />
          <Input
            allowClear
            value={filters.ruleCode}
            onChange={(event) => setFilters((prev) => ({ ...prev, ruleCode: event.target.value }))}
            placeholder="规则编码，如 VEHICLE_ROUTE_DEVIATION"
          />
          <Select value={filters.overdueOnly} options={overdueOptions} onChange={(value) => setFilters((prev) => ({ ...prev, overdueOnly: value }))} />
          <RangePicker
            showTime
            value={occurRange}
            onChange={(value) => setOccurRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['发生开始', '发生结束']}
          />
          <RangePicker
            showTime
            value={resolveRange}
            onChange={(value) => setResolveRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['关闭开始', '关闭结束']}
          />
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card className="glass-panel g-border-panel border"><Statistic title="预警总数" value={summary.total} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="待处置" value={summary.pending} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="处置中" value={summary.processing} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="高风险" value={summary.highRiskCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="合同预警" value={summary.contractCount} valueStyle={{ color: '#722ed1' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="人员预警" value={summary.userCount} valueStyle={{ color: '#1677ff' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="超时未闭环" value={summary.overdueCount} valueStyle={{ color: '#ef4444' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="平均处置(分)" value={summary.avgHandleMinutes} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="启用规则" value={summary.enabledRuleCount} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="启用推送" value={summary.enabledPushCount} /></Card>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[2fr_1fr]">
        <Card className="glass-panel g-border-panel border" title="预警实例列表">
          <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={{ pageSize: 8 }} />
        </Card>

        <div className="space-y-6">
          <Card className="glass-panel g-border-panel border" title="模型覆盖">
            <div className="grid grid-cols-2 gap-3">
              <Statistic title="规则总数" value={analytics.modelCoverage.totalRules} />
              <Statistic title="启用规则" value={analytics.modelCoverage.enabledRules} />
              <Statistic title="围栏挂接" value={analytics.modelCoverage.connectedFenceRules} />
              <Statistic title="推送挂接" value={analytics.modelCoverage.connectedPushRules} />
            </div>
            <div className="mt-4 flex flex-wrap gap-2">
              {sceneCoverage.length ? sceneCoverage.map(([key, value]) => <Tag key={key}>{key}: {value}</Tag>) : <span style={{ color: 'var(--text-secondary)' }}>暂无场景覆盖数据</span>}
            </div>
          </Card>

          <Card className="glass-panel g-border-panel border" title="高风险车辆 TOP">
            <List
              size="small"
              dataSource={topRisk}
              locale={{ emptyText: '暂无数据' }}
              renderItem={(item, index) => (
                <List.Item>
                  <div className="flex w-full items-center justify-between gap-3">
                      <div>
                      <div>{index + 1}. {item.targetName || '-'}</div>
                      <div style={{ color: 'var(--text-secondary)' }}>{item.extraName || '未关联车队'}</div>
                    </div>
                    <Tag color="red">{item.count || 0} 起</Tag>
                  </div>
                </List.Item>
              )}
            />
          </Card>

          <Card className="glass-panel g-border-panel border" title="高风险合同 / 人员">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <List
                size="small"
                header="合同 TOP"
                dataSource={topContractRisk}
                locale={{ emptyText: '暂无数据' }}
                renderItem={(item, index) => (
                  <List.Item>
                    <div className="flex w-full items-center justify-between gap-3">
                      <div>
                        <div>{index + 1}. {item.targetName}</div>
                        <div style={{ color: 'var(--text-secondary)' }}>{item.extraName || '合同预警'}</div>
                      </div>
                      <Tag color="purple">{item.count} 起</Tag>
                    </div>
                  </List.Item>
                )}
              />
              <List
                size="small"
                header="人员 TOP"
                dataSource={topUserRisk}
                locale={{ emptyText: '暂无数据' }}
                renderItem={(item, index) => (
                  <List.Item>
                    <div className="flex w-full items-center justify-between gap-3">
                      <div>
                        <div>{index + 1}. {item.targetName}</div>
                        <div style={{ color: 'var(--text-secondary)' }}>{item.extraName || '人员预警'}</div>
                      </div>
                      <Tag color="cyan">{item.count} 起</Tag>
                    </div>
                  </List.Item>
                )}
              />
            </div>
          </Card>

          <Card className="glass-panel g-border-panel border" title="启用中的电子围栏">
            <Space wrap>
              {fences.length ? fences.map((item) => <Tag key={item.id} color="blue">{item.fenceName || item.fenceCode}</Tag>) : <span style={{ color: 'var(--text-secondary)' }}>暂无启用围栏</span>}
            </Space>
          </Card>

          <Card className="glass-panel g-border-panel border" title="分布统计">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <List
                size="small"
                header="风险等级"
                dataSource={analytics.levelBuckets}
                locale={{ emptyText: '暂无数据' }}
                renderItem={(item) => (
                  <List.Item>
                    <span>{item.label}</span>
                    <Tag>{item.count}</Tag>
                  </List.Item>
                )}
              />
              <List
                size="small"
                header="预警来源"
                dataSource={analytics.sourceBuckets}
                locale={{ emptyText: '暂无数据' }}
                renderItem={(item) => (
                  <List.Item>
                    <span>{item.label}</span>
                    <Tag>{item.count}</Tag>
                  </List.Item>
                )}
              />
            </div>
          </Card>
        </div>

      <Drawer title="预警详情" open={detailOpen} onClose={() => setDetailOpen(false)} width={640} loading={detailLoading}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="预警编号">{detail?.alertNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="标题">{detail?.title || '-'}</Descriptions.Item>
          <Descriptions.Item label="规则编码">{detail?.ruleCode || '-'}</Descriptions.Item>
          <Descriptions.Item label="对象类型">{detail?.targetType || '-'}</Descriptions.Item>
          <Descriptions.Item label="对象名称">{detail?.targetName || '-'}</Descriptions.Item>
          <Descriptions.Item label="项目 / 场地">{detail?.projectName || '-'} / {detail?.siteName || '-'}</Descriptions.Item>
          <Descriptions.Item label="车辆 / 合同 / 人员">{detail?.vehicleNo || '-'} / {detail?.contractNo || '-'} / {detail?.userName || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Space>
              <Tag color={statusColorMap[detail?.status || ''] || 'default'}>{detail?.status || '-'}</Tag>
              {detail?.level ? <Tag color={detail.level === 'L3' ? 'red' : detail.level === 'L2' ? 'orange' : 'blue'}>{detail.level}</Tag> : null}
              {detail?.isOverdue ? <Tag color="red">超时未闭环</Tag> : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="来源">{detail?.sourceChannel || '-'}</Descriptions.Item>
          <Descriptions.Item label="发生时间">{detail?.occurTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="关闭时间">{detail?.resolveTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="持续时长">{detail?.durationMinutes != null ? `${detail.durationMinutes} 分钟` : '-'}</Descriptions.Item>
          <Descriptions.Item label="处置说明">{detail?.handleRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="预警内容">{detail?.content || '-'}</Descriptions.Item>
          <Descriptions.Item label="快照">
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{formatJsonText(detail?.snapshotJson)}</pre>
          </Descriptions.Item>
          <Descriptions.Item label="位置快照">
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{formatJsonText(detail?.latestPositionJson)}</pre>
          </Descriptions.Item>
        </Descriptions>
        <div className="mt-4 flex justify-end gap-2">
          {detail && detail.status !== 'PROCESSING' && detail.status !== 'CLOSED' ? (
            <Button onClick={() => openActionModal(detail, 'PROCESSING')}>
              开始处置
            </Button>
          ) : null}
          {detail && detail.status !== 'CONFIRMED' && detail.status !== 'CLOSED' ? (
            <Button onClick={() => openActionModal(detail, 'CONFIRMED')}>
              确认预警
            </Button>
          ) : null}
          {detail && detail.status !== 'CLOSED' ? (
            <Button type="primary" onClick={() => openActionModal(detail, 'CLOSED')}>
              关闭预警
            </Button>
          ) : null}
        </div>
      </Drawer>

      <Modal
        title={actionMode === 'CLOSED' ? '关闭预警' : actionMode === 'CONFIRMED' ? '确认预警' : '开始处置'}
        open={actionModalOpen}
        onCancel={() => {
          setActionModalOpen(false);
          setActionRecord(null);
          actionForm.resetFields();
        }}
        onOk={() => void submitAction()}
        confirmLoading={actionLoading}
      >
        <Form form={actionForm} layout="vertical">
          <Form.Item label="预警对象">
            <span>{actionRecord?.title || '-'}</span>
          </Form.Item>
          <Form.Item
            name="handleRemark"
            label={actionMode === 'CLOSED' ? '关闭原因' : '处置说明'}
            rules={[{ required: true, message: '请填写说明' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入处理结果、关闭原因或研判结论" />
          </Form.Item>
        </Form>
      </Modal>
  );
};
export default AlertsMonitor;
