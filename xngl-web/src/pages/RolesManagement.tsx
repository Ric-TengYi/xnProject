import React, { useState, useEffect } from 'react';
import { Card, Tree, List, Button, Tag, Select, Divider, message, Modal, Form, Input, Descriptions, Popconfirm, Space, Pagination } from 'antd';
import { PlusOutlined, SaveOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const { Option } = Select;

const RolesManagement: React.FC = () => {
    const [rolesList, setRolesList] = useState<any[]>([]);
    const [rolesTotal, setRolesTotal] = useState(0);
    const [rolesPageNo, setRolesPageNo] = useState(1);
    const [rolesPageSize] = useState(10);
    const [rolesKeyword, setRolesKeyword] = useState('');
    const [menuTreeData, setMenuTreeData] = useState<any[]>([]);
    const [selectedRole, setSelectedRole] = useState<any>(null);
    const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
    const [selectedMenuKey, setSelectedMenuKey] = useState<string | null>(null);
    const [dataScopeType, setDataScopeType] = useState('ORG_AND_CHILDREN');
    const [loading, setLoading] = useState(false);
    const [createOpen, setCreateOpen] = useState(false);
    const [createLoading, setCreateLoading] = useState(false);
    const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
    const [createForm] = Form.useForm();
    const [currentUserRole, setCurrentUserRole] = useState<any>(null);

    useEffect(() => {
        void fetchRoles();
        fetchMenus();
        fetchCurrentUserRole();
    }, [rolesPageNo, rolesKeyword]);

    const fetchCurrentUserRole = async () => {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            if (userInfo.roleId) {
                const res = await request.get(`/roles/${userInfo.roleId}`);
                if (res.code === 200) {
                    setCurrentUserRole(res.data);
                }
            }
        } catch (error) {
            console.error('Failed to fetch current user role', error);
        }
    };

    useEffect(() => {
        if (selectedRole) {
            fetchRolePermissions(selectedRole.id);
            fetchDataScopeRules(selectedRole.id, selectedRole.dataScopeTypeDefault);
        }
    }, [selectedRole]);

    const fetchRoles = async () => {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const res = await request.get('/roles', { params: { pageNo: rolesPageNo, pageSize: rolesPageSize, tenantId, keyword: rolesKeyword || undefined } });
            if (res.code === 200) {
                const roles = res.data.records || [];
                setRolesList(roles);
                setRolesTotal(res.data.total || 0);
                if (roles.length > 0 && !selectedRole) {
                    await handleSelectRole(roles[0].id);
                }
            }
        } catch (error) {
            console.error(error);
        }
    };

    const fetchRoleDetail = async (roleId: string) => {
        const res = await request.get(`/roles/${roleId}`);
        if (res.code === 200) {
            setSelectedRole(res.data);
            return res.data;
        }
        return null;
    };

    const handleSelectRole = async (roleId: string) => {
        try {
            await fetchRoleDetail(roleId);
        } catch (error) {
            console.error(error);
        }
    };

    const fetchMenus = async () => {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const res = await request.get('/menus/tree', { params: { tenantId } });
            if (res.code === 200) {
                const formatTree = (nodes: any[]): any[] => {
                    return nodes.map(node => ({
                        title: node.menuName,
                        key: node.id,
                        children: node.children ? formatTree(node.children) : [],
                    }));
                };
                setMenuTreeData(formatTree(res.data || []));
            }
        } catch (error) {
            console.error(error);
        }
    };

    const fetchRolePermissions = async (roleId: string) => {
        try {
            const res = await request.get(`/roles/${roleId}/permissions`);
            if (res.code === 200) {
                setCheckedKeys(res.data.menuIds || []);
            }
        } catch (error) {
            console.error(error);
        }
    };

    const fetchDataScopeRules = async (roleId: string, fallback?: string) => {
        try {
            const res = await request.get(`/roles/${roleId}/data-scope-rules`);
            if (res.code === 200) {
                const rules = res.data || [];
                if (rules.length > 0) {
                    setDataScopeType(rules[0].ruleType || fallback || 'ORG_AND_CHILDREN');
                } else {
                    setDataScopeType(fallback || 'ORG_AND_CHILDREN');
                }
            }
        } catch (error) {
            console.error(error);
            setDataScopeType(fallback || 'ORG_AND_CHILDREN');
        }
    };

    const handleSave = async () => {
        if (!selectedRole) return;
        setLoading(true);
        try {
            const permissionsRes = await request.put(`/roles/${selectedRole.id}/permissions`, {
                menuIds: checkedKeys,
                permissionIds: [] // TODO: Add permission handling if needed
            });
            const scopeRes = await request.put(`/roles/${selectedRole.id}/data-scope-rules`, [
                {
                    ruleType: dataScopeType,
                    ruleValue: '[]',
                    resourceCode: 'ALL',
                },
            ]);
            if (permissionsRes.code === 200 && scopeRes.code === 200) {
                message.success('保存成功');
                await fetchRoles();
            }
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const openCreateModal = () => {
        setEditingRoleId(null);
        createForm.resetFields();
        createForm.setFieldsValue({
            roleScope: 'TENANT',
            dataScopeTypeDefault: 'ORG_AND_CHILDREN',
        });
        setCreateOpen(true);
    };

    const openEditModal = () => {
        if (!selectedRole) return;
        setEditingRoleId(selectedRole.id);
        createForm.setFieldsValue({
            roleName: selectedRole.roleName,
            roleCode: selectedRole.roleCode,
            roleScope: selectedRole.roleScope,
            dataScopeTypeDefault: selectedRole.dataScopeTypeDefault || dataScopeType,
            description: selectedRole.description,
        });
        setCreateOpen(true);
    };

    const handleCreate = async () => {
        try {
            const values = await createForm.validateFields();
            setCreateLoading(true);
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const payload = {
                tenantId,
                roleCode: values.roleCode,
                roleName: values.roleName,
                roleScope: values.roleScope,
                roleCategory: 'CUSTOM',
                description: values.description,
                dataScopeTypeDefault: values.dataScopeTypeDefault,
            };
            const res = editingRoleId
                ? await request.put(`/roles/${editingRoleId}`, payload)
                : await request.post('/roles', payload);
            if (res.code === 200) {
                const roleId = editingRoleId || res.data;
                await request.put(`/roles/${roleId}/data-scope-rules`, [
                    {
                        ruleType: values.dataScopeTypeDefault,
                        ruleValue: '[]',
                        resourceCode: 'ALL',
                    },
                ]);
                message.success(editingRoleId ? '角色信息已更新' : '角色创建成功');
                setCreateOpen(false);
                setEditingRoleId(null);
                createForm.resetFields();
                await fetchRoles();
                await handleSelectRole(roleId);
            }
        } catch (error) {
            console.error(error);
        } finally {
            setCreateLoading(false);
        }
    };

    const handleDeleteRole = async () => {
        if (!selectedRole) return;
        try {
            const res = await request.delete(`/roles/${selectedRole.id}`);
            if (res.code === 200) {
                message.success('角色已删除');
                setSelectedRole(null);
                setCheckedKeys([]);
                await fetchRoles();
            }
        } catch (error) {
            console.error(error);
            message.error('删除角色失败');
        }
    };

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6 h-[calc(100vh-110px)] flex flex-col">

            <div className="flex gap-6 flex-1 min-h-0">
                {/* 左侧角色列表 */}
                <Card className="glass-panel g-border-panel border w-80 flex flex-col overflow-hidden" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between items-center g-bg-toolbar">
                        <span className="g-text-primary font-bold">角色列表</span>
                        <Button type="primary" size="small" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={openCreateModal}>新增</Button>
                    </div>
                    <div className="p-2 border-b g-border-panel">
                        <Input.Search placeholder="搜索角色名称" value={rolesKeyword} onChange={(e) => { setRolesKeyword(e.target.value); setRolesPageNo(1); }} />
                    </div>
                    <div className="flex-1 overflow-y-auto" style={{ minHeight: 0 }}>
                        <List
                            dataSource={rolesList}
                            loading={loading}
                            renderItem={item => (
                                <List.Item
                                    className={`px-3 py-1 cursor-pointer rounded mx-1 my-0.5 transition-all border ${selectedRole?.id === item.id ? 'bg-blue-500/20 border-blue-500/50' : 'border-transparent hover:bg-white/5'}`}
                                    style={{ borderBottom: 'none' }}
                                    onClick={() => void handleSelectRole(item.id)}
                                >
                                    <div className="w-full flex justify-between items-start gap-2 min-w-0">
                                        <div className="min-w-0 flex-1 space-y-1">
                                            <div className="font-semibold text-xs g-text-primary truncate">{item.roleName || '未命名'}</div>
                                            <div className="text-xs g-text-secondary truncate">{item.roleCode || '暂无编码'}</div>
                                        </div>
                                        <div className="flex flex-col gap-0.5 items-end flex-shrink-0 pr-1">
                                            <Tag size="small" color={item.status === 'ENABLED' ? 'success' : 'default'}>{item.status === 'ENABLED' ? '启用' : '停用'}</Tag>
                                            <Tag size="small" color={item.roleScope === 'SYSTEM' ? 'blue' : 'cyan'}>{item.roleScope === 'SYSTEM' ? '系统' : '租户'}</Tag>
                                        </div>
                                    </div>
                                </List.Item>
                            )}
                        />
                    </div>
                    <div className="px-2 py-2 border-t g-border-panel flex justify-center bg-white/5">
                        <Pagination current={rolesPageNo} pageSize={rolesPageSize} total={rolesTotal} onChange={setRolesPageNo} size="small" showTotal={(total) => `共${total}条`} />
                    </div>
                </Card>

                {/* 右侧权限配置 */}
                <Card 
                    className="glass-panel g-border-panel border flex-1 flex flex-col" 
                    bodyStyle={{ padding: '24px', flex: 1, overflow: 'auto' }}
                    title={<span className="g-text-primary">【{selectedRole?.roleName || '-'}】权限配置</span>}
                    extra={(
                        <Space>
                            <Button onClick={openEditModal} disabled={!selectedRole}>编辑角色</Button>
                            <Popconfirm title="确认删除当前角色？" onConfirm={() => void handleDeleteRole()}>
                                <Button danger disabled={!selectedRole}>删除角色</Button>
                            </Popconfirm>
                            <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={loading} className="bg-green-600 hover:bg-green-500 border-none" disabled={!selectedRole}>保存配置</Button>
                        </Space>
                    )}
                >
                    <div className="space-y-4">
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="角色编码">{selectedRole?.roleCode || '-'}</Descriptions.Item>
                            <Descriptions.Item label="角色分类">{selectedRole?.roleCategory || '-'}</Descriptions.Item>
                            <Descriptions.Item label="默认数据范围" span={2}>{selectedRole?.dataScopeTypeDefault || dataScopeType}</Descriptions.Item>
                            {selectedRole?.description && <Descriptions.Item label="角色描述" span={2}>{selectedRole.description}</Descriptions.Item>}
                        </Descriptions>

                        <div>
                            <div className="g-text-secondary font-bold mb-2 border-l-4 border-blue-500 pl-2 text-sm">数据权限范围</div>
                            <Select value={dataScopeType} className="w-64" popupClassName="bg-white" onChange={setDataScopeType} size="small">
                                <Option value="ALL">全部数据可见</Option>
                                <Option value="ORG_AND_CHILDREN">本组织及下属组织可见</Option>
                                <Option value="SELF">仅本人数据可见</Option>
                                <Option value="CUSTOM_ORG_SET">自定义指定组织</Option>
                            </Select>
                        </div>

                        <Divider className="g-border-panel border my-2" />

                        <div>
                            <div className="g-text-secondary font-bold mb-2 border-l-4 border-blue-500 pl-2 text-sm">菜单与按钮权限</div>
                            <div className="flex gap-4 h-80">
                                {/* 左侧菜单树 */}
                                <div className="w-48 border g-border-panel rounded-lg p-2 overflow-y-auto">
                                    {menuTreeData.length > 0 && (
                                        <>
                                            <div className="flex gap-1 mb-2 pb-2 border-b g-border-panel">
                                                <Button size="small" type="text" onClick={() => setCheckedKeys(menuTreeData.map(m => m.key))}>全选</Button>
                                                <Button size="small" type="text" onClick={() => setCheckedKeys([])}>反选</Button>
                                            </div>
                                            <Tree
                                                checkable
                                                defaultExpandAll
                                                treeData={menuTreeData}
                                                checkedKeys={checkedKeys}
                                                onCheck={(keys) => setCheckedKeys(keys as React.Key[])}
                                                onSelect={(keys) => setSelectedMenuKey(keys[0] as string)}
                                                className="bg-transparent g-text-secondary custom-tree"
                                            />
                                        </>
                                    )}
                                </div>
                                {/* 右侧按钮权限 */}
                                <div className="flex-1 overflow-y-auto space-y-2">
                                    {selectedMenuKey ? (
                                        menuTreeData
                                            .find(m => m.key === selectedMenuKey)
                                            ?.children?.map((btn: any) => (
                                                <div key={btn.key} className="g-bg-toolbar p-3 rounded border g-border-panel">
                                                    <div className="flex items-center gap-2">
                                                        <input
                                                            type="checkbox"
                                                            checked={checkedKeys.includes(btn.key)}
                                                            onChange={(e) => {
                                                                if (e.target.checked) {
                                                                    setCheckedKeys([...checkedKeys, btn.key]);
                                                                } else {
                                                                    setCheckedKeys(checkedKeys.filter(k => k !== btn.key));
                                                                }
                                                            }}
                                                        />
                                                        <span className="text-sm">{btn.title}</span>
                                                    </div>
                                                </div>
                                            ))
                                    ) : (
                                        <div className="text-center g-text-secondary py-8 text-sm">请选择左侧菜单查看按钮权限</div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>

            <Modal
                title={editingRoleId ? '编辑角色' : '新增角色'}
                open={createOpen}
                onCancel={() => {
                    setCreateOpen(false);
                    setEditingRoleId(null);
                    createForm.resetFields();
                }}
                onOk={() => void handleCreate()}
                confirmLoading={createLoading}
            >
                <Form form={createForm} layout="vertical">
                    <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
                        <Input placeholder="请输入角色名称" />
                    </Form.Item>
                    <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}>
                        <Input placeholder="请输入唯一角色编码" />
                    </Form.Item>
                    <Form.Item name="roleScope" label="角色范围" rules={[{ required: true, message: '请选择角色范围' }]}>
                        <Select disabled={currentUserRole?.roleScope === 'TENANT'} options={[
                            { label: '租户角色', value: 'TENANT' },
                            { label: '系统角色', value: 'SYSTEM', disabled: currentUserRole?.roleScope === 'TENANT' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="dataScopeTypeDefault" label="默认数据范围" rules={[{ required: true, message: '请选择默认数据范围' }]}>
                        <Select options={[
                            { label: '全部数据可见', value: 'ALL', disabled: currentUserRole?.dataScopeTypeDefault !== 'ALL' },
                            { label: '本组织及下属组织可见', value: 'ORG_AND_CHILDREN', disabled: !['ALL', 'ORG_AND_CHILDREN'].includes(currentUserRole?.dataScopeTypeDefault) },
                            { label: '仅本人数据可见', value: 'SELF' },
                            { label: '自定义组织范围', value: 'CUSTOM_ORG_SET', disabled: currentUserRole?.dataScopeTypeDefault === 'SELF' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="description" label="角色描述">
                        <Input.TextArea rows={3} placeholder="请输入角色职责说明" />
                    </Form.Item>
                </Form>
            </Modal>
        </motion.div>
    );
};

export default RolesManagement;
