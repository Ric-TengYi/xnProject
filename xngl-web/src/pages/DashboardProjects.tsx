import React, { useEffect, useMemo, useState } from 'react';
import { Card, Empty, Row, Col, Statistic, Progress, Spin, message, Select, DatePicker, Space } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { fetchProjectReportList, fetchProjectReportSummary, type ProjectReportItem, type ProjectReportSummary } from '../utils/reportApi';

type PeriodType = 'DAY' | 'MONTH' | 'YEAR';

const { Option } = Select;

const emptySummary: ProjectReportSummary = {
    periodType: 'MONTH',
    reportPeriod: '',
    projectCount: 0,
    activeProjectCount: 0,
    totalTrips: 0,
    periodVolume: 0,
    periodAmount: 0,
    projectTotal: 0,
    accumulatedVolume: 0,
    progressPercent: 0,

const DashboardProjects: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [periodType, setPeriodType] = useState<PeriodType>('MONTH');
    const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
    const [summary, setSummary] = useState<ProjectReportSummary>(emptySummary);
    const [projectData, setProjectData] = useState<ProjectReportItem[]>([]);

    const dateValue = useMemo(() => {
        if (periodType === 'YEAR') return selectedDate.startOf('year').format('YYYY-MM-DD');
        if (periodType === 'MONTH') return selectedDate.startOf('month').format('YYYY-MM-DD');
        return selectedDate.format('YYYY-MM-DD');
    }, [periodType, selectedDate]);

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            try {
                const [summaryRes, listRes] = await Promise.all([
                    fetchProjectReportSummary({ periodType, date: dateValue }),
                    fetchProjectReportList({ periodType, date: dateValue, pageNo: 1, pageSize: 20 }),
                ]);
                setSummary(summaryRes);
                setProjectData(listRes.records || []);
            } catch (error) {
                console.error(error);
                message.error('获取项目看板失败');
                setSummary(emptySummary);
                setProjectData([]);
            } finally {
                setLoading(false);
            }
        };

        void loadData();
    }, [periodType, dateValue]);

    const renderDatePicker = () => {
        if (periodType === 'YEAR') {
            return <DatePicker picker="year" value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
        }
        if (periodType === 'MONTH') {
            return <DatePicker picker="month" value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
        }
        return <DatePicker value={selectedDate} onChange={(value) => setSelectedDate(value || dayjs())} className="bg-white g-border-panel border" />;
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">项目数据看板</h1>
                    <p className="g-text-secondary mt-1">支持日 / 月 / 年工程量进度与消纳排名</p>
                </div>
                <Space>
                    <Select value={periodType} className="w-32" onChange={(value) => setPeriodType(value)}>
                        <Option value="DAY">日报</Option>
                        <Option value="MONTH">月报</Option>
                        <Option value="YEAR">年报</Option>
                    </Select>
                    {renderDatePicker()}
                </Space>
            </div>

            <Row gutter={[24, 24]}>
                <Col xs={24} md={8}>
                    <Card className="glass-panel g-border-panel border" loading={loading}>
                        <Statistic title="统计项目" value={summary.projectCount} suffix="个" />
                    </Card>
                </Col>
                <Col xs={24} md={8}>
                    <Card className="glass-panel g-border-panel border" loading={loading}>
                        <Statistic title="本期消纳量" value={summary.periodVolume} suffix="方" valueStyle={{ color: 'var(--primary)' }} />
                    </Card>
                </Col>
                <Col xs={24} md={8}>
                    <Card className="glass-panel g-border-panel border" loading={loading}>
                        <Statistic title="累计完成率" value={summary.progressPercent} suffix="%" valueStyle={{ color: summary.progressPercent >= 80 ? 'var(--success)' : 'var(--text-primary)' }} />
                    </Card>
                </Col>
            </Row>

            <Spin spinning={loading}>
                {projectData.length === 0 ? (
                    <Card className="glass-panel">
                        <Empty description="暂无项目看板数据" />
                    </Card>
                ) : (
                    <Row gutter={[24, 24]}>
                        {projectData.map((project, index) => {
                            const total = Number(project.projectTotal || 0);
                            const used = Number(project.accumulatedVolume || 0);
                            const percent = Number(project.progressPercent || 0);
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
                                                            {index + 1}
                                                        </div>
                                                        <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={project.projectName || '-'}>{project.projectName || '-'}</h3>
                                                    </div>
                                                    <div className="text-xs g-text-secondary mt-1 truncate pl-8">{project.orgName || '-'}</div>
                                                </div>
                                                <Statistic
                                                    title={<span className="g-text-secondary text-xs">{periodType === 'DAY' ? '当日出土' : periodType === 'YEAR' ? '年度消纳' : '本期消纳'}</span>}
                                                    value={Number(project.periodVolume || 0)}
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

export default DashboardProjects;
