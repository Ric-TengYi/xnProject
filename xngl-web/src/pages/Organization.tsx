import React, { useState, useEffect } from 'react';
import { Card, Tree, Table, Input, Button, Tag, Space, Modal, Form, Select, message } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const Organization: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [treeData, setTreeData] = useState<any[]>([]);
    const [usersData, setUsersData] = useState<any[]>([]);
    const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [addModalOpen, setAddModalOpen] = useState(false);
    const [rolesOpts, setRolesOpts] = useState<{ value: string; label: string; roleCode?: string }[]>([]);
    const [orgOpts, setOrgOpts] = useState<{ value: string; label: string }[]>([]);
    const [form] = Form.useForm();

    useEffect(() => {
        fetchOrgs();
    }, []);

    useEffect(() => {
        fetchUsers();
    }, [selectedOrgId, searchText]);

    useEffect(() => {
        const loadOptions = async () => {
            try {
                const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
                const tenantId = userInfo.tenantId || '1';
                const [orgRes, roleRes] = await Promise.all([
                    request.get('/orgs/tree', { params: { tenantId } }),
                    request.get('/roles', { params: { tenantId, pageNo: 1, pageSize: 200 } }),
                ]);
                if (orgRes?.code === 200 && orgRes?.data) {
                    const flatten = (nodes: any[], prefix = ''): { value: string; label: string }[] => {
                        return (nodes || []).flatMap((n: any) => [
                            { value: String(n.id), label: (prefix ? prefix + ' / ' : '') + (n.orgName || n.name || n.title || n.id) },
                            ...flatten(n.children || [], (n.orgName || n.name || n.title || String(n.id))),
                        ]);
                    };
                    const raw = Array.isArray(orgRes.data) ? orgRes.data : (orgRes.data as any)?.children || [];
                    setOrgOpts(flatten(raw));
                }
                if (roleRes?.code === 200 && roleRes?.data?.records) {
                    setRolesOpts((roleRes.data.records || []).map((r: any) => ({
                        value: String(r.id),
                        label: r.roleName || r.roleCode || String(r.id),
                        roleCode: r.roleCode,
                    })));
                }
            } catch (e) { console.error(e); }
        };
        if (addModalOpen) void loadOptions();
    }, [addModalOpen]);

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
                        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={() => setAddModalOpen(true)}>
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

            <Modal
                title="新增人员"
                open={addModalOpen}
                onCancel={() => { setAddModalOpen(false); form.resetFields(); }}
                onOk={async () => {
                    try {
                        const values = await form.validateFields();
                        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
                        let roleIds: string[] = Array.isArray(values.roleIds) ? [...values.roleIds] : [];
                        const userType = values.userType || 'TENANT_USER';
                        if (userType === 'DRIVER') {
                            const driverRoleId = rolesOpts.find((r) => r.roleCode === 'DRIVER')?.value;
                            if (driverRoleId && !roleIds.includes(driverRoleId)) roleIds.push(driverRoleId);
                        }
                        await request.post('/users', {
                            tenantId: userInfo.tenantId || '1',
                            username: values.username,
                            name: values.name,
                            userType,
                            mobile: values.mobile || undefined,
                            email: values.email || undefined,
                            mainOrgId: values.mainOrgId,
                            roleIds,
                            password: values.password || 'ChangeMe123',
                        });
                        message.success('新增成功');
                        setAddModalOpen(false);
                        form.resetFields();
                        fetchUsers();
                    } catch (err: any) {
                        message.error(err?.response?.data?.message || err?.message || '新增失败');
                    }
                }}
                okText="确定"
                cancelText="取消"
                destroyOnClose
                width={520}
            >
                <Form form={form} layout="vertical" className="mt-4" initialValues={{ userType: 'TENANT_USER' }}>
                    <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
                        <Input placeholder="请输入姓名" maxLength={32} />
                    </Form.Item>
                    <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
                        <Input placeholder="请输入登录账号" maxLength={64} />
                    </Form.Item>
                    <Form.Item name="password" label="初始密码">
                        <Input.Password placeholder="不填则默认 ChangeMe123" />
                    </Form.Item>
                    <Form.Item name="userType" label="用户类型" rules={[{ required: true }]}>
                        <Select placeholder="请选择" options={[
                            { value: 'TENANT_USER', label: '平台用户' },
                            { value: 'TENANT_ADMIN', label: '管理员' },
                            { value: 'DRIVER', label: '司机端（仅小程序司机工作台）' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="mainOrgId" label="所属组织" rules={[{ required: true, message: '请选择所属组织' }]}>
                        <Select placeholder="请选择组织" options={orgOpts} showSearch optionFilterProp="label" allowClear />
                    </Form.Item>
                    <Form.Item name="roleIds" label="角色" rules={[{ required: true, message: '请至少选择一个角色' }]}>
                        <Select mode="multiple" placeholder="请选择角色" options={rolesOpts} />
                    </Form.Item>
                    <Form.Item name="mobile" label="手机号">
                        <Input placeholder="请输入手机号" maxLength={11} />
                    </Form.Item>
                    <Form.Item name="email" label="邮箱">
                        <Input placeholder="请输入邮箱" maxLength={128} />
                    </Form.Item>
                </Form>
            </Modal>
        </motion.div>
    );
};

export default Organization;
