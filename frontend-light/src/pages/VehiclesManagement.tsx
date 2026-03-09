import React, { useState } from 'react';
import { Card, Table, Tag, Input, Button, Tabs, Row, Col, Statistic, Space, Drawer, Form, Select } from 'antd';
import { SearchOutlined, FilterOutlined, PlusOutlined, CarOutlined, SafetyCertificateOutlined, DashboardOutlined, ImportOutlined, ExportOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { TabPane } = Tabs;

const vehicleData = [
    { id: '川A88921', type: '重型自卸货车', company: '宏基渣土运输公司', status: '在用', driver: '张三丰', load: '30吨', nextMaintain: '2024-06-12' },
    { id: '川A6258W', type: '中型自卸货车', company: '顺达土方工程队', status: '维修', driver: '李四', load: '20吨', nextMaintain: '2024-05-01' },
    { id: '川A1192N', type: '重型自卸货车', company: '捷安运输', status: '在用', driver: '王五', load: '30吨', nextMaintain: '2024-08-20' },
    { id: '川A5582K', type: '重型自卸货车', company: '宏基渣土运输公司', status: '禁用', driver: '赵六', load: '35吨', nextMaintain: '2024-04-15' },
    { id: '川B44521', type: '中型自卸货车', company: '新思路运输', status: '在用', driver: '钱七', load: '20吨', nextMaintain: '2024-07-30' },
    { id: '川A99823', type: '重型自卸货车', company: '联运物流', status: '在用', driver: '孙八', load: '30吨', nextMaintain: '2024-09-11' },
];

const capacityData = [
    { company: '宏基渣土运输公司', total: 125, active: 110, warning: 5, score: 92 },
    { company: '顺达土方工程队', total: 80, active: 75, warning: 2, score: 95 },
    { company: '捷安运输', total: 45, active: 30, warning: 10, score: 78 },
    { company: '新思路运输', total: 60, active: 58, warning: 1, score: 98 },
    { company: '联运物流', total: 150, active: 140, warning: 8, score: 88 },
];

const VehiclesManagement: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [filterVisible, setFilterVisible] = useState(false);
    const [form] = Form.useForm();

    const vehicleColumns = [
        { title: '车牌号', dataIndex: 'id', key: 'id', render: (text: string) => <a className="g-text-primary-link font-bold text-lg">{text}</a> },
        { title: '车型', dataIndex: 'type', key: 'type', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '所属运输单位', dataIndex: 'company', key: 'company', render: (c: string) => <span className="g-text-secondary">{c}</span> },
        { title: '司乘人员', dataIndex: 'driver', key: 'driver', render: (d: string) => <span className="g-text-secondary">{d}</span> },
        { title: '核定载重', dataIndex: 'load', key: 'load', render: (l: string) => <span className="g-text-secondary">{l}</span> },
        {
            title: '当前状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '在用': 'green', '维修': 'orange', '禁用': 'red' };
                return <Tag color={colorMap[status]} className="border-none">{status}</Tag>
            }
        },
        {
            title: '保养到期日',
            dataIndex: 'nextMaintain',
            key: 'nextMaintain',
            render: (date: string) => <span className="g-text-secondary"><SafetyCertificateOutlined className="mr-1" />{date}</span>
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link">详情</a>
                    <a className="g-text-primary-link hover:g-text-primary-link">编辑</a>
                    {record.status === '禁用' ? (
                        <a className="g-text-success hover:g-text-success">解禁</a>
                    ) : (
                        <a className="g-text-error hover:g-text-error">禁用</a>
                    )}
                </Space>
            )
        },
    ];

    const companyColumns = [
        { title: '运输单位名称', dataIndex: 'company', key: 'company', render: (text: string) => <strong className="g-text-primary">{text}</strong> },
        { title: '入网车辆总数', dataIndex: 'total', key: 'total', render: (v: number) => <span className="text-blue-300 font-bold text-lg">{v}</span> },
        { title: '今日在线活跃', dataIndex: 'active', key: 'active', render: (v: number) => <span className="g-text-success">{v}</span> },
        { title: '违规预警数', dataIndex: 'warning', key: 'warning', render: (v: number) => <span className={v > 5 ? 'g-text-error font-bold' : 'g-text-secondary'}>{v}</span> },
        {
            title: '综合安全评分',
            dataIndex: 'score',
            key: 'score',
            render: (v: number) => (
                <span className={v > 90 ? 'g-text-success' : v > 80 ? 'g-text-warning' : 'g-text-error font-bold'}>
                    {v} 分
                </span>
            )
        },
        { title: '操作', key: 'action', render: () => <a className="g-text-primary-link hover:g-text-primary-link">运力分析报表</a> },
    ];

    return (
        <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">车辆与运力资源</h1>
                    <p className="g-text-secondary mt-1">全局管理系统内注册的运输车辆资质、状态以及各承运单位运力情况</p>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">全网注册车辆 (台)</span>} value={460} valueStyle={{ color: 'var(--text-primary)' }} prefix={<CarOutlined className="g-text-primary-link" />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">今日活跃率</span>} value={85.4} suffix="%" valueStyle={{ color: 'var(--success)' }} prefix={<DashboardOutlined />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel g-border-panel border">
                        <Statistic title={<span className="g-text-secondary">证件预警/禁运 (台)</span>} value={12} valueStyle={{ color: 'var(--error)' }} prefix={<SafetyCertificateOutlined />} />
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <Tabs
                    defaultActiveKey="1"
                    tabBarStyle={{ padding: '0 24px', marginBottom: 0, borderBottom: '1px solid rgba(255,255,255,0.08)' }}
                    className="custom-tabs"
                >
                    <TabPane tab="车辆信息库" key="1">
                        <div className="p-4 flex justify-between g-bg-toolbar">
                            <div className="flex gap-4">
                                <Input placeholder="搜索车牌号/司机姓名" prefix={<SearchOutlined className="g-text-secondary" />} className="w-72 bg-white g-border-panel border g-text-primary" value={searchText} onChange={e => setSearchText(e.target.value)} />
                                <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary" onClick={() => setFilterVisible(true)}>高级筛选</Button>
                            </div>
                            <Space>
                                <Button icon={<ImportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导入</Button>
                                <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出</Button>
                                <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none">新增车辆</Button>
                            </Space>
                        </div>
                        <Table columns={vehicleColumns} dataSource={vehicleData.filter(v => v.id.includes(searchText) || v.driver.includes(searchText))} pagination={{ pageSize: 5 }} className="bg-transparent g-text-secondary" rowClassName="hover:bg-white transition-colors" />
                    </TabPane>
                    <TabPane tab="承运单位运力" key="2">
                        <div className="p-4 g-bg-toolbar" />
                        <Table columns={companyColumns} dataSource={capacityData} pagination={false} className="bg-transparent g-text-secondary" rowClassName="hover:bg-white transition-colors" />
                    </TabPane>
                </Tabs>
            </Card>

            <Drawer
                title="高级筛选"
                placement="right"
                onClose={() => setFilterVisible(false)}
                open={filterVisible}
                extra={
                    <Space>
                        <Button onClick={() => form.resetFields()}>重置</Button>
                        <Button type="primary" onClick={() => setFilterVisible(false)}>
                            查询
                        </Button>
                    </Space>
                }
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="type" label="车型">
                        <Select
                            placeholder="请选择车型"
                            options={[
                                { value: '重型自卸货车', label: '重型自卸货车' },
                                { value: '中型自卸货车', label: '中型自卸货车' },
                                { value: '轻型自卸货车', label: '轻型自卸货车' },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item name="company" label="运输单位">
                        <Input placeholder="请输入运输单位名称" />
                    </Form.Item>
                    <Form.Item name="status" label="车辆状态">
                        <Select
                            placeholder="请选择状态"
                            options={[
                                { value: '在用', label: '在用' },
                                { value: '停用', label: '停用' },
                                { value: '维修', label: '维修' },
                                { value: '禁用', label: '禁用' },
                            ]}
                        />
                    </Form.Item>
                </Form>
            </Drawer>
        </motion.div>
    );
};

export default VehiclesManagement;
