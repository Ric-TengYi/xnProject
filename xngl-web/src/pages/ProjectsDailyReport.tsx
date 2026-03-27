import React, { useEffect, useState } from 'react';
import { Button, Card, DatePicker, Empty, Progress, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownloadOutlined } from '@ant-design/icons';
import dayjs, { Dayjs } from 'dayjs';
import {
  exportProjectDailyReport,
  fetchProjectDailyReport,
  type ProjectDailyReportItem,
} from '../utils/reportApi';

const ProjectsDailyReport: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [records, setRecords] = useState<ProjectDailyReportItem[]>([]);

  const loadReport = async (dateValue: Dayjs) => {
    setLoading(true);
    try {
      const page = await fetchProjectDailyReport({
        date: dateValue.format('YYYY-MM-DD'),
        pageNo: 1,
        pageSize: 100,
      });
      setRecords(page.records || []);
    } catch (error) {
      console.error(error);
      message.error('获取项目日报失败');
      setRecords([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadReport(selectedDate);
  }, [selectedDate]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const result = await exportProjectDailyReport({
        date: selectedDate.format('YYYY-MM-DD'),
      });
      message.success('已生成导出任务 #' + result.taskId);
    } catch (error) {
      console.error(error);
      message.error('导出任务创建失败');
    } finally {
      setExporting(false);
    }
  };

  const columns: ColumnsType<ProjectDailyReportItem> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (text: string, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{text}</span>
          <span className="text-xs g-text-secondary">
            {(record.projectCode || '-') + ' / ' + (record.orgName || '-')}
          </span>
        </div>
      ),
    },
    { title: '日期', dataIndex: 'reportDate', key: 'reportDate' },
    { title: '投入车辆数', dataIndex: 'vehicles', key: 'vehicles' },
    { title: '消纳趟次', dataIndex: 'trips', key: 'trips' },
    {
      title: '当日消纳量 (方)',
      dataIndex: 'todayVolume',
      key: 'todayVolume',
      render: (val: number) => (
        <span className="g-text-success font-bold">{Number(val || 0).toLocaleString()}</span>
      ),
    },
    {
      title: '累计消纳量 (方)',
      dataIndex: 'totalVolume',
      key: 'totalVolume',
      render: (val: number) => Number(val || 0).toLocaleString(),
    },
    {
      title: '工程总量 (方)',
      dataIndex: 'projectTotal',
      key: 'projectTotal',
      render: (val: number) => Number(val || 0).toLocaleString(),
    },
    {
      title: '完成进度',
      key: 'progress',
      render: (_, record) => (
        <Progress
          percent={record.progressPercent || 0}
          size="small"
          strokeColor={
            (record.progressPercent || 0) > 80
              ? 'var(--success)'
              : (record.progressPercent || 0) > 50
              ? 'var(--warning)'
              : 'var(--error)'
          }
          trailColor="rgba(0,0,0,0.06)"
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold g-text-primary m-0">项目日报</h1>

      <Card className="glass-panel g-border-panel border">
        <div className="flex justify-between mb-4">
          <Space>
            <DatePicker
              value={selectedDate}
              onChange={(value) => setSelectedDate(value || dayjs())}
              className="bg-white g-border-panel border"
            />
            <Button type="primary" onClick={() => void loadReport(selectedDate)}>
              生成报表
            </Button>
          </Space>
          <Button
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={() => void handleExport()}
            className="bg-white g-border-panel border g-text-primary hover:g-text-primary-link hover:border-blue-400"
          >
            导出 Excel
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="projectId"
          loading={loading}
          locale={{ emptyText: <Empty description="当前日期暂无项目日报数据" /> }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
          pagination={false}
        />
      </Card>
    </div>
    </div>
  );
};
export default ProjectsDailyReport;
