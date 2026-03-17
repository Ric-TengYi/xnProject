import React, { useState, useEffect } from 'react';
import { Card, Steps, Button, Table, Tag, Space, Avatar, Modal } from 'antd';
import { EditOutlined, UserOutlined, SettingOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const ApprovalConfig: React.FC = () => {
    const [rules, setRules] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [editFlowModalOpen, setEditFlowModalOpen] = useState(false);

    useEffect(() => {
        const fetchRules = async () => {
            setLoading(true);
            try {
                const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
                const res = await request.get('/approval-actor-rules', { params: { tenantId: userInfo.tenantId || '1', pageSize: 100 } });
                if (res.code === 200 && res.data && res.data.records) {
                    setRules(res.data.records.map((r: any) => ({
                        id: r.id,
                        name: r.ruleName || r.processKey || r.id,
                        status: r.status === 'ENABLED' ? '启用' : '停用',
                        updateTime: '-',
                    })));
                }
            } catch (e) {
                console.error(e);
            } finally {
                setLoading(false);
            }
        };
        fetchRules();
    }, []);

    const columns = [
        { title: '审批事项名称', dataIndex: 'name', key: 'name', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === '启用' ? 'success' : 'default'} className="border-none">{status}</Tag>
        },
        { title: '最后更新时间', dataIndex: 'updateTime', key: 'updateTime', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: () => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link"><SettingOutlined /> 流程配置</a>
                    <a className="g-text-primary-link hover:g-text-primary-link"><EditOutlined /> 材料配置</a>
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">审核审批配置</h1>
                    <p className="g-text-secondary mt-1">自定义配置各业务环节的审批流转节点及所需办事材料</p>
                </div>
            </div>

            <Card title="消纳合同审批 - 当前流程预览" className="glass-panel g-border-panel border mb-6" extra={<Button type="primary" size="small" onClick={() => setEditFlowModalOpen(true)}>编辑流程</Button>}>
                <div className="py-8 px-4 overflow-x-auto">
                    <Steps
                        current={-1}
                        items={[
                            {
                                title: <span className="g-text-primary">发起申请</span>,
                                description: <span className="g-text-secondary text-xs mt-1 block">发起人</span>,
                                icon: <Avatar className="bg-blue-600">发</Avatar>
                            },
                            {
                                title: <span className="g-text-primary">项目部初审</span>,
                                description: <span className="g-text-secondary text-xs mt-1 block">指定角色: 项目经理<br/><Tag color="blue" className="mt-1 border-none">或签</Tag></span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                            {
                                title: <span className="g-text-primary">财务复核</span>,
                                description: <span className="g-text-secondary text-xs mt-1 block">指定角色: 财务人员<br/><Tag color="orange" className="mt-1 border-none">会签</Tag></span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                            {
                                title: <span className="g-text-primary">领导终审</span>,
                                description: <span className="g-text-secondary text-xs mt-1 block">指定人: 张局长</span>,
                                icon: <Avatar className="bg-slate-700" icon={<UserOutlined />} />
                            },
                        ]}
                    />
                </div>
            </Card>

            <Modal title="编辑审批流程" open={editFlowModalOpen} onCancel={() => setEditFlowModalOpen(false)} footer={[<Button key="cancel" onClick={() => setEditFlowModalOpen(false)}>取消</Button>, <Button key="ok" type="primary" onClick={() => setEditFlowModalOpen(false)}>确定</Button>]} width={640}>
                <p className="g-text-secondary">此处可配置审批节点、审批人类型（指定人/角色/上级）、或签/会签等。当前为占位，后续对接审批流程配置接口。</p>
            </Modal>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={rules} 
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>
        </motion.div>
    );
};

export default ApprovalConfig;
