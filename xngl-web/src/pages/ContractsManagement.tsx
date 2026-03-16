import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Tag, Input, Button, DatePicker, Row, Col, Statistic, Progress, Space } from 'antd';
import { SearchOutlined, DownloadOutlined, DollarOutlined, FileTextOutlined, BlockOutlined, FilterOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { request } from '@/utils/request';

const { RangePicker } = DatePicker;

interface ContractListItem {
    key: string;
    id: string;
    contractId: string;
    type: string;
    project: string;
    site: string;
    amount: number;
    unearned: number;
    volume: number;
    status: string;
    date: string;
}

interface ContractApiItem {
    id?: number | string;
    contractNo?: string;
    code?: string;
    contractType?: string;
    projectName?: string;
    projectId?: number | string;
    siteName?: string;
    siteId?: number | string;
    contractAmount?: number;
    amount?: number;
    receivedAmount?: number;
    agreedVolume?: number;
    contractStatus?: string;
    approvalStatus?: string;
    status?: string | number;
    signDate?: string;
    effectiveDate?: string;
    createTime?: string;
}

const ContractsManagement: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [contractsData, setContractsData] = useState<ContractListItem[]>([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        void fetchContracts();
    }, []);

    const fetchContracts = async () => {
        setLoading(true);
        try {
            const res = await request.get('/contracts');
            const records = Array.isArray(res)
                ? res
                : Array.isArray((res as any)?.data)
                    ? (res as any).data
                    : [];
            setContractsData(
                records.map((item: ContractApiItem) => {
                    const totalAmount = Number(item.contractAmount ?? item.amount ?? 0);
                    const receivedAmount = Number(item.receivedAmount ?? 0);
                    return {
                        key: String(item.id ?? item.contractNo ?? item.code ?? Math.random()),
                        id: item.contractNo || item.code || String(item.id ?? '-'),
                        contractId: String(item.id ?? ''),
                        type: item.contractType || '-',
                        project: item.projectName || (item.projectId != null ? `项目#${item.projectId}` : '-'),
                        site: item.siteName || (item.siteId != null ? `场地#${item.siteId}` : '-'),
                        amount: totalAmount,
                        unearned: Math.max(totalAmount - receivedAmount, 0),
                        volume: Number(item.agreedVolume ?? 0),
                        status: String(item.contractStatus ?? item.approvalStatus ?? item.status ?? '-'),
                        date: item.signDate || item.effectiveDate || item.createTime || '-',
                    };
                })
            );
        } catch (error) {
            console.error(error);
            setContractsData([]);
        } finally {
            setLoading(false);
        }
    };

    const isEffectiveStatus = (status: string) => {
        const normalized = String(status || '').toUpperCase();
        return ['EFFECTIVE', 'EXECUTING', 'IN_PROGRESS', 'NORMAL', '生效'].includes(normalized);
    };

    const renderStatus = (status: string) => {
        const normalized = String(status || '').toUpperCase();
        switch (normalized) {
            case 'EFFECTIVE':
            case 'EXECUTING':
            case 'IN_PROGRESS':
            case 'NORMAL':
            case '生效':
                return <Tag color="green" className="border-none">生效</Tag>;
            case 'APPROVING':
            case 'PENDING':
            case '审批中':
                return <Tag color="processing" className="border-none">审批中</Tag>;
            case 'TERMINATED':
            case '终止':
                return <Tag color="default" className="border-none g-text-secondary bg-white">终止</Tag>;
            case 'CANCELLED':
            case 'VOID':
            case '作废':
                return <Tag color="error" className="border-none">作废</Tag>;
            default:
                return <Tag color="default">{status}</Tag>;
        }
    };

    const filteredContracts = useMemo(
        () =>
            contractsData.filter((item) => {
                const keyword = searchText.trim();
                return !keyword || item.id.includes(keyword) || item.project.includes(keyword);
            }),
        [contractsData, searchText],
    );

    const columns = [
        { title: '合同编号', dataIndex: 'id', key: 'id', render: (t: string) => <span className="g-text-primary-link font-mono tracking-wide">{t}</span> },
        { title: '合同类型', dataIndex: 'type', key: 'type', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '关联项目', dataIndex: 'project', key: 'project', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '约定消纳场', dataIndex: 'site', key: 'site', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        {
            title: '总金额/已入账',
            key: 'money',
            render: (_: any, record: any) => {
                const earned = record.amount - record.unearned;
                const percent = record.amount === 0 ? 100 : Math.round((earned / record.amount) * 100);
                return (
                    <div className="flex flex-col gap-1 w-32">
                        <span className="g-text-success font-bold">¥ {(record.amount / 10000).toFixed(1)} 万</span>
                        <Progress percent={percent} size="small" showInfo={false} strokeColor="var(--success)" trailColor="rgba(0,0,0,0.06)" />
                        <span className="text-xs g-text-secondary">已入账 {(earned / 10000).toFixed(1)} 万</span>
                    </div>
                );
            }
        },
        { title: '约定方量(m³)', dataIndex: 'volume', key: 'volume', render: (t: number) => <span className="g-text-secondary">{t.toLocaleString()}</span> },
        { title: '状态', dataIndex: 'status', key: 'status', render: renderStatus },
        { title: '签订日期', dataIndex: 'date', key: 'date', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link" onClick={() => navigate(`/contracts/${record.contractId}`)}>详情</a>
                    {isEffectiveStatus(record.status) && <a className="g-text-success hover:g-text-success">入账</a>}
                    {isEffectiveStatus(record.status) && <a className="g-text-warning hover:g-text-warning">变更</a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">合同与财务结算</h1>
                    <p className="g-text-secondary mt-1">管理各项目的消纳合同、资金流向、分次入账以及账单结算体系</p>
                </div>
                <div className="text-right">
                    <Button type="primary" icon={<DownloadOutlined />} className="g-btn-primary border-none mr-3 text-white">
                        导出月度报表
                    </Button>
                    <Button type="primary" className="g-btn-primary border-none">
                        发起新合同审批
                    </Button>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月累计流水 (元)</span>} value={1420500} valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }} prefix={<DollarOutlined />} />
                        <div className="mt-2 text-xs g-text-secondary">已入账 85 笔</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">待入账/应收 (元)</span>} value={4050000} valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }} prefix={<FileTextOutlined />} />
                        <div className="mt-2 text-xs g-text-secondary">含审批中合同</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">生效消纳合同 (份)</span>} value={128} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<BlockOutlined className="g-text-primary-link" />} />
                        <div className="mt-2 text-xs g-text-secondary">本周新增 12 份</div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">财务结算单 (张)</span>} value={42} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<FileTextOutlined className="g-text-primary-link" />} />
                        <div className="mt-2 text-xs g-text-secondary">待财务复核 3 张</div>
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
                    <Input placeholder="搜索合同编号或项目名称" prefix={<SearchOutlined className="g-text-secondary" />} className="w-64 bg-white g-border-panel border g-text-primary" value={searchText} onChange={e => setSearchText(e.target.value)} />
                    <RangePicker className="bg-white g-border-panel border"  />
                    <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">状态筛选</Button>
                </div>
                <Table
                    columns={columns}
                    dataSource={filteredContracts}
                    rowKey="key"
                    loading={loading}
                    pagination={{ pageSize: 5 }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default ContractsManagement;
