import React, { useState } from 'react';
import { Card, Table, Button, Input, Tabs, Space, Tree } from 'antd';
import { SearchOutlined, FolderOpenOutlined, EyeOutlined, DownloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { DirectoryTree } = Tree;

const treeData = [
    {
        title: '所有场地',
        key: '0-0',
        children: [
            { title: '东区临时消纳场', key: '1', isLeaf: true },
            { title: '南郊复合型消纳中心', key: '2', isLeaf: true },
            { title: '北区填埋场', key: '3', isLeaf: true },
            { title: '西郊临时周转站', key: '4', isLeaf: true },
        ],
    },
];

const docsData = [
    { id: '1', siteId: '1', site: '东区临时消纳场', type: '环评批复', count: 2, lastUpdate: '2024-02-15 10:00:00', uploader: '张三' },
    { id: '2', siteId: '1', site: '东区临时消纳场', type: '土地租赁合同', count: 1, lastUpdate: '2024-01-20 14:30:00', uploader: '李四' },
    { id: '3', siteId: '2', site: '南郊复合型消纳中心', type: '营业执照', count: 1, lastUpdate: '2023-11-11 09:15:00', uploader: '王五' },
];

const SitesDocuments: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('approval');

    const columns = [
        { title: '场地名称', dataIndex: 'site', key: 'site', render: (text: string) => <span className="text-blue-600 dark:text-blue-400 font-medium">{text}</span> },
        { title: '资料类型', dataIndex: 'type', key: 'type' },
        { title: '文件数量', dataIndex: 'count', key: 'count', render: (val: number) => <span className="text-slate-600 dark:text-slate-300">{val} 个</span> },
        { title: '最近上传时间', dataIndex: 'lastUpdate', key: 'lastUpdate' },
        { title: '上传人', dataIndex: 'uploader', key: 'uploader' },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => navigate(`/sites/${record.siteId}?tab=docs`)}>查看</Button>
                    <Button type="link" size="small" icon={<DownloadOutlined />}>下载</Button>
                </Space>
            ),
        },
    ];

    return (
        <div className="space-y-6 h-[calc(100vh-120px)] flex flex-col">
            <div className="flex justify-between items-center mb-2 shrink-0">
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">场地资料库</h1>
            </div>

            <div className="flex flex-1 gap-4 overflow-hidden">
                {/* 左侧场地树 */}
                <Card className="glass-panel w-64 shrink-0 overflow-auto border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: '16px' }}>
                    <Input placeholder="搜索场地" prefix={<SearchOutlined />} className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white mb-4" />
                    <DirectoryTree
                        multiple
                        defaultExpandAll
                        treeData={treeData}
                        className="bg-transparent text-slate-600 dark:text-slate-300"
                    />
                </Card>

                {/* 右侧资料列表 */}
                <Card className="glass-panel flex-1 overflow-auto border-slate-200 dark:border-slate-700/50 flex flex-col" bodyStyle={{ padding: '16px', display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <Tabs
                        activeKey={activeTab}
                        onChange={setActiveTab}
                        className="custom-tabs shrink-0"
                        items={[
                            { key: 'approval', label: '审批资料' },
                            { key: 'operation', label: '运营资料' },
                            { key: 'transfer', label: '移交资料' },
                        ]}
                    />
                    
                    <div className="flex justify-between mb-4 shrink-0">
                        <Space>
                            <Input placeholder="搜索资料名称" prefix={<SearchOutlined />} className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white w-64" />
                            <Button type="primary">查询</Button>
                        </Space>
                        <Button type="primary" icon={<FolderOpenOutlined />}>批量上传</Button>
                    </div>

                    <div className="flex-1 overflow-auto">
                        <Table 
                            columns={columns} 
                            dataSource={docsData} 
                            className="bg-transparent"
                            rowClassName="hover:bg-white dark:bg-slate-800/50 transition-colors"
                            pagination={{ pageSize: 10 }}
                        />
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default SitesDocuments;
