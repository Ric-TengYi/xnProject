import React, { useEffect, useState } from 'react';
import { Card, Empty, Row, Col, Statistic, Progress, Tag, Spin, message } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { fetchSiteRanking, type SiteRankingItem } from '../utils/reportApi';

const DashboardSites: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [siteData, setSiteData] = useState<SiteRankingItem[]>([]);

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            try {
                setSiteData(await fetchSiteRanking());
            } catch (error) {
                console.error(error);
                message.error('获取场地看板失败');
                setSiteData([]);
            } finally {
                setLoading(false);
            }
        };

        void loadData();
    }, []);

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">消纳场数据看板</h1>
                    <p className="g-text-secondary mt-1">实时展示各消纳场地的容量使用情况与当日消纳排名</p>
                </div>
                <div className="g-text-secondary text-sm">自动刷新: 开启</div>
            </div>

            <Spin spinning={loading}>
                {siteData.length === 0 ? (
                    <Card className="glass-panel">
                        <Empty description="暂无场地看板数据" />
                    </Card>
                ) : (
                    <Row gutter={[24, 24]}>
                        {siteData.map((site, index) => {
                            const capacity = Number(site.capacity || 0);
                            const used = Number(site.used || 0);
                            const percent = capacity > 0 ? Math.round((used / capacity) * 100) : 0;
                            const isWarning = percent >= 80;
                            const remaining = Math.max(capacity - used, 0);

                            return (
                                <Col xs={24} sm={12} lg={8} key={site.siteId || index}>
                                    <motion.div
                                        initial={{ opacity: 0, y: 20 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ duration: 0.3, delay: index * 0.1 }}
                                    >
                                        <Card
                                            className={`glass-panel hover:-translate-y-1 transition-transform cursor-pointer border-t-4 ${index === 0 ? 'border-t-yellow-400' : index === 1 ? 'border-t-slate-300' : index === 2 ? 'border-t-orange-400' : 'border-t-blue-500'}`}
                                            onClick={() => navigate(`/sites/${site.siteId}`)}
                                        >
                                            <div className="flex justify-between items-start mb-4">
                                                <div>
                                                    <div className="flex items-center gap-2 mb-1">
                                                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${index < 3 ? 'bg-gradient-to-br from-yellow-400 to-orange-500 g-text-primary' : 'bg-slate-700 g-text-secondary'}`}>
                                                            {site.rank || index + 1}
                                                        </div>
                                                        <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={site.siteName || '-'}>{site.siteName || '-'}</h3>
                                                    </div>
                                                    <Tag color="blue" className="border-none bg-opacity-20 mt-1">{site.siteType || '-'}</Tag>
                                                </div>
                                                <Statistic
                                                    title={<span className="g-text-secondary text-xs">今日消纳</span>}
                                                    value={Number(site.today || 0)}
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
                                                    <span>已用: {(used / 10000).toFixed(1)}万方</span>
                                                    <span>剩余: {(remaining / 10000).toFixed(1)}万方</span>
                                                </div>
                                            </div>
                                        </Card>
                                    </motion.div>
                                </Col>
                            );
                        })}
                    </Row>
                )}
            </Spin>
        </div>
    );
};

export default DashboardSites;
