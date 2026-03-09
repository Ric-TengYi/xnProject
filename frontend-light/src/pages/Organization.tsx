import React, { useState } from 'react';
import { Card, Tree, Table, Input, Button, Tag, Space } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const treeData = [
    {
        title: '城管执法局',
        key: '0-0',
        children: [
            { title: '渣土管理大队', key: '0-0-0' },
            { title: '一中队', key: '0-0-1' },
            { title: '二中队', key: '0-0-2' },
        ],
    },
    {
        title: '运输企业',
        key: '0-1',
        children: [
            { title: '宏基渣土运输公司', key: '0-1-0' },
            { title: '顺达土方工程队', key: '0-1-1' },
        ],
    },
];

const usersData = [
    { id: 'U001', name: '张建国', phone: '13800138001', org: '渣土管理大队', roles: ['系统管理员', '执法领导'], status: '正常' },
    { id: 'U002', name: '李志强', phone: '13900139002', org: '一中队', roles: ['执法人员'], status: '正常' },
    { id: 'U003', name: '王海波', phone: '13700137003', org: '宏基渣土运输公司', roles: ['车队管理员'], status: '正常' },
    { id: 'U004', name: '赵铁柱', phone: '13600136004', org: '顺达土方工程队', roles: ['司机'], status: '停用' },
];

const Organization: React.FC = () => {
    const [searchText, setSearchText] = useState('');

    const columns = [
        { title: '姓名', dataIndex: 'name', key: 'name', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '手机号(账号)', dataIndex: 'phone', key: 'phone', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: '所属组织', dataIndex: 'org', key: 'org', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { 
            title: '角色', 
            dataIndex: 'roles', 
            key: 'roles',
            render: (roles: string[]) => (
                <Space size={[0, 4]} wrap>
                    {roles.map(role => <Tag color="blue" key={role} className="border-none">{role}</Tag>)}
                </Space>
            )
        },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === '正常' ? 'success' : 'error'} className="border-none">{status}</Tag>
        },
        { 
            title: '操作', 
            key: 'action', 
            render: () => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link"><EditOutlined /> 编辑</a>
                    <a className="g-text-error hover:g-text-error"><DeleteOutlined /> 停用</a>
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6 h-[calc(100vh-110px)] flex flex-col">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">组织与人员管理</h1>
                    <p className="g-text-secondary mt-1">管理系统组织架构树及各组织下属人员账号</p>
                </div>
            </div>

            <div className="flex gap-6 flex-1 min-h-0">
                {/* 左侧组织树 */}
                <Card className="glass-panel g-border-panel border w-80 flex flex-col" bodyStyle={{ padding: '16px', flex: 1, overflow: 'auto' }}>
                    <div className="flex justify-between items-center mb-4">
                        <span className="g-text-primary font-bold">组织架构</span>
                        <Button type="link" icon={<PlusOutlined />} size="small">新增</Button>
                    </div>
                    <Input placeholder="搜索组织..." prefix={<SearchOutlined className="g-text-secondary" />} className="bg-white g-border-panel border g-text-primary mb-4" />
                    <Tree
                        defaultExpandAll
                        treeData={treeData}
                        className="bg-transparent g-text-secondary custom-tree"
                    />
                </Card>

                {/* 右侧人员列表 */}
                <Card className="glass-panel g-border-panel border flex-1 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between g-bg-toolbar">
                        <Input 
                            placeholder="搜索姓名/手机号" 
                            prefix={<SearchOutlined className="g-text-secondary" />} 
                            className="w-64 bg-white g-border-panel border g-text-primary"
                            value={searchText}
                            onChange={e => setSearchText(e.target.value)}
                        />
                        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none">
                            新增人员
                        </Button>
                    </div>
                    <div className="flex-1 overflow-auto">
                        <Table 
                            columns={columns} 
                            dataSource={usersData.filter(u => u.name.includes(searchText) || u.phone.includes(searchText))} 
                            rowKey="id"
                            pagination={{ defaultPageSize: 10, className: 'pr-4 pb-2' }}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </div>
                </Card>
            </div>
        </motion.div>
    );
};

export default Organization;
