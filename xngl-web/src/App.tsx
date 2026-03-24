import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ProjectsManagement from './pages/ProjectsManagement';
import ProjectDetail from './pages/ProjectDetail';
import ProjectsPayments from './pages/ProjectsPayments';
import ProjectsPermits from './pages/ProjectsPermits';
import ProjectsDailyReport from './pages/ProjectsDailyReport';
import ProjectsReports from './pages/ProjectsReports';
import CheckinRecords from './pages/CheckinRecords';
import DisposalRecords from './pages/DisposalRecords';
import DashboardSites from './pages/DashboardSites';
import DashboardProjects from './pages/DashboardProjects';
import DashboardMap from './pages/DashboardMap';
import VehicleCapacityAnalysis from './pages/VehicleCapacityAnalysis';
import SitesManagement from './pages/SitesManagement';
import SiteDetail from './pages/SiteDetail';
import SitesDisposals from './pages/SitesDisposals';
import SitesDocuments from './pages/SitesDocuments';
import SitesBasicInfo from './pages/SitesBasicInfo';
import SitesReports from './pages/SitesReports';
import VehiclesManagement from './pages/VehiclesManagement';
import VehicleModelsManagement from './pages/VehicleModelsManagement';
import FleetManagement from './pages/FleetManagement';
import VehiclesCards from './pages/VehiclesCards';
import VehicleInsurances from './pages/VehicleInsurances';
import VehicleMaintenancePlans from './pages/VehicleMaintenancePlans';
import VehiclePersonnelCertificates from './pages/VehiclePersonnelCertificates';
import VehicleRepairs from './pages/VehicleRepairs';
import VehicleTracking from './pages/VehicleTracking';
import ViolationsList from './pages/ViolationsList';
import ContractsManagement from './pages/ContractsManagement';
import ContractDetail from './pages/ContractDetail';
import ContractTransfers from './pages/ContractTransfers';
import ContractsPayments from './pages/ContractsPayments';
import Settlements from './pages/Settlements';
import MonthlyReport from './pages/MonthlyReport';
import AlertsMonitor from './pages/AlertsMonitor';
import AlertConfig from './pages/AlertConfig';
import EventsManagement from './pages/EventsManagement';
import SecurityLedger from './pages/SecurityLedger';
import OrgManagement from './pages/OrgManagement';
import UsersManagement from './pages/UsersManagement';
import RolesManagement from './pages/RolesManagement';
import Dictionary from './pages/Dictionary';
import ApprovalConfig from './pages/ApprovalConfig';
import SystemParams from './pages/SystemParams';
import SystemLogs from './pages/SystemLogs';
import MessageCenter from './pages/MessageCenter';
import PlatformIntegrations from './pages/PlatformIntegrations';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="dashboard/sites" element={<DashboardSites />} />
          <Route path="dashboard/projects" element={<DashboardProjects />} />
          <Route path="dashboard/map" element={<DashboardMap />} />
          <Route path="dashboard/capacity-analysis" element={<VehicleCapacityAnalysis />} />

          <Route path="projects" element={<ProjectsManagement />} />
          <Route path="projects/:id" element={<ProjectDetail />} />
          <Route path="projects/payments" element={<ProjectsPayments />} />
          <Route path="projects/permits" element={<ProjectsPermits />} />
          <Route path="projects/daily-report" element={<ProjectsDailyReport />} />
          <Route path="projects/reports" element={<ProjectsReports />} />
          <Route path="queries/checkins" element={<CheckinRecords />} />
          <Route path="queries/disposals" element={<DisposalRecords />} />

          <Route path="sites" element={<SitesManagement />} />
          <Route path="sites/:id" element={<SiteDetail />} />
          <Route path="sites/disposals" element={<SitesDisposals />} />
          <Route path="sites/documents" element={<SitesDocuments />} />
          <Route path="sites/basic-info" element={<SitesBasicInfo />} />
          <Route path="sites/reports" element={<SitesReports />} />

          <Route path="vehicles" element={<VehiclesManagement />} />
          <Route path="vehicles/models" element={<VehicleModelsManagement />} />
          <Route path="vehicles/fleet" element={<FleetManagement />} />
          <Route path="vehicles/cards" element={<VehiclesCards />} />
          <Route path="vehicles/insurances" element={<VehicleInsurances />} />
          <Route path="vehicles/maintenance" element={<VehicleMaintenancePlans />} />
          <Route path="vehicles/personnel-certificates" element={<VehiclePersonnelCertificates />} />
          <Route path="vehicles/repairs" element={<VehicleRepairs />} />
          <Route path="vehicles/tracking" element={<VehicleTracking />} />
          <Route path="vehicles/violations" element={<ViolationsList />} />

          <Route path="contracts" element={<ContractsManagement />} />
          <Route path="contracts/:id" element={<ContractDetail />} />
          <Route path="contracts/transfers" element={<ContractTransfers />} />
          <Route path="contracts/payments" element={<ContractsPayments />} />
          <Route path="contracts/settlements" element={<Settlements />} />
          <Route path="contracts/monthly-report" element={<MonthlyReport />} />

          <Route path="alerts" element={<AlertsMonitor />} />
          <Route path="alerts/config" element={<AlertConfig />} />
          <Route path="alerts/events" element={<EventsManagement />} />
          <Route path="alerts/security" element={<SecurityLedger />} />
          <Route path="messages" element={<MessageCenter />} />

          <Route path="settings/org" element={<OrgManagement />} />
          <Route path="settings/users" element={<UsersManagement />} />
          <Route path="settings/roles" element={<RolesManagement />} />
          <Route path="settings/dictionary" element={<Dictionary />} />
          <Route path="settings/approvals" element={<ApprovalConfig />} />
          <Route path="settings/system-params" element={<SystemParams />} />
          <Route path="settings/platform-integrations" element={<PlatformIntegrations />} />
          <Route path="settings/logs" element={<SystemLogs />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
