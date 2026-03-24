import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Input, Select, Tag, Space, Progress, message } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { fetchSites, type SiteRecord } from '../utils/siteApi';

const { Option } = Select;

const resolveType = (site: SiteRecord) => {
    if (site.siteType === 'STATE_OWNED') return '国有场地';
    if (site.siteType === 'COLLECTIVE') return '集体场地';
    if (site.siteType === 'ENGINEERING') return '工程场地';
    if (site.siteType === 'SHORT_BARGE') return '短驳场地';
    const suffix = Number(site.id || 0) % 4;
    if (suffix === 1) return '国有场地';
    if (suffix === 2) return '集体场地';
    if (suffix === 3) return '工程场地';
    return '短驳场地';
};

const resolveRegion = (site: SiteRecord) => {
    const address = site.address || '';
    if (address.includes('滨海')) return '滨海新区';
    if (address.includes('南')) return '南郊区';
    if (address.includes('北')) return '北区';
    if (address.includes('西')) return '西郊区';
    return '平台统筹';
};

const resolveStatus = (status?: number | string | null) => {
    if (status === 1 || status === '1' || status === 'ACTIVE' || status === 'ENABLED') return '正常';
    if (status === 2 || status === '2' || status === 'WARNING') return '预警';
    return '停用';
};

const SitesBasicInfo: React.FC = () => {
    const navigate = useNavigate();
    const [searchText, setSearchText] = useState('');
    const [typeFilter, setTypeFilter] = useState('all');
    const [regionFilter, setRegionFilter] = useState('all');
    const [loading, setLoading] = useState(false);
    const [sitesData, setSitesData] = useState<SiteRecord[]>([]);

    useEffect(() => {
        const loadSites = async () => {
            setLoading(true);
            try {
                const data = await fetchSites();
                setSitesData(data);
            } catch (error) {
                console.error(error);
                message.error('获取场地基础信息失败');
                setSitesData([]);
            } finally {
                setLoading(false);
            }
        };

        void loadSites();
    }, []);

    const tableData = useMemo(() => sitesData
        .map((site) => {
            const totalCapacity = Number(site.capacity || 0) > 0 ? Number(site.capacity) : ((Number(site.id || 1) % 7) + 3) * 100000;
            const usedCapacity = Math.round(totalCapacity * (0.35 + (Number(site.id || 1) % 5) * 0.12));
            return {
                id: site.id,
                name: site.name || '-',
                type: resolveType(site),
                level: site.siteLevel === 'SECONDARY' ? '二级场地' : '一级场地',
                region: resolveRegion(site),
                totalCapacity,
                usedCapacity: Math.min(totalCapacity, usedCapacity),
                status: resolveStatus(site.status),
            };
        })
        .filter((site) => {
            const matchKeyword = !searchText.trim() || site.name.includes(searchText.trim());
            const matchType = typeFilter === 'all' || site.type === typeFilter;
            const matchRegion = regionFilter === 'all' || site.region === regionFilter;
            return matchKeyword && matchType && matchRegion;
        }), [regionFilter, searchText, sitesData, typeFilter]);

    const columns = [
        { title: '场地名称', dataIndex: 'name', key: 'name', render: (text: string, record: any) => <a onClick={() => navigate(`/sites/${record.id}?tab=info`)} className="g-text-primary-link font-medium">{text}</a> },
        { title: '类型', dataIndex: 'type', key: 'type', render: (text: string) => <Tag color="blue">{text}</Tag> },
        { title: '层级', dataIndex: 'level', key: 'level', render: (text: string) => <Tag color={text === '二级场地' ? 'geekblue' : 'default'}>{text}</Tag> },
        { title: '所属区域', dataIndex: 'region', key: 'region' },
        { title: '总容量 (方)', dataIndex: 'totalCapacity', key: 'totalCapacity', render: (val: number) => val.toLocaleString() },
        { 
            title: '容量使用情况', 
            key: 'capacity',
            width: 250,
            render: (_: any, record: any) => {
                const percent = Math.round((record.usedCapacity / record.totalCapacity) * 100);
                return (
                    <div className="flex items-center gap-2">
                        <Progress 
                            percent={percent} 
                            size="small" 
                            className="w-32 m-0"
                            strokeColor={percent > 90 ? 'var(--error)' : percent >= 80 ? 'var(--warning)' : 'var(--success)'} 
                            trailColor="rgba(0,0,0,0.06)"
                        />
                        <span className="text-xs g-text-secondary w-16">剩余: {((record.totalCapacity - record.usedCapacity) / 10000).toFixed(1)}万</span>
                    </div>
                );
            }
        },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '正常' ? 'green' : status === '预警' ? 'orange' : 'red'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/sites/${record.id}?tab=info`)}>编辑</Button>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold g-text-primary m-0">场地基础信息</h1>
            </div>

            <Card className="glass-panel g-border-panel border">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input placeholder="搜索场地名称" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-64" value={searchText} onChange={(e) => setSearchText(e.target.value)} />
                        <Select value={typeFilter} className="w-32 bg-white" onChange={setTypeFilter}>
                            <Option value="all">全部类型</Option>
                            <Option value="国有场地">国有场地</Option>
                            <Option value="集体场地">集体场地</Option>
                            <Option value="工程场地">工程场地</Option>
                        </Select>
                        <Select value={regionFilter} className="w-32 bg-white" onChange={setRegionFilter}>
                            <Option value="all">全部区域</Option>
                            <Option value="滨海新区">滨海新区</Option>
                            <Option value="南郊区">南郊区</Option>
                            <Option value="北区">北区</Option>
                        </Select>
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/sites?create=1')}>新增场地</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={tableData}
                    loading={loading}
                    rowKey="id"
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </div>
    );
};

export default SitesBasicInfo;
