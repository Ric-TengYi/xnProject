import React, { useState, useEffect } from 'react';
import { Card, Tree, List, Button, Tag, Select, Divider, message } from 'antd';
import { PlusOutlined, SaveOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const { Option } = Select;

const RolesManagement: React.FC = () => {
    const [rolesList, setRolesList] = useState<any[]>([]);
    const [menuTreeData, setMenuTreeData] = useState<any[]>([]);
    const [selectedRole, setSelectedRole] = useState<any>(null);
    const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchRoles();
        fetchMenus();
    }, []);

    useEffect(() => {
        if (selectedRole) {
            fetchRolePermissions(selectedRole.id);
        }
    }, [selectedRole]);

    const fetchRoles = async () => {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const tenantId = userInfo.tenantId || '1';
            const res = await request.get('/roles', { params: { pageSize: 100, tenantId } });
            if (res.code === 200) {
                const roles = res.data.records || [];
                setRolesList(roles);
                if (roles.length > 0 && !selectedRole) {
                    setSelectedRole(roles[0]);
                }
            }
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

    const handleSave = async () => {
        if (!selectedRole) return;
        setLoading(true);
        try {
            const res = await request.put(`/roles/${selectedRole.id}/permissions`, {
                menuIds: checkedKeys,
                permissionIds: [] // TODO: Add permission handling if needed
            });
            if (res.code === 200) {
                message.success('保存成功');
            }
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6 h-[calc(100vh-110px)] flex flex-col">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">角色与权限管理</h1>
                    <p className="g-text-secondary mt-1">创建角色并分配菜单权限、按钮权限及数据可见范围</p>
                </div>
            </div>

            <div className="flex gap-6 flex-1 min-h-0">
                {/* 左侧角色列表 */}
                <Card className="glass-panel g-border-panel border w-80 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between items-center g-bg-toolbar">
                        <span className="g-text-primary font-bold">角色列表</span>
                        <Button type="primary" size="small" icon={<PlusOutlined />} className="g-btn-primary border-none">新增</Button>
                    </div>
                    <div className="flex-1 overflow-auto p-2">
                        <List
                            dataSource={rolesList}
                            renderItem={item => (
                                <List.Item 
                                    className={`px-4 py-3 cursor-pointer rounded mb-1 transition-colors border-none ${selectedRole?.id === item.id ? 'bg-blue-600/20 border border-blue-500/50' : 'hover:bg-white'}`}
                                    onClick={() => setSelectedRole(item)}
                                >
                                    <div className="w-full">
                                        <div className="flex justify-between items-center mb-1">
                                            <span className={`font-bold ${selectedRole?.id === item.id ? 'g-text-primary-link' : 'g-text-primary'}`}>{item.roleName}</span>
                                            <Tag color={item.roleScope === 'SYSTEM' ? 'default' : 'blue'} className="border-none m-0">{item.roleScope === 'SYSTEM' ? '系统内置' : '业务角色'}</Tag>
                                        </div>
                                        <div className="text-xs g-text-secondary truncate">{item.description || '暂无描述'}</div>
                                    </div>
                                </List.Item>
                            )}
                        />
                    </div>
                </Card>

                {/* 右侧权限配置 */}
                <Card 
                    className="glass-panel g-border-panel border flex-1 flex flex-col" 
                    bodyStyle={{ padding: '24px', flex: 1, overflow: 'auto' }}
                    title={<span className="g-text-primary">【{selectedRole?.roleName || '-'}】权限配置</span>}
                    extra={<Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={loading} className="bg-green-600 hover:bg-green-500 border-none">保存配置</Button>}
                >
                    <div className="space-y-8">
                        <div>
                            <div className="g-text-secondary font-bold mb-4 border-l-4 border-blue-500 pl-2">数据权限范围</div>
                            <Select defaultValue="all" className="w-64" popupClassName="bg-white">
                                <Option value="all">全部数据可见</Option>
                                <Option value="org">本组织及下属组织可见</Option>
                                <Option value="self">仅本人数据可见</Option>
                                <Option value="custom">自定义指定组织</Option>
                            </Select>
                        </div>

                        <Divider className="g-border-panel border" />

                        <div>
                            <div className="g-text-secondary font-bold mb-4 border-l-4 border-blue-500 pl-2">菜单与按钮权限</div>
                            <div className="g-bg-toolbar p-4 rounded-lg border g-border-panel border">
                                {menuTreeData.length > 0 && (
                                    <Tree
                                        checkable
                                        defaultExpandAll
                                        checkedKeys={checkedKeys}
                                        onCheck={(keys) => setCheckedKeys(keys as React.Key[])}
                                        treeData={menuTreeData}
                                        className="bg-transparent g-text-secondary custom-tree"
                                    />
                                )}
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </motion.div>
    );
};

export default RolesManagement;
