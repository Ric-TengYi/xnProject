import React, { useState } from 'react';
import { Card, Input, Button, Tag, Progress, Row, Col, Dropdown, Tooltip, Modal, Form, Select, InputNumber, Drawer, Space } from 'antd';
import type { MenuProps } from 'antd';
import { SearchOutlined, FilterOutlined, EnvironmentOutlined, MoreOutlined, PlusOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

const SITE_TYPES = [
    { value: '国有场地', label: '国有场地' },
    { value: '集体场地', label: '集体场地' },
    { value: '工程场地', label: '工程场地' },
    { value: '短驳场地', label: '短驳场地' },
];

// 模拟的消纳场地数据（可被新增扩展）
const initialSitesData = [
    {
        id: 1,
        name: '东区临时消纳场',
        type: '国有场地',
        capacity: 500000,
        used: 350000,
        status: '正常',
        address: '经济开发区科技路 128 号',
        cameras: 4,
        waitCount: 12,
    },
    {
        id: 2,
        name: '南郊复合型消纳中心',
        type: '集体场地',
        capacity: 1200000,
        used: 980000,
        status: '预警',
        address: '南部新区环城南路 88 段',
        cameras: 8,
        waitCount: 35,
    },
    {
        id: 3,
        name: '北区填埋场',
        type: '工程场地',
        capacity: 300000,
        used: 45000,
        status: '正常',
        address: '北部高新产业园 C 区',
        cameras: 2,
        waitCount: 2,
    },
    {
        id: 4,
        name: '西郊临时周转站',
        type: '短驳场地',
        capacity: 100000,
        used: 95000,
        status: '预警',
        address: '西部物流园配套 3 号地块',
        cameras: 3,
        waitCount: 8,
    },
    {
        id: 5,
        name: '老城改造一期专供场',
        type: '工程场地',
        capacity: 250000,
        used: 248000,
        status: '满载',
        address: '老城区城中村改造片区',
        cameras: 5,
        waitCount: 0,
    },
];

const SitesManagement: React.FC = () => {
    const [searchTerm, setSearchTerm] = useState('');
    const [sitesData, setSitesData] = useState(initialSitesData);
    const [addModalOpen, setAddModalOpen] = useState(false);
    const [filterVisible, setFilterVisible] = useState(false);
    const [form] = Form.useForm();
    const [filterForm] = Form.useForm();
    const navigate = useNavigate();

    const handleAddSite = () => setAddModalOpen(true);
    const handleAddModalCancel = () => {
        setAddModalOpen(false);
        form.resetFields();
    };
    const handleAddModalOk = async () => {
        try {
            const values = await form.validateFields();
            const newId = Math.max(...sitesData.map(s => s.id), 0) + 1;
            setSitesData(prev => [...prev, {
                id: newId,
                name: values.name,
                type: values.type,
                capacity: Number(values.capacity) || 0,
                used: 0,
                status: '正常',
                address: values.address || '',
                cameras: 0,
                waitCount: 0,
            }]);
            handleAddModalCancel();
        } catch {
            // 校验未通过
        }
    };

    const getItems = (id: number): MenuProps['items'] => [
        { key: '1', label: '查看详情', onClick: () => navigate(`/sites/${id}`) },
        { key: '2', label: '配置设备', onClick: () => navigate(`/sites/${id}?tab=config`) },
        { type: 'divider' },
        { key: '3', label: '临时停用', danger: true },
    ];

    const [filterType, setFilterType] = useState<string | null>(null);
    const [filterStatus, setFilterStatus] = useState<string | null>(null);
    const applyFilter = () => {
        const v = filterForm.getFieldsValue();
        setFilterType(v.type ?? null);
        setFilterStatus(v.status ?? null);
        setFilterVisible(false);
    };
    const filteredSites = sitesData.filter(site => {
        const hitSearch = !searchTerm || site.name.includes(searchTerm) || site.address.includes(searchTerm);
        const hitType = !filterType || site.type === filterType;
        const hitStatus = !filterStatus || site.status === filterStatus;
        return hitSearch && hitType && hitStatus;
    });

    const getStatusColor = (status: string) => {
        switch (status) {
            case '正常': return 'green';
            case '预警': return 'orange';
            case '满载': return 'red';
            default: return 'default';
        }
    };

    const getTypeColor = (type: string) => {
        switch (type) {
            case '国有场地': return 'blue';
            case '集体场地': return 'cyan';
            case '工程场地': return 'purple';
            case '短驳场地': return 'magenta';
            default: return 'default';
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">消纳场地管理</h1>
                    <p className="g-text-secondary mt-1">全局管控 {sitesData.length} 个消纳场地的容量与运营状态</p>
                </div>
                <div className="flex gap-3">
                    <Input
                        placeholder="搜索场地名称或地址"
                        prefix={<SearchOutlined className="g-text-secondary" />}
                        className="w-64 glass-panel g-border-panel border g-text-primary"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                    <Button icon={<FilterOutlined />} className="bg-white g-text-secondary g-border-panel border hover:g-text-primary hover:border-slate-500" onClick={() => setFilterVisible(true)}>
                        高级筛选
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]" onClick={handleAddSite}>
                        新增场地
                    </Button>
                </div>
            </div>

            <Modal
                title="新增场地"
                open={addModalOpen}
                onOk={handleAddModalOk}
                onCancel={handleAddModalCancel}
                destroyOnClose
                okText="确定"
                cancelText="取消"
                width={520}
            >
                <Form form={form} layout="vertical" className="mt-4">
                    <Form.Item name="name" label="场地名称" rules={[{ required: true, message: '请输入场地名称' }]}>
                        <Input placeholder="请输入场地名称" maxLength={50} showCount />
                    </Form.Item>
                    <Form.Item name="type" label="场地类型" rules={[{ required: true, message: '请选择场地类型' }]}>
                        <Select placeholder="请选择场地类型" options={SITE_TYPES} />
                    </Form.Item>
                    <Form.Item name="address" label="地址" rules={[{ required: true, message: '请输入地址' }]}>
                        <Input placeholder="请输入地址" maxLength={200} showCount />
                    </Form.Item>
                    <Form.Item name="capacity" label="总容量（立方米）" rules={[{ required: true, message: '请输入总容量' }, { type: 'number', min: 1, message: '容量须大于 0' }]}>
                        <InputNumber className="w-full" placeholder="请输入总容量" min={1} />
                    </Form.Item>
                </Form>
            </Modal>

            <Drawer title="高级筛选" placement="right" open={filterVisible} onClose={() => setFilterVisible(false)} width={360}
                extra={<Space><Button onClick={() => filterForm.resetFields()}>重置</Button><Button type="primary" onClick={applyFilter}>查询</Button></Space>}>
                <Form form={filterForm} layout="vertical">
                    <Form.Item name="type" label="场地类型">
                        <Select placeholder="请选择" allowClear options={SITE_TYPES} />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select placeholder="请选择" allowClear options={[{ value: '正常', label: '正常' }, { value: '预警', label: '预警' }, { value: '满载', label: '满载' }]} />
                    </Form.Item>
                </Form>
            </Drawer>

            <Row gutter={[24, 24]}>
                {filteredSites.map((site, index) => {
                    const percent = Math.round((site.used / site.capacity) * 100);
                    return (
                        <Col xs={24} sm={12} lg={8} xl={6} key={site.id}>
                            <motion.div
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: index * 0.1 }}
                            >
                                <Card
                                    className="glass-panel hover:-translate-y-1 transition-transform duration-300 g-border-panel border hover:border-blue-500/50 group"
                                    bodyStyle={{ padding: '20px' }}
                                    actions={[
                                        <Tooltip title="监控画面"><VideoCameraOutlined key="camera" className="g-text-secondary hover:g-text-primary-link" /></Tooltip>,
                                        <Tooltip title="地理位置"><EnvironmentOutlined key="map" className="g-text-secondary hover:g-text-primary-link" /></Tooltip>,
                                        <Dropdown menu={{ items: getItems(site.id) }} trigger={['click']}>
                                            <MoreOutlined key="more" className="g-text-secondary hover:g-text-primary" />
                                        </Dropdown>
                                    ]}
                                >
                                    <div className="flex justify-between items-start mb-4">
                                        <div className="flex flex-col gap-2">
                                            <Tag color={getTypeColor(site.type)} className="w-fit m-0 border-none bg-opacity-20">{site.type}</Tag>
                                            <h3 className="text-lg font-bold g-text-primary m-0 truncate w-40" title={site.name}>{site.name}</h3>
                                        </div>
                                        <Tag color={getStatusColor(site.status)} className="border-none shadow-sm">{site.status}</Tag>
                                    </div>

                                    <p className="g-text-secondary text-sm mb-4 truncate" title={site.address}>
                                        <EnvironmentOutlined className="mr-1" /> {site.address}
                                    </p>

                                    <div className="mb-2">
                                        <div className="flex justify-between text-xs mb-1">
                                            <span className="g-text-secondary">容量使用率</span>
                                            <span className={percent > 80 ? 'g-text-error' : percent >= 60 ? 'g-text-warning' : 'g-text-success'}>{percent}%</span>
                                        </div>
                                        <Progress
                                            percent={percent}
                                            showInfo={false}
                                            strokeColor={percent > 80 ? 'var(--error)' : percent >= 60 ? 'var(--warning)' : 'var(--success)'}
                                            trailColor="rgba(0,0,0,0.06)"
                                            size="small"
                                        />
                                        <div className="flex justify-between text-xs mt-1 g-text-secondary">
                                            <span>已用 {site.used / 10000}万</span>
                                            <span>总容量 {site.capacity / 10000}万</span>
                                        </div>
                                    </div>

                                    <div className="flex gap-4 mt-4 pt-4 border-t g-border-panel border">
                                        <div className="flex flex-col">
                                            <span className="g-text-secondary font-bold">{site.waitCount}</span>
                                            <span className="g-text-secondary text-xs text-nowrap">排队车辆</span>
                                        </div>
                                        <div className="flex flex-col">
                                            <span className="g-text-secondary font-bold">{site.cameras}</span>
                                            <span className="g-text-secondary text-xs text-nowrap">在线监控</span>
                                        </div>
                                    </div>
                                </Card>
                            </motion.div>
                        </Col>
                    )
                })}
            </Row>
        </div>
    );
};

export default SitesManagement;
