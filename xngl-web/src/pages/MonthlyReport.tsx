import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, DatePicker, Select, Row, Col, Statistic, Empty, Spin, message } from 'antd';
import { DownloadOutlined, BarChartOutlined, LineChartOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from 'recharts';
import dayjs, { Dayjs } from 'dayjs';
import { fetchMonthlySummary, fetchMonthlyTrend, fetchMonthlyTypes } from '../utils/contractApi';
import { useTheme } from '../contexts/ThemeContext';

const { MonthPicker } = DatePicker;
const { Option } = Select;

interface SummaryState {
  month: string;
  contractCount: number;
  newContractCount: number;
  contractAmount: number;
  receiptAmount: number;
  settlementAmount: number;
  agreedVolume: number;
  actualVolume: number;
}

interface TrendState {
  month: string;
  volume: number;
  amount: number;
  receiptAmount: number;
}

interface TypeState {
  contractType: string;
  count: number;
  amount: number;
  volume: number;
}

const ZERO_SUMMARY: SummaryState = {
  month: '',
  contractCount: 0,
  newContractCount: 0,
  contractAmount: 0,
  receiptAmount: 0,
  settlementAmount: 0,
  agreedVolume: 0,
  actualVolume: 0,
};

const MonthlyReport: React.FC = () => {
  const { isDarkMode } = useTheme();
  const [mode, setMode] = useState('natural');
  const [selectedMonth, setSelectedMonth] = useState<Dayjs>(dayjs());
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<SummaryState>(ZERO_SUMMARY);
  const [trend, setTrend] = useState<TrendState[]>([]);
  const [types, setTypes] = useState<TypeState[]>([]);

  const monthValue = selectedMonth.format('YYYY-MM');

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [summaryData, trendData, typeData] = await Promise.all([
          fetchMonthlySummary(monthValue),
          fetchMonthlyTrend(12),
          fetchMonthlyTypes(monthValue),
        ]);

        setSummary({
          month: summaryData?.month || monthValue,
          contractCount: Number(summaryData?.contractCount || 0),
          newContractCount: Number(summaryData?.newContractCount || 0),
          contractAmount: Number(summaryData?.contractAmount || 0),
          receiptAmount: Number(summaryData?.receiptAmount || 0),
          settlementAmount: Number(summaryData?.settlementAmount || 0),
          agreedVolume: Number(summaryData?.agreedVolume || 0),
          actualVolume: Number(summaryData?.actualVolume || 0),
        });
        setTrend(
          Array.isArray(trendData)
            ? trendData.map((item) => ({
                month: item.month,
                volume: Number(item.volume || 0),
                amount: Number(item.amount || 0),
                receiptAmount: Number(item.receiptAmount || 0),
              }))
            : []
        );
        setTypes(
          Array.isArray(typeData)
            ? typeData.map((item) => ({
                contractType: item.contractType || '-',
                count: Number(item.count || 0),
                amount: Number(item.amount || 0),
                volume: Number(item.volume || 0),
              }))
            : []
        );
      } catch (error) {
        console.error(error);
        message.error('获取合同月报失败');
        setSummary({ ...ZERO_SUMMARY, month: monthValue });
        setTrend([]);
        setTypes([]);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [monthValue]);

  const chartData = useMemo(() => {
    const sorted = [...trend].sort((a, b) => a.month.localeCompare(b.month));
    const filtered = sorted.filter((item) => item.month <= monthValue);
    return (filtered.length > 0 ? filtered : sorted).slice(-6);
  }, [monthValue, trend]);

  const columns = [
    {
      title: '合同类型',
      dataIndex: 'contractType',
      key: 'contractType',
      render: (value: string) => <strong className="g-text-primary">{value}</strong>,
    },
    {
      title: '本月新增合同数',
      dataIndex: 'count',
      key: 'count',
      render: (value: number) => <span className="g-text-primary-link">{value}</span>,
    },
    {
      title: '涉及总金额(元)',
      dataIndex: 'amount',
      key: 'amount',
      render: (value: number) => <span className="g-text-secondary">¥ {value.toLocaleString()}</span>,
    },
    {
      title: '涉及总方量(m³)',
      dataIndex: 'volume',
      key: 'volume',
      render: (value: number) => (
        <span className="g-text-secondary">{value === 0 ? '--' : value.toLocaleString()}</span>
      ),
    },
  ];

  const renderChartEmpty = (description: string) => (
    <div className="h-full flex items-center justify-center">
      <Empty description={description} />
    </div>
  );

  const receiptRate =
    summary.contractAmount > 0
      ? ((summary.receiptAmount / summary.contractAmount) * 100).toFixed(2)
      : '0.00';

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">月报统计</h1>
          <p className="g-text-secondary mt-1">财务月度对账与合同数据综合统计分析</p>
        </div>
        <div className="flex gap-4">
          <Select value={mode} className="w-32" popupClassName="bg-white" onChange={setMode}>
            <Option value="natural">自然月结算</Option>
            <Option value="custom">自定义结算日</Option>
          </Select>
          <MonthPicker
            value={selectedMonth}
            onChange={(value) => setSelectedMonth(value || dayjs())}
            placeholder="选择统计月份"
            className="bg-white g-border-panel border g-text-primary"
          />
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]"
          >
            导出月报
          </Button>
        </div>

      <Spin spinning={loading}>
        <Row gutter={[24, 24]}>
          <Col span={6}>
            <Card className="glass-panel g-border-panel border">
              <Statistic
                title={<span className="g-text-secondary">本月合同总数</span>}
                value={summary.contractCount}
                valueStyle={{ color: 'var(--text-primary)' }}
              />
              <div className="mt-2 text-xs g-text-success">本月新增 {summary.newContractCount} 份</div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel g-border-panel border">
              <Statistic
                title={<span className="g-text-secondary">本月合同总金额 (元)</span>}
                value={summary.contractAmount}
                valueStyle={{ color: 'var(--primary)' }}
              />
              <div className="mt-2 text-xs g-text-secondary">统计月份 {summary.month || monthValue}</div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel g-border-panel border">
              <Statistic
                title={<span className="g-text-secondary">本月已入账金额 (元)</span>}
                value={summary.receiptAmount}
                valueStyle={{ color: 'var(--success)' }}
              />
              <div className="mt-2 text-xs g-text-secondary">入账率 {receiptRate}%</div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel g-border-panel border">
              <Statistic
                title={<span className="g-text-secondary">本月应收余额 (元)</span>}
                value={Math.max(summary.contractAmount - summary.receiptAmount, 0)}
                valueStyle={{ color: 'var(--warning)' }}
              />
              <div className="mt-2 text-xs g-text-secondary">
                {mode === 'custom' ? '自定义结算日模式' : '自然月结算模式'}
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={[24, 24]}>
          <Col span={12}>
            <Card
              title={
                <span className="g-text-primary">
                  <BarChartOutlined className="mr-2" />
                  近半年消纳量趋势 (m³)
                </span>
              }
              className="glass-panel g-border-panel border h-96"
            >
              {chartData.length === 0 ? (
                renderChartEmpty('暂无趋势数据')
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}
                      vertical={false}
                    />
                    <XAxis
                      dataKey="month"
                      stroke={isDarkMode ? '#94a3b8' : '#64748b'}
                      tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }}
                    />
                    <YAxis
                      stroke={isDarkMode ? '#94a3b8' : '#64748b'}
                      tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }}
                    />
                    <RechartsTooltip
                      contentStyle={{
                        backgroundColor: isDarkMode
                          ? 'rgba(15, 23, 42, 0.9)'
                          : 'rgba(255, 255, 255, 0.9)',
                        borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
                        color: isDarkMode ? '#fff' : '#000',
                      }}
                      itemStyle={{ color: 'var(--primary)' }}
                    />
                    <Bar
                      dataKey="volume"
                      name="消纳量"
                      fill="var(--primary)"
                      radius={[4, 4, 0, 0]}
                      barSize={30}
                    />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card>
          </Col>
          <Col span={12}>
            <Card
              title={
                <span className="g-text-primary">
                  <LineChartOutlined className="mr-2" />
                  近半年结算金额趋势 (元)
                </span>
              }
              className="glass-panel g-border-panel border h-96"
            >
              {chartData.length === 0 ? (
                renderChartEmpty('暂无趋势数据')
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}
                      vertical={false}
                    />
                    <XAxis
                      dataKey="month"
                      stroke={isDarkMode ? '#94a3b8' : '#64748b'}
                      tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }}
                    />
                    <YAxis
                      stroke={isDarkMode ? '#94a3b8' : '#64748b'}
                      tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }}
                    />
                    <RechartsTooltip
                      contentStyle={{
                        backgroundColor: isDarkMode
                          ? 'rgba(15, 23, 42, 0.9)'
                          : 'rgba(255, 255, 255, 0.9)',
                        borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
                        color: isDarkMode ? '#fff' : '#000',
                      }}
                      itemStyle={{ color: 'var(--success)' }}
                    />
                    <Line
                      type="monotone"
                      dataKey="amount"
                      name="结算金额"
                      stroke="var(--success)"
                      strokeWidth={3}
                      dot={{
                        r: 4,
                        fill: 'var(--success)',
                        strokeWidth: 2,
                        stroke: 'var(--bg-panel)',
                      }}
                      activeDot={{ r: 6 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Card>
          </Col>
        </Row>

        <Card title="各类型合同统计明细" className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
          <Table
            columns={columns}
            dataSource={types}
            rowKey="contractType"
            locale={{ emptyText: <Empty description="暂无类型统计数据" /> }}
            pagination={false}
            className="bg-transparent"
            rowClassName="hover:bg-white transition-colors"
          />
        </Card>
      </Spin>
    </motion.div>
    </div>
  );
};

export default MonthlyReport;
