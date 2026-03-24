import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, DatePicker, Empty, Input, Progress, Select, Space, Statistic, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { BarChartOutlined, DownloadOutlined } from '@ant-design/icons';
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import {
  exportProjectReport,
  fetchProjectViolationAnalysis,
  fetchProjectReportList,
  fetchProjectReportSummary,
  fetchProjectReportTrend,
  type ProjectViolationAnalysis,
  type ProjectViolationStatItem,
  type ProjectReportItem,
  type ProjectReportSummary,
} from '../utils/reportApi';
import { fetchProjects } from '../utils/projectApi';
import type { ProjectRecord } from '../utils/projectApi';

const { Search } = Input;
const { Option } = Select;

type PeriodType = 'DAY' | 'MONTH' | 'YEAR';

const emptySummary: ProjectReportSummary = {
  periodType: 'MONTH',
  reportPeriod: '',
  projectCount: 0,
  activeProjectCount: 0,
  totalTrips: 0,
  periodVolume: 0,
  periodAmount: 0,
  projectTotal: 0,
  accumulatedVolume: 0,
  progressPercent: 0,
};

const emptyViolationAnalysis: ProjectViolationAnalysis = {
  summary: {
    reportPeriod: '',
    totalViolations: 0,
    handledCount: 0,
    pendingCount: 0,
    vehicleCount: 0,
    fleetCount: 0,
    teamCount: 0,
  },
  byFleet: [],
  byPlate: [],
  byTeam: [],
};

const ProjectsReports: React.FC = () => {
  const [periodType, setPeriodType] = useState<PeriodType>('MONTH');
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [projectId, setProjectId] = useState<string>();
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [summary, setSummary] = useState<ProjectReportSummary>(emptySummary);
  const [records, setRecords] = useState<ProjectReportItem[]>([]);
  const [violationAnalysis, setViolationAnalysis] = useState<ProjectViolationAnalysis>(emptyViolationAnalysis);
  const [trend, setTrend] = useState<{ periodLabel: string; volume: number; trips: number }[]>([]);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);

  const dateValue = useMemo(() => {
    if (periodType === 'YEAR') {
      return selectedDate.startOf('year').format('YYYY-MM-DD');
    }
    if (periodType === 'MONTH') {
      return selectedDate.startOf('month').format('YYYY-MM-DD');
    }
    return selectedDate.format('YYYY-MM-DD');
  }, [periodType, selectedDate]);

  const loadData = async (searchKeyword = keyword) => {
    setLoading(true);
    try {
      const [summaryRes, listRes, trendRes, violationRes, projectRes] = await Promise.all([
        fetchProjectReportSummary({ periodType, date: dateValue, projectId, keyword: searchKeyword }),
        fetchProjectReportList({ periodType, date: dateValue, projectId, keyword: searchKeyword, pageNo: 1, pageSize: 100 }),
        fetchProjectReportTrend({ periodType, date: dateValue, projectId, keyword: searchKeyword, limit: 6 }),
        fetchProjectViolationAnalysis({ periodType, date: dateValue, keyword: searchKeyword }),
        fetchProjects({ pageNo: 1, pageSize: 200 }),
      ]);
      setSummary(summaryRes);
      setRecords(listRes.records || []);
      setTrend((trendRes || []).map((item) => ({ periodLabel: item.periodLabel, volume: item.volume, trips: item.trips })));
      setViolationAnalysis(violationRes);
      setProjects(projectRes.records || []);
    } catch (error) {
      console.error(error);
      message.error('获取项目报表失败');
      setSummary(emptySummary);
      setRecords([]);
      setTrend([]);
      setViolationAnalysis(emptyViolationAnalysis);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [periodType, dateValue, projectId]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const result = await exportProjectReport({ periodType, date: dateValue, projectId, keyword });
      message.success('已生成项目报表导出任务 #' + result.taskId);
    } catch (error) {
      console.error(error);
      message.error('创建导出任务失败');
    } finally {
      setExporting(false);
    }
  };

  const columns: ColumnsType<ProjectReportItem> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (value: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{value}</span>
          <span className="text-xs g-text-secondary">{(record.projectCode || '-') + ' / ' + (record.orgName || '-')}</span>
        </div>
      ),
    },
    { title: '统计周期', dataIndex: 'reportPeriod', key: 'reportPeriod' },
    {
      title: '本期消纳量 (方)',
      dataIndex: 'periodVolume',
      key: 'periodVolume',
      render: (value: number) => <span className="g-text-success font-semibold">{value.toLocaleString()}</span>,
    },
    {
      title: '本期趟次',
      dataIndex: 'periodTrips',
      key: 'periodTrips',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '累计消纳量 (方)',
      dataIndex: 'accumulatedVolume',
      key: 'accumulatedVolume',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '剩余工程量 (方)',
      dataIndex: 'remainingVolume',
      key: 'remainingVolume',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '整体进度',
      dataIndex: 'progressPercent',
      key: 'progressPercent',
      render: (value: number) => (
        <Progress
          percent={value}
          size="small"
          strokeColor={value >= 80 ? 'var(--success)' : value >= 50 ? 'var(--warning)' : 'var(--primary)'}
          trailColor="rgba(0,0,0,0.06)"
        />
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => (
        <span className={value === '在建' ? 'g-text-success' : value === '预警' ? 'g-text-warning' : 'g-text-primary'}>{value}</span>
      ),
    },
  ];

  const violationColumns: ColumnsType<ProjectViolationStatItem> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '违规次数', dataIndex: 'violationCount', key: 'violationCount' },
    { title: '已处理', dataIndex: 'handledCount', key: 'handledCount' },
    { title: '待处理', dataIndex: 'pendingCount', key: 'pendingCount' },
    { title: '最近触发', dataIndex: 'latestTriggerTime', key: 'latestTriggerTime', render: (value?: string | null) => value || '-' },
  ];

  const renderDatePicker = () => {
    if (periodType === 'YEAR') {
      return <DatePicker picker="year" value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
    }
    if (periodType === 'MONTH') {
      return <DatePicker picker="month" value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
    }
    return <DatePicker value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">项目报表</h1>
          <p className="g-text-secondary mt-1">支持项目日 / 月 / 年统计、趋势分析与导出</p>
        </div>
        <Button icon={<DownloadOutlined />} loading={exporting} onClick={() => void handleExport()}>
          导出报表
        </Button>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap gap-3 justify-between">
          <Space wrap>
            <Select value={periodType} className="w-32" onChange={(value) => setPeriodType(value)}>
              <Option value="DAY">日报</Option>
              <Option value="MONTH">月报</Option>
              <Option value="YEAR">年报</Option>
            </Select>
            {renderDatePicker()}
            <Select allowClear placeholder="全部项目" value={projectId} onChange={(value) => setProjectId(value)} className="w-64">
              {projects.map((project) => (
                <Option key={project.id} value={project.id}>{project.name}</Option>
              ))}
            </Select>
            <Search
              placeholder="搜索项目名称/编码/单位"
              allowClear
              className="w-64"
              enterButton="查询"
              onSearch={(value) => {
                const nextKeyword = value.trim();
                setKeyword(nextKeyword);
                void loadData(nextKeyword);
              }}
            />
          </Space>
          <Button type="primary" onClick={() => void loadData()}>刷新统计</Button>
        </div>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="纳入统计项目" value={summary.projectCount} suffix="个" /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="活跃项目" value={summary.activeProjectCount} suffix="个" valueStyle={{ color: 'var(--success)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="本期消纳量" value={summary.periodVolume} suffix="方" valueStyle={{ color: 'var(--primary)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="累计完成率" value={summary.progressPercent} suffix="%" valueStyle={{ color: summary.progressPercent >= 80 ? 'var(--success)' : 'var(--text-primary)' }} /></Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        <Card className="glass-panel g-border-panel border xl:col-span-3" title={<span className="g-text-primary"><BarChartOutlined className="mr-2" />近六期项目趋势</span>}>
          <div className="h-80">
            {trend.length === 0 ? (
              <Empty description="暂无趋势数据" />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.08)" vertical={false} />
                  <XAxis dataKey="periodLabel" stroke="var(--text-secondary)" />
                  <YAxis yAxisId="left" stroke="var(--text-secondary)" />
                  <YAxis yAxisId="right" orientation="right" stroke="var(--text-secondary)" />
                  <RechartsTooltip />
                  <Bar yAxisId="left" dataKey="volume" fill="var(--primary)" radius={[6, 6, 0, 0]} />
                  <Line yAxisId="right" type="monotone" dataKey="trips" stroke="var(--success)" strokeWidth={2} />
                </ComposedChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>

        <Card className="glass-panel g-border-panel border xl:col-span-2" title="统计摘要" loading={loading}>
          <div className="space-y-4 text-sm">
            <div className="flex justify-between"><span className="g-text-secondary">统计周期</span><span className="g-text-primary">{summary.reportPeriod || '-'}</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">本期运输趟次</span><span className="g-text-primary-link">{summary.totalTrips.toLocaleString()}</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">本期结算金额</span><span className="g-text-primary">¥ {summary.periodAmount.toLocaleString()}</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">累计消纳量</span><span className="g-text-primary">{summary.accumulatedVolume.toLocaleString()} 方</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">项目总工程量</span><span className="g-text-primary">{summary.projectTotal.toLocaleString()} 方</span></div>
          </div>
        </Card>
      </div>

      <Card className="glass-panel g-border-panel border">
        <Table
          rowKey="projectId"
          columns={columns}
          dataSource={records}
          loading={loading}
          pagination={false}
          locale={{ emptyText: <Empty description="当前筛选条件暂无项目报表数据" /> }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>

      <div className="space-y-4">
        <Card className="glass-panel g-border-panel border" title="违法统计">
          <div className="mb-3 text-sm g-text-secondary">
            当前口径基于平台违规运输记录，按所属车队、车辆车牌、处理中队聚合统计；与项目时间筛选保持一致。
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
            <Card className="bg-white border-0 shadow-none"><Statistic title="违规总数" value={violationAnalysis.summary.totalViolations} /></Card>
            <Card className="bg-white border-0 shadow-none"><Statistic title="已处理" value={violationAnalysis.summary.handledCount} valueStyle={{ color: 'var(--success)' }} /></Card>
            <Card className="bg-white border-0 shadow-none"><Statistic title="待处理" value={violationAnalysis.summary.pendingCount} valueStyle={{ color: 'var(--warning)' }} /></Card>
            <Card className="bg-white border-0 shadow-none"><Statistic title="覆盖车辆 / 车队 / 中队" value={`${violationAnalysis.summary.vehicleCount} / ${violationAnalysis.summary.fleetCount} / ${violationAnalysis.summary.teamCount}`} /></Card>
          </div>
        </Card>

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          <Card className="glass-panel g-border-panel border" title="按所属车队统计">
            <Table<ProjectViolationStatItem>
              rowKey="name"
              columns={violationColumns}
              dataSource={violationAnalysis.byFleet}
              pagination={false}
              size="small"
              locale={{ emptyText: <Empty description="当前周期暂无车队违规数据" /> }}
            />
          </Card>
          <Card className="glass-panel g-border-panel border" title="按车辆车牌统计">
            <Table<ProjectViolationStatItem>
              rowKey="name"
              columns={violationColumns}
              dataSource={violationAnalysis.byPlate}
              pagination={false}
              size="small"
              locale={{ emptyText: <Empty description="当前周期暂无车辆违规数据" /> }}
            />
          </Card>
          <Card className="glass-panel g-border-panel border" title="按处理中队统计">
            <Table<ProjectViolationStatItem>
              rowKey="name"
              columns={violationColumns}
              dataSource={violationAnalysis.byTeam}
              pagination={false}
              size="small"
              locale={{ emptyText: <Empty description="当前周期暂无处理中队数据" /> }}
            />
          </Card>
        </div>
      </div>
    </div>
  );
};

export default ProjectsReports;
