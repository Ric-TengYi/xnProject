import React, { useMemo, useState } from 'react';
import { Card, Select, DatePicker, Button, Space, Tag } from 'antd';
import { EnvironmentOutlined, CarOutlined, ProjectOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker } from '../components/TiandituMap';

const { RangePicker } = DatePicker;
const { Option } = Select;

const siteItems = [
  { id: 'site-east', name: '东区临时消纳场', queue: 8, position: [120.2535, 30.3274] as [number, number] },
  { id: 'site-south', name: '南郊复合型消纳中心', queue: 14, position: [120.1814, 30.2147] as [number, number] },
  { id: 'site-north', name: '北区工程回填场', queue: 5, position: [120.1126, 30.3652] as [number, number] },
];

const projectItems = [
  { id: 'project-metro', name: '地铁延长线三期', volume: 2680, position: [120.1692, 30.2756] as [number, number] },
  { id: 'project-hub', name: '滨江交通枢纽', volume: 1940, position: [120.2163, 30.2084] as [number, number] },
  { id: 'project-renew', name: '老旧片区更新工程', volume: 1230, position: [120.1387, 30.2891] as [number, number] },
];

const vehicleItems = [
  { id: '川A88921', status: '运输中', position: [120.2021, 30.2864] as [number, number] },
  { id: '川A6258W', status: '排队进场', position: [120.1862, 30.2294] as [number, number] },
  { id: '川A1192N', status: '返程中', position: [120.1465, 30.3151] as [number, number] },
  { id: '川B44521', status: '待调度', position: [120.2298, 30.2518] as [number, number] },
];

const DashboardMap: React.FC = () => {
  const [filterType, setFilterType] = useState('all');

  const markers = useMemo<MapMarker[]>(() => {
    const result: MapMarker[] = [];

    if (filterType === 'all' || filterType === 'sites') {
      result.push(...siteItems.map((item) => ({ id: item.id, position: item.position })));
    }
    if (filterType === 'all' || filterType === 'projects') {
      result.push(...projectItems.map((item) => ({ id: item.id, position: item.position })));
    }
    if (filterType === 'all' || filterType === 'vehicles') {
      result.push(...vehicleItems.map((item) => ({ id: item.id, position: item.position })));
    }

    return result;
  }, [filterType]);

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold g-text-primary m-0">全局地图监控</h1>
        <Space>
          <Select
            value={filterType}
            onChange={setFilterType}
            style={{ width: 150 }}
            className="bg-white"
          >
            <Option value="all">全部显示</Option>
            <Option value="sites">仅看消纳场</Option>
            <Option value="projects">仅看项目</Option>
            <Option value="vehicles">仅看车辆</Option>
          </Select>
          <RangePicker
            showTime
            defaultValue={[dayjs().startOf('day'), dayjs()]}
            className="bg-white g-border-panel border"
          />
          <Button type="primary">查询轨迹</Button>
        </Space>
      </div>

      <div className="flex-1 flex gap-4">
        <div className="w-80 flex flex-col gap-4">
          <Card className="glass-panel flex-1 overflow-auto" bodyStyle={{ padding: '16px' }}>
            <h3 className="g-text-primary font-bold mb-4 flex items-center gap-2">
              <EnvironmentOutlined className="g-text-primary-link" /> 场地分布 ({siteItems.length})
            </h3>
            <div className="space-y-3">
              {siteItems.map((site) => (
                <div key={site.id} className="p-2 bg-white rounded border g-border-panel border cursor-pointer hover:border-blue-500/50">
                  <div className="text-sm g-text-primary">{site.name}</div>
                  <div className="text-xs g-text-secondary mt-1">当前排队: {site.queue} 辆</div>
                </div>
              ))}
            </div>

            <h3 className="g-text-primary font-bold mt-6 mb-4 flex items-center gap-2">
              <ProjectOutlined className="g-text-success" /> 活跃项目 ({projectItems.length})
            </h3>
            <div className="space-y-3">
              {projectItems.map((project) => (
                <div key={project.id} className="p-2 bg-white rounded border g-border-panel border cursor-pointer hover:border-green-500/50">
                  <div className="text-sm g-text-primary">{project.name}</div>
                  <div className="text-xs g-text-secondary mt-1">今日出土: {project.volume} 方</div>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <Card className="glass-panel flex-1 relative overflow-hidden" bodyStyle={{ padding: 0, height: '100%' }}>
          <TiandituMap
            className="absolute inset-0"
            center={[120.1551, 30.2741]}
            zoom={11}
            markers={markers}
            loadingText="天地图加载中..."
          />

          <div className="absolute top-4 left-4 bg-white/95 backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl">
            <div className="flex flex-wrap gap-2 mb-3">
              <Tag color="blue">消纳场 {siteItems.length}</Tag>
              <Tag color="green">项目 {projectItems.length}</Tag>
              <Tag color="gold">车辆 {vehicleItems.length}</Tag>
            </div>
            <div className="text-xs g-text-secondary leading-6">
              当前底图已切换为天地图，支持场地、项目、车辆分布统一查看。
            </div>
          </div>

          <div className="absolute top-4 right-4 bg-white backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl">
            <div className="grid grid-cols-2 gap-4 text-center">
              <div>
                <div className="text-xs g-text-secondary mb-1">在线车辆</div>
                <div className="text-xl font-bold g-text-primary-link">1,284</div>
              </div>
              <div>
                <div className="text-xs g-text-secondary mb-1">今日预警</div>
                <div className="text-xl font-bold g-text-error">32</div>
              </div>
            </div>
          </div>

          <div className="absolute bottom-4 left-4 right-4 bg-white backdrop-blur-md p-3 rounded-lg border g-border-panel border flex items-center gap-4">
            <Button type="primary" shape="circle" icon={<CarOutlined />} />
            <div className="flex-1 h-2 bg-black/10 rounded-full overflow-hidden">
              <div className="h-full bg-blue-500 w-1/3"></div>
            </div>
            <div className="text-xs g-text-secondary">2026-03-20 14:30:00</div>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default DashboardMap;
