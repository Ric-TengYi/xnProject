import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ProjectsManagement from './pages/ProjectsManagement';
import Placeholder from './pages/Placeholder';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="dashboard/sites" element={<Placeholder title="消纳场数据" />} />
          <Route path="dashboard/projects" element={<Placeholder title="项目数据" />} />
          <Route path="dashboard/map" element={<Placeholder title="地图展示" />} />

          <Route path="projects" element={<ProjectsManagement />} />
          <Route path="projects/:id" element={<Placeholder title="项目详情" />} />
          <Route path="projects/payments" element={<Placeholder title="交款数据" />} />
          <Route path="projects/permits" element={<Placeholder title="处置证清单" />} />
          <Route path="projects/daily-report" element={<Placeholder title="项目日报" />} />

          <Route path="sites" element={<Placeholder title="场地列表" />} />
          <Route path="sites/:id" element={<Placeholder title="场地详情" />} />
          <Route path="sites/disposals" element={<Placeholder title="消纳清单" />} />
          <Route path="sites/documents" element={<Placeholder title="场地资料" />} />
          <Route path="sites/basic-info" element={<Placeholder title="基础信息" />} />

          <Route path="vehicles" element={<Placeholder title="车辆信息" />} />
          <Route path="vehicles/fleet" element={<Placeholder title="车队管理" />} />
          <Route path="vehicles/cards" element={<Placeholder title="油电卡管理" />} />
          <Route path="vehicles/tracking" element={<Placeholder title="送货跟踪" />} />
          <Route path="vehicles/violations" element={<Placeholder title="违规车辆清单" />} />

          <Route path="contracts" element={<Placeholder title="合同清单" />} />
          <Route path="contracts/:id" element={<Placeholder title="合同详情" />} />
          <Route path="contracts/payments" element={<Placeholder title="合同入账" />} />
          <Route path="contracts/settlements" element={<Placeholder title="结算管理" />} />
          <Route path="contracts/monthly-report" element={<Placeholder title="月报统计" />} />

          <Route path="alerts" element={<Placeholder title="系统预警" />} />
          <Route path="alerts/config" element={<Placeholder title="预警配置" />} />
          <Route path="alerts/events" element={<Placeholder title="事件管理" />} />
          <Route path="alerts/security" element={<Placeholder title="安全台账" />} />

          <Route path="settings/organization" element={<Placeholder title="组织人员" />} />
          <Route path="settings/roles" element={<Placeholder title="角色管理" />} />
          <Route path="settings/dictionary" element={<Placeholder title="数据字典" />} />
          <Route path="settings/approvals" element={<Placeholder title="审批配置" />} />
          <Route path="settings/logs" element={<Placeholder title="系统日志" />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
