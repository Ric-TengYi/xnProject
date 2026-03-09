import React from 'react';
import { Card, Table, Button, Input, Select, Tag, Space, Progress } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Option } = Select;

const sitesData = [
    { id: '1', name: '东区临时消纳场', type: '国有场地', region: '滨海新区', totalCapacity: 500000, usedCapacity: 350000, status: '正常' },
    { id: '2', name: '南郊复合型消纳中心', type: '集体场地', region: '南郊区', totalCapacity: 1200000, usedCapacity: 980000, status: '预警' },
    { id: '3', name: '北区填埋场', type: '工程场地', region: '北区', totalCapacity: 300000, usedCapacity: 45000, status: '正常' },
    { id: '4', name: '西郊临时周转站', type: '短驳场地', region: '西郊区', totalCapacity: 100000, usedCapacity: 95000, status: '预警' },
];

const SitesBasicInfo: React.FC = () => {
    const navigate = useNavigate();

    const columns = [
        { title: '场地名称', dataIndex: 'name', key: 'name', render: (text: string, record: any) => <a onClick={() => navigate(`/sites/${record.id}?tab=info`)} className="g-text-primary-link font-medium">{text}</a> },
        { title: '类型', dataIndex: 'type', key: 'type', render: (text: string) => <Tag color="blue">{text}</Tag> },
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
                        <Input placeholder="搜索场地名称" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-64" />
                        <Select defaultValue="all" className="w-32 bg-white">
                            <Option value="all">全部类型</Option>
                            <Option value="国有场地">国有场地</Option>
                            <Option value="集体场地">集体场地</Option>
                            <Option value="工程场地">工程场地</Option>
                        </Select>
                        <Select defaultValue="all" className="w-32 bg-white">
                            <Option value="all">全部区域</Option>
                            <Option value="滨海新区">滨海新区</Option>
                            <Option value="南郊区">南郊区</Option>
                            <Option value="北区">北区</Option>
                        </Select>
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />}>新增场地</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={sitesData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </div>
    );
};

export default SitesBasicInfo;
