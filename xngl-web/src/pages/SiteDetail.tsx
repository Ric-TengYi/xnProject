import React, { useEffect, useMemo, useState } from 'react';
import { Card, Tabs, Descriptions, Tag, Button, Space, Table, List, Switch, InputNumber, Select, Empty, Spin, message } from 'antd';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { fetchSiteDetail, fetchSiteDisposals, type DisposalRecord, type SiteRecord } from '../utils/siteApi';

const resolveSiteType = (site?: SiteRecord | null) => {
    if (!site) return '-';
    const code = site.code ?? '';
    const suffix = Number(site.id || 0) % 4;
    if (code.startsWith('GY') || suffix === 1) return '国有场地';
    if (code.startsWith('JT') || suffix === 2) return '集体场地';
    if (code.startsWith('GC') || suffix === 3) return '工程场地';
    return '短驳场地';
};

const resolveStatus = (status?: number | string | null) => {
    if (status === 1 || status === '1' || status === 'ACTIVE' || status === 'ENABLED') return '正常';
    if (status === 2 || status === '2' || status === 'WARNING') return '预警';
    if (status === 0 || status === '0' || status === 'INACTIVE' || status === 'DISABLED') return '停用';
    return '正常';
};

const buildCapacity = (site?: SiteRecord | null) => {
    if (!site) return 0;
    return ((Number(site.id || 1) % 7) + 3) * 100000;
};

const SiteDetail: React.FC = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const defaultTab = searchParams.get('tab') || 'info';
    const [loading, setLoading] = useState(false);
    const [siteInfo, setSiteInfo] = useState<SiteRecord | null>(null);
    const [disposalsLoading, setDisposalsLoading] = useState(false);
    const [disposals, setDisposals] = useState<DisposalRecord[]>([]);

    useEffect(() => {
        if (!id) {
            return;
        }
        const loadData = async () => {
            setLoading(true);
            setDisposalsLoading(true);
            try {
                const [site, disposalPage] = await Promise.all([
                    fetchSiteDetail(id),
                    fetchSiteDisposals({ siteId: id, pageNo: 1, pageSize: 10 }),
                ]);
                setSiteInfo(site);
                setDisposals(disposalPage.records || []);
            } catch (error) {
                console.error(error);
                message.error('获取场地详情失败');
                setSiteInfo(null);
                setDisposals([]);
            } finally {
                setDisposalsLoading(false);
                setLoading(false);
            }
        };

        void loadData();
    }, [id]);

    const capacity = useMemo(() => buildCapacity(siteInfo), [siteInfo]);
    const used = useMemo(() => Math.round(capacity * (0.35 + (Number(siteInfo?.id || 1) % 5) * 0.12)), [capacity, siteInfo]);
    const remaining = Math.max(capacity - used, 0);
    const typeText = useMemo(() => resolveSiteType(siteInfo), [siteInfo]);
    const statusText = useMemo(() => resolveStatus(siteInfo?.status), [siteInfo]);

    const items = [
        {
            key: 'info',
            label: '基础信息',
            children: (
                <div className="space-y-6">
                    <Card title="场地基础信息" className="glass-panel g-border-panel border" extra={<Button type="link" icon={<EditOutlined />}>编辑</Button>}>
                        <Descriptions column={3} className="g-text-secondary">
                            <Descriptions.Item label="场地名称">{siteInfo?.name || '-'}</Descriptions.Item>
                            <Descriptions.Item label="场地类型"><Tag color="blue">{typeText}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地状态"><Tag color={statusText === '正常' ? 'green' : statusText === '预警' ? 'orange' : 'default'}>{statusText}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地编码">{siteInfo?.code || '-'}</Descriptions.Item>
                            <Descriptions.Item label="总容量">{(capacity / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="已用容量">{(used / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="剩余容量">{(remaining / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="详细地址" span={2}>{siteInfo?.address || '-'}</Descriptions.Item>
                            <Descriptions.Item label="关联项目ID">{siteInfo?.projectId || '-'}</Descriptions.Item>
                            <Descriptions.Item label="所属组织ID">{siteInfo?.orgId || '-'}</Descriptions.Item>
                            <Descriptions.Item label="创建时间">{siteInfo?.createTime || '-'}</Descriptions.Item>
                            <Descriptions.Item label="更新时间">{siteInfo?.updateTime || '-'}</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    <Card title="结算规则配置" className="glass-panel g-border-panel border">
                        <Descriptions column={2} className="g-text-secondary">
                            <Descriptions.Item label="结算方式">{typeText === '国有场地' ? '按月结算申请' : '按实际消纳量结算'}</Descriptions.Item>
                            <Descriptions.Item label="计费规则">金额 = 消纳量 × 单价(政府定价或合同单价)</Descriptions.Item>
                        </Descriptions>
                    </Card>
                </div>
            ),
        },
        {
            key: 'disposals',
            label: '消纳清单',
            children: (
                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between">
                        <Space>
                            <Select defaultValue="all" style={{ width: 120 }} options={[{ value: 'all', label: '全部来源' }, { value: 'scale', label: '地磅称重' }, { value: 'manual', label: '人工录入' }]} />
                            <Select defaultValue="valid" style={{ width: 120 }} options={[{ value: 'valid', label: '有效记录' }, { value: 'invalid', label: '已作废' }]} />
                        </Space>
                        <Button>导出 Excel</Button>
                    </div>
                    <Table
                        locale={{ emptyText: disposalsLoading ? '加载中...' : '当前无消纳记录' }}
                        dataSource={disposals.map((item) => ({
                            id: item.id,
                            time: item.time || '-',
                            car: item.plate || '-',
                            project: item.project || '-',
                            source: item.source || '-',
                            amount: item.volume ?? 0,
                            status: item.status || '空',
                        }))}
                        loading={disposalsLoading}
                        rowKey="id"
                        columns={[
                            { title: '记录编号', dataIndex: 'id', key: 'id' },
                            { title: '消纳时间', dataIndex: 'time', key: 'time' },
                            { title: '车牌号', dataIndex: 'car', key: 'car' },
                            { title: '来源', dataIndex: 'source', key: 'source' },
                            { title: '消纳量(方)', dataIndex: 'amount', key: 'amount' },
                            { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === '正常' || s === '有效' ? 'success' : 'default'}>{s}</Tag> },
                            { title: '操作', key: 'action', render: () => <a className="g-text-error">作废</a> }
                        ]}
                        pagination={false}
                        className="bg-transparent"
                        rowClassName="hover:bg-white transition-colors"
                    />
                </Card>
            ),
        },
        {
            key: 'docs',
            label: '场地资料',
            children: (
                <div className="space-y-6">
                    <Card title="审批阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">上传资料</Button>}>
                        <List
                            dataSource={[
                                { name: '立项批复文件.pdf', time: '2023-10-01', uploader: '系统管理员' },
                                { name: '环评批复报告.pdf', time: '2023-10-15', uploader: '系统管理员' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>预览</a>, <a>下载</a>]}>
                                    <List.Item.Meta title={<span className="g-text-primary">{item.name}</span>} description={<span className="g-text-secondary">{item.uploader} 上传于 {item.time}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="运营阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">上传资料</Button>}>
                        <List
                            dataSource={[
                                { name: '2024年1月安全检查记录.docx', time: '2024-02-01', uploader: '李四' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>预览</a>, <a>下载</a>]}>
                                    <List.Item.Meta title={<span className="g-text-primary">{item.name}</span>} description={<span className="g-text-secondary">{item.uploader} 上传于 {item.time}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'config',
            label: '场地配置',
            children: (
                <div className="space-y-6">
                    <Card title="设备配置" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">新增设备</Button>}>
                        <List
                            dataSource={[
                                { name: '入口抓拍机', type: '抓拍机', ip: '192.168.1.101', status: '在线' },
                                { name: '1号地磅', type: '地磅', ip: '192.168.1.102', status: '在线' },
                                { name: '全景监控', type: '视频', ip: '192.168.1.103', status: '离线' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>配置</a>]}>
                                    <List.Item.Meta
                                        title={<Space><span className="g-text-primary">{item.name}</span><Tag color={item.status === '在线' ? 'success' : 'error'}>{item.status}</Tag></Space>}
                                        description={<span className="g-text-secondary">类型: {item.type} | IP: {item.ip}</span>}
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                    
                    <Card title="运营配置" className="glass-panel g-border-panel border">
                        <Descriptions column={1} labelStyle={{ width: '200px' }}>
                            <Descriptions.Item label="排号规则">
                                <Switch defaultChecked /> <span className="ml-2 g-text-secondary">开启后车辆需排队入场</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="最大等待数">
                                <InputNumber defaultValue={50} min={1} />
                            </Descriptions.Item>
                            <Descriptions.Item label="人工消纳开关">
                                <Switch /> <span className="ml-2 g-text-secondary">开启后可在系统手动录入消纳记录(用于异常情况)</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="范围检测半径 (米)">
                                <InputNumber defaultValue={200} min={50} />
                            </Descriptions.Item>
                        </Descriptions>
                        <div className="mt-4">
                            <Button type="primary">保存配置</Button>
                        </div>
                    </Card>
                </div>
            ),
        }
    ];

    return (
        <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3 }}
            className="space-y-6 pb-10"
        >
            <div className="flex items-center gap-4 mb-6">
                <Button 
                    type="text" 
                    icon={<ArrowLeftOutlined />} 
                    onClick={() => navigate('/sites')}
                    className="g-text-secondary hover:g-text-primary"
                />
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">{siteInfo?.name || '场地详情'}</h1>
                    <p className="g-text-secondary mt-1">场地类型: {typeText}</p>
                </div>
            </div>
            <Spin spinning={loading}>
                {siteInfo ? (
                    <Tabs
                        defaultActiveKey={defaultTab}
                        items={items}
                        className="custom-tabs"
                    />
                ) : (
                    <Card className="glass-panel g-border-panel border">
                        <Empty description="场地不存在或暂无数据" />
                    </Card>
                )}
            </Spin>
        </motion.div>
    );
};

export default SiteDetail;
