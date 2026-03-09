import React, { useState } from 'react';
import { Card, Tree, List, Button, Tag, Select, Divider } from 'antd';
import { PlusOutlined, SaveOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Option } = Select;

const rolesList = [
    { id: 'R01', name: '系统管理员', type: '系统内置', desc: '拥有系统所有权限' },
    { id: 'R02', name: '执法领导', type: '业务角色', desc: '查看全局数据及宏观统计' },
    { id: 'R03', name: '执法人员', type: '业务角色', desc: '处理违规预警及现场执法' },
    { id: 'R04', name: '车队管理员', type: '业务角色', desc: '管理本车队车辆及人员' },
    { id: 'R05', name: '场地管理员', type: '业务角色', desc: '管理本场地消纳及设备' },
];

const menuTreeData = [
    {
        title: '数据看板',
        key: 'dashboard',
        children: [
            { title: '总体分析', key: 'dashboard-all' },
            { title: '消纳场数据', key: 'dashboard-site' },
            { title: '项目数据', key: 'dashboard-project' },
        ],
    },
    {
        title: '项目管理',
        key: 'projects',
        children: [
            { title: '项目清单', key: 'projects-list' },
            { title: '交款数据', key: 'projects-payment' },
        ],
    },
    {
        title: '预警与安全',
        key: 'alerts',
        children: [
            { title: '系统预警', key: 'alerts-list' },
            { title: '预警配置', key: 'alerts-config' },
        ],
    },
];

const RolesManagement: React.FC = () => {
    const [selectedRole, setSelectedRole] = useState(rolesList[1]);

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6 h-[calc(100vh-110px)] flex flex-col">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">角色与权限管理</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">创建角色并分配菜单权限、按钮权限及数据可见范围</p>
                </div>
            </div>

            <div className="flex gap-6 flex-1 min-h-0">
                {/* 左侧角色列表 */}
                <Card className="glass-panel border-slate-200 dark:border-slate-700/50 w-80 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b border-slate-200 dark:border-slate-700/50 flex justify-between items-center bg-slate-50 dark:bg-slate-900/30">
                        <span className="text-slate-900 dark:text-white font-bold">角色列表</span>
                        <Button type="primary" size="small" icon={<PlusOutlined />} className="bg-blue-600 hover:bg-blue-500 border-none">新增</Button>
                    </div>
                    <div className="flex-1 overflow-auto p-2">
                        <List
                            dataSource={rolesList}
                            renderItem={item => (
                                <List.Item 
                                    className={`px-4 py-3 cursor-pointer rounded mb-1 transition-colors border-none ${selectedRole.id === item.id ? 'bg-blue-600/20 border border-blue-500/50' : 'hover:bg-white dark:bg-slate-800/50'}`}
                                    onClick={() => setSelectedRole(item)}
                                >
                                    <div className="w-full">
                                        <div className="flex justify-between items-center mb-1">
                                            <span className={`font-bold ${selectedRole.id === item.id ? 'text-blue-600 dark:text-blue-400' : 'text-slate-700 dark:text-slate-200'}`}>{item.name}</span>
                                            <Tag color={item.type === '系统内置' ? 'default' : 'blue'} className="border-none m-0">{item.type}</Tag>
                                        </div>
                                        <div className="text-xs text-slate-600 dark:text-slate-400 truncate">{item.desc}</div>
                                    </div>
                                </List.Item>
                            )}
                        />
                    </div>
                </Card>

                {/* 右侧权限配置 */}
                <Card 
                    className="glass-panel border-slate-200 dark:border-slate-700/50 flex-1 flex flex-col" 
                    bodyStyle={{ padding: '24px', flex: 1, overflow: 'auto' }}
                    title={<span className="text-slate-900 dark:text-white">【{selectedRole.name}】权限配置</span>}
                    extra={<Button type="primary" icon={<SaveOutlined />} className="bg-green-600 hover:bg-green-500 border-none">保存配置</Button>}
                >
                    <div className="space-y-8">
                        <div>
                            <div className="text-slate-600 dark:text-slate-300 font-bold mb-4 border-l-4 border-blue-500 pl-2">数据权限范围</div>
                            <Select defaultValue="all" className="w-64" popupClassName="bg-white dark:bg-slate-800">
                                <Option value="all">全部数据可见</Option>
                                <Option value="org">本组织及下属组织可见</Option>
                                <Option value="self">仅本人数据可见</Option>
                                <Option value="custom">自定义指定组织</Option>
                            </Select>
                        </div>

                        <Divider className="border-slate-200 dark:border-slate-700/50" />

                        <div>
                            <div className="text-slate-600 dark:text-slate-300 font-bold mb-4 border-l-4 border-blue-500 pl-2">菜单与按钮权限</div>
                            <div className="bg-slate-50 dark:bg-slate-900/30 p-4 rounded-lg border border-slate-200 dark:border-slate-700/50">
                                <Tree
                                    checkable
                                    defaultExpandAll
                                    defaultCheckedKeys={['dashboard', 'dashboard-all', 'alerts', 'alerts-list']}
                                    treeData={menuTreeData}
                                    className="bg-transparent text-slate-600 dark:text-slate-300 custom-tree"
                                />
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </motion.div>
    );
};

export default RolesManagement;
