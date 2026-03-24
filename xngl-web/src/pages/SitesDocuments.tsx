import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Input, Tabs, Space, Tree, message, Tag, Select, Statistic } from 'antd';
import { SearchOutlined, FolderOpenOutlined, EyeOutlined, DownloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { fetchSiteDocumentSummary, fetchSites, type SiteDocumentSummaryRecord, type SiteRecord } from '../utils/siteApi';

const { DirectoryTree } = Tree;

const resolveStageLabel = (stageCode?: string | null) => {
    if (stageCode === 'APPROVAL') return '审批资料';
    if (stageCode === 'CONSTRUCTION') return '建设资料';
    if (stageCode === 'OPERATION') return '运营资料';
    if (stageCode === 'TRANSFER') return '移交资料';
    return stageCode || '-';
};

const resolveApprovalLabel = (approvalType?: string | null) => {
    switch (approvalType) {
        case 'PROJECT':
            return '立项';
        case 'EIA':
            return '环评';
        case 'LAND':
            return '用地/租赁';
        case 'LICENSE':
            return '证照';
        case 'CONSTRUCTION':
            return '建设';
        case 'SAFETY':
            return '安全运营';
        case 'TRANSFER':
            return '移交验收';
        default:
            return approvalType || '未分类';
    }
};

const resolveDocumentLabel = (documentType?: string | null) => {
    switch (documentType) {
        case 'PROJECT_APPROVAL':
            return '立项批复';
        case 'EIA_APPROVAL':
            return '环评批复';
        case 'LAND_LEASE':
            return '土地租赁合同';
        case 'BUSINESS_LICENSE':
            return '营业执照';
        case 'CONSTRUCTION_PLAN':
            return '建设方案';
        case 'BOUNDARY_SURVEY':
            return '红线测绘资料';
        case 'COMPLETION_ACCEPTANCE':
            return '竣工验收资料';
        case 'SAFETY_INSPECTION':
            return '安全检查记录';
        case 'OPERATION_LEDGER':
            return '运营台账';
        case 'WEIGHBRIDGE_RECORD':
            return '地磅记录';
        case 'TRANSFER_ACCEPTANCE':
            return '移交验收资料';
        case 'SITE_ARCHIVE':
            return '场地归档包';
        default:
            return documentType || '-';
    }
};

const SitesDocuments: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('approval');
    const [keyword, setKeyword] = useState('');
    const [siteKeyword, setSiteKeyword] = useState('');
    const [approvalType, setApprovalType] = useState<string>();
    const [selectedSiteId, setSelectedSiteId] = useState<string>();
    const [sites, setSites] = useState<SiteRecord[]>([]);
    const [records, setRecords] = useState<SiteDocumentSummaryRecord[]>([]);
    const [loading, setLoading] = useState(false);

    const stageCode = useMemo(() => {
        if (activeTab === 'approval') return 'APPROVAL';
        if (activeTab === 'construction') return 'CONSTRUCTION';
        if (activeTab === 'operation') return 'OPERATION';
        if (activeTab === 'transfer') return 'TRANSFER';
        return undefined;
    }, [activeTab]);

    useEffect(() => {
        void fetchSites().then(setSites).catch((error) => {
            console.error(error);
            message.error('获取场地列表失败');
        });
    }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            setRecords(await fetchSiteDocumentSummary({
                siteId: selectedSiteId,
                stageCode,
                approvalType,
                keyword: keyword.trim() || undefined,
            }));
        } catch (error) {
            console.error(error);
            message.error('获取场地资料汇总失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadData();
    }, [activeTab, selectedSiteId, approvalType]);

    const treeData = useMemo(() => [{
        title: '所有场地',
        key: 'root',
        children: sites
            .filter((item) => !siteKeyword.trim() || item.name.toLowerCase().includes(siteKeyword.trim().toLowerCase()))
            .map((item) => ({
            title: item.name,
            key: item.id,
            isLeaf: true,
        })),
    }], [siteKeyword, sites]);

    const totalDocuments = useMemo(
        () => records.reduce((sum, item) => sum + Number(item.documentCount || 0), 0),
        [records],
    );

    const columns = [
        { title: '场地名称', dataIndex: 'site', key: 'site', render: (text: string) => <span className="g-text-primary-link font-medium">{text}</span> },
        {
            title: '资料归类',
            dataIndex: 'type',
            key: 'type',
            render: (text: string, record: any) => (
                <Space wrap>
                    <Tag color="blue">{resolveStageLabel(record.stageCode)}</Tag>
                    <Tag color="gold">{resolveApprovalLabel(record.approvalType)}</Tag>
                    <span>{text}</span>
                </Space>
            ),
        },
        { title: '文件数量', dataIndex: 'count', key: 'count', render: (val: number) => <span className="g-text-secondary">{val} 个</span> },
        { title: '最近上传时间', dataIndex: 'lastUpdate', key: 'lastUpdate' },
        { title: '上传人', dataIndex: 'uploader', key: 'uploader' },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => navigate(`/sites/${record.siteId}?tab=docs`)}>查看</Button>
                    <Button type="link" size="small" icon={<DownloadOutlined />} onClick={() => navigate(`/sites/${record.siteId}?tab=docs`)}>下载</Button>
                </Space>
            ),
        },
    ];

    return (
        <div className="space-y-6 h-[calc(100vh-120px)] flex flex-col">
            <div className="flex justify-between items-center mb-2 shrink-0">
                <h1 className="text-2xl font-bold g-text-primary m-0">场地资料库</h1>
            </div>

            <div className="flex flex-1 gap-4 overflow-hidden">
                {/* 左侧场地树 */}
                <Card className="glass-panel w-64 shrink-0 overflow-auto g-border-panel border" bodyStyle={{ padding: '16px' }}>
                    <Input
                        value={siteKeyword}
                        onChange={(e) => setSiteKeyword(e.target.value)}
                        placeholder="搜索场地"
                        prefix={<SearchOutlined />}
                        className="bg-white g-border-panel border g-text-primary mb-4"
                    />
                    <DirectoryTree
                        defaultExpandAll
                        treeData={treeData}
                        onSelect={(keys) => setSelectedSiteId(keys[0] && keys[0] !== 'root' ? String(keys[0]) : undefined)}
                        className="bg-transparent g-text-secondary"
                    />
                </Card>

                {/* 右侧资料列表 */}
                <Card className="glass-panel flex-1 overflow-auto g-border-panel border flex flex-col" bodyStyle={{ padding: '16px', display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <Tabs
                        activeKey={activeTab}
                        onChange={setActiveTab}
                        className="custom-tabs shrink-0"
                        items={[
                            { key: 'approval', label: '审批资料' },
                            { key: 'construction', label: '建设资料' },
                            { key: 'operation', label: '运营资料' },
                            { key: 'transfer', label: '移交资料' },
                        ]}
                    />

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4 shrink-0">
                        <Card className="bg-white/70">
                            <Statistic title="归档分组数" value={records.length} suffix="类" />
                        </Card>
                        <Card className="bg-white/70">
                            <Statistic title="文件总数" value={totalDocuments} suffix="份" />
                        </Card>
                        <Card className="bg-white/70">
                            <Statistic title="当前场地" value={selectedSiteId ? 1 : sites.length} suffix={selectedSiteId ? '个' : '个场地'} />
                        </Card>
                    </div>
                    
                    <div className="flex justify-between mb-4 shrink-0">
                        <Space>
                            <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="搜索资料名称" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-64" />
                            <Select
                                allowClear
                                placeholder="全部审批类型"
                                value={approvalType}
                                onChange={(value) => setApprovalType(value)}
                                className="w-44"
                                options={[
                                    { value: 'PROJECT', label: '立项' },
                                    { value: 'EIA', label: '环评' },
                                    { value: 'LAND', label: '用地/租赁' },
                                    { value: 'LICENSE', label: '证照' },
                                    { value: 'CONSTRUCTION', label: '建设' },
                                    { value: 'SAFETY', label: '安全运营' },
                                    { value: 'TRANSFER', label: '移交验收' },
                                ]}
                            />
                            <Button type="primary" onClick={() => void loadData()}>查询</Button>
                        </Space>
                        <Button type="primary" icon={<FolderOpenOutlined />} onClick={() => navigate(`/sites/${selectedSiteId || sites[0]?.id || '1'}?tab=docs`)}>上传资料</Button>
                    </div>

                    <div className="flex-1 overflow-auto">
                        <Table 
                            loading={loading}
                            columns={columns} 
                            rowKey={(record) => `${record.siteId}-${record.stageCode}-${record.approvalType}-${record.documentType}`}
                            dataSource={records.map((item) => ({
                                siteId: item.siteId,
                                site: item.siteName || '-',
                                type: resolveDocumentLabel(item.documentType),
                                count: item.documentCount || 0,
                                lastUpdate: item.lastUpdateTime || '-',
                                uploader: item.uploaderName || '-',
                                stageCode: item.stageCode,
                                approvalType: item.approvalType,
                                documentType: item.documentType,
                            }))} 
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                            pagination={{ pageSize: 10 }}
                        />
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default SitesDocuments;
