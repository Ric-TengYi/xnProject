import React from 'react';
import { Card, Table, Button, Input, Select, DatePicker, Tag, Space } from 'antd';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons';

const { RangePicker } = DatePicker;
const { Option } = Select;

const disposalsData = [
    { id: 'REC-001', site: '东区临时消纳场', time: '2024-03-05 14:30:22', plate: '京A·12345', project: '市中心地铁延长线三期工程', source: '工地直运', volume: 20, status: '正常' },
    { id: 'REC-002', site: '南郊复合型消纳中心', time: '2024-03-05 14:15:10', plate: '京B·67890', project: '滨海新区基础建设B标段', source: '中转站调拨', volume: 18, status: '正常' },
    { id: 'REC-003', site: '东区临时消纳场', time: '2024-03-05 13:50:05', plate: '京A·54321', project: '未知项目', source: '不明来源', volume: 15, status: '异常' },
    { id: 'REC-004', site: '北区填埋场', time: '2024-03-05 13:20:00', plate: '京C·11223', project: '老旧小区改造工程综合包', source: '工地直运', volume: 22, status: '正常' },
];

const SitesDisposals: React.FC = () => {
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
                        <Select defaultValue="all" className="w-40 bg-white">
                            <Option value="all">全部场地</Option>
                            <Option value="1">东区临时消纳场</Option>
                            <Option value="2">南郊复合型消纳中心</Option>
                        </Select>
                        <Input placeholder="搜索车牌/项目" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-48" />
                        <RangePicker className="bg-white g-border-panel border" showTime />
                        <Select defaultValue="all" className="w-32 bg-white">
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
                    dataSource={disposalsData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </div>
    );
};

export default SitesDisposals;
