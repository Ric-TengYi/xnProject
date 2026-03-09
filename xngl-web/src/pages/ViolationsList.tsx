import React, { useState } from 'react';
import { Card, Table, Tag, Input, Button, DatePicker, Select, Space } from 'antd';
import { SearchOutlined, FilterOutlined, ExportOutlined, UnlockOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { RangePicker } = DatePicker;
const { Option } = Select;

const violationData = [
    { id: 'V001', plate: '川A88921', type: '偏航预警', time: '2024-03-05 14:30:00', location: '科技路与创新大道交汇处', status: '待处理', penalty: '--' },
    { id: 'V002', plate: '川A6258W', type: '未打卡入场', time: '2024-03-04 09:15:00', location: '城东一号消纳场', status: '已处理', penalty: '警告教育' },
    { id: 'V003', plate: '川A1192N', type: '闯禁区', time: '2024-03-03 22:10:00', location: '一环路南段', status: '禁用中', penalty: '停运3天' },
    { id: 'V004', plate: '川B44521', type: '证件过期', time: '2024-03-01 08:00:00', location: '--', status: '已解禁', penalty: '补办证件' },
    { id: 'V005', plate: '川A5582K', type: '超速行驶', time: '2024-02-28 15:45:00', location: '绕城高速东段', status: '已处理', penalty: '罚款200元' },
];

const ViolationsList: React.FC = () => {
    const [searchText, setSearchText] = useState('');

    const columns = [
        { title: '车牌号', dataIndex: 'plate', key: 'plate', render: (text: string) => <strong className="g-text-primary-link">{text}</strong> },
        { 
            title: '违规类型', 
            dataIndex: 'type', 
            key: 'type',
            render: (type: string) => {
                let color = 'default';
                if (type === '闯禁区' || type === '证件过期') color = 'red';
                else if (type === '偏航预警' || type === '超速行驶') color = 'orange';
                else if (type === '未打卡入场') color = 'warning';
                return <Tag color={color} className="border-none">{type}</Tag>;
            }
        },
        { title: '违规时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: '违规地点', dataIndex: 'location', key: 'location', render: (l: string) => <span className="g-text-secondary">{l}</span> },
        { 
            title: '处理状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '待处理': 'error', '已处理': 'success', '禁用中': 'red', '已解禁': 'default' };
                return <Tag color={colorMap[status]} className="border-none">{status}</Tag>;
            }
        },
        { title: '处罚结果', dataIndex: 'penalty', key: 'penalty', render: (p: string) => <span className="g-text-secondary">{p}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link">详情</a>
                    {record.status === '待处理' && <a className="g-text-warning hover:g-text-warning">去处理</a>}
                    {record.status === '禁用中' && <a className="g-text-success hover:g-text-success"><UnlockOutlined /> 提前解禁</a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">违规车辆清单</h1>
                    <p className="g-text-secondary mt-1">记录并处理车辆在运输过程中的各类违规行为及禁用状态</p>
                </div>
            </div>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
                    <Input 
                        placeholder="搜索车牌号" 
                        prefix={<SearchOutlined className="g-text-secondary" />} 
                        className="w-64 bg-white g-border-panel border g-text-primary"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                    />
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white">
                        <Option value="all">全部违规类型</Option>
                        <Option value="type1">闯禁区</Option>
                        <Option value="type2">偏航预警</Option>
                        <Option value="type3">未打卡入场</Option>
                    </Select>
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white">
                        <Option value="all">全部处理状态</Option>
                        <Option value="pending">待处理</Option>
                        <Option value="processed">已处理</Option>
                        <Option value="banned">禁用中</Option>
                    </Select>
                    <RangePicker className="bg-white g-border-panel border g-text-primary" />
                    <div className="flex-1 flex justify-end gap-3">
                        <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">重置</Button>
                        <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出报表</Button>
                    </div>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={violationData.filter(v => v.plate.includes(searchText))} 
                    rowKey="id"
                    pagination={{ defaultPageSize: 10, showSizeChanger: true, className: 'pr-4 pb-2' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default ViolationsList;
