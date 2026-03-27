import React, { useEffect, useMemo, useState } from 'react';
import { Card, DatePicker, Empty, Input, Select, Space, Statistic, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CarOutlined, ThunderboltOutlined } from '@ant-design/icons';
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
  fetchVehicleCapacityAnalysis,
} from '../utils/reportApi';
import type {
  VehicleCapacityAnalysis as VehicleCapacityAnalysisData,
  VehicleCapacityItem,
} from '../utils/reportApi';

const { Search } = Input;
const { Option } = Select;

type PeriodType = 'DAY' | 'MONTH' | 'YEAR';

const emptyAnalysis: VehicleCapacityAnalysisData = {
  summary: {
    periodType: 'MONTH',
    reportPeriod: '',
    totalVehicles: 0,
    activeVehicles: 0,
    averageVolume: 0,
    loadedMileage: 0,
    emptyMileage: 0,
    energyConsumption: 0,
  },
  trend: [],
  records: [],

const VehicleCapacityAnalysis: React.FC = () => {
  const [periodType, setPeriodType] = useState<PeriodType>('MONTH');
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState<VehicleCapacityAnalysisData>(emptyAnalysis);

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
      const result = await fetchVehicleCapacityAnalysis({ periodType, date: dateValue, keyword: searchKeyword });
      setAnalysis(result);
    } catch (error) {
      console.error(error);
      message.error('获取运力分析失败');
      setAnalysis(emptyAnalysis);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [periodType, dateValue]);

  const summaryLabel = periodType === 'DAY' ? '日均运载量' : periodType === 'YEAR' ? '年均运载量' : '月均运载量';

  const columns: ColumnsType<VehicleCapacityItem> = [
    {
      title: '车辆',
      dataIndex: 'plateNo',
      key: 'plateNo',
      render: (value: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{value}</span>
          <span className="text-xs g-text-secondary">{record.orgName} / {record.fleetName}</span>
        </div>
      ),
    },
    { title: '能源类型', dataIndex: 'energyType', key: 'energyType' },
    { title: '状态', dataIndex: 'statusLabel', key: 'statusLabel' },
    {
      title: summaryLabel,
      dataIndex: 'averageVolume',
      key: 'averageVolume',
      render: (value: number) => <span className="g-text-primary font-semibold">{value.toLocaleString()} 方</span>,
    },
    {
      title: '荷载里程',
      dataIndex: 'loadedMileage',
      key: 'loadedMileage',
      render: (value: number) => `${value.toLocaleString()} km`,
    },
    {
      title: '空载里程',
      dataIndex: 'emptyMileage',
      key: 'emptyMileage',
      render: (value: number) => `${value.toLocaleString()} km`,
    },
    {
      title: '运营能耗',
      dataIndex: 'energyConsumption',
      key: 'energyConsumption',
      render: (value: number) => value.toLocaleString(),
    },
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
          <h1 className="text-2xl font-bold g-text-primary m-0">运力分析</h1>
          <p className="g-text-secondary mt-1">按日 / 月 / 年分析车辆运载量、里程与运营能耗</p>
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
            <Search
              placeholder="搜索车牌/单位/车队"
              allowClear
              enterButton="查询"
              className="w-72"
              onSearch={(value) => {
                setKeyword(value.trim());
                void loadData(value.trim());
              }}
            />
          </Space>
        </div>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="纳入分析车辆" value={analysis.summary.totalVehicles} prefix={<CarOutlined />} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="活跃车辆" value={analysis.summary.activeVehicles} valueStyle={{ color: 'var(--success)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title={summaryLabel} value={analysis.summary.averageVolume} suffix="方" valueStyle={{ color: 'var(--primary)' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="运营能耗" value={analysis.summary.energyConsumption} prefix={<ThunderboltOutlined />} /></Card>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        <Card className="glass-panel g-border-panel border xl:col-span-3" title="运力变化趋势" loading={loading}>
          <div className="h-80">
            {analysis.trend.length === 0 ? (
              <Empty description="暂无运力趋势数据" />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={analysis.trend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.08)" vertical={false} />
                  <XAxis dataKey="periodLabel" stroke="var(--text-secondary)" />
                  <YAxis yAxisId="left" stroke="var(--text-secondary)" />
                  <YAxis yAxisId="right" orientation="right" stroke="var(--text-secondary)" />
                  <RechartsTooltip />
                  <Bar yAxisId="left" dataKey="volume" fill="var(--primary)" radius={[6, 6, 0, 0]} />
                  <Line yAxisId="right" type="monotone" dataKey="amount" stroke="var(--warning)" strokeWidth={2} />
                </ComposedChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>
        <Card className="glass-panel g-border-panel border xl:col-span-2" title="统计摘要" loading={loading}>
          <div className="space-y-4 text-sm">
            <div className="flex justify-between"><span className="g-text-secondary">统计周期</span><span className="g-text-primary">{analysis.summary.reportPeriod || '-'}</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">荷载里程</span><span className="g-text-primary-link">{analysis.summary.loadedMileage.toLocaleString()} km</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">空载里程</span><span className="g-text-primary">{analysis.summary.emptyMileage.toLocaleString()} km</span></div>
            <div className="flex justify-between"><span className="g-text-secondary">单车平均运量</span><span className="g-text-primary">{analysis.summary.averageVolume.toLocaleString()} 方</span></div>
          </div>
        </Card>

      <Card className="glass-panel g-border-panel border">
        <Table
          rowKey="vehicleId"
          columns={columns}
          dataSource={analysis.records}
          loading={loading}
          pagination={{ pageSize: 10 }}
          locale={{ emptyText: <Empty description="当前条件暂无运力分析数据" /> }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>
    </div>
    </div>
  );
};
export default VehicleCapacityAnalysis;
