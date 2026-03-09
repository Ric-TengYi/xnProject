import React from 'react';
import { Card, Steps, Button, Table, Tag, Space, Avatar } from 'antd';
import { EditOutlined, UserOutlined, SettingOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const approvalTypes = [
    { id: 'contract_approve', name: '消纳合同审批', status: '启用', updateTime: '2024-03-01 10:00' },
    { id: 'delay_approve', name: '项目延期审批', status: '启用', updateTime: '2024-02-15 14:30' },
    { id: 'settle_approve', name: '财务结算单审批', status: '启用', updateTime: '2024-01-20 09:15' },
    { id: 'event_approve', name: '异常事件处理审批', status: '停用', updateTime: '2023-12-10 16:45' },
];

const ApprovalConfig: React.FC = () => {
    const columns = [
        { title: '审批事项名称', dataIndex: 'name', key: 'name', render: (t: string) => <strong className="text-slate-700 dark:text-slate-200">{t}</strong> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === '启用' ? 'success' : 'default'} className="border-none">{status}</Tag>
        },
        { title: '最后更新时间', dataIndex: 'updateTime', key: 'updateTime', render: (t: string) => <span className="text-slate-600 dark:text-slate-400">{t}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: () => (
                <Space size="middle">
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400"><SettingOutlined /> 流程配置</a>
                    <a className="text-blue-600 dark:text-blue-500 hover:text-blue-600 dark:text-blue-400"><EditOutlined /> 材料配置</a>
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">审核审批配置</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">自定义配置各业务环节的审批流转节点及所需办事材料</p>
                </div>
            </div>

            <Card title="消纳合同审批 - 当前流程预览" className="glass-panel border-slate-200 dark:border-slate-700/50 mb-6" extra={<Button type="primary" size="small">编辑流程</Button>}>
                <div className="py-8 px-4 overflow-x-auto">
                    <Steps
                        current={-1}
                        items={[
                            {
                                title: <span className="text-slate-900 dark:text-white">发起申请</span>,
                                description: <span className="text-slate-600 dark:text-slate-400 text-xs mt-1 block">发起人</span>,
                                icon: <Avatar className="bg-blue-600">发</Avatar>
                            },
                            {
                                title: <span className="text-slate-900 dark:text-white">项目部初审</span>,
                                description: <span className="text-slate-600 dark:text-slate-400 text-xs mt-1 block">指定角色: 项目经理<br/><Tag color="blue" className="mt-1 border-none">或签</Tag></span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                            {
                                title: <span className="text-slate-900 dark:text-white">财务复核</span>,
                                description: <span className="text-slate-600 dark:text-slate-400 text-xs mt-1 block">指定角色: 财务人员<br/><Tag color="orange" className="mt-1 border-none">会签</Tag></span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                            {
                                title: <span className="text-slate-900 dark:text-white">领导终审</span>,
                                description: <span className="text-slate-600 dark:text-slate-400 text-xs mt-1 block">指定人: 张局长</span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                        ]}
                    />
                </div>
            </Card>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={approvalTypes} 
                    rowKey="id"
                    pagination={false}
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/40 transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default ApprovalConfig;
