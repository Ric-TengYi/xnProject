import React, { useState } from 'react';
import { Card, Tabs, Table, Input, DatePicker, Button, Tag } from 'antd';
import { SearchOutlined, ExportOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { TabPane } = Tabs;
const { RangePicker } = DatePicker;

const loginLogs = [
    { id: 1, account: 'admin', time: '2024-03-05 10:00:23', ip: '192.168.1.100', browser: 'Chrome 120.0', os: 'Windows 11', status: '成功' },
    { id: 2, account: 'zhangsan', time: '2024-03-05 09:45:12', ip: '114.254.12.33', browser: 'Safari 17.0', os: 'macOS', status: '失败 (密码错误)' },
    { id: 3, account: 'lisi', time: '2024-03-05 08:30:00', ip: '10.0.0.55', browser: 'WeChat', os: 'iOS', status: '成功' },
];

const operateLogs = [
    { id: 1, operator: '系统管理员', module: '合同管理', action: '新增合同', content: '创建合同 HT-2403-0102', time: '2024-03-05 10:15:30', ip: '192.168.1.100' },
    { id: 2, operator: '李场地', module: '消纳场地', action: '设备配置', content: '修改 1号地磅 IP 地址', time: '2024-03-04 16:20:11', ip: '10.0.0.88' },
    { id: 3, operator: '王执法', module: '预警管理', action: '预警处置', content: '处理违规记录 V002', time: '2024-03-04 11:05:45', ip: '114.254.12.33' },
];

const SystemLogs: React.FC = () => {
    const [searchText, setSearchText] = useState('');

    const loginColumns = [
        { title: '登录账号', dataIndex: 'account', key: 'account', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '登录时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: 'IP 地址', dataIndex: 'ip', key: 'ip', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '终端信息', key: 'device', render: (_: any, r: any) => <span className="g-text-secondary text-sm">{r.os} / {r.browser}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === '成功' ? 'success' : 'error'} className="border-none">{status}</Tag>
        },
    ];

    const operateColumns = [
        { title: '操作人', dataIndex: 'operator', key: 'operator', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '操作模块', dataIndex: 'module', key: 'module', render: (t: string) => <Tag color="blue" className="border-none">{t}</Tag> },
        { title: '操作类型', dataIndex: 'action', key: 'action', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '操作内容', dataIndex: 'content', key: 'content', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '操作时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: 'IP 地址', dataIndex: 'ip', key: 'ip', render: (t: string) => <span className="g-text-secondary text-sm">{t}</span> },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">系统日志</h1>
                    <p className="g-text-secondary mt-1">查询系统登录日志、操作行为审计及系统错误日志</p>
                </div>
            </div>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <Tabs defaultActiveKey="1" className="custom-tabs px-6 pt-4">
                    <TabPane tab="操作日志" key="1">
                        <div className="pb-4 flex flex-wrap gap-4">
                            <Input placeholder="搜索操作人/模块/内容" prefix={<SearchOutlined className="g-text-secondary" />} className="w-64 bg-white g-border-panel border g-text-primary" value={searchText} onChange={e => setSearchText(e.target.value)} />
                            <RangePicker className="bg-white g-border-panel border g-text-primary" />
                            <div className="flex-1 flex justify-end">
                                <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出</Button>
                            </div>
                        </div>
                        <Table 
                            columns={operateColumns} 
                            dataSource={operateLogs.filter(l => l.operator.includes(searchText) || l.content.includes(searchText))} 
                            rowKey="id"
                            pagination={{ defaultPageSize: 10, className: 'pb-4' }}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </TabPane>
                    <TabPane tab="登录日志" key="2">
                        <div className="pb-4 flex flex-wrap gap-4">
                            <Input placeholder="搜索账号/IP" prefix={<SearchOutlined className="g-text-secondary" />} className="w-64 bg-white g-border-panel border g-text-primary" />
                            <RangePicker className="bg-white g-border-panel border g-text-primary" />
                            <div className="flex-1 flex justify-end">
                                <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出</Button>
                            </div>
                        </div>
                        <Table 
                            columns={loginColumns} 
                            dataSource={loginLogs} 
                            rowKey="id"
                            pagination={{ defaultPageSize: 10, className: 'pb-4' }}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </TabPane>
                    <TabPane tab="错误日志" key="3">
                        <div className="p-10 text-center g-text-secondary">
                            暂无系统异常错误记录
                        </div>
                    </TabPane>
                </Tabs>
            </Card>
        </motion.div>
    );
};

export default SystemLogs;
