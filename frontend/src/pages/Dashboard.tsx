import React from 'react';
import { Card, Row, Col, Statistic, Table, Tag } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';

const data = [
    { name: '01', 消纳量: 4000, 违规台次: 24 },
    { name: '02', 消纳量: 3000, 违规台次: 13 },
    { name: '03', 消纳量: 2000, 违规台次: 58 },
    { name: '04', 消纳量: 2780, 违规台次: 39 },
    { name: '05', 消纳量: 1890, 违规台次: 48 },
    { name: '06', 消纳量: 2390, 违规台次: 38 },
    { name: '07', 消纳量: 3490, 违规台次: 43 },
];

const columns = [
    {
        title: '项目名称',
        dataIndex: 'name',
        key: 'name',
        render: (text: string) => <a className="text-blue-600 dark:text-blue-400">{text}</a>,
    },
    {
        title: '关联场地',
        dataIndex: 'site',
        key: 'site',
    },
    {
        title: '进度',
        dataIndex: 'progress',
        key: 'progress',
        render: (progress: number) => (
            <Tag color={progress > 80 ? 'green' : progress > 50 ? 'orange' : 'red'}>
                {progress}%
            </Tag>
        ),
    },
    {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (status: string) => (
            <span className={status === '正常' ? 'text-green-600 dark:text-green-500' : 'text-red-600 dark:text-red-500'}>
                <div className={`w-2 h-2 rounded-full inline-block mr-2 ${status === '正常' ? 'bg-green-500' : 'bg-red-500 shadow-[0_0_8px_#ef4444]'}`}></div>
                {status}
            </span>
        ),
    },
];

const tableData = [
    { key: '1', name: '滨海新区基础建设B标段', site: '东区临时消纳场', progress: 85, status: '正常' },
    { key: '2', name: '市中心地铁延长线三期工程', site: '南郊复合型消纳中心', progress: 42, status: '预警' },
    { key: '3', name: '科创园四期土地平整项目', site: '北区填埋场', progress: 92, status: '正常' },
    { key: '4', name: '老旧小区改造工程综合包', site: '西郊临时周转站', progress: 15, status: '正常' },
];

import { useTheme } from '../contexts/ThemeContext';

const Dashboard: React.FC = () => {
    const { isDarkMode } = useTheme();

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">综合数据大屏</h1>
                <div className="text-slate-600 dark:text-slate-400 text-sm">更新时间: 刚刚</div>
            </div>

            {/* 核心指标卡片组 */}
            <Row gutter={[24, 24]}>
                <Col span={6}>
                    <Card className="glass-panel overflow-hidden relative border-l-4 border-l-blue-500 hover:shadow-[0_0_20px_rgba(24,144,255,0.2)] transition-all">
                        <div className="absolute -right-4 -top-4 w-24 h-24 bg-blue-500 opacity-10 rounded-full blur-xl"></div>
                        <Statistic
                            title={<span className="text-slate-600 dark:text-slate-300">本月消纳总量 (m³)</span>}
                            value={112893}
                            valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
                            prefix={<ArrowUpOutlined className="text-sm text-green-600 dark:text-green-500" />}
                            suffix="万方"
                        />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">环比上月 +8.2%</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel overflow-hidden relative border-l-4 border-l-green-500 hover:shadow-[0_0_20px_rgba(16,185,129,0.2)] transition-all">
                        <div className="absolute -right-4 -top-4 w-24 h-24 bg-green-500 opacity-10 rounded-full blur-xl"></div>
                        <Statistic
                            title={<span className="text-slate-600 dark:text-slate-300">活跃消纳场地</span>}
                            value={42}
                            valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
                            suffix="/ 50"
                        />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">平均容量使用率 68%</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel overflow-hidden relative border-l-4 border-l-orange-500 hover:shadow-[0_0_20px_rgba(245,158,11,0.2)] transition-all">
                        <div className="absolute -right-4 -top-4 w-24 h-24 bg-orange-500 opacity-10 rounded-full blur-xl"></div>
                        <Statistic
                            title={<span className="text-slate-600 dark:text-slate-300">今日运营车辆 (台次)</span>}
                            value={3248}
                            valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
                            prefix={<ArrowDownOutlined className="text-sm text-red-600 dark:text-red-500" />}
                        />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">高峰时段监控中</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel overflow-hidden relative border-l-4 border-l-red-500 hover:shadow-[0_0_20px_rgba(239,68,68,0.2)] transition-all">
                        <div className="absolute -right-4 -top-4 w-24 h-24 bg-red-500 opacity-10 rounded-full blur-xl"></div>
                        <Statistic
                            title={<span className="text-slate-600 dark:text-slate-300">系统拦截高风险 (次)</span>}
                            value={15}
                            valueStyle={{ color: 'var(--error)', fontWeight: 'bold' }}
                        />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">闯禁/偏航/超时等异常</div>
                    </Card>
                </Col>
            </Row>

            {/* 图表与表格区域 */}
            <Row gutter={[24, 24]}>
                <Col span={14}>
                    <Card
                        title={<span className="text-slate-700 dark:text-slate-200">近七日消纳趋势</span>}
                        className="glass-panel h-[400px]"
                        headStyle={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}
                    >
                        <div className="h-[300px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorPv" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.8} />
                                            <stop offset="95%" stopColor="var(--primary)" stopOpacity={0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" stroke={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"} vertical={false} />
                                    <XAxis dataKey="name" stroke={isDarkMode ? "#94a3b8" : "#64748b"} />
                                    <YAxis stroke={isDarkMode ? "#94a3b8" : "#64748b"} />
                                    <RechartsTooltip
                                        contentStyle={{ backgroundColor: isDarkMode ? 'rgba(15, 23, 42, 0.9)' : 'rgba(255, 255, 255, 0.9)', borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)', borderRadius: '8px' }}
                                        itemStyle={{ color: isDarkMode ? '#fff' : '#000' }}
                                    />
                                    <Area type="monotone" dataKey="消纳量" stroke="var(--primary)" fillOpacity={1} fill="url(#colorPv)" />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </Card>
                </Col>
                <Col span={10}>
                    <Card
                        title={<span className="text-slate-700 dark:text-slate-200">重点项目告警与进度</span>}
                        className="glass-panel h-[400px]"
                        headStyle={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}
                        bodyStyle={{ padding: 0 }}
                    >
                        <Table
                            columns={columns}
                            dataSource={tableData}
                            pagination={false}
                            className="bg-transparent"
                            rowClassName="hover:bg-white dark:bg-slate-800/50"
                        />
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

export default Dashboard;
