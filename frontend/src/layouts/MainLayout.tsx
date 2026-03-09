import React, { useState } from 'react';
import { Layout, Menu, Button, Input, Badge, Avatar, Switch } from 'antd';
import {
    PieChartOutlined,
    DesktopOutlined,
    ContainerOutlined,
    AlertOutlined,
    EnvironmentOutlined,
    CarOutlined,
    BellOutlined,
    SearchOutlined,
    UserOutlined,
    SettingOutlined,
    SunOutlined,
    MoonOutlined
} from '@ant-design/icons';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useTheme } from '../contexts/ThemeContext';

const { Header, Content, Sider } = Layout;

type MenuItem = {
    key: string;
    icon?: React.ReactNode;
    children?: MenuItem[];
    label: React.ReactNode;
};

function getItem(
    label: React.ReactNode,
    key: string,
    icon?: React.ReactNode,
    children?: MenuItem[],
): MenuItem {
    return {
        key,
        icon,
        children,
        label,
    } as MenuItem;
}

const items: MenuItem[] = [
    getItem('数据看板', 'dashboard', <PieChartOutlined />, [
        getItem(<Link to="/">总体分析</Link>, '/'),
        getItem(<Link to="/dashboard/sites">消纳场数据</Link>, '/dashboard/sites'),
        getItem(<Link to="/dashboard/projects">项目数据</Link>, '/dashboard/projects'),
        getItem(<Link to="/dashboard/map">地图展示</Link>, '/dashboard/map'),
    ]),
    getItem('项目管理', 'projects', <DesktopOutlined />, [
        getItem(<Link to="/projects">项目清单</Link>, '/projects'),
        getItem(<Link to="/projects/payments">交款数据</Link>, '/projects/payments'),
        getItem(<Link to="/projects/permits">处置证清单</Link>, '/projects/permits'),
        getItem(<Link to="/projects/daily-report">项目日报</Link>, '/projects/daily-report'),
    ]),
    getItem('消纳场地', 'sites', <EnvironmentOutlined />, [
        getItem(<Link to="/sites">场地列表</Link>, '/sites'),
        getItem(<Link to="/sites/disposals">消纳清单</Link>, '/sites/disposals'),
        getItem(<Link to="/sites/documents">场地资料</Link>, '/sites/documents'),
        getItem(<Link to="/sites/basic-info">基础信息</Link>, '/sites/basic-info'),
    ]),
    getItem('车辆与运力', 'vehicles', <CarOutlined />, [
        getItem(<Link to="/vehicles">车辆信息</Link>, '/vehicles'),
        getItem(<Link to="/vehicles/fleet">车队管理</Link>, '/vehicles/fleet'),
        getItem(<Link to="/vehicles/cards">油电卡管理</Link>, '/vehicles/cards'),
        getItem(<Link to="/vehicles/tracking">送货跟踪</Link>, '/vehicles/tracking'),
        getItem(<Link to="/vehicles/violations">违规车辆清单</Link>, '/vehicles/violations'),
    ]),
    getItem('合同与结算', 'contracts', <ContainerOutlined />, [
        getItem(<Link to="/contracts">合同清单</Link>, '/contracts'),
        getItem(<Link to="/contracts/payments">合同入账</Link>, '/contracts/payments'),
        getItem(<Link to="/contracts/settlements">结算管理</Link>, '/contracts/settlements'),
        getItem(<Link to="/contracts/monthly-report">月报统计</Link>, '/contracts/monthly-report'),
    ]),
    getItem('预警与安全', 'alerts', <AlertOutlined />, [
        getItem(<Link to="/alerts">系统预警</Link>, '/alerts'),
        getItem(<Link to="/alerts/config">预警配置</Link>, '/alerts/config'),
        getItem(<Link to="/alerts/events">事件管理</Link>, '/alerts/events'),
        getItem(<Link to="/alerts/security">安全台账</Link>, '/alerts/security'),
    ]),
    getItem('系统设置', 'settings', <SettingOutlined />, [
        getItem(<Link to="/settings/organization">组织人员</Link>, '/settings/organization'),
        getItem(<Link to="/settings/roles">角色管理</Link>, '/settings/roles'),
        getItem(<Link to="/settings/dictionary">数据字典</Link>, '/settings/dictionary'),
        getItem(<Link to="/settings/approvals">审批配置</Link>, '/settings/approvals'),
        getItem(<Link to="/settings/logs">系统日志</Link>, '/settings/logs'),
    ]),
];

const MainLayout: React.FC = () => {
    const [collapsed, setCollapsed] = useState(false);
    const location = useLocation();
    const { isDarkMode, toggleTheme } = useTheme();

    return (
        <Layout style={{ minHeight: '100vh', background: 'transparent' }}>
            <Sider
                collapsible
                collapsed={collapsed}
                onCollapse={(value) => setCollapsed(value)}
                width={240}
                theme={isDarkMode ? 'dark' : 'light'}
                style={{
                    overflow: 'auto',
                    height: '100vh',
                    position: 'fixed',
                    left: 0,
                    top: 0,
                    bottom: 0,
                    borderRight: '1px solid var(--border-color)',
                    boxShadow: isDarkMode ? '4px 0 24px rgba(0,0,0,0.2)' : '4px 0 24px rgba(0,0,0,0.05)',
                    background: 'var(--bg-panel)',
                    backdropFilter: 'blur(16px)',
                    WebkitBackdropFilter: 'blur(16px)',
                }}
            >
                <div className="h-16 flex items-center justify-center p-4">
                    <motion.div
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        className="flex items-center gap-2"
                    >
                        <div className="w-8 h-8 rounded-lg bg-blue-500 flex items-center justify-center font-bold text-white shadow-[0_0_15px_rgba(24,144,255,0.5)]">
                            渣
                        </div>
                        {!collapsed && <span className="text-slate-900 dark:text-white font-bold text-lg truncate block">智慧渣土消纳管控</span>}
                    </motion.div>
                </div>
                <Menu
                    theme={isDarkMode ? 'dark' : 'light'}
                    defaultSelectedKeys={[location.pathname]}
                    selectedKeys={[location.pathname]}
                    defaultOpenKeys={['dashboard', 'projects', 'sites', 'vehicles', 'contracts', 'alerts', 'settings'].filter(key => location.pathname.includes(key) || (key === 'dashboard' && location.pathname === '/'))}
                    mode="inline"
                    items={items}
                    style={{ background: 'transparent', borderRight: 0 }}
                />
            </Sider>
            <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'all 0.2s', background: 'transparent' }}>
                <Header
                    style={{
                        padding: '0 24px',
                        background: 'var(--bg-panel)',
                        backdropFilter: 'blur(16px)',
                        WebkitBackdropFilter: 'blur(16px)',
                        borderBottom: '1px solid var(--border-color)',
                        position: 'sticky',
                        top: 0,
                        zIndex: 10,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        height: '64px'
                    }}
                >
                    <div className="w-1/3">
                        <Input
                            placeholder="搜索项目、场地或车牌号..."
                            prefix={<SearchOutlined className="text-slate-600 dark:text-slate-400" />}
                            style={{ background: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                            className="hover:border-blue-500 focus:border-blue-500"
                        />
                    </div>
                    <div className="flex items-center gap-6">
                        <Switch
                            checked={isDarkMode}
                            onChange={toggleTheme}
                            checkedChildren={<MoonOutlined />}
                            unCheckedChildren={<SunOutlined />}
                            className="bg-slate-300 dark:bg-slate-600"
                        />
                        <Badge dot color="var(--error)" size="default">
                            <Button type="text" icon={<BellOutlined style={{ fontSize: '18px', color: 'var(--text-primary)' }} />} />
                        </Badge>
                        <div className="flex items-center gap-2 cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800 p-1 pr-3 rounded-full transition-colors">
                            <Avatar icon={<UserOutlined />} src="https://api.dicebear.com/7.x/notionists/svg?seed=Felix" />
                            <span className="text-slate-900 dark:text-slate-200 text-sm">超级管理员</span>
                        </div>
                    </div>
                </Header>
                <Content style={{ margin: '24px 24px 0', overflow: 'initial' }}>
                    <motion.div
                        key={location.pathname}
                        initial={{ y: 20, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        transition={{ duration: 0.3 }}
                        style={{ minHeight: 'calc(100vh - 110px)' }}
                    >
                        <Outlet />
                    </motion.div>
                </Content>
            </Layout>
        </Layout>
    );
};

export default MainLayout;
