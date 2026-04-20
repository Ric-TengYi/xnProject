import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, DatePicker, Empty, Input, Select, Space, Statistic, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { BarChartOutlined, DownloadOutlined } from '@ant-design/icons';
import {
  Bar,
  CartesianGrid,
  Line,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
  ComposedChart,
} from 'recharts';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import {
  downloadExportTask,
  exportSiteReport,
  fetchExportTask,
  fetchSiteReportList,
  fetchSiteReportSummary,
  fetchSiteReportTrend,
} from '../utils/reportApi';
import type { SiteReportItem, SiteReportSummary } from '../utils/reportApi';
import { fetchSites } from '../utils/siteApi';
import type { SiteRecord } from '../utils/siteApi';

const { Search } = Input;
const { RangePicker } = DatePicker;

type PeriodType = 'DAY' | 'MONTH' | 'YEAR' | 'CUSTOM';

const emptySummary: SiteReportSummary = {
  periodType: 'MONTH',
  reportPeriod: '',
  siteCount: 0,
  activeSiteCount: 0,
  totalTrips: 0,
  periodVolume: 0,
  periodAmount: 0,
  totalCapacity: 0,
  accumulatedVolume: 0,
  utilizationRate: 0,
};

const SitesReports: React.FC = () => {
  const [periodType, setPeriodType] = useState<PeriodType>('MONTH');
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [customRange, setCustomRange] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()]);
  const [siteId, setSiteId] = useState<string>();
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [summary, setSummary] = useState<SiteReportSummary>(emptySummary);
  const [records, setRecords] = useState<SiteReportItem[]>([]);
  const [trend, setTrend] = useState<{ periodLabel: string; volume: number; trips: number }[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);

  const dateValue = useMemo(() => {
    if (periodType === 'CUSTOM') {
      return undefined;
    }
    if (periodType === 'YEAR') {
      return selectedDate.startOf('year').format('YYYY-MM-DD');
    }
    if (periodType === 'MONTH') {
      return selectedDate.startOf('month').format('YYYY-MM-DD');
    }
    return selectedDate.format('YYYY-MM-DD');
  }, [periodType, selectedDate]);

  const customStartDate = useMemo(
    () => (periodType === 'CUSTOM' ? customRange[0]?.format('YYYY-MM-DD') : undefined),
    [customRange, periodType],
  );
  const customEndDate = useMemo(
    () => (periodType === 'CUSTOM' ? customRange[1]?.format('YYYY-MM-DD') : undefined),
    [customRange, periodType],
  );

  const loadData = async (searchKeyword = keyword) => {
    setLoading(true);
    try {
      const [summaryRes, listRes, trendRes, siteRes] = await Promise.all([
        fetchSiteReportSummary({
          periodType,
          date: dateValue,
          startDate: customStartDate,
          endDate: customEndDate,
          siteId,
          keyword: searchKeyword,
        }),
        fetchSiteReportList({
          periodType,
          date: dateValue,
          startDate: customStartDate,
          endDate: customEndDate,
          siteId,
          keyword: searchKeyword,
          pageNo: 1,
          pageSize: 100,
        }),
        fetchSiteReportTrend({
          periodType,
          date: dateValue,
          startDate: customStartDate,
          endDate: customEndDate,
          siteId,
          keyword: searchKeyword,
          limit: 6,
        }),
        fetchSites(),
      ]);
      setSummary(summaryRes);
      setRecords(listRes.records || []);
      setTrend((trendRes || []).map((item) => ({ periodLabel: item.periodLabel, volume: item.volume, trips: item.trips })));
      setSites(siteRes || []);
    } catch (error) {
      console.error(error);
      message.error('获取消纳场报表失败');
      setSummary(emptySummary);
      setRecords([]);
      setTrend([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [periodType, dateValue, customStartDate, customEndDate, siteId]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const result = await exportSiteReport({
        periodType,
        date: dateValue,
        startDate: customStartDate,
        endDate: customEndDate,
        siteId,
        keyword,
      });
      let task = await fetchExportTask(result.taskId);
      for (let i = 0; i < 8 && (task.status === 'PENDING' || task.status === 'PROCESSING'); i += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 400));
        task = await fetchExportTask(result.taskId);
      }
      if (task.status !== 'COMPLETED') {
        throw new Error(task.failReason || '导出任务未完成');
      }
      const blob = await downloadExportTask(result.taskId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = task.fileName || 'site_reports.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('场地报表已导出');
    } catch (error) {
      console.error(error);
      message.error(error instanceof Error ? error.message : '创建导出任务失败');
    } finally {
      setExporting(false);
    }
  };

  const columns: ColumnsType<SiteReportItem> = [
    {
      title: '场地名称',
      dataIndex: 'siteName',
      key: 'siteName',
      render: (value: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{value}</span>
          <span className="text-xs g-text-secondary">{(record.siteCode || '-') + ' / ' + (record.siteType || '-')}</span>
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
      title: '剩余容量 (方)',
      dataIndex: 'remainingCapacity',
      key: 'remainingCapacity',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '容量使用率',
      dataIndex: 'utilizationRate',
      key: 'utilizationRate',
      render: (value: number) => `${value}%`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => (
        <span className={value === '正常' ? 'g-text-success' : value === '预警' ? 'g-text-warning' : 'g-text-error'}>{value}</span>
      ),
    },
  ];

  const renderDatePicker = () => {
    if (periodType === 'CUSTOM') {
      return (
        <RangePicker
          value={customRange}
          onChange={(value) => {
            if (value?.[0] && value?.[1]) {
              setCustomRange([value[0], value[1]]);
            }
          }}
          className="bg-white g-border-panel border"
        />
      );
    }
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
          <h1 className="text-2xl font-bold g-text-primary m-0">消纳报表</h1>
          <p className="g-text-secondary mt-1">支持场地日 / 月 / 年报自动统计与自定义时间查询</p>
        </div>
        <Button icon={<DownloadOutlined />} loading={exporting} onClick={() => void handleExport()}>
          导出报表
        </Button>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap gap-3 justify-between">
          <Space wrap>
            <Select value={periodType} className="w-32" onChange={(value) => setPeriodType(value as PeriodType)} options={[
              { value: 'DAY', label: '日报' },
              { value: 'MONTH', label: '月报' },
              { value: 'YEAR', label: '年报' },
              { value: 'CUSTOM', label: '自定义' }
            ]} />
            {renderDatePicker()}
            <Select allowClear placeholder="全部场地" value={siteId} onChange={(value) => setSiteId(value)} className="w-56" options={
              sites.map(site => ({ value: String(site.id), label: site.name }))
            } />
            <Search
              placeholder="搜索场地名称/类型"
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
        <Card className="glass-panel g-border-panel border"><Statistic title="纳入统计场地" value={summary.siteCount} suffix="个" /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="活跃场地" value={summary.activeSiteCount} suffix="个" valueStyle={{ color: 'var(--success)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="本期消纳量" value={summary.periodVolume} suffix="方" valueStyle={{ color: 'var(--primary)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="容量使用率" value={summary.utilizationRate} suffix="%" valueStyle={{ color: summary.utilizationRate >= 80 ? 'var(--warning)' : 'var(--text-primary)' }} /></Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        <Card className="glass-panel g-border-panel border xl:col-span-3" title={<span className="g-text-primary"><BarChartOutlined className="mr-2" />近六期消纳趋势</span>}>
          <div className="h-80">
            {trend.length === 0 ? (
              <Empty description="暂无趋势数据" />
            ) : (
              // @ts-ignore
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
            <div className="flex justify-between"><span className="g-text-secondary">总容量</span><span className="g-text-primary">{summary.totalCapacity.toLocaleString()} 方</span></div>
          </div>
        </Card>
      </div>

      <Card className="glass-panel g-border-panel border">
        <Table
          rowKey="siteId"
          columns={columns}
          dataSource={records}
          loading={loading}
          pagination={false}
          locale={{ emptyText: <Empty description="当前筛选条件暂无场地报表数据" /> }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>
    </div>
  );
};
export default SitesReports;
