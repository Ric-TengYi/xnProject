import React from 'react';
import { Card, Tabs, Timeline, Tag, Button, Row, Col, Statistic } from 'antd';
import { SafetyCertificateOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { TabPane } = Tabs;

const SecurityLedger: React.FC = () => {
    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">安全台账管理</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">记录人员、车辆、场地的日常安全检查及整改闭环历史</p>
                </div>
                <Button type="primary" icon={<SafetyCertificateOutlined />} className="bg-blue-600 hover:bg-blue-500 border-none">
                    新增检查记录
                </Button>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">本月安全检查 (次)</span>} value={45} valueStyle={{ color: 'var(--text-primary)' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">发现隐患 (项)</span>} value={12} valueStyle={{ color: 'var(--warning)' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-300">已整改闭环 (项)</span>} value={10} valueStyle={{ color: 'var(--success)' }} suffix="/ 12" />
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50" bodyStyle={{ padding: 0 }}>
                <Tabs defaultActiveKey="1" className="custom-tabs px-6 pt-4">
                    <TabPane tab="场地安全检查" key="1">
                        <div className="p-4">
                            <Timeline
                                items={[
                                    {
                                        color: 'red',
                                        dot: <ExclamationCircleOutlined className="text-lg" />,
                                        children: (
                                            <div className="mb-6 bg-white dark:bg-slate-800/30 p-4 rounded-lg border border-red-500/30">
                                                <div className="flex justify-between items-center mb-2">
                                                    <span className="text-slate-900 dark:text-white font-bold text-lg">东区临时消纳场 - 消防设施检查</span>
                                                    <span className="text-slate-600 dark:text-slate-400">2024-03-05 10:00</span>
                                                </div>
                                                <p className="text-slate-600 dark:text-slate-300 mb-2">检查结果：<Tag color="error" className="border-none">不合格</Tag></p>
                                                <p className="text-slate-600 dark:text-slate-400 text-sm mb-3">问题描述：入口处2号灭火器压力不足，需立即更换。</p>
                                                <div className="flex gap-2">
                                                    <Button size="small" type="primary" danger>登记整改</Button>
                                                </div>
                                            </div>
                                        ),
                                    },
                                    {
                                        color: 'green',
                                        dot: <CheckCircleOutlined className="text-lg" />,
                                        children: (
                                            <div className="mb-6 bg-white dark:bg-slate-800/30 p-4 rounded-lg border border-green-500/30">
                                                <div className="flex justify-between items-center mb-2">
                                                    <span className="text-slate-900 dark:text-white font-bold text-lg">南郊复合型消纳中心 - 坝体安全巡检</span>
                                                    <span className="text-slate-600 dark:text-slate-400">2024-03-01 14:30</span>
                                                </div>
                                                <p className="text-slate-600 dark:text-slate-300 mb-2">检查结果：<Tag color="success" className="border-none">合格</Tag></p>
                                                <p className="text-slate-600 dark:text-slate-400 text-sm">巡检说明：坝体位移监测数据正常，无渗漏现象。</p>
                                            </div>
                                        ),
                                    }
                                ]}
                            />
                        </div>
                    </TabPane>
                    <TabPane tab="车辆运营安全" key="2">
                        <div className="p-4 text-slate-600 dark:text-slate-400 text-center py-10">暂无车辆安全检查记录</div>
                    </TabPane>
                    <TabPane tab="人员日常安全" key="3">
                        <div className="p-4 text-slate-600 dark:text-slate-400 text-center py-10">暂无人员安全检查记录</div>
                    </TabPane>
                </Tabs>
            </Card>
        </motion.div>
    );
};

export default SecurityLedger;
