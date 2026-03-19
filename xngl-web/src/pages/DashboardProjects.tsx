import React, { useEffect, useState } from 'react';
import { Card, Empty, Row, Col, Statistic, Progress, Spin, message } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { fetchProjectRanking, type ProjectRankingItem } from '../utils/reportApi';

const DashboardProjects: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [projectData, setProjectData] = useState<ProjectRankingItem[]>([]);

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            try {
                setProjectData(await fetchProjectRanking());
            } catch (error) {
                console.error(error);
                message.error('获取项目看板失败');
                setProjectData([]);
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
                    <h1 className="text-2xl font-bold g-text-primary m-0">项目数据看板</h1>
                    <p className="g-text-secondary mt-1">实时展示各项目的工程量进度与当日消纳排名</p>
                </div>
                <div className="g-text-secondary text-sm">自动刷新: 开启</div>
            </div>

            <Spin spinning={loading}>
                {projectData.length === 0 ? (
                    <Card className="glass-panel">
                        <Empty description="暂无项目看板数据" />
                    </Card>
                ) : (
                    <Row gutter={[24, 24]}>
                        {projectData.map((project, index) => {
                            const total = Number(project.total || 0);
                            const used = Number(project.used || 0);
                            const percent = total === 0 ? 0 : Math.round((used / total) * 100);
                            let progressStatus: 'normal' | 'exception' | 'success' | 'active' = 'normal';
                            if (percent >= 100) progressStatus = 'success';
                            else if (project.status === '预警') progressStatus = 'exception';
                            else if (project.status === '在建') progressStatus = 'active';

                            return (
                                <Col xs={24} sm={12} lg={8} key={project.projectId || index}>
                                    <motion.div
                                        initial={{ opacity: 0, y: 20 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        transition={{ duration: 0.3, delay: index * 0.1 }}
                                    >
                                        <Card
                                            className="glass-panel hover:-translate-y-1 transition-transform cursor-pointer g-border-panel border hover:border-blue-500/50"
                                            onClick={() => navigate(`/projects/${project.projectId}`)}
                                        >
                                            <div className="flex justify-between items-start mb-4">
                                                <div className="w-2/3">
                                                    <div className="flex items-center gap-2 mb-1">
                                                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${index < 3 ? 'bg-blue-500/20 g-text-primary-link' : 'bg-black/10 g-text-secondary'}`}>
                                                            {project.rank || index + 1}
                                                        </div>
                                                        <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={project.projectName || '-'}>{project.projectName || '-'}</h3>
                                                    </div>
                                                    <div className="text-xs g-text-secondary mt-1 truncate pl-8">{project.orgName || '-'}</div>
                                                </div>
                                                <Statistic
                                                    title={<span className="g-text-secondary text-xs">今日出土</span>}
                                                    value={Number(project.today || 0)}
                                                    suffix="方"
                                                    valueStyle={{ color: 'var(--primary)', fontSize: '1.25rem', fontWeight: 'bold' }}
                                                />
                                            </div>

                                            <div className="mt-6">
                                                <div className="flex justify-between text-xs mb-1">
                                                    <span className="g-text-secondary">项目总进度</span>
                                                    <span className="g-text-secondary">{percent}%</span>
                                                </div>
                                                <Progress
                                                    percent={percent}
                                                    showInfo={false}
                                                    status={progressStatus}
                                                    trailColor="rgba(0,0,0,0.06)"
                                                />
                                                <div className="flex justify-between text-xs mt-2 g-text-secondary">
                                                    <span>已消纳: {(used / 10000).toFixed(2)}万</span>
                                                    <span>总工程量: {(total / 10000).toFixed(2)}万</span>
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

export default DashboardProjects;
