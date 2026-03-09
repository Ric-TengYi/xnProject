import React from 'react';
import { Card, Table, Tag, Button, DatePicker, Select, Space, Row, Col, Statistic } from 'antd';
import { DownloadOutlined, CalculatorOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { RangePicker } = DatePicker;
const { Option } = Select;

const settlementData = [
    { id: 'JS-202403-001', type: '按项目结算', target: '滨海新区基础建设B标段', period: '2024-02-01 ~ 2024-02-29', volume: 45000, amount: 1125000, status: '已结算', time: '2024-03-05' },
    { id: 'JS-202403-002', type: '按场地结算', target: '东区临时消纳场', period: '2024-02-01 ~ 2024-02-29', volume: 85000, amount: 2125000, status: '待审批', time: '2024-03-06' },
    { id: 'JS-202403-003', type: '按项目结算', target: '老旧小区改造工程', period: '2024-02-01 ~ 2024-02-29', volume: 12000, amount: 300000, status: '已结算', time: '2024-03-02' },
    { id: 'JS-202403-004', type: '按场地结算', target: '南郊复合型消纳中心', period: '2024-02-01 ~ 2024-02-29', volume: 120000, amount: 3000000, status: '草稿', time: '--' },
];

const Settlements: React.FC = () => {
    const columns = [
        { title: '结算单号', dataIndex: 'id', key: 'id', render: (t: string) => <span className="g-text-primary-link font-mono tracking-wide">{t}</span> },
        { title: '结算类型', dataIndex: 'type', key: 'type', render: (t: string) => <Tag color="blue" className="border-none">{t}</Tag> },
        { title: '结算对象', dataIndex: 'target', key: 'target', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '结算周期', dataIndex: 'period', key: 'period', render: (t: string) => <span className="g-text-secondary text-sm">{t}</span> },
        { title: '消纳量(m³)', dataIndex: 'volume', key: 'volume', render: (t: number) => <span className="g-text-secondary">{t.toLocaleString()}</span> },
        { title: '结算金额(元)', dataIndex: 'amount', key: 'amount', render: (t: number) => <span className="g-text-success font-bold">¥ {t.toLocaleString()}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '已结算': 'success', '待审批': 'warning', '草稿': 'default' };
                return <Tag color={colorMap[status]} className="border-none">{status}</Tag>;
            }
        },
        { title: '生成时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary text-sm">{t}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link">查看明细</a>
                    {record.status === '草稿' && <a className="g-text-success hover:g-text-success">提交审批</a>}
                    {record.status === '已结算' && <a className="g-text-secondary hover:g-text-primary"><DownloadOutlined /></a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">结算管理</h1>
                    <p className="g-text-secondary mt-1">按项目或场地维度进行消纳数据结算，生成财务结算报表</p>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月待结算金额 (元)</span>} value={2125000} valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }} prefix={<ClockCircleOutlined />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月已结算金额 (元)</span>} value={1425000} valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }} prefix={<CheckCircleOutlined />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">本月结算单总数 (张)</span>} value={4} valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }} prefix={<CalculatorOutlined className="g-text-primary-link" />} />
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white">
                        <Option value="all">全部结算类型</Option>
                        <Option value="project">按项目结算</Option>
                        <Option value="site">按场地结算</Option>
                    </Select>
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white">
                        <Option value="all">全部状态</Option>
                        <Option value="draft">草稿</Option>
                        <Option value="pending">待审批</Option>
                        <Option value="done">已结算</Option>
                    </Select>
                    <RangePicker className="bg-white g-border-panel border g-text-primary" />
                    <div className="flex-1 flex justify-end gap-3">
                        <Button type="primary" icon={<CalculatorOutlined />} className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]">
                            发起新结算
                        </Button>
                    </div>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={settlementData} 
                    rowKey="id"
                    pagination={{ defaultPageSize: 10, className: 'pr-4 pb-2' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default Settlements;
