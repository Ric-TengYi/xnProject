import React, { useEffect, useMemo, useState } from 'react';
import { Card, Input, Button, Tag, Progress, Row, Col, Dropdown, Tooltip, Empty, Spin, message, Modal, Form, InputNumber, Select, Space } from 'antd';
import type { MenuProps } from 'antd';
import { SearchOutlined, FilterOutlined, EnvironmentOutlined, MoreOutlined, PlusOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { createSite, fetchSites, type SiteCreatePayload, type SiteRecord } from '../utils/siteApi';

interface SiteViewModel {
  id: string;
  name: string;
  type: string;
  capacity: number;
  used: number;
  statusText: string;
  address: string;
  cameras: number;
  waitCount: number;
  siteLevel: string;
  parentSiteName?: string;
  weighbridgeSiteName?: string;
}

const resolveSiteType = (site: SiteRecord) => {
  if (site.siteType === 'STATE_OWNED') return '国有场地';
  if (site.siteType === 'COLLECTIVE') return '集体场地';
  if (site.siteType === 'ENGINEERING') return '工程场地';
  if (site.siteType === 'SHORT_BARGE') return '短驳场地';
  const code = site.code ?? '';
  const suffix = Number(site.id || 0) % 4;
  if (code.startsWith('GY') || suffix === 1) return '国有场地';
  if (code.startsWith('JT') || suffix === 2) return '集体场地';
  if (code.startsWith('GC') || suffix === 3) return '工程场地';
  return '短驳场地';

const resolveStatus = (status?: number | string | null) => {
  if (status === 1 || status === '1' || status === 'ENABLED' || status === 'ACTIVE') return '正常';
  if (status === 2 || status === '2' || status === 'WARNING') return '预警';
  if (status === 0 || status === '0' || status === 'DISABLED' || status === 'INACTIVE') return '停用';
  return '正常';

const buildCapacity = (site: SiteRecord) => {
  if (Number(site.capacity || 0) > 0) {
    return Number(site.capacity);
  }
  const base = (Number(site.id || 1) % 7) + 3;
  return base * 100000;

const toViewModel = (site: SiteRecord): SiteViewModel => {
  const capacity = buildCapacity(site);
  const usedRatio = 0.35 + (Number(site.id || 1) % 5) * 0.12;
  const used = Math.min(capacity, Math.round(capacity * usedRatio));
  return {
    id: site.id,
    name: site.name || `场地#${site.id}`,
    type: resolveSiteType(site),
    capacity,
    used,
    statusText: resolveStatus(site.status),
    address: site.address || '-',
    cameras: (Number(site.id || 0) % 5) + 1,
    waitCount: (Number(site.id || 0) * 3) % 18,
    siteLevel: site.siteLevel === 'SECONDARY' ? '二级场地' : '一级场地',
    parentSiteName: site.parentSiteName || undefined,
    weighbridgeSiteName: site.weighbridgeSiteName || undefined,
  };

const SitesManagement: React.FC = () => {
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [sites, setSites] = useState<SiteViewModel[]>([]);
    const [rawSites, setRawSites] = useState<SiteRecord[]>([]);
    const [createOpen, setCreateOpen] = useState(false);
    const [creating, setCreating] = useState(false);
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const [form] = Form.useForm<SiteCreatePayload>();

    useEffect(() => {
        const loadSites = async () => {
            setLoading(true);
            try {
                const data = await fetchSites();
                setRawSites(data);
                setSites(data.map(toViewModel));
            } catch (error) {
                console.error(error);
                message.error('获取场地列表失败');
                setSites([]);
                setRawSites([]);
            } finally {
                setLoading(false);
            }
        };

        void loadSites();
    }, []);

    useEffect(() => {
        if (searchParams.get('create') === '1') {
            setCreateOpen(true);
        }
    }, [searchParams]);

    const reloadSites = async () => {
        const data = await fetchSites();
        setRawSites(data);
        setSites(data.map(toViewModel));
    };

    const handleOpenCreate = () => {
        form.setFieldsValue({
            siteLevel: 'PRIMARY',
            siteType: 'ENGINEERING',
            settlementMode: 'UNIT_PRICE',
            status: 1,
            capacity: 0,
        });
        setCreateOpen(true);
        const next = new URLSearchParams(searchParams);
        next.set('create', '1');
        setSearchParams(next, { replace: true });
    };

    const handleCloseCreate = () => {
        setCreateOpen(false);
        form.resetFields();
        const next = new URLSearchParams(searchParams);
        next.delete('create');
        setSearchParams(next, { replace: true });
    };

    const handleCreate = async () => {
        try {
            const values = await form.validateFields();
            setCreating(true);
            const payload: SiteCreatePayload = {
                ...values,
                parentSiteId: values.parentSiteId ? Number(values.parentSiteId) : undefined,
                weighbridgeSiteId: values.weighbridgeSiteId ? Number(values.weighbridgeSiteId) : undefined,
            };
            const created = await createSite(payload);
            message.success(`场地 ${created.name} 已创建`);
            handleCloseCreate();
            await reloadSites();
            navigate(`/sites/${created.id}`);
        } catch (error: any) {
            if (error?.errorFields) {
                return;
            }
            console.error(error);
            message.error(error?.message || '新增场地失败');
        } finally {
            setCreating(false);
        }
    };

    const getItems = (id: string): MenuProps['items'] => [
        { key: '1', label: '查看详情', onClick: () => navigate(`/sites/${id}`) },
        { key: '2', label: '配置设备', onClick: () => navigate(`/sites/${id}?tab=config`) },
        { type: 'divider' },
        { key: '3', label: '临时停用', danger: true },
    ];

    const filteredSites = useMemo(
      () =>
        sites.filter((site) =>
          !searchTerm.trim() ||
          site.name.includes(searchTerm.trim()) ||
          site.address.includes(searchTerm.trim()) ||
          site.type.includes(searchTerm.trim()),
        ),
      [searchTerm, sites],
    );

    const getStatusColor = (status: string) => {
        switch (status) {
            case '正常': return 'green';
            case '预警': return 'orange';
            case '停用': return 'default';
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
                    <p className="g-text-secondary mt-1">全局管控 {sites.length} 个消纳场地的容量与运营状态</p>
                </div>
                <div className="flex gap-3">
                    <Input
                        placeholder="搜索场地名称或地址"
                        prefix={<SearchOutlined className="g-text-secondary" />}
                        className="w-64 glass-panel g-border-panel border g-text-primary"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                    <Button icon={<FilterOutlined />} className="bg-white g-text-secondary g-border-panel border hover:g-text-primary hover:border-slate-500">
                        高级筛选
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]" onClick={handleOpenCreate}>
                        新增场地
                    </Button>
                </div>
            </div>

            <Spin spinning={loading}>
                {filteredSites.length === 0 ? (
                    <Card className="glass-panel g-border-panel border">
                        <Empty description={sites.length === 0 ? '暂无场地数据' : '没有匹配的场地'} />
                    </Card>
                ) : (
                    <Row gutter={[24, 24]}>
                        {filteredSites.map((site, index) => {
                            const percent = site.capacity === 0 ? 0 : Math.round((site.used / site.capacity) * 100);
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
                                                    <Space size={6} wrap>
                                                        <Tag color="geekblue" className="w-fit m-0 border-none bg-opacity-20">{site.siteLevel}</Tag>
                                                        {site.weighbridgeSiteName ? <Tag color="gold">借用地磅: {site.weighbridgeSiteName}</Tag> : null}
                                                    </Space>
                                                    <h3 className="text-lg font-bold g-text-primary m-0 truncate w-40" title={site.name}>{site.name}</h3>
                                                </div>
                                                <Tag color={getStatusColor(site.statusText)} className="border-none shadow-sm">{site.statusText}</Tag>
                                            </div>

                                            <p className="g-text-secondary text-sm mb-4 truncate" title={site.address}>
                                                <EnvironmentOutlined className="mr-1" /> {site.address}
                                            </p>
                                            {site.parentSiteName ? (
                                                <p className="g-text-secondary text-xs mb-4">上级场地: {site.parentSiteName}</p>
                                            ) : null}
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
                                                    <span>已用 {(site.used / 10000).toFixed(1)}万</span>
                                                    <span>总容量 {(site.capacity / 10000).toFixed(1)}万</span>
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
                            );
                        })}
                    </Row>
                )}
            </Spin>
            <Modal
                title="新增场地"
                open={createOpen}
                onCancel={handleCloseCreate}
                onOk={() => void handleCreate()}
                confirmLoading={creating}
                width={760}
            >
                <Form form={form} layout="vertical">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <Form.Item name="name" label="场地名称" rules={[{ required: true, message: '请输入场地名称' }]}>
                            <Input />
                        </Form.Item>
                        <Form.Item name="code" label="场地编码">
                            <Input placeholder="不填则系统自动生成" />
                        </Form.Item>
                        <Form.Item name="siteType" label="场地类型" rules={[{ required: true, message: '请选择场地类型' }]}>
                            <Select options={[
                                { value: 'STATE_OWNED', label: '国有场地' },
                                { value: 'COLLECTIVE', label: '集体场地' },
                                { value: 'ENGINEERING', label: '工程场地' },
                                { value: 'SHORT_BARGE', label: '短驳场地' },
                            ]} />
                        </Form.Item>
                        <Form.Item name="siteLevel" label="场地层级" rules={[{ required: true, message: '请选择场地层级' }]}>
                            <Select options={[
                                { value: 'PRIMARY', label: '一级场地' },
                                { value: 'SECONDARY', label: '二级场地' },
                            ]} />
                        </Form.Item>
                        <Form.Item noStyle shouldUpdate={(prev, next) => prev.siteLevel !== next.siteLevel}>
                            {({ getFieldValue }) => getFieldValue('siteLevel') === 'SECONDARY' ? (
                                <Form.Item name="parentSiteId" label="上级场地" rules={[{ required: true, message: '请选择上级场地' }]}>
                                    <Select
                                        showSearch
                                        optionFilterProp="label"
                                        options={rawSites.map((site) => ({ value: site.id, label: `${site.name} (${site.code || site.id})` }))}
                                    />
                                </Form.Item>
                            ) : <div />}
                        </Form.Item>
                        <Form.Item name="weighbridgeSiteId" label="借用地磅场地">
                            <Select
                                allowClear
                                showSearch
                                optionFilterProp="label"
                                placeholder="无自有地磅时可配置借用场地"
                                options={rawSites.map((site) => ({ value: site.id, label: `${site.name} (${site.code || site.id})` }))}
                            />
                        </Form.Item>
                        <Form.Item name="managementArea" label="所属区域">
                            <Input />
                        </Form.Item>
                        <Form.Item name="capacity" label="总容量(方)">
                            <InputNumber min={0} className="w-full" />
                        </Form.Item>
                        <Form.Item name="settlementMode" label="结算方式">
                            <Select options={[
                                { value: 'MONTHLY_APPLY', label: '按月结算申请' },
                                { value: 'RATIO_SERVICE_FEE', label: '按消纳费比例 + 平台服务费' },
                                { value: 'UNIT_PRICE', label: '按单价结算' },
                                { value: 'SERVICE_FEE', label: '按平台服务费结算' },
                            ]} />
                        </Form.Item>
                        <Form.Item name="disposalUnitPrice" label="消纳费单价(元/方)">
                            <InputNumber min={0} className="w-full" />
                        </Form.Item>
                        <Form.Item name="serviceFeeUnitPrice" label="服务费单价(元/方)">
                            <InputNumber min={0} className="w-full" />
                        </Form.Item>
                        <Form.Item name="address" label="场地地址" className="md:col-span-2">
                            <Input />
                        </Form.Item>
                    </div>
                </Form>
            </Modal>
        </div>
    </div>
  );
};
export default SitesManagement;
