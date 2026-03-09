import React, { useState } from 'react';
import { Card, Table, Tag, Input, Button, Drawer, Space, Descriptions, Divider } from 'antd';
import { SearchOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const eventsData = [
    { id: 'EV-20240305-01', type: '延期申报', title: '滨海新区项目因暴雨申请延期3天', reporter: '王建国', time: '2024-03-05 09:30', status: '待审核' },
    { id: 'EV-20240304-12', type: '场地事件', title: '东区消纳场入口道路损坏', reporter: '李场地', time: '2024-03-04 15:20', status: '处理中' },
    { id: 'EV-20240303-05', type: '违规举报', title: '群众举报夜间噪音扰民', reporter: '匿名', time: '2024-03-03 22:15', status: '已完成' },
    { id: 'EV-20240302-08', type: '其他', title: '地磅设备网络故障报修', reporter: '张三', time: '2024-03-02 10:00', status: '已关闭' },
];

const EventsManagement: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [drawerVisible, setDrawerVisible] = useState(false);
    const [selectedEvent, setSelectedEvent] = useState<any>(null);

    const handleView = (record: any) => {
        setSelectedEvent(record);
        setDrawerVisible(true);
    };

    const columns = [
        { title: '事件编号', dataIndex: 'id', key: 'id', render: (t: string) => <span className="text-blue-600 dark:text-blue-400 font-mono">{t}</span> },
        { title: '事件类型', dataIndex: 'type', key: 'type', render: (t: string) => <Tag color="blue" className="border-none">{t}</Tag> },
        { title: '标题', dataIndex: 'title', key: 'title', render: (t: string) => <strong className="text-slate-700 dark:text-slate-200">{t}</strong> },
        { title: '申报人', dataIndex: 'reporter', key: 'reporter', render: (t: string) => <span className="text-slate-600 dark:text-slate-400">{t}</span> },
        { title: '申报时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="text-slate-600 dark:text-slate-400">{t}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '待审核': 'warning', '处理中': 'processing', '已完成': 'success', '已关闭': 'default' };
                return <Tag color={colorMap[status]} className="border-none">{status}</Tag>;
            }
        },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: any, record: any) => (
                <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400" onClick={() => handleView(record)}>
                    {record.status === '待审核' ? '审核' : '详情'}
                </a>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">事件管理</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">管理人工上报的异常事件，支持审核流转与闭环处理</p>
                </div>
            </div>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <div className="p-4 border-b border-slate-200 dark:border-slate-700/50 flex gap-4 bg-slate-50 dark:bg-slate-900/30">
                    <Input 
                        placeholder="搜索事件标题/编号" 
                        prefix={<SearchOutlined className="text-slate-600 dark:text-slate-400" />} 
                        className="w-72 bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                    />
                </div>

                <Table 
                    columns={columns} 
                    dataSource={eventsData.filter(d => d.title.includes(searchText) || d.id.includes(searchText))} 
                    rowKey="id"
                    pagination={{ defaultPageSize: 10, className: 'pr-4 pb-2' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors"
                />
            </Card>

            <Drawer
                title={<span className="text-slate-900 dark:text-white">事件详情</span>}
                placement="right"
                width={500}
                onClose={() => setDrawerVisible(false)}
                open={drawerVisible}
                extra={
                    selectedEvent?.status === '待审核' && (
                        <Space>
                            <Button danger icon={<CloseCircleOutlined />}>退回</Button>
                            <Button type="primary" icon={<CheckCircleOutlined />} className="bg-green-600 hover:bg-green-500 border-none">通过</Button>
                        </Space>
                    )
                }
            >
                {selectedEvent && (
                    <div className="space-y-6">
                        <Descriptions column={1} labelStyle={{ width: '100px' }} contentStyle={{ color: 'var(--text-primary)' }}>
                            <Descriptions.Item label="事件编号">{selectedEvent.id}</Descriptions.Item>
                            <Descriptions.Item label="事件类型"><Tag color="blue">{selectedEvent.type}</Tag></Descriptions.Item>
                            <Descriptions.Item label="当前状态"><Tag color="warning">{selectedEvent.status}</Tag></Descriptions.Item>
                            <Descriptions.Item label="申报人">{selectedEvent.reporter}</Descriptions.Item>
                            <Descriptions.Item label="申报时间">{selectedEvent.time}</Descriptions.Item>
                        </Descriptions>
                        
                        <Divider className="border-slate-200 dark:border-slate-700/50" />
                        
                        <div>
                            <div className="text-slate-600 dark:text-slate-400 mb-2">事件标题</div>
                            <div className="text-slate-900 dark:text-white text-lg font-bold">{selectedEvent.title}</div>
                        </div>
                        
                        <div>
                            <div className="text-slate-600 dark:text-slate-400 mb-2">详细描述</div>
                            <div className="text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-800/50 p-4 rounded-lg border border-slate-200 dark:border-slate-700/50 min-h-[100px]">
                                根据气象台预报，未来三天有持续暴雨，导致现场无法正常施工出土，特申请项目消纳期限顺延3天。
                            </div>
                        </div>
                    </div>
                )}
            </Drawer>
        </motion.div>
    );
};

export default EventsManagement;
