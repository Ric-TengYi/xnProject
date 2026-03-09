import React, { useState } from 'react';
import { Card, Table, Button, Input, Select, Tag, Space, Drawer, Descriptions } from 'antd';
import { SearchOutlined, EyeOutlined } from '@ant-design/icons';

const { Option } = Select;

const permitData = [
    { id: 'PZ-2024-001', type: '排放证', project: '市中心地铁延长线三期工程', plate: '京A·12345', site: '东区临时消纳场', validUntil: '2024-12-31', status: '有效', bindStatus: '已绑定' },
    { id: 'PZ-2024-002', type: '准运证', project: '滨海新区基础建设B标段', plate: '京B·67890', site: '南郊复合型消纳中心', validUntil: '2024-06-30', status: '即将过期', bindStatus: '已绑定' },
    { id: 'PZ-2024-003', type: '排放证', project: '老旧小区改造工程综合包', plate: '-', site: '西郊临时周转站', validUntil: '2025-01-15', status: '有效', bindStatus: '未绑定' },
];

const ProjectsPermits: React.FC = () => {
    const [drawerVisible, setDrawerVisible] = useState(false);
    const [currentPermit, setCurrentPermit] = useState<any>(null);

    const columns = [
        { title: '处置证号', dataIndex: 'id', key: 'id', render: (text: string) => <span className="font-mono text-slate-600 dark:text-slate-300">{text}</span> },
        { title: '类型', dataIndex: 'type', key: 'type', render: (text: string) => <Tag color="blue">{text}</Tag> },
        { title: '关联项目', dataIndex: 'project', key: 'project' },
        { title: '绑定车牌', dataIndex: 'plate', key: 'plate' },
        { title: '指定消纳场', dataIndex: 'site', key: 'site' },
        { title: '有效期至', dataIndex: 'validUntil', key: 'validUntil' },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '有效' ? 'green' : status === '即将过期' ? 'orange' : 'red'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => { setCurrentPermit(record); setDrawerVisible(true); }}>查看</Button>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">处置证清单</h1>
            </div>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input placeholder="搜索证号/项目/车牌" prefix={<SearchOutlined />} className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white w-64" />
                        <Select defaultValue="all" className="w-32 bg-white dark:bg-slate-800">
                            <Option value="all">全部类型</Option>
                            <Option value="排放证">排放证</Option>
                            <Option value="准运证">准运证</Option>
                        </Select>
                        <Select defaultValue="all" className="w-32 bg-white dark:bg-slate-800">
                            <Option value="all">全部状态</Option>
                            <Option value="有效">有效</Option>
                            <Option value="即将过期">即将过期</Option>
                            <Option value="已过期">已过期</Option>
                        </Select>
                        <Button type="primary">查询</Button>
                    </Space>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={permitData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/50 transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Drawer
                title="处置证详情"
                placement="right"
                onClose={() => setDrawerVisible(false)}
                open={drawerVisible}
                width={500}
                className="dark-drawer"
            >
                {currentPermit && (
                    <Descriptions column={1} bordered className="dark-descriptions">
                        <Descriptions.Item label="处置证号">{currentPermit.id}</Descriptions.Item>
                        <Descriptions.Item label="证件类型"><Tag color="blue">{currentPermit.type}</Tag></Descriptions.Item>
                        <Descriptions.Item label="关联项目">{currentPermit.project}</Descriptions.Item>
                        <Descriptions.Item label="指定消纳场">{currentPermit.site}</Descriptions.Item>
                        <Descriptions.Item label="绑定车牌">{currentPermit.plate}</Descriptions.Item>
                        <Descriptions.Item label="有效期">{currentPermit.validUntil}</Descriptions.Item>
                        <Descriptions.Item label="当前状态"><Tag color={currentPermit.status === '有效' ? 'green' : 'orange'}>{currentPermit.status}</Tag></Descriptions.Item>
                        <Descriptions.Item label="绑定状态">{currentPermit.bindStatus}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </div>
    );
};

export default ProjectsPermits;
