import React, { useEffect, useState } from 'react';
import { Card, Col, DatePicker, Empty, Row, Space, Statistic, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { AlertOutlined, CarOutlined, EnvironmentOutlined, ProjectOutlined, TeamOutlined } from '@ant-design/icons';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import {
  fetchDashboardOverview,
  fetchDashboardOrgAnalysis,
  fetchDashboardTrend,
  fetchProjectAlerts,
} from '../utils/reportApi';
import type { DashboardOrgItem, DashboardOverview, ProjectAlertItem } from '../utils/reportApi';

const emptyOverview: DashboardOverview = {
  reportDate: '',
  totalSites: 0,
  activeSites: 0,
  totalProjects: 0,
  activeProjects: 0,
  totalOrgs: 0,
  activeOrgs: 0,
  totalVehicles: 0,
  movingVehicles: 0,
  dailyVolume: 0,
  monthlyVolume: 0,
  warningCount: 0,
};

const Dashboard: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [loading, setLoading] = useState(false);
  const [overview, setOverview] = useState<DashboardOverview>(emptyOverview);
  const [trend, setTrend] = useState<{ periodLabel: string; volume: number; warningCount: number }[]>([]);
  const [alerts, setAlerts] = useState<ProjectAlertItem[]>([]);
  const [orgs, setOrgs] = useState<DashboardOrgItem[]>([]);

  const loadData = async (dateValue: Dayjs) => {
    setLoading(true);
    try {
      const date = dateValue.format('YYYY-MM-DD');
      const [overviewRes, trendRes, alertsRes, orgRes] = await Promise.all([
        fetchDashboardOverview({ date }),
        fetchDashboardTrend({ date, days: 7 }),
        fetchProjectAlerts({ date, limit: 6 }),
        fetchDashboardOrgAnalysis({ date, limit: 6 }),
      ]);
      setOverview(overviewRes);
      setTrend((trendRes || []).map((item) => ({ periodLabel: item.periodLabel, volume: item.volume, warningCount: item.activeCount })));
      setAlerts(alertsRes || []);
      setOrgs(orgRes || []);
    } catch (error) {
      console.error(error);
      message.error('获取总体分析失败');
      setOverview(emptyOverview);
      setTrend([]);
      setAlerts([]);
      setOrgs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData(selectedDate);
  }, [selectedDate]);

  const columns: ColumnsType<ProjectAlertItem> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (value: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{value}</span>
          <span className="text-xs g-text-secondary">关联场地: {record.siteName || '-'}</span>
        </div>
      ),
    },
    {
      title: '进度',
      dataIndex: 'progressPercent',
      key: 'progressPercent',
      render: (value: number) => <Tag color={value >= 80 ? 'green' : value >= 50 ? 'gold' : 'red'}>{value}%</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => <span className={value === '预警' ? 'g-text-error' : 'g-text-primary'}>{value}</span>,
    },
    {
      title: '预警等级',
      dataIndex: 'warningLevel',
      key: 'warningLevel',
      render: (value: number) => <Tag color={value >= 3 ? 'red' : value >= 2 ? 'orange' : 'blue'}>L{value}</Tag>,
    },
  ];

  const orgColumns: ColumnsType<DashboardOrgItem> = [
    {
      title: '单位',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{value}</span>
          <span className="text-xs g-text-secondary">排名 #{record.rank || '-'}</span>
        </div>
      ),
    },
    {
      title: '活跃项目',
      dataIndex: 'activeProjectCount',
      key: 'activeProjectCount',
      render: (value: number) => `${value} 个`,
    },
    {
      title: '在运车辆',
      dataIndex: 'movingVehicles',
      key: 'movingVehicles',
      render: (value: number, record) => `${value} / ${record.totalVehicles}`,
    },
    {
      title: '累计消纳',
      dataIndex: 'volume',
      key: 'volume',
      render: (value: number) => `${value.toLocaleString()} 方`,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h1 className="text-2xl font-bold m-0 g-text-primary">总体分析</h1>
          <div className="text-sm g-text-secondary mt-1">统计日期: {overview.reportDate || selectedDate.format('YYYY-MM-DD')}</div>
        </div>
        <Space>
          <DatePicker value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />
        </Space>
      </div>
      <Row gutter={[24, 24]}>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel overflow-hidden relative border-l-4" style={{ borderLeftColor: 'var(--primary)' }} loading={loading}>
            <Statistic title="当日消纳总量" value={overview.dailyVolume} suffix="方" valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<EnvironmentOutlined className="g-text-primary-link" />} />
            <div className="mt-2 text-xs g-text-secondary">本月累计 {overview.monthlyVolume.toLocaleString()} 方</div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel overflow-hidden relative border-l-4" style={{ borderLeftColor: 'var(--success)' }} loading={loading}>
            <Statistic title="活跃消纳场地" value={overview.activeSites} suffix={'/ ' + overview.totalSites} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<EnvironmentOutlined className="g-text-success" />} />
            <div className="mt-2 text-xs g-text-secondary">场地在线率 {(overview.totalSites > 0 ? Math.round((overview.activeSites / overview.totalSites) * 100) : 0)}%</div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel overflow-hidden relative border-l-4" style={{ borderLeftColor: 'var(--warning)' }} loading={loading}>
            <Statistic title="活跃项目" value={overview.activeProjects} suffix={'/ ' + overview.totalProjects} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<ProjectOutlined className="g-text-warning" />} />
            <div className="mt-2 text-xs g-text-secondary">运输车辆 {overview.movingVehicles.toLocaleString()} 台</div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel overflow-hidden relative border-l-4" style={{ borderLeftColor: 'var(--primary)' }} loading={loading}>
            <Statistic title="活跃单位" value={overview.activeOrgs} suffix={'/ ' + overview.totalOrgs} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<TeamOutlined className="g-text-primary-link" />} />
            <div className="mt-2 text-xs g-text-secondary">单位活跃率 {(overview.totalOrgs > 0 ? Math.round((overview.activeOrgs / overview.totalOrgs) * 100) : 0)}%</div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel overflow-hidden relative border-l-4" style={{ borderLeftColor: 'var(--error)' }} loading={loading}>
            <Statistic title="综合预警数" value={overview.warningCount} valueStyle={{ color: 'var(--error)', fontWeight: 'bold' }} prefix={<AlertOutlined className="g-text-error" />} />
            <div className="mt-2 text-xs g-text-secondary">车辆总量 {overview.totalVehicles.toLocaleString()} 台</div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col xs={24} xl={14}>
          <Card title={<span className="g-text-primary">近七日消纳趋势</span>} className="glass-panel h-[400px]" headStyle={{ borderBottom: '1px solid var(--border-color)' }} loading={loading}>
            <div className="h-[300px] w-full">
              {trend.length === 0 ? (
                <Empty description="暂无总体趋势数据" />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trend} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="dashboardVolume" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.8} />
                        <stop offset="95%" stopColor="var(--primary)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.08)" vertical={false} />
                    <XAxis dataKey="periodLabel" stroke="var(--text-secondary)" />
                    <YAxis stroke="var(--text-secondary)" />
                    <RechartsTooltip />
                    <Area type="monotone" dataKey="volume" stroke="var(--primary)" fillOpacity={1} fill="url(#dashboardVolume)" />
                    <Area type="monotone" dataKey="warningCount" stroke="var(--warning)" fillOpacity={0.15} fill="var(--warning)" />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} xl={10}>
          <Card title={<span className="g-text-primary">重点项目告警与进度</span>} className="glass-panel h-[400px]" headStyle={{ borderBottom: '1px solid var(--border-color)' }} bodyStyle={{ padding: 0 }} loading={loading}>
            <Table columns={columns} dataSource={alerts} rowKey="projectId" pagination={false} locale={{ emptyText: <Empty description="暂无项目预警数据" /> }} className="bg-transparent" rowClassName="hover:bg-[#fafafa]" />
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col xs={24} xl={10}>
          <Card title={<span className="g-text-primary">重点单位运营概览</span>} className="glass-panel h-[360px]" headStyle={{ borderBottom: '1px solid var(--border-color)' }} bodyStyle={{ padding: 0 }} loading={loading}>
            <Table columns={orgColumns} dataSource={orgs} rowKey="orgId" pagination={false} locale={{ emptyText: <Empty description="暂无单位分析数据" /> }} className="bg-transparent" rowClassName="hover:bg-[#fafafa]" />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border" loading={loading}>
            <Statistic title="场地活跃率" value={overview.totalSites > 0 ? Math.round((overview.activeSites / overview.totalSites) * 100) : 0} suffix="%" prefix={<EnvironmentOutlined className="g-text-primary-link" />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={5}>
          <Card className="glass-panel g-border-panel border" loading={loading}>
            <Statistic title="项目开工率" value={overview.totalProjects > 0 ? Math.round((overview.activeProjects / overview.totalProjects) * 100) : 0} suffix="%" prefix={<ProjectOutlined className="g-text-success" />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={5}>
          <Card className="glass-panel g-border-panel border" loading={loading}>
            <Statistic title="单位活跃率" value={overview.totalOrgs > 0 ? Math.round((overview.activeOrgs / overview.totalOrgs) * 100) : 0} suffix="%" prefix={<TeamOutlined className="g-text-primary-link" />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={4}>
          <Card className="glass-panel g-border-panel border" loading={loading}>
            <Statistic title="车辆在线率" value={overview.totalVehicles > 0 ? Math.round((overview.movingVehicles / overview.totalVehicles) * 100) : 0} suffix="%" prefix={<CarOutlined className="g-text-warning" />} />
          </Card>
        </Col>
      </Row>
    </div>
    </div>
  );
};

export default Dashboard;
