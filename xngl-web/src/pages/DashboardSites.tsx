import React, { useEffect, useMemo, useState } from 'react';
import { Card, Empty, Row, Col, Statistic, Progress, Tag, Spin, message, Select, DatePicker, Space } from 'antd';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { fetchSiteReportList, fetchSiteReportSummary, type SiteReportItem, type SiteReportSummary } from '../utils/reportApi';

type PeriodType = 'DAY' | 'MONTH' | 'YEAR';

const { Option } = Select;

const emptySummary: SiteReportSummary = {
  periodType: 'MONTH',
  reportPeriod: '',
  siteCount: 0,
  activeSiteCount: 0,
  totalTrips: 0,
  periodVolume: 0,
  periodAmount: 0,
  totalCapacity: 0,
  accumulatedVolume: 0,
  utilizationRate: 0,
};

const DashboardSites: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [periodType, setPeriodType] = useState<PeriodType>('MONTH');
    const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
    const [summary, setSummary] = useState<SiteReportSummary>(emptySummary);
    const [siteData, setSiteData] = useState<SiteReportItem[]>([]);

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
                    fetchSiteReportSummary({ periodType, date: dateValue }),
                    fetchSiteReportList({ periodType, date: dateValue, pageNo: 1, pageSize: 20 }),
                ]);
                setSummary(summaryRes);
                setSiteData(listRes.records || []);
            } catch (error) {
                console.error(error);
                message.error('获取场地看板失败');
                setSummary(emptySummary);
                setSiteData([]);
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
                    <h1 className="text-2xl font-bold g-text-primary m-0">消纳场数据看板</h1>
                    <p className="g-text-secondary mt-1">支持日 / 月 / 年消纳统计与容量使用排名</p>
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
                        <Statistic title="统计场地" value={summary.siteCount} suffix="个" />
                    </Card>
                </Col>
                <Col xs={24} md={8}>
                    <Card className="glass-panel g-border-panel border" loading={loading}>
                        <Statistic title="本期消纳量" value={summary.periodVolume} suffix="方" valueStyle={{ color: 'var(--primary)' }} />
                    </Card>
                </Col>
                <Col xs={24} md={8}>
                    <Card className="glass-panel g-border-panel border" loading={loading}>
                        <Statistic title="容量使用率" value={summary.utilizationRate} suffix="%" valueStyle={{ color: summary.utilizationRate >= 80 ? 'var(--warning)' : 'var(--text-primary)' }} />
                    </Card>
                </Col>
            </Row>

            <Spin spinning={loading}>
                {siteData.length === 0 ? (
                    <Card className="glass-panel">
                        <Empty description="暂无场地看板数据" />
                    </Card>
                ) : (
                    <Row gutter={[24, 24]}>
                        {siteData.map((site, index) => {
                            const capacity = Number(site.capacity || 0);
                            const used = Number(site.accumulatedVolume || 0);
                            const percent = Number(site.utilizationRate || 0);
                            const isWarning = percent >= 80;
                            const remaining = Number(site.remainingCapacity || Math.max(capacity - used, 0));

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
                                                            {index + 1}
                                                        </div>
                                                        <h3 className="text-lg font-bold g-text-primary m-0 truncate" title={site.siteName || '-'}>{site.siteName || '-'}</h3>
                                                    </div>
                                                    <Tag color="blue" className="border-none bg-opacity-20 mt-1">{site.siteType || '-'}</Tag>
                                                </div>
                                                <Statistic
                                                    title={<span className="g-text-secondary text-xs">{periodType === 'DAY' ? '当日消纳' : periodType === 'YEAR' ? '年度消纳' : '本期消纳'}</span>}
                                                    value={Number(site.periodVolume || 0)}
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
