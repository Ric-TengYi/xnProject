import React, { useState, useEffect, useRef } from 'react';
import { Card, Select, DatePicker, Button, Space } from 'antd';
import { EnvironmentOutlined, CarOutlined, ProjectOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;

declare global {
    interface Window {
        AMap?: any;
    }
}

const DashboardMap: React.FC = () => {
    const [filterType, setFilterType] = useState('all');
    const mapRef = useRef<HTMLDivElement>(null);
    const mapInstanceRef = useRef<any>(null);

    useEffect(() => {
        if (typeof window !== 'undefined' && window.AMap && mapRef.current && !mapInstanceRef.current) {
            mapInstanceRef.current = new window.AMap.Map(mapRef.current, {
                zoom: 11,
                center: [104.065735, 30.657689],
                viewMode: '2D',
            });
        }
        return () => {
            if (mapInstanceRef.current) {
                mapInstanceRef.current.destroy();
                mapInstanceRef.current = null;
            }
        };
    }, []);

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
                {/* 左侧数据面板 */}
                <div className="w-80 flex flex-col gap-4">
                    <Card className="glass-panel flex-1 overflow-auto" bodyStyle={{ padding: '16px' }}>
                        <h3 className="g-text-primary font-bold mb-4 flex items-center gap-2">
                            <EnvironmentOutlined className="g-text-primary-link" /> 场地分布 (5)
                        </h3>
                        <div className="space-y-3">
                            {['东区临时消纳场', '南郊复合型消纳中心', '北区填埋场'].map((site, i) => (
                                <div key={i} className="p-2 bg-white rounded border g-border-panel border cursor-pointer hover:border-blue-500/50">
                                    <div className="text-sm g-text-primary">{site}</div>
                                    <div className="text-xs g-text-secondary mt-1">当前排队: {Math.floor(Math.random() * 20)}辆</div>
                                </div>
                            ))}
                        </div>

                        <h3 className="g-text-primary font-bold mt-6 mb-4 flex items-center gap-2">
                            <ProjectOutlined className="g-text-success" /> 活跃项目 (12)
                        </h3>
                        <div className="space-y-3">
                            {['市中心地铁延长线三期', '滨海新区基础建设', '老旧小区改造'].map((proj, i) => (
                                <div key={i} className="p-2 bg-white rounded border g-border-panel border cursor-pointer hover:border-green-500/50">
                                    <div className="text-sm g-text-primary">{proj}</div>
                                    <div className="text-xs g-text-secondary mt-1">今日出土: {Math.floor(Math.random() * 5000)}方</div>
                                </div>
                            ))}
                        </div>
                    </Card>
                </div>

                {/* 右侧地图区域 */}
                <Card className="glass-panel flex-1 relative overflow-hidden" bodyStyle={{ padding: 0, height: '100%' }}>
                    <div ref={mapRef} className="absolute inset-0" style={{ minHeight: 400 }} />
                    {typeof window !== 'undefined' && !window.AMap && (
                        <div className="absolute inset-0 g-bg-toolbar flex items-center justify-center pointer-events-none">
                            <div className="text-center">
                                <EnvironmentOutlined className="text-6xl text-slate-700 mb-4" />
                                <p className="text-slate-600">高德地图加载中…</p>
                            </div>
                        </div>
                    )}

                    {/* 悬浮统计面板 */}
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

                    {/* 底部轨迹回放控制条 (可选展示) */}
                    <div className="absolute bottom-4 left-4 right-4 bg-white backdrop-blur-md p-3 rounded-lg border g-border-panel border flex items-center gap-4">
                        <Button type="primary" shape="circle" icon={<CarOutlined />} />
                        <div className="flex-1 h-2 bg-black/10 rounded-full overflow-hidden">
                            <div className="h-full bg-blue-500 w-1/3"></div>
                        </div>
                        <div className="text-xs g-text-secondary">2024-03-05 14:30:00</div>
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default DashboardMap;
