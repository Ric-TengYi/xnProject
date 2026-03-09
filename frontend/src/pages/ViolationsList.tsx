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
        { title: '车牌号', dataIndex: 'plate', key: 'plate', render: (text: string) => <strong className="text-blue-600 dark:text-blue-400">{text}</strong> },
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
        { title: '违规时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="text-slate-600 dark:text-slate-400 font-mono">{t}</span> },
        { title: '违规地点', dataIndex: 'location', key: 'location', render: (l: string) => <span className="text-slate-600 dark:text-slate-300">{l}</span> },
        { 
            title: '处理状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '待处理': 'error', '已处理': 'success', '禁用中': 'red', '已解禁': 'default' };
                return <Tag color={colorMap[status]} className="border-none">{status}</Tag>;
            }
        },
        { title: '处罚结果', dataIndex: 'penalty', key: 'penalty', render: (p: string) => <span className="text-slate-600 dark:text-slate-400">{p}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400">详情</a>
                    {record.status === '待处理' && <a className="text-orange-600 dark:text-orange-500 hover:text-orange-600 dark:text-orange-400">去处理</a>}
                    {record.status === '禁用中' && <a className="text-green-600 dark:text-green-500 hover:text-green-600 dark:text-green-400"><UnlockOutlined /> 提前解禁</a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">违规车辆清单</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">记录并处理车辆在运输过程中的各类违规行为及禁用状态</p>
                </div>
            </div>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b border-slate-200 dark:border-slate-700/50 flex flex-wrap gap-4 bg-slate-50 dark:bg-slate-900/30">
                    <Input 
                        placeholder="搜索车牌号" 
                        prefix={<SearchOutlined className="text-slate-600 dark:text-slate-400" />} 
                        className="w-64 bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                    />
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white dark:bg-slate-800">
                        <Option value="all">全部违规类型</Option>
                        <Option value="type1">闯禁区</Option>
                        <Option value="type2">偏航预警</Option>
                        <Option value="type3">未打卡入场</Option>
                    </Select>
                    <Select defaultValue="all" className="w-40" popupClassName="bg-white dark:bg-slate-800">
                        <Option value="all">全部处理状态</Option>
                        <Option value="pending">待处理</Option>
                        <Option value="processed">已处理</Option>
                        <Option value="banned">禁用中</Option>
                    </Select>
                    <RangePicker className="bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white" />
                    <div className="flex-1 flex justify-end gap-3">
                        <Button icon={<FilterOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white">重置</Button>
                        <Button icon={<ExportOutlined />} className="bg-transparent text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:text-slate-900 dark:text-white">导出报表</Button>
                    </div>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={violationData.filter(v => v.plate.includes(searchText))} 
                    rowKey="id"
                    pagination={{ defaultPageSize: 10, showSizeChanger: true, className: 'pr-4 pb-2' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default ViolationsList;
