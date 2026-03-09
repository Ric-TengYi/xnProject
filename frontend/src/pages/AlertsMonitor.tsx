import React, { useState, useEffect } from 'react';
import { Card, Row, Col, List, Tag, Avatar, Badge, Button, Select, Tabs } from 'antd';
import { Clock, MapPin, Truck, ShieldAlert } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const { TabPane } = Tabs;

const initialAlerts = [
    { id: 'AL-1001', type: '偏航预警', level: '高', vehicle: '川A88921', project: '滨海新区基础建设B标段', time: '刚刚', status: '待处置' },
    { id: 'AL-1002', type: '未打卡入场', level: '中', vehicle: '川A6258W', project: '市中心地铁延长线', time: '5分钟前', status: '待处置' },
    { id: 'AL-1003', type: '超载预警', level: '高', vehicle: '川A1192N', project: '老旧小区改造工程', time: '12分钟前', status: '处置中' },
    { id: 'AL-1004', type: '闯行禁区', level: '高', vehicle: '川A5582K', project: '科创园四期土地平整', time: '28分钟前', status: '已处置' },
    { id: 'AL-1005', type: '超时停留', level: '低', vehicle: '川B44521', project: '东区临时消纳场', time: '1小时前', status: '已关闭' },
];

const getLevelColor = (level: string) => {
    switch (level) {
        case '高': return 'var(--error)'; // red
        case '中': return 'var(--warning)'; // amber
        case '低': return 'var(--primary)'; // blue
        default: return '#94a3b8';
    }
};

const AlertsMonitor: React.FC = () => {
    const [alerts, setAlerts] = useState(initialAlerts);
    const [filter, setFilter] = useState('全部');

    // 模拟警报实时接收
    useEffect(() => {
        const timer = setInterval(() => {
            const newAlert = {
                id: `AL-${Math.floor(1000 + Math.random() * 9000)}`,
                type: ['偏航预警', '车辆违规', '闯行禁区', '未打卡入场'][Math.floor(Math.random() * 4)],
                level: ['高', '中', '低'][Math.floor(Math.random() * 3)],
                vehicle: `川A${Math.floor(1000 + Math.random() * 9000)}${String.fromCharCode(65 + Math.floor(Math.random() * 26))}`,
                project: '滨海新区基础建设B标段',
                time: '刚刚',
                status: '待处置'
            };

            setAlerts(prev => {
                const next = [newAlert, ...prev];
                if (next.length > 8) next.pop(); // 保持列表长度
                return next;
            });
        }, 15000); // 每 15 秒推一条新告警

        return () => clearInterval(timer);
    }, []);

    const filteredAlerts = filter === '全部' ? alerts : alerts.filter(a => a.level === filter);

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0 flex items-center gap-2">
                        <ShieldAlert className="text-red-600 dark:text-red-500" />
                        预警与监控中心
                    </h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">依托违规车辆研判模型，对违法事件实时感知与阻断</p>
                </div>
                <div className="flex gap-4 items-center">
                    <Badge status="processing" text={<span className="text-green-600 dark:text-green-400">研判模型运行中</span>} />
                    <Select
                        defaultValue="全部"
                        style={{ width: 120 }}
                        onChange={(val) => setFilter(val)}
                        className="bg-white dark:bg-slate-800/80 rounded"
                        popupClassName="bg-white dark:bg-slate-800"
                        options={[
                            { value: '全部', label: '全部告警' },
                            { value: '高', label: '高风险 (L3)' },
                            { value: '中', label: '中风险 (L2)' },
                        ]}
                    />
                </div>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={16}>
                    <Card
                        className="glass-panel min-h-[500px] border-slate-200 dark:border-slate-700/50"
                        bodyStyle={{ padding: 0 }}
                    >
                        <Tabs defaultActiveKey="1" className="custom-tabs px-6 pt-4">
                            <TabPane tab={<span className="flex items-center gap-2"><Truck size={16} /> 车辆预警</span>} key="1">
                                <List
                                    dataSource={filteredAlerts}
                                    renderItem={(item) => (
                                        <AnimatePresence>
                                            <motion.div
                                                initial={{ opacity: 0, x: -50, height: 0 }}
                                                animate={{ opacity: 1, x: 0, height: 'auto' }}
                                                exit={{ opacity: 0, height: 0 }}
                                                transition={{ type: 'spring', stiffness: 200, damping: 20 }}
                                            >
                                                <List.Item className="border-b border-slate-200 dark:border-slate-700/50 py-4 hover:bg-white dark:bg-slate-800/30 px-2 rounded transition-colors group">
                                                    <List.Item.Meta
                                                        avatar={
                                                            <div className="relative">
                                                                <Avatar size="large" icon={<Truck size={20} />} className="bg-white dark:bg-slate-800 border border-slate-600 text-slate-600 dark:text-slate-300" />
                                                                <div className="absolute -top-1 -right-1 w-3 h-3 rounded-full border-2 border-white dark:border-[#0b1120]" style={{ backgroundColor: getLevelColor(item.level) }}></div>
                                                            </div>
                                                        }
                                                        title={
                                                            <div className="flex justify-between items-center w-full">
                                                                <div className="flex items-center gap-3">
                                                                    <span className="text-slate-700 dark:text-slate-200 font-bold text-lg">{item.vehicle}</span>
                                                                    <Tag color={getLevelColor(item.level)} className="border-none font-bold">
                                                                        {item.type}
                                                                    </Tag>
                                                                    <Tag color={item.status === '待处置' ? 'magenta' : item.status === '处置中' ? 'orange' : 'default'} className="border-slate-600 bg-transparent text-slate-600 dark:text-slate-300">
                                                                        {item.status}
                                                                    </Tag>
                                                                </div>
                                                                <span className="text-slate-600 dark:text-slate-400 text-sm flex items-center gap-1">
                                                                    <Clock size={14} /> {item.time}
                                                                </span>
                                                            </div>
                                                        }
                                                        description={
                                                            <div className="mt-2 text-slate-600 dark:text-slate-400 flex items-center justify-between">
                                                                <span className="flex items-center gap-1"><MapPin size={14} /> 所属项目: <span className="text-slate-600 dark:text-slate-300">{item.project}</span></span>
                                                                {item.status === '待处置' && (
                                                                    <Button type="primary" size="small" danger className="opacity-0 group-hover:opacity-100 transition-opacity">
                                                                        立即研判处置
                                                                    </Button>
                                                                )}
                                                            </div>
                                                        }
                                                    />
                                                </List.Item>
                                            </motion.div>
                                        </AnimatePresence>
                                    )}
                                />
                            </TabPane>
                            <TabPane tab="场地预警" key="2">
                                <div className="p-4 text-slate-600 dark:text-slate-400 text-center">暂无场地预警数据</div>
                            </TabPane>
                            <TabPane tab="项目预警" key="3">
                                <div className="p-4 text-slate-600 dark:text-slate-400 text-center">暂无项目预警数据</div>
                            </TabPane>
                        </Tabs>
                    </Card>
                </Col>

                <Col span={8}>
                    <div className="space-y-6">
                        <Card
                            title={<span className="text-slate-900 dark:text-white">高风险车队 TOP 3</span>}
                            className="glass-panel border-slate-200 dark:border-slate-700/50"
                            headStyle={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}
                        >
                            {[
                                { name: '宏基渣土运输公司', count: 42, up: true },
                                { name: '顺达土方工程队', count: 35, up: true },
                                { name: '捷安运输', count: 18, up: false },
                            ].map((team, idx) => (
                                <div key={idx} className="flex justify-between items-center mb-4 last:mb-0">
                                    <div className="flex items-center gap-3">
                                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${idx === 0 ? 'bg-red-500/20 text-red-600 dark:text-red-500' : idx === 1 ? 'bg-orange-500/20 text-orange-600 dark:text-orange-500' : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300'}`}>
                                            {idx + 1}
                                        </div>
                                        <span className="text-slate-600 dark:text-slate-300">{team.name}</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <span className="text-slate-600 dark:text-slate-400 text-sm">{team.count} 起</span>
                                        <span className={team.up ? 'text-red-600 dark:text-red-500' : 'text-green-600 dark:text-green-500'}>{team.up ? '↑' : '↓'}</span>
                                    </div>
                                </div>
                            ))}
                        </Card>

                        <Card
                            title={<span className="text-slate-900 dark:text-white">动态电子围栏状态</span>}
                            className="glass-panel border-slate-200 dark:border-slate-700/50"
                            headStyle={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}
                        >
                            <div className="flex flex-wrap gap-2">
                                <Tag color="var(--success)" className="border-none bg-opacity-20 flex items-center gap-1 p-1 pr-2"><div className="w-2 h-2 rounded-full bg-green-500"></div>东区临时消纳场(入场)</Tag>
                                <Tag color="var(--success)" className="border-none bg-opacity-20 flex items-center gap-1 p-1 pr-2"><div className="w-2 h-2 rounded-full bg-green-500"></div>高新产研园(禁行)</Tag>
                                <Tag color="var(--error)" className="border-none bg-opacity-20 flex items-center gap-1 p-1 pr-2 shadow-[0_0_10px_rgba(239,68,68,0.3)]"><div className="w-2 h-2 rounded-full bg-red-500"></div>环南路(停留超时监控)</Tag>
                            </div>
                            <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700/50 flex justify-between">
                                <span className="text-slate-600 dark:text-slate-400 text-sm">当前布控区域：12 个</span>
                                <a className="text-blue-600 dark:text-blue-500 text-sm hover:text-blue-600 dark:text-blue-400">去配置</a>
                            </div>
                        </Card>
                    </div>
                </Col>
            </Row>
        </div>
    );
};

export default AlertsMonitor;
