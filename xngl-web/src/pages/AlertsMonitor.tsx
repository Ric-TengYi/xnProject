import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Input,
  List,
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
  fetchAlertAnalytics,
  fetchAlertDetail,
  fetchAlerts,
  fetchAlertSummary,
  fetchFenceStatus,
  fetchTopRiskVehicles,
  handleAlert,
  type AlertAnalyticsRecord,
  type AlertRecord,
  type AlertSummaryRecord,
} from '../utils/alertApi';

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

const statusColorMap: Record<string, string> = {
  PENDING: 'red',
  PROCESSING: 'orange',
  CLOSED: 'default',
  CONFIRMED: 'green',
};

const emptySummary: AlertSummaryRecord = {
  total: 0,
  pending: 0,
  processing: 0,
  closed: 0,
  confirmed: 0,
  vehicleCount: 0,
  siteCount: 0,
  projectCount: 0,
  highRiskCount: 0,
  avgHandleMinutes: 0,
  overdueCount: 0,
  enabledRuleCount: 0,
  enabledFenceCount: 0,
  enabledPushCount: 0,
};

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
};

const AlertsMonitor: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [summary, setSummary] = useState<AlertSummaryRecord>(emptySummary);
  const [analytics, setAnalytics] = useState<AlertAnalyticsRecord>(emptyAnalytics);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [topRisk, setTopRisk] = useState<any[]>([]);
  const [fences, setFences] = useState<any[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<AlertRecord | null>(null);
  const [filters, setFilters] = useState({
    keyword: '',
    level: 'ALL',
    targetType: 'ALL',
    status: 'ALL',
    sourceChannel: 'ALL',
  });

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
    return params;
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [summaryData, analyticsData, alertList, topRiskList, fenceList] = await Promise.all([
        fetchAlertSummary(),
        fetchAlertAnalytics(),
        fetchAlerts(buildParams()),
        fetchTopRiskVehicles(),
        fetchFenceStatus(),
      ]);
      setSummary(summaryData);
      setAnalytics(analyticsData);
      setRecords(alertList);
      setTopRisk(topRiskList);
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
  }, [filters.keyword, filters.level, filters.targetType, filters.status, filters.sourceChannel]);

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

  const handleAction = async (action: () => Promise<void>, successText: string, id?: string) => {
    try {
      await action();
      message.success(successText);
      if (id) {
        await refreshDetail(id);
      } else {
        await loadData();
      }
    } catch (error) {
      console.error(error);
      message.error(successText.replace('成功', '失败'));
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
                onClick={() =>
                  void handleAction(
                    () => handleAlert(record.id, { status: 'PROCESSING', handleRemark: '已开始处置' }),
                    '预警已转入处置中',
                    detail?.id === record.id ? record.id : undefined,
                  )
                }
              >
                开始处置
              </Button>
            ) : null}
            {record.status !== 'CLOSED' ? (
              <Button
                type="link"
                size="small"
                onClick={() =>
                  void handleAction(
                    () => closeAlert(record.id, { handleRemark: '已关闭' }),
                    '预警已关闭',
                    detail?.id === record.id ? record.id : undefined,
                  )
                }
              >
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
      <div>
        <h1 className="text-2xl font-bold g-text-primary m-0">预警与监控中心</h1>
        <p className="g-text-secondary mt-1">补实预警规则、实例闭环和告警模型覆盖情况</p>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
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
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card className="glass-panel g-border-panel border"><Statistic title="预警总数" value={summary.total} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="待处置" value={summary.pending} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="处置中" value={summary.processing} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="高风险" value={summary.highRiskCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="超时未闭环" value={summary.overdueCount} valueStyle={{ color: '#ef4444' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="平均处置(分)" value={summary.avgHandleMinutes} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="启用规则" value={summary.enabledRuleCount} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="启用推送" value={summary.enabledPushCount} /></Card>
      </div>

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
                      <div>{index + 1}. {item.vehicleNo || '-'}</div>
                      <div style={{ color: 'var(--text-secondary)' }}>{item.fleetName || '未关联车队'}</div>
                    </div>
                    <Tag color="red">{item.count || 0} 起</Tag>
                  </div>
                </List.Item>
              )}
            />
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
      </div>

      <Drawer title="预警详情" open={detailOpen} onClose={() => setDetailOpen(false)} width={640} loading={detailLoading}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="预警编号">{detail?.alertNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="标题">{detail?.title || '-'}</Descriptions.Item>
          <Descriptions.Item label="规则编码">{detail?.ruleCode || '-'}</Descriptions.Item>
          <Descriptions.Item label="对象类型">{detail?.targetType || '-'}</Descriptions.Item>
          <Descriptions.Item label="项目 / 场地">{detail?.projectName || '-'} / {detail?.siteName || '-'}</Descriptions.Item>
          <Descriptions.Item label="车辆 / 合同">{detail?.vehicleNo || '-'} / {detail?.contractNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Space>
              <Tag color={statusColorMap[detail?.status || ''] || 'default'}>{detail?.status || '-'}</Tag>
              {detail?.level ? <Tag color={detail.level === 'L3' ? 'red' : detail.level === 'L2' ? 'orange' : 'blue'}>{detail.level}</Tag> : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="来源">{detail?.sourceChannel || '-'}</Descriptions.Item>
          <Descriptions.Item label="发生时间">{detail?.occurTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="关闭时间">{detail?.resolveTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="持续时长">{detail?.durationMinutes != null ? `${detail.durationMinutes} 分钟` : '-'}</Descriptions.Item>
          <Descriptions.Item label="处置说明">{detail?.handleRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="预警内容">{detail?.content || '-'}</Descriptions.Item>
          <Descriptions.Item label="快照">{detail?.snapshotJson || '-'}</Descriptions.Item>
          <Descriptions.Item label="位置快照">{detail?.latestPositionJson || '-'}</Descriptions.Item>
        </Descriptions>
        <div className="mt-4 flex justify-end gap-2">
          {detail && detail.status !== 'PROCESSING' && detail.status !== 'CLOSED' ? (
            <Button onClick={() => void handleAction(() => handleAlert(detail.id, { status: 'PROCESSING', handleRemark: '已开始处置' }), '预警已转入处置中', detail.id)}>
              开始处置
            </Button>
          ) : null}
          {detail && detail.status !== 'CLOSED' ? (
            <Button type="primary" onClick={() => void handleAction(() => closeAlert(detail.id, { handleRemark: '已关闭' }), '预警已关闭', detail.id)}>
              关闭预警
            </Button>
          ) : null}
        </div>
      </Drawer>
    </div>
  );
};

export default AlertsMonitor;
