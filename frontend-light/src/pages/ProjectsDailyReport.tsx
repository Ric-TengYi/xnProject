import React from 'react';
import { Card, Table, Button, DatePicker, Space, Progress } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

const reportData = [
    { id: '1', project: '市中心地铁延长线三期工程', date: '2024-03-05', vehicles: 45, trips: 180, todayVolume: 3600, totalVolume: 1250000, projectTotal: 3800000 },
    { id: '2', project: '滨海新区基础建设B标段', date: '2024-03-05', vehicles: 32, trips: 125, todayVolume: 2500, totalVolume: 850000, projectTotal: 1500000 },
    { id: '3', project: '老旧小区改造工程综合包', date: '2024-03-05', vehicles: 12, trips: 48, todayVolume: 960, totalVolume: 45000, projectTotal: 200000 },
];

const ProjectsDailyReport: React.FC = () => {
    const columns = [
        { title: '项目名称', dataIndex: 'project', key: 'project', render: (text: string) => <span className="g-text-primary-link font-medium">{text}</span> },
        { title: '日期', dataIndex: 'date', key: 'date' },
        { title: '投入车辆数', dataIndex: 'vehicles', key: 'vehicles' },
        { title: '消纳趟次', dataIndex: 'trips', key: 'trips' },
        { title: '当日消纳量 (方)', dataIndex: 'todayVolume', key: 'todayVolume', render: (val: number) => <span className="g-text-success font-bold">{val.toLocaleString()}</span> },
        { title: '累计消纳量 (方)', dataIndex: 'totalVolume', key: 'totalVolume', render: (val: number) => val.toLocaleString() },
        { title: '工程总量 (方)', dataIndex: 'projectTotal', key: 'projectTotal', render: (val: number) => val.toLocaleString() },
        { 
            title: '完成进度', 
            key: 'progress',
            render: (_: any, record: any) => {
                const percent = Math.round((record.totalVolume / record.projectTotal) * 100);
                return (
                    <Progress 
                        percent={percent} 
                        size="small" 
                        strokeColor={percent > 80 ? 'var(--success)' : percent > 50 ? 'var(--warning)' : 'var(--error)'} 
                        trailColor="rgba(0,0,0,0.06)"
                    />
                );
            }
        },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold g-text-primary m-0">项目日报</h1>
            </div>

            <Card className="glass-panel g-border-panel border">
                <div className="flex justify-between mb-4">
                    <Space>
                        <DatePicker defaultValue={dayjs()} className="bg-white g-border-panel border" />
                        <Button type="primary">生成报表</Button>
                    </Space>
                    <Button icon={<DownloadOutlined />} className="bg-white g-border-panel border g-text-primary hover:g-text-primary-link hover:border-blue-400">导出 Excel</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={reportData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={false}
                />
            </Card>
        </div>
    );
};

export default ProjectsDailyReport;
