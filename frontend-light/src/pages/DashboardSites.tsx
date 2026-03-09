import React from 'react';
import { Card, Row, Col, Statistic, Progress, Tag } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

const siteData = [
    { id: 1, name: '东区临时消纳场', type: '国有场地', capacity: 500000, used: 350000, today: 12500, rank: 1, status: '正常' },
    { id: 2, name: '南郊复合型消纳中心', type: '集体场地', capacity: 1200000, used: 980000, today: 8900, rank: 2, status: '预警' },
    { id: 4, name: '西郊临时周转站', type: '短驳场地', capacity: 100000, used: 95000, today: 5200, rank: 3, status: '预警' },
    { id: 5, name: '老城改造一期专供场', type: '工程场地', capacity: 250000, used: 248000, today: 1200, rank: 4, status: '满载' },
    { id: 3, name: '北区填埋场', type: '工程场地', capacity: 300000, used: 45000, today: 800, rank: 5, status: '正常' },
];

const DashboardSites: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">消纳场数据看板</h1>
                    <p className="g-text-secondary mt-1">实时展示各消纳场地的容量使用情况与当日消纳排名</p>
                </div>
                <div className="g-text-secondary text-sm">自动刷新: 开启</div>
            </div>

            <Row gutter={[24, 24]}>
                {siteData.map((site, index) => {
                    const percent = Math.round((site.used / site.capacity) * 100);
                    const isWarning = percent >= 80;
                    
                    return (
                        <Col xs={24} sm={12} lg={8} key={site.id}>
                            <motion.div
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: index * 0.1 }}
                            >
                                <Card 
                                    className={`glass-panel hover:-translate-y-1 transition-transform cursor-pointer border-t-4 ${index === 0 ? 'border-t-yellow-400' : index === 1 ? 'border-t-slate-300' : index === 2 ? 'border-t-orange-400' : 'border-t-blue-500'}`}
                                    onClick={() => navigate(`/sites/${site.id}`)}
                                >
                                    <div className="flex justify-between items-start mb-4">
                                        <div>
                                            <div className="flex items-center gap-2 mb-1">
                                                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${index < 3 ? 'bg-gradient-to-br from-yellow-400 to-orange-500 g-text-primary' : 'bg-slate-700 g-text-secondary'}`}>
                                                    {site.rank}
                                                </div>
                                                <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={site.name}>{site.name}</h3>
                                            </div>
                                            <Tag color="blue" className="border-none bg-opacity-20 mt-1">{site.type}</Tag>
                                        </div>
                                        <Statistic 
                                            value={site.today} 
                                            suffix="方" 
                                            valueStyle={{ color: 'var(--success)', fontSize: '1.25rem', fontWeight: 'bold' }} 
                                        />
                                    </div>

                                    <div className="mt-6">
                                        <div className="flex justify-between text-xs mb-1">
                                            <span className="g-text-secondary">容量使用率</span>
                                            <span className={isWarning ? 'g-text-error' : 'g-text-primary-link'}>{percent}%</span>
                                        </div>
                                        <Progress
                                            percent={percent}
                                            showInfo={false}
                                            strokeColor={percent > 90 ? 'var(--error)' : percent >= 80 ? 'var(--warning)' : 'var(--primary)'}
                                            trailColor="rgba(0,0,0,0.06)"
                                        />
                                        <div className="flex justify-between text-xs mt-2 g-text-secondary">
                                            <span>已用: {(site.used / 10000).toFixed(1)}万方</span>
                                            <span>剩余: {((site.capacity - site.used) / 10000).toFixed(1)}万方</span>
                                        </div>
                                    </div>
                                </Card>
                            </motion.div>
                        </Col>
                    );
                })}
            </Row>
        </div>
    );
};

export default DashboardSites;
