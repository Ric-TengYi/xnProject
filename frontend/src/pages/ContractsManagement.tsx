import React, { useState } from 'react';
import { Card, Table, Tag, Input, Button, DatePicker, Row, Col, Statistic, Progress, Space } from 'antd';
import { SearchOutlined, DownloadOutlined, DollarOutlined, FileTextOutlined, BlockOutlined, FilterOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

const { RangePicker } = DatePicker;

const contractsData = [
    { id: 'HT-2401-0012', type: '正常合同', project: '滨海新区基础建设B标段', site: '东区临时消纳场', amount: 3500000, unearned: 2100000, volume: 150000, status: '生效', date: '2024-01-10' },
    { id: 'HT-2402-0045', type: '三方合同', project: '老旧小区改造工程', site: '南郊复合型消纳中心', amount: 850000, unearned: 700000, volume: 45000, status: '生效', date: '2024-02-15' },
    { id: 'HT-2403-0102', type: '正常合同', project: '科创园四期平整', site: '北区填埋场', amount: 1200000, unearned: 1200000, volume: 80000, status: '审批中', date: '2024-04-05' },
    { id: 'HT-2311-0088', type: '正常合同', project: '地铁三期二号线', site: '西郊临时周转站', amount: 5500000, unearned: 150000, volume: 300000, status: '生效', date: '2023-11-20' },
    { id: 'HT-2308-0125', type: '租赁合同', project: '环路改造工程', site: '无', amount: 320000, unearned: 0, volume: 0, status: '终止', date: '2023-08-10' },
];

const ContractsManagement: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const navigate = useNavigate();

    const renderStatus = (status: string) => {
        switch (status) {
            case '生效': return <Tag color="green" className="border-none">{status}</Tag>;
            case '审批中': return <Tag color="processing" className="border-none">{status}</Tag>;
            case '终止': return <Tag color="default" className="border-none text-slate-600 dark:text-slate-400 bg-white dark:bg-slate-800">{status}</Tag>;
            case '作废': return <Tag color="error" className="border-none">{status}</Tag>;
            default: return <Tag color="default">{status}</Tag>;
        }
    };

    const columns = [
        { title: '合同编号', dataIndex: 'id', key: 'id', render: (t: string) => <span className="text-blue-600 dark:text-blue-400 font-mono tracking-wide">{t}</span> },
        { title: '合同类型', dataIndex: 'type', key: 'type', render: (t: string) => <span className="text-slate-600 dark:text-slate-300">{t}</span> },
        { title: '关联项目', dataIndex: 'project', key: 'project', render: (t: string) => <strong className="text-slate-700 dark:text-slate-200">{t}</strong> },
        { title: '约定消纳场', dataIndex: 'site', key: 'site', render: (t: string) => <span className="text-slate-600 dark:text-slate-400">{t}</span> },
        {
            title: '总金额/已入账',
            key: 'money',
            render: (_: any, record: any) => {
                const earned = record.amount - record.unearned;
                const percent = record.amount === 0 ? 100 : Math.round((earned / record.amount) * 100);
                return (
                    <div className="flex flex-col gap-1 w-32">
                        <span className="text-green-600 dark:text-green-400 font-bold">¥ {(record.amount / 10000).toFixed(1)} 万</span>
                        <Progress percent={percent} size="small" showInfo={false} strokeColor="var(--success)" trailColor="rgba(255,255,255,0.1)" />
                        <span className="text-xs text-slate-600 dark:text-slate-400">已入账 {(earned / 10000).toFixed(1)} 万</span>
                    </div>
                );
            }
        },
        { title: '约定方量(m³)', dataIndex: 'volume', key: 'volume', render: (t: number) => <span className="text-slate-600 dark:text-slate-300">{t.toLocaleString()}</span> },
        { title: '状态', dataIndex: 'status', key: 'status', render: renderStatus },
        { title: '签订日期', dataIndex: 'date', key: 'date', render: (t: string) => <span className="text-slate-600 dark:text-slate-400">{t}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400" onClick={() => navigate(`/contracts/${record.id}`)}>详情</a>
                    {record.status === '生效' && <a className="text-green-600 dark:text-green-500 hover:text-green-600 dark:text-green-400">入账</a>}
                    {record.status === '生效' && <a className="text-orange-600 dark:text-orange-500 hover:text-orange-600 dark:text-orange-400">变更</a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">合同与财务结算</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">管理各项目的消纳合同、资金流向、分次入账以及账单结算体系</p>
                </div>
                <div className="text-right">
                    <Button type="primary" icon={<DownloadOutlined />} className="bg-slate-800 hover:bg-slate-700 border-none mr-3 text-white">
                        导出月度报表
                    </Button>
                    <Button type="primary" className="bg-blue-600 hover:bg-blue-500 border-none">
                        发起新合同审批
                    </Button>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={6}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">本月累计流水 (元)</span>} value={1420500} valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }} prefix={<DollarOutlined />} />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">已入账 85 笔</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">待入账/应收 (元)</span>} value={4050000} valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }} prefix={<FileTextOutlined />} />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">含审批中合同</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">生效消纳合同 (份)</span>} value={128} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<BlockOutlined className="text-blue-600 dark:text-blue-500" />} />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">本周新增 12 份</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">财务结算单 (张)</span>} value={42} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<FileTextOutlined className="text-blue-600 dark:text-blue-500" />} />
                        <div className="mt-2 text-xs text-slate-600 dark:text-slate-400">待财务复核 3 张</div>
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b border-slate-200 dark:border-slate-700/50 flex flex-wrap gap-4 bg-slate-50 dark:bg-slate-900/30">
                    <Input placeholder="搜索合同编号或项目名称" prefix={<SearchOutlined className="text-slate-600 dark:text-slate-400" />} className="w-64 bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white" value={searchText} onChange={e => setSearchText(e.target.value)} />
                    <RangePicker className="bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700" popupClassName="dark-picker" />
                    <Button icon={<FilterOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white">状态筛选</Button>
                </div>
                <Table columns={columns} dataSource={contractsData.filter(d => d.id.includes(searchText) || d.project.includes(searchText))} pagination={{ pageSize: 5 }} className="bg-transparent" rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors" />
            </Card>
        </motion.div>
    );
};

export default ContractsManagement;
