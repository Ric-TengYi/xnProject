import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Input, Badge, Avatar, Dropdown } from 'antd';
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
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import request from '../utils/request';

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
    getItem(<Link to="/dashboard/capacity-analysis">运力分析</Link>, '/dashboard/capacity-analysis'),
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
    getItem(<Link to="/sites/reports">消纳报表</Link>, '/sites/reports'),
    getItem(<Link to="/sites/documents">场地资料</Link>, '/sites/documents'),
    getItem(<Link to="/sites/basic-info">基础信息</Link>, '/sites/basic-info'),
  ]),
  getItem('车辆与运力', 'vehicles', <CarOutlined />, [
    getItem(<Link to="/vehicles">车辆信息</Link>, '/vehicles'),
    getItem(<Link to="/vehicles/models">车型管理</Link>, '/vehicles/models'),
    getItem(<Link to="/vehicles/fleet">车队管理</Link>, '/vehicles/fleet'),
    getItem(<Link to="/vehicles/cards">油电卡管理</Link>, '/vehicles/cards'),
    getItem(<Link to="/vehicles/insurances">保险管理</Link>, '/vehicles/insurances'),
    getItem(<Link to="/vehicles/maintenance">维保计划</Link>, '/vehicles/maintenance'),
    getItem(<Link to="/vehicles/personnel-certificates">人证管理</Link>, '/vehicles/personnel-certificates'),
    getItem(<Link to="/vehicles/repairs">维修管理</Link>, '/vehicles/repairs'),
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
    getItem(<Link to="/settings/units">单位管理</Link>, '/settings/units'),
    getItem(<Link to="/settings/organization">组织人员</Link>, '/settings/organization'),
    getItem(<Link to="/settings/roles">角色管理</Link>, '/settings/roles'),
    getItem(<Link to="/settings/dictionary">数据字典</Link>, '/settings/dictionary'),
    getItem(<Link to="/settings/approvals">审批配置</Link>, '/settings/approvals'),
    getItem(<Link to="/settings/system-params">系统参数</Link>, '/settings/system-params'),
    getItem(<Link to="/settings/logs">系统日志</Link>, '/settings/logs'),
  ]),
];

const SIDER_COLLAPSED_WIDTH = 80;
const SIDER_DEFAULT_WIDTH = 176;
const SIDER_MIN_WIDTH = 140;
const SIDER_MAX_WIDTH = 280;

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [siderWidth, setSiderWidth] = useState(SIDER_DEFAULT_WIDTH);
  const [fullscreen, setFullscreen] = useState(false);
  const [resizing, setResizing] = useState(false);
  const [resizeStartX, setResizeStartX] = useState(0);
  const [resizeStartWidth, setResizeStartWidth] = useState(SIDER_DEFAULT_WIDTH);
  const [userInfo, setUserInfo] = useState<any>(null);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const res = await request.get('/me');
        if (res.code === 200) {
          setUserInfo(res.data);
        }
      } catch (error) {
        console.error('Failed to fetch user info', error);
      }
    };
    fetchUserInfo();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    navigate('/login', { replace: true });
  };

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  useEffect(() => {
    document.body.classList.add('g-layout-body');
    return () => document.body.classList.remove('g-layout-body');
  }, []);

  useEffect(() => {
    if (!resizing) return;
    const onMouseMove = (e: MouseEvent) => {
      const delta = e.clientX - resizeStartX;
      const next = Math.min(SIDER_MAX_WIDTH, Math.max(SIDER_MIN_WIDTH, resizeStartWidth + delta));
      setSiderWidth(next);
    };
    const onMouseUp = () => setResizing(false);
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    return () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [resizing, resizeStartX, resizeStartWidth]);

  const handleResizeStart = (e: React.MouseEvent) => {
    e.preventDefault();
    setResizing(true);
    setResizeStartX(e.clientX);
    setResizeStartWidth(siderWidth);
  };

  useEffect(() => {
    const onFullscreenChange = () => {
      setFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange);
  }, []);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen?.();
    } else {
      document.exitFullscreen?.();
    }
  };

  return (
    <Layout style={{ minHeight: '100vh', background: 'transparent' }}>
      <div
        className="g-sider-wrap"
        style={{
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          width: collapsed ? SIDER_COLLAPSED_WIDTH : siderWidth,
          zIndex: 100,
          transition: resizing ? 'none' : 'width 0.2s ease',
        }}
      >
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={(value) => setCollapsed(value)}
          width={collapsed ? SIDER_COLLAPSED_WIDTH : siderWidth}
          collapsedWidth={SIDER_COLLAPSED_WIDTH}
          theme="light"
          trigger={null}
          style={{
            overflow: 'hidden',
            height: '100vh',
            position: 'absolute',
            left: 0,
            top: 0,
            bottom: 0,
            width: collapsed ? SIDER_COLLAPSED_WIDTH : siderWidth,
            borderRight: '1px solid var(--sider-border-glass)',
            boxShadow: '2px 0 12px rgba(0,0,0,0.06)',
          }}
          className="g-main-layout-sider"
        >
        <div className="g-sider-header">
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="g-sider-header-inner"
          >
            <div
              className="g-sider-logo"
              style={{ background: 'var(--primary)' }}
            >
              渣
            </div>
            {!collapsed && (
              <span className="g-sider-title">智慧渣土消纳管控</span>
            )}
          </motion.div>
        </div>
        <div className="g-sider-menu-wrap">
          <Menu
            theme="light"
            defaultSelectedKeys={[location.pathname]}
            selectedKeys={[location.pathname]}
            defaultOpenKeys={['dashboard', 'projects', 'sites', 'vehicles', 'contracts', 'alerts', 'settings'].filter(
              (key) => location.pathname.includes(key) || (key === 'dashboard' && location.pathname === '/')
            )}
            mode="inline"
            items={items}
            style={{
              background: 'transparent',
              borderRight: 0,
              padding: '8px 12px 0',
            }}
            className="g-main-layout-menu"
          />
        </div>
        <div className={`g-sider-footer ${collapsed ? 'g-sider-footer-collapsed' : ''}`}>
          <Dropdown menu={{ items: userMenuItems }} placement="topRight" trigger={['click']}>
            <div className="g-sider-footer-user" style={{ cursor: 'pointer' }}>
              <Avatar
                size={collapsed ? 32 : 36}
                icon={<UserOutlined />}
                src="https://api.dicebear.com/7.x/notionists/svg?seed=Felix"
                className="g-sider-footer-avatar"
              />
              {!collapsed && (
                <span className="g-sider-footer-name">{userInfo?.name || userInfo?.username || '加载中...'}</span>
              )}
            </div>
          </Dropdown>
          <div
            className={`g-sider-collapse-btn ${collapsed ? 'g-sider-collapse-btn-collapsed' : ''}`}
            onClick={() => setCollapsed(!collapsed)}
            role="button"
            aria-label={collapsed ? '展开菜单' : '收起菜单'}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>
        </div>
      </Sider>
      {!collapsed && (
        <div
          className="g-sider-resize-handle"
          onMouseDown={handleResizeStart}
          role="separator"
          aria-label="拖拽调整菜单宽度"
        />
      )}
      </div>
      <Layout
        style={{
          marginLeft: collapsed ? SIDER_COLLAPSED_WIDTH : siderWidth,
          transition: resizing ? 'none' : 'margin-left 0.2s ease',
          background: 'transparent',
        }}
      >
        <Header
          className="g-main-layout-header"
          style={{
            padding: '0 24px',
            position: 'sticky',
            top: 0,
            zIndex: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            height: '64px',
            boxShadow: '0 3px 6px -4px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div className="w-1/3">
            <Input
              placeholder="搜索项目、场地或车牌号..."
              prefix={<SearchOutlined className="text-[var(--text-secondary)]" />}
              style={{
                background: 'rgba(0,0,0,0.04)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-primary)',
              }}
              className="hover:border-[var(--primary)] focus:border-[var(--primary)]"
            />
          </div>
          <div className="flex items-center gap-6">
            <Badge dot color="var(--error)" size="default">
              <Button type="text" icon={<BellOutlined style={{ fontSize: '18px', color: 'var(--text-primary)' }} />} aria-label="报警" />
            </Badge>
            {/* 后续大屏入口可放此处 */}
            <Button
              type="text"
              icon={
                fullscreen ? (
                  <FullscreenExitOutlined style={{ fontSize: '18px', color: 'var(--text-primary)' }} />
                ) : (
                  <FullscreenOutlined style={{ fontSize: '18px', color: 'var(--text-primary)' }} />
                )
              }
              onClick={toggleFullscreen}
              aria-label={fullscreen ? '退出全屏' : '全屏'}
              className="g-header-fullscreen-btn"
            />
          </div>
        </Header>
        <Content className="g-main-layout-content" style={{ margin: 16, overflow: 'initial', background: 'transparent' }}>
          <motion.div
            key={location.pathname}
            initial={{ y: 16, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.25 }}
            className="min-h-[calc(100vh-110px)] rounded-lg p-4 g-page-container"
            style={{
              background: 'var(--page-container-bg)',
              boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
              borderRadius: 8,
            }}
          >
            <Outlet />
          </motion.div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
