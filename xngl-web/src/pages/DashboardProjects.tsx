import React from 'react';
import { Card, Row, Col, Statistic, Progress } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

const projectData = [
    { id: 'PRJ-24012', name: '市中心地铁延长线三期工程', builder: '中铁十四局', total: 3800000, used: 3750000, today: 25000, rank: 1, status: '预警' },
    { id: 'PRJ-24001', name: '滨海新区基础建设B标段', builder: '中建八局', total: 1500000, used: 1250000, today: 18000, rank: 2, status: '在建' },
    { id: 'PRJ-23190', name: '环城高速南段拓宽工程', builder: '省交投集团', total: 5200000, used: 5198000, today: 5000, rank: 3, status: '完工' },
    { id: 'PRJ-24005', name: '老旧小区改造工程综合包', builder: '市建工集团', total: 200000, used: 45000, today: 1500, rank: 4, status: '在建' },
    { id: 'PRJ-24028', name: '科创园四期土地平整项目', builder: '高新产投', total: 850000, used: 0, today: 0, rank: 5, status: '立项' },
];

const DashboardProjects: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">项目数据看板</h1>
                    <p className="g-text-secondary mt-1">实时展示各项目的工程量进度与当日消纳排名</p>
                </div>
                <div className="g-text-secondary text-sm">自动刷新: 开启</div>
            </div>

            <Row gutter={[24, 24]}>
                {projectData.map((project, index) => {
                    const percent = project.total === 0 ? 0 : Math.round((project.used / project.total) * 100);
                    let progressStatus: 'normal' | 'exception' | 'success' | 'active' = 'normal';
                    if (percent >= 100) progressStatus = 'success';
                    else if (project.status === '预警') progressStatus = 'exception';
                    else if (project.status === '在建') progressStatus = 'active';

                    return (
                        <Col xs={24} sm={12} lg={8} key={project.id}>
                            <motion.div
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: index * 0.1 }}
                            >
                                <Card 
                                    className="glass-panel hover:-translate-y-1 transition-transform cursor-pointer g-border-panel border hover:border-blue-500/50"
                                    onClick={() => navigate(`/projects/${project.id}`)}
                                >
                                    <div className="flex justify-between items-start mb-4">
                                        <div className="w-2/3">
                                            <div className="flex items-center gap-2 mb-1">
                                                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${index < 3 ? 'bg-blue-500/20 g-text-primary-link' : 'bg-black/10 g-text-secondary'}`}>
                                                    {project.rank}
                                                </div>
                                                <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={project.name}>{project.name}</h3>
                                            </div>
                                            <div className="text-xs g-text-secondary mt-1 truncate pl-8">{project.builder}</div>
                                        </div>
                                        <Statistic 
                                            title={<span className="g-text-secondary text-xs">今日出土</span>}
                                            value={project.today} 
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
                                            <span>已消纳: {(project.used / 10000).toFixed(2)}万</span>
                                            <span>总工程量: {(project.total / 10000).toFixed(2)}万</span>
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

export default DashboardProjects;
