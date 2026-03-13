import React, { useState, useEffect } from 'react';
import { Card, Tree, Table, Input, Button, Tag, Space } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const Organization: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [treeData, setTreeData] = useState<any[]>([]);
    const [usersData, setUsersData] = useState<any[]>([]);
    const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchOrgs();
    }, []);

    useEffect(() => {
        fetchUsers();
    }, [selectedOrgId, searchText]);

    const fetchOrgs = async () => {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const res = await request.get('/orgs/tree', { params: { tenantId } });
            if (res.code === 200) {
                const formatTree = (nodes: any[]): any[] => {
                    return nodes.map(node => ({
                        title: node.orgName,
                        key: node.id,
                        children: node.children ? formatTree(node.children) : [],
                    }));
                };
                setTreeData(formatTree(res.data || []));
            }
        } catch (error) {
            console.error(error);
        }
    };

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const params: any = {
                pageNo: 1,
                pageSize: 100,
                tenantId,
            };
            if (selectedOrgId) params.orgId = selectedOrgId;
            if (searchText) params.keyword = searchText;
            
            const res = await request.get('/users', { params });
            if (res.code === 200) {
                setUsersData(res.data.records || []);
            }
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        { title: '姓名', dataIndex: 'name', key: 'name', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '账号', dataIndex: 'username', key: 'username', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: '所属组织', dataIndex: 'mainOrgName', key: 'mainOrgName', render: (t: string) => <span className="g-text-secondary">{t || '-'}</span> },
        { 
            title: '角色', 
            dataIndex: 'roleNames', 
            key: 'roleNames',
            render: (roleNames: string[]) => (
                <Space size={[0, 4]} wrap>
                    {roleNames?.map((name: string, i: number) => <Tag color="blue" key={i} className="border-none">{name}</Tag>)}
                </Space>
            )
        },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === 'ENABLED' ? 'success' : 'error'} className="border-none">{status === 'ENABLED' ? '正常' : '停用'}</Tag>
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
                        onSelect={(selectedKeys) => setSelectedOrgId(selectedKeys[0] as string)}
                        className="bg-transparent g-text-secondary custom-tree"
                    />
                </Card>

                {/* 右侧人员列表 */}
                <Card className="glass-panel g-border-panel border flex-1 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between g-bg-toolbar">
                        <Input 
                            placeholder="搜索姓名/账号" 
                            prefix={<SearchOutlined className="g-text-secondary" />} 
                            className="w-64 bg-white g-border-panel border g-text-primary"
                            value={searchText}
                            onChange={e => setSearchText(e.target.value)}
                            onPressEnter={fetchUsers}
                        />
                        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none">
                            新增人员
                        </Button>
                    </div>
                    <div className="flex-1 overflow-auto">
                        <Table 
                            columns={columns} 
                            dataSource={usersData} 
                            rowKey="id"
                            loading={loading}
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
