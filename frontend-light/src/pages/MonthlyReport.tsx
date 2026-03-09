import React from 'react';
import { Card, Table, Button, DatePicker, Select, Row, Col, Statistic } from 'antd';
import { DownloadOutlined, BarChartOutlined, LineChartOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, LineChart, Line } from 'recharts';

const { MonthPicker } = DatePicker;
const { Option } = Select;

const monthlyData = [
    { month: '2023-10', volume: 120000, amount: 3000000 },
    { month: '2023-11', volume: 150000, amount: 3750000 },
    { month: '2023-12', volume: 180000, amount: 4500000 },
    { month: '2024-01', volume: 140000, amount: 3500000 },
    { month: '2024-02', volume: 160000, amount: 4000000 },
    { month: '2024-03', volume: 90000, amount: 2250000 }, // 当月未完
];

const contractTypeData = [
    { type: '正常合同', count: 45, amount: 15000000, volume: 600000 },
    { type: '三方合同', count: 12, amount: 3500000, volume: 140000 },
    { type: '租赁合同', count: 8, amount: 1200000, volume: 0 },
    { type: '用工合同', count: 5, amount: 800000, volume: 0 },
];

import { useTheme } from '../contexts/ThemeContext';

const MonthlyReport: React.FC = () => {
    const { isDarkMode } = useTheme();

    const columns = [
        { title: '合同类型', dataIndex: 'type', key: 'type', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '本月新增合同数', dataIndex: 'count', key: 'count', render: (t: number) => <span className="g-text-primary-link">{t}</span> },
        { title: '涉及总金额(元)', dataIndex: 'amount', key: 'amount', render: (t: number) => <span className="g-text-secondary">¥ {t.toLocaleString()}</span> },
        { title: '涉及总方量(m³)', dataIndex: 'volume', key: 'volume', render: (t: number) => <span className="g-text-secondary">{t === 0 ? '--' : t.toLocaleString()}</span> },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">月报统计</h1>
                    <p className="g-text-secondary mt-1">财务月度对账与合同数据综合统计分析</p>
                </div>
                <div className="flex gap-4">
                    <Select defaultValue="natural" className="w-32" popupClassName="bg-white">
                        <Option value="natural">自然月结算</Option>
                        <Option value="custom">自定义结算日</Option>
                    </Select>
                    <MonthPicker placeholder="选择统计月份" className="bg-white g-border-panel border g-text-primary" />
                    <Button type="primary" icon={<DownloadOutlined />} className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]">
                        导出月报
                    </Button>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月合同总数</span>} value={70} valueStyle={{ color: 'var(--text-primary)' }} />
                        <div className="mt-2 text-xs g-text-success">较上月 +12%</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月合同总金额 (元)</span>} value={20500000} valueStyle={{ color: 'var(--primary)' }} />
                        <div className="mt-2 text-xs g-text-success">较上月 +8%</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月已入账金额 (元)</span>} value={1425000} valueStyle={{ color: 'var(--success)' }} />
                        <div className="mt-2 text-xs g-text-secondary">入账率 6.95%</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月应收余额 (元)</span>} value={19075000} valueStyle={{ color: 'var(--warning)' }} />
                        <div className="mt-2 text-xs g-text-secondary">待结算/未入账</div>
                    </Card>
                </Col>
            </Row>

            <Row gutter={[24, 24]}>
                <Col span={12}>
                    <Card title={<span className="g-text-primary"><BarChartOutlined className="mr-2" />近半年消纳量趋势 (m³)</span>} className="glass-panel g-border-panel border h-96">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={monthlyData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" stroke={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"} vertical={false} />
                                <XAxis dataKey="month" stroke={isDarkMode ? "#94a3b8" : "#64748b"} tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }} />
                                <YAxis stroke={isDarkMode ? "#94a3b8" : "#64748b"} tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }} />
                                <RechartsTooltip 
                                    contentStyle={{ backgroundColor: isDarkMode ? 'rgba(15, 23, 42, 0.9)' : 'rgba(255, 255, 255, 0.9)', borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)', color: isDarkMode ? '#fff' : '#000' }}
                                    itemStyle={{ color: 'var(--primary)' }}
                                />
                                <Bar dataKey="volume" name="消纳量" fill="var(--primary)" radius={[4, 4, 0, 0]} barSize={30} />
                            </BarChart>
                        </ResponsiveContainer>
                    </Card>
                </Col>
                <Col span={12}>
                    <Card title={<span className="g-text-primary"><LineChartOutlined className="mr-2" />近半年结算金额趋势 (元)</span>} className="glass-panel g-border-panel border h-96">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={monthlyData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" stroke={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"} vertical={false} />
                                <XAxis dataKey="month" stroke={isDarkMode ? "#94a3b8" : "#64748b"} tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }} />
                                <YAxis stroke={isDarkMode ? "#94a3b8" : "#64748b"} tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b' }} />
                                <RechartsTooltip 
                                    contentStyle={{ backgroundColor: isDarkMode ? 'rgba(15, 23, 42, 0.9)' : 'rgba(255, 255, 255, 0.9)', borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)', color: isDarkMode ? '#fff' : '#000' }}
                                    itemStyle={{ color: 'var(--success)' }}
                                />
                                <Line type="monotone" dataKey="amount" name="结算金额" stroke="var(--success)" strokeWidth={3} dot={{ r: 4, fill: 'var(--success)', strokeWidth: 2, stroke: 'var(--bg-panel)' }} activeDot={{ r: 6 }} />
                            </LineChart>
                        </ResponsiveContainer>
                    </Card>
                </Col>
            </Row>

            <Card title="各类型合同统计明细" className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={contractTypeData} 
                    rowKey="type"
                    pagination={false}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default MonthlyReport;
