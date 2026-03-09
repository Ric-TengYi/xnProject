import React, { useState } from 'react';
import { Card, Input, Button, Tag, Select, List, Badge, Space, Slider } from 'antd';
import { SearchOutlined, EnvironmentOutlined, PlayCircleOutlined, PauseCircleOutlined, FastForwardOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Option } = Select;

const VehicleTracking: React.FC = () => {
    const [isPlaying, setIsPlaying] = useState(false);
    const [progress, setProgress] = useState(0);

    const vehicleList = [
        { id: '川A88921', company: '宏基渣土运输公司', status: '行驶中', speed: '45 km/h', location: '科技路与创新大道交汇处' },
        { id: '川A6258W', company: '顺达土方工程队', status: '静止', speed: '0 km/h', location: '城东一号消纳场内' },
        { id: '川A1192N', company: '捷安运输', status: '行驶中', speed: '52 km/h', location: '环城南路 88 段' },
        { id: '川B44521', company: '新思路运输', status: '离线', speed: '--', location: '最后位置: 高新产业园C区' },
    ];

    return (
        <div className="flex h-[calc(100vh-110px)] gap-6">
            {/* 左侧车辆列表面板 */}
            <motion.div 
                initial={{ opacity: 0, x: -20 }} 
                animate={{ opacity: 1, x: 0 }} 
                className="w-80 flex flex-col gap-4"
            >
                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: '16px' }}>
                    <h2 className="text-lg font-bold g-text-primary mb-4">车辆追踪</h2>
                    <div className="space-y-3">
                        <Select defaultValue="all" className="w-full" popupClassName="bg-white">
                            <Option value="all">全部车队</Option>
                            <Option value="hongji">宏基渣土运输公司</Option>
                            <Option value="shunda">顺达土方工程队</Option>
                        </Select>
                        <Select defaultValue="all" className="w-full" popupClassName="bg-white">
                            <Option value="all">全部状态</Option>
                            <Option value="moving">行驶中</Option>
                            <Option value="stopped">静止</Option>
                            <Option value="offline">离线</Option>
                        </Select>
                        <Input placeholder="搜索车牌号..." prefix={<SearchOutlined className="g-text-secondary" />} className="bg-white g-border-panel border g-text-primary" />
                    </div>
                </Card>

                <Card className="glass-panel g-border-panel border flex-1 overflow-auto" bodyStyle={{ padding: 0 }}>
                    <List
                        dataSource={vehicleList}
                        renderItem={item => (
                            <List.Item className="px-4 py-3 hover:bg-white cursor-pointer border-b g-border-panel border transition-colors">
                                <div className="w-full">
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="g-text-primary font-bold">{item.id}</span>
                                        <Badge status={item.status === '行驶中' ? 'success' : item.status === '静止' ? 'warning' : 'default'} text={<span className="g-text-secondary text-xs">{item.status}</span>} />
                                    </div>
                                    <div className="text-xs g-text-secondary mb-2">{item.company}</div>
                                    <div className="flex justify-between text-xs g-text-secondary">
                                        <span className="truncate w-32" title={item.location}><EnvironmentOutlined /> {item.location}</span>
                                        <span className="g-text-primary-link">{item.speed}</span>
                                    </div>
                                </div>
                            </List.Item>
                        )}
                    />
                </Card>
            </motion.div>

            {/* 右侧地图与回放面板 */}
            <motion.div 
                initial={{ opacity: 0, scale: 0.98 }} 
                animate={{ opacity: 1, scale: 1 }} 
                className="flex-1 flex flex-col gap-4 relative"
            >
                {/* 模拟地图区域 */}
                <div className="flex-1 glass-panel g-border-panel border rounded-lg overflow-hidden relative g-bg-toolbar flex items-center justify-center">
                    {/* 模拟地图底图纹理 */}
                    <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(#1890ff 1px, transparent 1px)', backgroundSize: '20px 20px' }}></div>
                    
                    {/* 模拟车辆位置点 */}
                    <div className="absolute top-1/3 left-1/4 flex flex-col items-center">
                        <div className="bg-green-500 w-4 h-4 rounded-full border-2 border-white shadow-[0_0_10px_rgba(16,185,129,0.8)] animate-pulse"></div>
                        <Tag color="green" className="mt-1 border-none g-bg-toolbar backdrop-blur-sm">川A88921</Tag>
                    </div>
                    
                    <div className="absolute top-1/2 left-1/2 flex flex-col items-center">
                        <div className="bg-orange-500 w-4 h-4 rounded-full border-2 border-white shadow-[0_0_10px_rgba(245,158,11,0.8)]"></div>
                        <Tag color="orange" className="mt-1 border-none g-bg-toolbar backdrop-blur-sm">川A6258W</Tag>
                    </div>

                    <div className="z-10 g-text-secondary font-light text-xl tracking-widest">地图组件加载区 (高德/百度暗色主题)</div>
                </div>

                {/* 轨迹回放控制条 */}
                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: '16px 24px' }}>
                    <div className="flex items-center gap-6">
                        <Space size="large">
                            <Button 
                                type="primary" 
                                shape="circle" 
                                icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />} 
                                size="large"
                                onClick={() => setIsPlaying(!isPlaying)}
                                className="g-btn-primary border-none"
                            />
                            <Button shape="circle" icon={<FastForwardOutlined />} className="bg-white g-text-secondary border-slate-600 hover:g-text-primary" />
                        </Space>
                        <div className="flex-1 flex items-center gap-4">
                            <span className="g-text-secondary text-sm">08:00</span>
                            <Slider 
                                className="flex-1" 
                                value={progress} 
                                onChange={setProgress} 
                                tooltip={{ formatter: (val) => `14:${Math.floor((val || 0) * 0.6).toString().padStart(2, '0')}` }}
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
