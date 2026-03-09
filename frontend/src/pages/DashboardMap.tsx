import React, { useState } from 'react';
import { Card, Select, DatePicker, Button, Space } from 'antd';
import { EnvironmentOutlined, CarOutlined, ProjectOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;

const DashboardMap: React.FC = () => {
    const [filterType, setFilterType] = useState('all');

    return (
        <div className="h-[calc(100vh-120px)] flex flex-col space-y-4">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">全局地图监控</h1>
                <Space>
                    <Select 
                        value={filterType} 
                        onChange={setFilterType} 
                        style={{ width: 150 }}
                        className="bg-white dark:bg-slate-800"
                    >
                        <Option value="all">全部显示</Option>
                        <Option value="sites">仅看消纳场</Option>
                        <Option value="projects">仅看项目</Option>
                        <Option value="vehicles">仅看车辆</Option>
                    </Select>
                    <RangePicker 
                        showTime 
                        defaultValue={[dayjs().startOf('day'), dayjs()]}
                        className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700"
                    />
                    <Button type="primary">查询轨迹</Button>
                </Space>
            </div>

            <div className="flex-1 flex gap-4">
                {/* 左侧数据面板 */}
                <div className="w-80 flex flex-col gap-4">
                    <Card className="glass-panel flex-1 overflow-auto" bodyStyle={{ padding: '16px' }}>
                        <h3 className="text-slate-900 dark:text-white font-bold mb-4 flex items-center gap-2">
                            <EnvironmentOutlined className="text-blue-600 dark:text-blue-400" /> 场地分布 (5)
                        </h3>
                        <div className="space-y-3">
                            {['东区临时消纳场', '南郊复合型消纳中心', '北区填埋场'].map((site, i) => (
                                <div key={i} className="p-2 bg-white dark:bg-slate-800/50 rounded border border-slate-200 dark:border-slate-700/50 cursor-pointer hover:border-blue-500/50">
                                    <div className="text-sm text-slate-700 dark:text-slate-200">{site}</div>
                                    <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">当前排队: {Math.floor(Math.random() * 20)}辆</div>
                                </div>
                            ))}
                        </div>

                        <h3 className="text-slate-900 dark:text-white font-bold mt-6 mb-4 flex items-center gap-2">
                            <ProjectOutlined className="text-green-600 dark:text-green-400" /> 活跃项目 (12)
                        </h3>
                        <div className="space-y-3">
                            {['市中心地铁延长线三期', '滨海新区基础建设', '老旧小区改造'].map((proj, i) => (
                                <div key={i} className="p-2 bg-white dark:bg-slate-800/50 rounded border border-slate-200 dark:border-slate-700/50 cursor-pointer hover:border-green-500/50">
                                    <div className="text-sm text-slate-700 dark:text-slate-200">{proj}</div>
                                    <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">今日出土: {Math.floor(Math.random() * 5000)}方</div>
                                </div>
                            ))}
                        </div>
                    </Card>
                </div>

                {/* 右侧地图区域 */}
                <Card className="glass-panel flex-1 relative overflow-hidden" bodyStyle={{ padding: 0, height: '100%' }}>
                    {/* 地图占位 */}
                    <div className="absolute inset-0 bg-slate-50 dark:bg-slate-900 flex items-center justify-center">
                        <div className="text-center">
                            <EnvironmentOutlined className="text-6xl text-slate-700 mb-4" />
                            <h2 className="text-xl text-slate-600 dark:text-slate-400">地图组件加载区</h2>
                            <p className="text-slate-600">在此接入百度/高德/天地图 SDK</p>
                        </div>
                    </div>

                    {/* 悬浮统计面板 */}
                    <div className="absolute top-4 right-4 bg-white dark:bg-slate-800/80 backdrop-blur-md p-4 rounded-lg border border-slate-200 dark:border-slate-700/50 shadow-xl">
                        <div className="grid grid-cols-2 gap-4 text-center">
                            <div>
                                <div className="text-xs text-slate-600 dark:text-slate-400 mb-1">在线车辆</div>
                                <div className="text-xl font-bold text-blue-600 dark:text-blue-400">1,284</div>
                            </div>
                            <div>
                                <div className="text-xs text-slate-600 dark:text-slate-400 mb-1">今日预警</div>
                                <div className="text-xl font-bold text-red-600 dark:text-red-400">32</div>
                            </div>
                        </div>
                    </div>

                    {/* 底部轨迹回放控制条 (可选展示) */}
                    <div className="absolute bottom-4 left-4 right-4 bg-white dark:bg-slate-800/80 backdrop-blur-md p-3 rounded-lg border border-slate-200 dark:border-slate-700/50 flex items-center gap-4">
                        <Button type="primary" shape="circle" icon={<CarOutlined />} />
                        <div className="flex-1 h-2 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                            <div className="h-full bg-blue-500 w-1/3"></div>
                        </div>
                        <div className="text-xs text-slate-600 dark:text-slate-400">2024-03-05 14:30:00</div>
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default DashboardMap;
