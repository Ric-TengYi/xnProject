import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Input, Select, DatePicker, Tag, Space, message } from 'antd';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons';
import { fetchSiteDisposals, fetchSites, type DisposalRecord, type SiteRecord } from '../utils/siteApi';

const { RangePicker } = DatePicker;
const { Option } = Select;

const SitesDisposals: React.FC = () => {
    const [sites, setSites] = useState<SiteRecord[]>([]);
    const [loading, setLoading] = useState(false);
    const [siteId, setSiteId] = useState('all');
    const [searchText, setSearchText] = useState('');
    const [status, setStatus] = useState('all');
    const [pageNo, setPageNo] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [records, setRecords] = useState<DisposalRecord[]>([]);

    useEffect(() => {
        const loadSites = async () => {
            try {
                const data = await fetchSites();
                setSites(data);
            } catch (error) {
                console.error(error);
            }
        };
        void loadSites();
    }, []);

    useEffect(() => {
        const loadDisposals = async () => {
            setLoading(true);
            try {
                const page = await fetchSiteDisposals({
                    siteId: siteId === 'all' ? undefined : siteId,
                    keyword: searchText.trim() || undefined,
                    status: status === 'all' ? undefined : status,
                    pageNo,
                    pageSize,
                });
                setRecords(page.records || []);
                setTotal(page.total || 0);
            } catch (error) {
                console.error(error);
                message.error('获取消纳清单失败');
                setRecords([]);
                setTotal(0);
            } finally {
                setLoading(false);
            }
        };
        void loadDisposals();
    }, [pageNo, pageSize, searchText, siteId, status]);

    const tableData = useMemo(() => records.map((item) => ({
        id: item.id,
        site: item.site || sites.find((site) => String(site.id) === String(item.siteId))?.name || '-',
        time: item.time || '-',
        plate: item.plate || '-',
        project: item.project || '-',
        source: item.source || '-',
        volume: item.volume ?? 0,
        status: item.status || '暂无',
    })), [records, sites]);

    const columns = [
        { title: '记录编号', dataIndex: 'id', key: 'id', render: (text: string) => <span className="font-mono g-text-secondary">{text}</span> },
        { title: '场地名称', dataIndex: 'site', key: 'site', render: (text: string) => <span className="g-text-primary-link">{text}</span> },
        { title: '消纳时间', dataIndex: 'time', key: 'time' },
        { title: '车牌号', dataIndex: 'plate', key: 'plate', render: (text: string) => <Tag color="blue">{text}</Tag> },
        { title: '关联项目', dataIndex: 'project', key: 'project' },
        { title: '来源', dataIndex: 'source', key: 'source' },
        { title: '消纳量 (方)', dataIndex: 'volume', key: 'volume', render: (val: number) => <span className="g-text-success font-bold">{val}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '正常' ? 'green' : 'red'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: () => (
                <Button type="link" size="small">详情</Button>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold g-text-primary m-0">全局消纳清单</h1>
            </div>

            <Card className="glass-panel g-border-panel border">
                <div className="flex justify-between mb-4 flex-wrap gap-4">
                    <Space className="flex-wrap">
                        <Select value={siteId} className="w-40 bg-white" onChange={(value) => { setSiteId(value); setPageNo(1); }}>
                            <Option value="all">全部场地</Option>
                            {sites.map((site) => (
                                <Option value={site.id} key={site.id}>{site.name}</Option>
                            ))}
                        </Select>
                        <Input placeholder="搜索车牌/项目" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-48" value={searchText} onChange={(e) => { setSearchText(e.target.value); setPageNo(1); }} />
                        <RangePicker className="bg-white g-border-panel border" showTime />
                        <Select value={status} className="w-32 bg-white" onChange={(value) => { setStatus(value); setPageNo(1); }}>
                            <Option value="all">全部状态</Option>
                            <Option value="正常">正常</Option>
                            <Option value="异常">异常</Option>
                        </Select>
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button icon={<DownloadOutlined />} className="bg-white g-border-panel border g-text-primary hover:g-text-primary-link hover:border-blue-400">导出数据</Button>
                </div>
                <Table 
                    columns={columns} 
                    dataSource={tableData}
                    rowKey="id"
                    loading={loading}
                    locale={{ emptyText: '当前暂无消纳记录，后续接入 biz_disposal 后会自动展示' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{
                        current: pageNo,
                        pageSize,
                        total,
                        showSizeChanger: true,
                        onChange: (nextPage, nextPageSize) => {
                            setPageNo(nextPage);
                            setPageSize(nextPageSize);
                        },
                    }}
                />
            </Card>
        </div>
    );

export default SitesDisposals;
