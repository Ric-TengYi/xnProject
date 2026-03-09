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
        { title: '车牌号', dataIndex: 'id', key: 'id', render: (text: string) => <a className="text-blue-600 dark:text-blue-400 font-bold text-lg">{text}</a> },
        { title: '车型', dataIndex: 'type', key: 'type', render: (t: string) => <span className="text-slate-600 dark:text-slate-300">{t}</span> },
        { title: '所属运输单位', dataIndex: 'company', key: 'company', render: (c: string) => <span className="text-slate-600 dark:text-slate-300">{c}</span> },
        { title: '司乘人员', dataIndex: 'driver', key: 'driver', render: (d: string) => <span className="text-slate-600 dark:text-slate-300">{d}</span> },
        { title: '核定载重', dataIndex: 'load', key: 'load', render: (l: string) => <span className="text-slate-600 dark:text-slate-400">{l}</span> },
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
            render: (date: string) => <span className="text-slate-600 dark:text-slate-400"><SafetyCertificateOutlined className="mr-1" />{date}</span>
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400">详情</a>
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400">编辑</a>
                    {record.status === '禁用' ? (
                        <a className="text-green-600 dark:text-green-500 hover:text-green-600 dark:text-green-400">解禁</a>
                    ) : (
                        <a className="text-red-600 dark:text-red-500 hover:text-red-600 dark:text-red-400">禁用</a>
                    )}
                </Space>
            )
        },
    ];

    const companyColumns = [
        { title: '运输单位名称', dataIndex: 'company', key: 'company', render: (text: string) => <strong className="text-slate-700 dark:text-slate-200">{text}</strong> },
        { title: '入网车辆总数', dataIndex: 'total', key: 'total', render: (v: number) => <span className="text-blue-300 font-bold text-lg">{v}</span> },
        { title: '今日在线活跃', dataIndex: 'active', key: 'active', render: (v: number) => <span className="text-green-600 dark:text-green-400">{v}</span> },
        { title: '违规预警数', dataIndex: 'warning', key: 'warning', render: (v: number) => <span className={v > 5 ? 'text-red-600 dark:text-red-500 font-bold' : 'text-slate-600 dark:text-slate-400'}>{v}</span> },
        {
            title: '综合安全评分',
            dataIndex: 'score',
            key: 'score',
            render: (v: number) => (
                <span className={v > 90 ? 'text-green-600 dark:text-green-400' : v > 80 ? 'text-orange-600 dark:text-orange-400' : 'text-red-600 dark:text-red-500 font-bold'}>
                    {v} 分
                </span>
            )
        },
        { title: '操作', key: 'action', render: () => <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400">运力分析报表</a> },
    ];

    return (
        <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">车辆与运力资源</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">全局管理系统内注册的运输车辆资质、状态以及各承运单位运力情况</p>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">全网注册车辆 (台)</span>} value={460} valueStyle={{ color: 'var(--text-primary)' }} prefix={<CarOutlined className="text-blue-600 dark:text-blue-500" />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">今日活跃率</span>} value={85.4} suffix="%" valueStyle={{ color: 'var(--success)' }} prefix={<DashboardOutlined />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">证件预警/禁运 (台)</span>} value={12} valueStyle={{ color: 'var(--error)' }} prefix={<SafetyCertificateOutlined />} />
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <Tabs
                    defaultActiveKey="1"
                    tabBarStyle={{ padding: '0 24px', marginBottom: 0, borderBottom: '1px solid rgba(255,255,255,0.08)' }}
                    className="custom-tabs"
                >
                    <TabPane tab="车辆信息库" key="1">
                        <div className="p-4 flex justify-between bg-slate-50 dark:bg-slate-900/40">
                            <div className="flex gap-4">
                                <Input placeholder="搜索车牌号/司机姓名" prefix={<SearchOutlined className="text-slate-600 dark:text-slate-400" />} className="w-72 bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white" value={searchText} onChange={e => setSearchText(e.target.value)} />
                                <Button icon={<FilterOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white" onClick={() => setFilterVisible(true)}>高级筛选</Button>
                            </div>
                            <Space>
                                <Button icon={<ImportOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white">导入</Button>
                                <Button icon={<ExportOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white">导出</Button>
                                <Button type="primary" icon={<PlusOutlined />} className="bg-blue-600 hover:bg-blue-500 border-none">新增车辆</Button>
                            </Space>
                        </div>
                        <Table columns={vehicleColumns} dataSource={vehicleData.filter(v => v.id.includes(searchText) || v.driver.includes(searchText))} pagination={{ pageSize: 5 }} className="bg-transparent text-slate-600 dark:text-slate-300" rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors" />
                    </TabPane>
                    <TabPane tab="承运单位运力" key="2">
                        <div className="p-4 bg-slate-50 dark:bg-slate-900/40" />
                        <Table columns={companyColumns} dataSource={capacityData} pagination={false} className="bg-transparent text-slate-600 dark:text-slate-300" rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors" />
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
