import React, { useEffect, useMemo, useState } from 'react';
import { Badge, Button, Card, Empty, Input, List, Select, Slider, Space, Spin, Tag, message } from 'antd';
import {
  SearchOutlined,
  EnvironmentOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  FastForwardOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker, MapPoint, MapPolyline } from '../components/TiandituMap';
import { fetchVehicleDetail, fetchVehicles } from '../utils/vehicleApi';
import type { VehicleDetailRecord } from '../utils/vehicleApi';

const { Option } = Select;

type TrackingVehicle = {
  id: string;
  company: string;
  status: string;
  statusCode: 'moving' | 'stopped' | 'offline';
  speed: string;
  location: string;
  position: MapPoint;
  path: MapPoint[];
};

const defaultCenter: MapPoint = [120.1551, 30.2741];

const interpolatePosition = (path: MapPoint[], progress: number): MapPoint => {
  if (path.length <= 1) {
    return path[0] || defaultCenter;
  }
  const normalized = Math.min(Math.max(progress, 0), 100) / 100;
  const scaled = normalized * (path.length - 1);
  const index = Math.min(Math.floor(scaled), path.length - 2);
  const ratio = scaled - index;
  const start = path[index];
  const end = path[index + 1];
  return [start[0] + (end[0] - start[0]) * ratio, start[1] + (end[1] - start[1]) * ratio];
};

const fallbackPoint = (index: number): MapPoint => [120.12 + index * 0.022, 30.21 + index * 0.018];

const buildPath = (position: MapPoint, index: number): MapPoint[] => {
  const offset = 0.012 + (index % 4) * 0.004;
  return [
    [position[0] - offset, position[1] - offset * 0.7],
    [position[0] - offset * 0.45, position[1] - offset * 0.2],
    position,
    [position[0] + offset * 0.35, position[1] + offset * 0.55],
    [position[0] + offset * 0.7, position[1] + offset * 0.95],
  ];
};

const resolveTrackingVehicle = (record: VehicleDetailRecord, index: number): TrackingVehicle => {
  const position: MapPoint = record.lng != null && record.lat != null ? [record.lng, record.lat] : fallbackPoint(index);
  const runningStatus = (record.runningStatus || '').toUpperCase();
  const statusCode: 'moving' | 'stopped' | 'offline' =
    runningStatus === 'MOVING' ? 'moving' : record.status === 3 ? 'offline' : 'stopped';
  const status = statusCode === 'moving' ? '行驶中' : statusCode === 'stopped' ? '静止' : '离线';
  const location = record.remark || record.orgName || record.fleetName || '平台车辆在线定位';
  return {
    id: record.plateNo,
    company: record.orgName || '未归属单位',
    status,
    statusCode,
    speed: record.currentSpeed != null ? String(record.currentSpeed) + ' km/h' : '--',
    location,
    position,
    path: buildPath(position, index),
  };
};

const VehicleTracking: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [fleetFilter, setFleetFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [keyword, setKeyword] = useState('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(45);
  const [vehicles, setVehicles] = useState<TrackingVehicle[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState('');

  useEffect(() => {
    if (!isPlaying) {
      return;
    }
    const timer = window.setInterval(() => {
      setProgress((value) => {
        if (value >= 100) {
          window.clearInterval(timer);
          setIsPlaying(false);
          return 100;
        }
        return Math.min(value + 2, 100);
      });
    }, 800);
    return () => window.clearInterval(timer);
  }, [isPlaying]);

  useEffect(() => {
    const loadVehicles = async () => {
      setLoading(true);
      try {
        const page = await fetchVehicles({ pageNo: 1, pageSize: 50 });
        const detailRecords = await Promise.all(
          (page.records || []).map(async (record) => fetchVehicleDetail(record.id))
        );
        const mapped = detailRecords.map((record, index) => resolveTrackingVehicle(record, index));
        setVehicles(mapped);
        setSelectedVehicleId((current) => current || (mapped[0] ? mapped[0].id : ''));
      } catch (error) {
        console.error(error);
        message.error('获取车辆追踪数据失败');
        setVehicles([]);
      } finally {
        setLoading(false);
      }
    };

    void loadVehicles();
  }, []);

  const fleetOptions = useMemo(() => {
    return Array.from(new Set(vehicles.map((vehicle) => vehicle.company))).filter(Boolean);
  }, [vehicles]);

  const filteredVehicles = useMemo(() => {
    const search = keyword.trim();
    return vehicles.filter((vehicle) => {
      if (fleetFilter !== 'all' && vehicle.company !== fleetFilter) {
        return false;
      }
      if (statusFilter !== 'all' && vehicle.statusCode !== statusFilter) {
        return false;
      }
      if (search && !vehicle.id.includes(search) && !vehicle.company.includes(search) && !vehicle.location.includes(search)) {
        return false;
      }
      return true;
    });
  }, [fleetFilter, keyword, statusFilter, vehicles]);

  useEffect(() => {
    if (!filteredVehicles.some((vehicle) => vehicle.id === selectedVehicleId)) {
      setSelectedVehicleId(filteredVehicles[0] ? filteredVehicles[0].id : '');
      setProgress(0);
    }
  }, [filteredVehicles, selectedVehicleId]);

  const selectedVehicle = useMemo(() => {
    return filteredVehicles.find((vehicle) => vehicle.id === selectedVehicleId)
      || vehicles.find((vehicle) => vehicle.id === selectedVehicleId)
      || filteredVehicles[0]
      || vehicles[0]
      || null;
  }, [filteredVehicles, selectedVehicleId, vehicles]);

  const activePosition = useMemo(() => {
    return selectedVehicle ? interpolatePosition(selectedVehicle.path, progress) : defaultCenter;
  }, [progress, selectedVehicle]);

  const markers = useMemo<MapMarker[]>(() => {
    return filteredVehicles.map((vehicle) => ({
      id: vehicle.id,
      position: selectedVehicle && vehicle.id === selectedVehicle.id ? activePosition : vehicle.position,
    }));
  }, [activePosition, filteredVehicles, selectedVehicle]);

  const polylines = useMemo<MapPolyline[]>(() => {
    if (!selectedVehicle) {
      return [];
    }
    return [{ id: selectedVehicle.id + '-route', path: selectedVehicle.path, color: '#1677ff', weight: 5 }];
  }, [selectedVehicle]);

  return (
    <div className="flex h-[calc(100vh-110px)] gap-6">
      <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="w-80 flex flex-col gap-4">
        <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: '16px' }}>
          <h2 className="text-lg font-bold g-text-primary mb-4">车辆追踪</h2>
          <div className="space-y-3">
            <Select value={fleetFilter} className="w-full" popupClassName="bg-white" onChange={setFleetFilter}>
              <Option value="all">全部车队</Option>
              {fleetOptions.map((company) => (
                <Option key={company} value={company}>{company}</Option>
              ))}
            </Select>
            <Select value={statusFilter} className="w-full" popupClassName="bg-white" onChange={setStatusFilter}>
              <Option value="all">全部状态</Option>
              <Option value="moving">行驶中</Option>
              <Option value="stopped">静止</Option>
              <Option value="offline">离线</Option>
            </Select>
            <Input
              placeholder="搜索车牌号..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="bg-white g-border-panel border g-text-primary"
            />
          </div>
        </Card>

        <Card className="glass-panel g-border-panel border flex-1 overflow-auto" bodyStyle={{ padding: 0 }}>
          <Spin spinning={loading}>
            {filteredVehicles.length === 0 ? (
              <div className="py-16"><Empty description="暂无可追踪车辆" /></div>
            ) : (
              <List
                dataSource={filteredVehicles}
                renderItem={(item) => (
                  <List.Item
                    className={[
                      'px-4 py-3 hover:bg-white cursor-pointer border-b g-border-panel border transition-colors',
                      item.id === (selectedVehicle && selectedVehicle.id) ? 'bg-white/80' : '',
                    ].join(' ')}
                    onClick={() => {
                      setSelectedVehicleId(item.id);
                      setProgress(0);
                    }}
                  >
                    <div className="w-full">
                      <div className="flex justify-between items-center mb-1">
                        <span className="g-text-primary font-bold">{item.id}</span>
                        <Badge
                          status={item.statusCode === 'moving' ? 'success' : item.statusCode === 'stopped' ? 'warning' : 'default'}
                          text={<span className="g-text-secondary text-xs">{item.status}</span>}
                        />
                      </div>
                      <div className="text-xs g-text-secondary mb-2">{item.company}</div>
                      <div className="flex justify-between text-xs g-text-secondary gap-2">
                        <span className="truncate w-40" title={item.location}><EnvironmentOutlined /> {item.location}</span>
                        <span className="g-text-primary-link whitespace-nowrap">{item.speed}</span>
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            )}
          </Spin>
        </Card>
      </motion.div>

      <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} className="flex-1 flex flex-col gap-4 relative">
        <div className="flex-1 glass-panel g-border-panel border rounded-lg overflow-hidden relative">
          <TiandituMap
            className="absolute inset-0"
            center={activePosition}
            zoom={12}
            markers={markers}
            polylines={polylines}
            loadingText="天地图车辆追踪加载中..."
          />

          {selectedVehicle && (
            <div className="absolute top-4 left-4 bg-white/95 backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl min-w-72">
              <div className="flex items-center justify-between gap-3 mb-3">
                <div>
                  <div className="text-lg font-semibold g-text-primary">{selectedVehicle.id}</div>
                  <div className="text-xs g-text-secondary mt-1">{selectedVehicle.company}</div>
                </div>
                <Tag color={selectedVehicle.statusCode === 'moving' ? 'green' : selectedVehicle.statusCode === 'stopped' ? 'orange' : 'default'}>
                  {selectedVehicle.status}
                </Tag>
              </div>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="bg-slate-50 rounded-lg px-3 py-2">
                  <div className="text-xs g-text-secondary mb-1">当前速度</div>
                  <div className="font-semibold g-text-primary-link">{selectedVehicle.speed}</div>
                </div>
                <div className="bg-slate-50 rounded-lg px-3 py-2">
                  <div className="text-xs g-text-secondary mb-1">轨迹进度</div>
                  <div className="font-semibold g-text-primary">{progress}%</div>
                </div>
              </div>
              <div className="text-xs g-text-secondary mt-3 leading-6"><EnvironmentOutlined /> {selectedVehicle.location}</div>
            </div>
          )}

          <div className="absolute top-4 right-4 bg-white/95 backdrop-blur-md p-4 rounded-lg border g-border-panel border shadow-xl">
            <div className="text-sm font-semibold g-text-primary mb-2">地图能力</div>
            <div className="space-y-2 text-xs g-text-secondary">
              <div>真实车辆列表接入</div>
              <div>天地图位置分布</div>
              <div>单车轨迹回放</div>
            </div>
          </div>
        </div>

        <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: '16px 24px' }}>
          <div className="flex items-center gap-6">
            <Space size="large">
              <Button
                type="primary"
                shape="circle"
                icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                size="large"
                disabled={!selectedVehicle}
                onClick={() => setIsPlaying(!isPlaying)}
                className="g-btn-primary border-none"
              />
              <Button
                shape="circle"
                icon={<FastForwardOutlined />}
                disabled={!selectedVehicle}
                className="bg-white g-text-secondary border-slate-600 hover:g-text-primary"
                onClick={() => setProgress((value) => Math.min(value + 10, 100))}
              />
            </Space>
            <div className="flex-1 flex items-center gap-4">
              <span className="g-text-secondary text-sm">08:00</span>
              <Slider
                className="flex-1"
                value={progress}
                onChange={setProgress}
                tooltip={{ formatter: (value) => '14:' + Math.floor((value || 0) * 0.6).toString().padStart(2, '0') }}
              />
              <span className="g-text-secondary text-sm">18:00</span>
            </div>
            <div className="g-text-secondary text-sm">
              当前时间: <span className="g-text-primary-link font-mono">14:30:00</span>
            </div>
          </div>
        </Card>
      </motion.div>
    </div>
  );
};

export default VehicleTracking;
