import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, DatePicker, Empty, Select, Space, Spin, Tag, message } from 'antd';
import {
  CarOutlined,
  EnvironmentOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ProjectOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker, MapPoint, MapPolygon, MapPolyline } from '../components/TiandituMap';
import { fetchProjects } from '../utils/projectApi';
import { fetchDashboardOverview, fetchProjectRanking, fetchSiteRanking } from '../utils/reportApi';
import { parseGeoJsonPolygon } from '../utils/mapGeometry';
import { fetchSiteMapLayers } from '../utils/siteApi';
import {
  fetchVehicleDetail,
  fetchVehicleTrackHistory,
  fetchVehicles,
} from '../utils/vehicleApi';

const { RangePicker } = DatePicker;
const { Option } = Select;

type FilterType = 'all' | 'sites' | 'projects' | 'vehicles';
type RangeValue = [Dayjs, Dayjs];

type SiteMapItem = {
  id: string;
  name: string;
  todayVolume: number;
  position: MapPoint;
  boundaryGeoJson?: string | null;
  devices: {
    id: string;
    name: string;
    type?: string | null;
    status?: string | null;
    position: MapPoint;
  }[];
};

type ProjectMapItem = {
  id: string;
  name: string;
  todayVolume: number;
  position: MapPoint;
};

type VehicleMapItem = {
  id: string;
  plateNo: string;
  statusLabel: string;
  position: MapPoint;
  gpsTime?: string | null;
};

type TrackPoint = {
  position: MapPoint;
  locateTime?: string | null;
};

const MAP_CENTER: MapPoint = [120.1551, 30.2741];

function buildStablePosition(seedText: string, category: 'site' | 'project' | 'vehicle'): MapPoint {
  const seed = seedText.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  const angle = (((seed * 47) % 360) * Math.PI) / 180;
  const radiusBase = category === 'site' ? 0.055 : category === 'project' ? 0.04 : 0.028;
  const radius = radiusBase + (seed % 7) * 0.006;
  return [
    Number((MAP_CENTER[0] + Math.cos(angle) * radius).toFixed(6)),
    Number((MAP_CENTER[1] + Math.sin(angle) * radius * 0.72).toFixed(6)),
  ];
}

function formatDateTime(value?: Dayjs | null) {
  return value ? value.format('YYYY-MM-DD HH:mm:ss') : undefined;
}

const DashboardMap: React.FC = () => {
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [loading, setLoading] = useState(false);
  const [sites, setSites] = useState<SiteMapItem[]>([]);
  const [projects, setProjects] = useState<ProjectMapItem[]>([]);
  const [vehicles, setVehicles] = useState<VehicleMapItem[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<string>();
  const [trackRange, setTrackRange] = useState<RangeValue>([
    dayjs().startOf('day'),
    dayjs(),
  ]);
  const [trackLoading, setTrackLoading] = useState(false);
  const [trackPoints, setTrackPoints] = useState<TrackPoint[]>([]);
  const [trackIndex, setTrackIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [overview, setOverview] = useState({
    movingVehicles: 0,
    warningCount: 0,
    totalSites: 0,
    totalProjects: 0,
    totalVehicles: 0,
    totalSiteDevices: 0,
  });

  useEffect(() => {
    let timer: number | undefined;
    if (playing && trackPoints.length > 1) {
      timer = window.setInterval(() => {
        setTrackIndex((current) => (current >= trackPoints.length - 1 ? 0 : current + 1));
      }, 1400);
    }
    return () => {
      if (timer) {
        window.clearInterval(timer);
      }
    };
  }, [playing, trackPoints]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [siteLayers, projectPage, vehiclePage, siteRankings, projectRankings, overviewRes] =
        await Promise.all([
          fetchSiteMapLayers(),
          fetchProjects({ pageNo: 1, pageSize: 200 }),
          fetchVehicles({ pageNo: 1, pageSize: 100 }),
          fetchSiteRanking({ date: dayjs().format('YYYY-MM-DD'), limit: 100 }),
          fetchProjectRanking({ date: dayjs().format('YYYY-MM-DD'), limit: 100 }),
          fetchDashboardOverview({ date: dayjs().format('YYYY-MM-DD') }),
        ]);

      const [vehicleDetails] = await Promise.all([
        Promise.all((vehiclePage.records || []).map((record) => fetchVehicleDetail(record.id))),
      ]);

      const siteRankMap = new Map(siteRankings.map((item) => [String(item.siteId), item]));
      const projectRankMap = new Map(projectRankings.map((item) => [String(item.projectId), item]));

      const mappedSites = siteLayers.map((site) => ({
        id: String(site.id),
        name: site.name,
        todayVolume: Number(siteRankMap.get(String(site.id))?.today || 0),
        position:
          site.lng != null && site.lat != null
            ? ([site.lng, site.lat] as MapPoint)
            : buildStablePosition(`site-${site.id}-${site.name}`, 'site'),
        boundaryGeoJson: site.boundaryGeoJson,
        devices:
          site.devices?.map((device) => ({
            id: String(device.id),
            name: device.deviceName || device.deviceCode || '场地设备',
            type: device.deviceType,
            status: device.status,
            position:
              device.lng != null && device.lat != null
                ? ([device.lng, device.lat] as MapPoint)
                : buildStablePosition(`device-${device.id}-${site.id}`, 'site'),
          })) || [],
      }));
      const mappedProjects = (projectPage.records || []).map((project) => ({
        id: String(project.id),
        name: project.name,
        todayVolume: Number(projectRankMap.get(String(project.id))?.today || 0),
        position: buildStablePosition(`project-${project.id}-${project.name}`, 'project'),
      }));
      const mappedVehicles = vehicleDetails.map((vehicle) => ({
        id: String(vehicle.id),
        plateNo: vehicle.plateNo,
        statusLabel: vehicle.runningStatusLabel || vehicle.statusLabel || vehicle.runningStatus || '未知',
        position:
          vehicle.lng != null && vehicle.lat != null
            ? ([vehicle.lng, vehicle.lat] as MapPoint)
            : buildStablePosition(`vehicle-${vehicle.id}-${vehicle.plateNo}`, 'vehicle'),
        gpsTime: vehicle.gpsTime || null,
      }));

      setSites(mappedSites);
      setProjects(mappedProjects);
      setVehicles(mappedVehicles);
      setOverview({
        movingVehicles: overviewRes.movingVehicles,
        warningCount: overviewRes.warningCount,
        totalSites: mappedSites.length,
        totalProjects: mappedProjects.length,
        totalVehicles: mappedVehicles.length,
        totalSiteDevices: mappedSites.reduce((sum, item) => sum + item.devices.length, 0),
      });
      if (!selectedVehicleId && mappedVehicles.length > 0) {
        setSelectedVehicleId(mappedVehicles[0].id);
      }
    } catch (error) {
      console.error(error);
      message.error('获取地图分布数据失败');
      setSites([]);
      setProjects([]);
      setVehicles([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const handleTrackQuery = async () => {
    if (!selectedVehicleId) {
      message.warning('请先选择车辆');
      return;
    }
    setTrackLoading(true);
    try {
      const history = await fetchVehicleTrackHistory(selectedVehicleId, {
        startTime: formatDateTime(trackRange[0]),
        endTime: formatDateTime(trackRange[1]),
      });
      const points = (history.points || []).map((item) => ({
        position: [item.lng, item.lat] as MapPoint,
        locateTime: item.locateTime,
      }));
      setTrackPoints(points);
      setTrackIndex(0);
      setPlaying(points.length > 1);
      if (points.length === 0) {
        message.info('当前时间范围暂无轨迹点，已仅展示车辆分布');
      }
    } catch (error) {
      console.error(error);
      message.error('查询车辆轨迹失败');
      setTrackPoints([]);
      setTrackIndex(0);
      setPlaying(false);
    } finally {
      setTrackLoading(false);
    }
  };

  const distributionMarkers = useMemo<MapMarker[]>(() => {
    const result: MapMarker[] = [];
    if (filterType === 'all' || filterType === 'sites') {
      result.push(
        ...sites.map((item) => ({
          id: `site-${item.id}`,
          position: item.position,
          title: `${item.name}（消纳场）`,
        })),
      );
    }
    if (filterType === 'all' || filterType === 'projects') {
      result.push(
        ...projects.map((item) => ({
          id: `project-${item.id}`,
          position: item.position,
          title: `${item.name}（项目）`,
        })),
      );
    }
    if (filterType === 'all' || filterType === 'vehicles') {
      result.push(
        ...vehicles.map((item) => ({
          id: `vehicle-${item.id}`,
          position: item.position,
          title: `${item.plateNo}（车辆）`,
        })),
      );
    }
    return result;
  }, [filterType, projects, sites, vehicles]);

  const siteDeviceMarkers = useMemo<MapMarker[]>(() => {
    if (filterType !== 'all' && filterType !== 'sites') {
      return [];
    }
    return sites.flatMap((site) =>
      site.devices.map((device) => ({
        id: `site-device-${device.id}`,
        position: device.position,
        title: `${site.name} · ${device.name}`,
      })),
    );
  }, [filterType, sites]);

  const currentTrackPoint = trackPoints.length > 0 ? trackPoints[Math.min(trackIndex, trackPoints.length - 1)] : null;

  const markers = useMemo<MapMarker[]>(() => {
    if (currentTrackPoint) {
      return [
        { id: 'active-track-point', position: currentTrackPoint.position, title: '轨迹播放点' },
        ...siteDeviceMarkers,
        ...distributionMarkers,
      ];
    }
    return [...siteDeviceMarkers, ...distributionMarkers];
  }, [currentTrackPoint, distributionMarkers, siteDeviceMarkers]);

  const polylines = useMemo<MapPolyline[]>(() => {
    if (trackPoints.length <= 1) {
      return [];
    }
    return [
      {
        id: 'vehicle-track',
        path: trackPoints.map((item) => item.position),
        color: '#1677ff',
        weight: 5,
        opacity: 0.85,
      },
    ];
  }, [trackPoints]);

  const polygons = useMemo<MapPolygon[]>(() => {
    if (filterType !== 'all' && filterType !== 'sites') {
      return [];
    }
    return sites.reduce<MapPolygon[]>((result, site) => {
      const path = parseGeoJsonPolygon(site.boundaryGeoJson);
      if (path.length >= 3) {
        result.push({
          id: `site-boundary-${site.id}`,
          path,
          color: '#1677ff',
          fillColor: '#91caff',
          fillOpacity: 0.22,
          weight: 3,
          opacity: 0.9,
        });
      }
      return result;
    }, []);
  }, [filterType, sites]);

  const mapCenter = currentTrackPoint?.position || vehicles[0]?.position || projects[0]?.position || sites[0]?.position || MAP_CENTER;

  const selectedVehicle = vehicles.find((item) => item.id === selectedVehicleId) || null;

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col space-y-4">
        </div>
        <Space wrap>
          <Select value={filterType} onChange={(value) => setFilterType(value)} style={{ width: 150 }} className="bg-white">
            <Option value="all">全部显示</Option>
            <Option value="sites">仅看消纳场</Option>
            <Option value="projects">仅看项目</Option>
            <Option value="vehicles">仅看车辆</Option>
          </Select>
          <Select
            value={selectedVehicleId}
            onChange={setSelectedVehicleId}
            placeholder="选择车辆"
            style={{ width: 180 }}
            showSearch
            optionFilterProp="children"
          >
            {vehicles.map((vehicle) => (
              <Option key={vehicle.id} value={vehicle.id}>
                {vehicle.plateNo}
              </Option>
            ))}
          </Select>
          <RangePicker
            showTime
            value={trackRange}
            onChange={(value) => setTrackRange(value as RangeValue)}
            className="bg-white g-border-panel border"
          />
          <Button type="primary" loading={trackLoading} onClick={() => void handleTrackQuery()}>
            查询轨迹
          </Button>
          <Button
            icon={playing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            disabled={trackPoints.length <= 1}
            onClick={() => setPlaying((current) => !current)}
          >
            {playing ? '暂停回放' : '开始回放'}
          </Button>
        </Space>

      <div className="flex-1 flex gap-4 min-h-0">
        <div className="w-80 flex flex-col gap-4 min-h-0">
          <Card className="glass-panel flex-1 overflow-auto" bodyStyle={{ padding: '16px' }}>
            <Spin spinning={loading}>
              <h3 className="g-text-primary font-bold mb-4 flex items-center gap-2">
                <EnvironmentOutlined className="g-text-primary-link" /> 场地分布 ({sites.length})
              </h3>
              <div className="space-y-3">
                {sites.length === 0 ? (
                  <Empty description="暂无场地数据" />
                ) : (
                  sites.map((site) => (
                    <div key={site.id} className="p-2 bg-white rounded border g-border-panel border">
                      <div className="text-sm g-text-primary">{site.name}</div>
                      <div className="text-xs g-text-secondary mt-1">今日消纳: {site.todayVolume.toLocaleString()} 方</div>
                      <div className="text-xs g-text-secondary mt-1">红线: {site.boundaryGeoJson ? '已配置' : '回退生成'} · 设备: {site.devices.length}</div>
                    </div>
                  ))
                )}
              </div>

              <h3 className="g-text-primary font-bold mt-6 mb-4 flex items-center gap-2">
                <ProjectOutlined className="g-text-success" /> 活跃项目 ({projects.length})
              </h3>
              <div className="space-y-3">
                {projects.length === 0 ? (
                  <Empty description="暂无项目数据" />
                ) : (
                  projects.map((project) => (
                    <div key={project.id} className="p-2 bg-white rounded border g-border-panel border">
                      <div className="text-sm g-text-primary">{project.name}</div>
                      <div className="text-xs g-text-secondary mt-1">今日出土: {project.todayVolume.toLocaleString()} 方</div>
                    </div>
                  ))
                )}
              </div>

              <h3 className="g-text-primary font-bold mt-6 mb-4 flex items-center gap-2">
                <CarOutlined className="g-text-warning" /> 车辆态势 ({vehicles.length})
              </h3>
              <div className="space-y-3">
                {vehicles.length === 0 ? (
                  <Empty description="暂无车辆数据" />
                ) : (
                  vehicles.slice(0, 8).map((vehicle) => (
                    <div
                      key={vehicle.id}
                      className={`p-2 bg-white rounded border g-border-panel border cursor-pointer ${selectedVehicleId === vehicle.id ? 'border-blue-500' : ''}`}
                      onClick={() => setSelectedVehicleId(vehicle.id)}
                    >
                      <div className="text-sm g-text-primary">{vehicle.plateNo}</div>
                      <div className="text-xs g-text-secondary mt-1">{vehicle.statusLabel}</div>
                    </div>
                  ))
                )}
              </div>
            </Spin>
          </Card>
        </div>

        <Card className="glass-panel flex-1 relative overflow-hidden" bodyStyle={{ padding: 0, height: '100%' }}>
          <TiandituMap
            className="absolute inset-0"
            center={mapCenter}
            zoom={11}
            markers={markers}
            polylines={polylines}
            polygons={polygons}
            loadingText="天地图加载中..."
          />

          <div className="absolute top-4 left-4 bg-white/95 backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl">
            <div className="flex flex-wrap gap-2 mb-3">
              <Tag color="blue">消纳场 {overview.totalSites}</Tag>
              <Tag color="cyan">场地设备 {overview.totalSiteDevices}</Tag>
              <Tag color="green">项目 {overview.totalProjects}</Tag>
              <Tag color="gold">车辆 {overview.totalVehicles}</Tag>
            </div>
            <div className="text-xs g-text-secondary leading-6">
              当前地图已切换为天地图，消纳场优先展示真实红线和设备点位；项目与场地主数据缺经纬度时使用稳定回退坐标，车辆优先展示真实 GPS 坐标。
            </div>
          </div>

          <div className="absolute top-4 right-4 bg-white backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl">
            <div className="grid grid-cols-2 gap-4 text-center">
              <div>
                <div className="text-xs g-text-secondary mb-1">在线车辆</div>
                <div className="text-xl font-bold g-text-primary-link">{overview.movingVehicles}</div>
              </div>
              <div>
                <div className="text-xs g-text-secondary mb-1">今日预警</div>
                <div className="text-xl font-bold g-text-error">{overview.warningCount}</div>
              </div>
            </div>
          </div>

          <div className="absolute bottom-4 left-4 right-4 bg-white backdrop-blur-md p-3 rounded-lg border g-border-panel border flex items-center gap-4">
            <Button
              type="primary"
              shape="circle"
              icon={playing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              disabled={trackPoints.length <= 1}
              onClick={() => setPlaying((current) => !current)}
            />
            <div className="flex-1 h-2 bg-black/10 rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-500 transition-all"
                style={{
                  width:
                    trackPoints.length > 1
                      ? `${Math.round((trackIndex / (trackPoints.length - 1)) * 100)}%`
                      : '0%',
                }}
              />
            </div>
            <div className="text-xs g-text-secondary min-w-[220px] text-right">
              {selectedVehicle ? `${selectedVehicle.plateNo} · ` : ''}
              {currentTrackPoint?.locateTime || selectedVehicle?.gpsTime || '暂无轨迹时间'}
            </div>
          </div>
        </Card>
    </div>
    </div>
  );
};

export default DashboardMap;
