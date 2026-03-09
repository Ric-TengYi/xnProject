import React from 'react';
import { Card, Row, Col, Statistic, Table, Tag } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
} from 'recharts';

const data = [
  { name: '01', 消纳量: 4000, 违规台次: 24 },
  { name: '02', 消纳量: 3000, 违规台次: 13 },
  { name: '03', 消纳量: 2000, 违规台次: 58 },
  { name: '04', 消纳量: 2780, 违规台次: 39 },
  { name: '05', 消纳量: 1890, 违规台次: 48 },
  { name: '06', 消纳量: 2390, 违规台次: 38 },
  { name: '07', 消纳量: 3490, 违规台次: 43 },
];

const columns = [
  {
    title: '项目名称',
    dataIndex: 'name',
    key: 'name',
    render: (text: string) => (
      <a className="text-[var(--primary)] hover:opacity-80">{text}</a>
    ),
  },
  {
    title: '关联场地',
    dataIndex: 'site',
    key: 'site',
  },
  {
    title: '进度',
    dataIndex: 'progress',
    key: 'progress',
    render: (progress: number) => (
      <Tag color={progress > 80 ? 'green' : progress > 50 ? 'orange' : 'red'}>
        {progress}%
      </Tag>
    ),
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status: string) => (
      <span
        className={
          status === '正常'
            ? 'text-[var(--success)]'
            : 'text-[var(--error)]'
        }
      >
        <span
          className={`inline-block w-2 h-2 rounded-full mr-2 align-middle ${
            status === '正常' ? 'bg-[var(--success)]' : 'bg-[var(--error)]'
          }`}
        />
        {status}
      </span>
    ),
  },
];

const tableData = [
  { key: '1', name: '滨海新区基础建设B标段', site: '东区临时消纳场', progress: 85, status: '正常' },
  { key: '2', name: '市中心地铁延长线三期工程', site: '南郊复合型消纳中心', progress: 42, status: '预警' },
  { key: '3', name: '科创园四期土地平整项目', site: '北区填埋场', progress: 92, status: '正常' },
  { key: '4', name: '老旧小区改造工程综合包', site: '西郊临时周转站', progress: 15, status: '正常' },
];

const Dashboard: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold m-0" style={{ color: 'var(--text-primary)' }}>
          综合数据大屏
        </h1>
        <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>
          更新时间: 刚刚
        </div>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={6}>
          <Card
            className="glass-panel overflow-hidden relative border-l-4 hover:shadow-md transition-all"
            style={{ borderLeftColor: 'var(--primary)' }}
          >
            <div
              className="absolute -right-4 -top-4 w-24 h-24 rounded-full blur-xl opacity-20"
              style={{ background: 'var(--primary)' }}
            />
            <Statistic
              title={
                <span style={{ color: 'var(--text-secondary)' }}>
                  本月消纳总量 (m³)
                </span>
              }
              value={112893}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={
                <ArrowUpOutlined
                  className="text-sm"
                  style={{ color: 'var(--success)' }}
                />
              }
              suffix="万方"
            />
            <div className="mt-2 text-xs" style={{ color: 'var(--text-secondary)' }}>
              环比上月 +8.2%
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card
            className="glass-panel overflow-hidden relative border-l-4 hover:shadow-md transition-all"
            style={{ borderLeftColor: 'var(--success)' }}
          >
            <div
              className="absolute -right-4 -top-4 w-24 h-24 rounded-full blur-xl opacity-20"
              style={{ background: 'var(--success)' }}
            />
            <Statistic
              title={
                <span style={{ color: 'var(--text-secondary)' }}>
                  活跃消纳场地
                </span>
              }
              value={42}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              suffix="/ 50"
            />
            <div className="mt-2 text-xs" style={{ color: 'var(--text-secondary)' }}>
              平均容量使用率 68%
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card
            className="glass-panel overflow-hidden relative border-l-4 hover:shadow-md transition-all"
            style={{ borderLeftColor: 'var(--warning)' }}
          >
            <div
              className="absolute -right-4 -top-4 w-24 h-24 rounded-full blur-xl opacity-20"
              style={{ background: 'var(--warning)' }}
            />
            <Statistic
              title={
                <span style={{ color: 'var(--text-secondary)' }}>
                  今日运营车辆 (台次)
                </span>
              }
              value={3248}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={
                <ArrowDownOutlined
                  className="text-sm"
                  style={{ color: 'var(--error)' }}
                />
              }
            />
            <div className="mt-2 text-xs" style={{ color: 'var(--text-secondary)' }}>
              高峰时段监控中
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card
            className="glass-panel overflow-hidden relative border-l-4 hover:shadow-md transition-all"
            style={{ borderLeftColor: 'var(--error)' }}
          >
            <div
              className="absolute -right-4 -top-4 w-24 h-24 rounded-full blur-xl opacity-20"
              style={{ background: 'var(--error)' }}
            />
            <Statistic
              title={
                <span style={{ color: 'var(--text-secondary)' }}>
                  系统拦截高风险 (次)
                </span>
              }
              value={15}
              valueStyle={{ color: 'var(--error)', fontWeight: 'bold' }}
            />
            <div className="mt-2 text-xs" style={{ color: 'var(--text-secondary)' }}>
              闯禁/偏航/超时等异常
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col span={14}>
          <Card
            title={
              <span style={{ color: 'var(--text-primary)' }}>
                近七日消纳趋势
              </span>
            }
            className="glass-panel h-[400px]"
            headStyle={{ borderBottom: '1px solid var(--border-color)' }}
          >
            <div className="h-[300px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorPvLight" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.8} />
                      <stop offset="95%" stopColor="var(--primary)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(0,0,0,0.08)"
                    vertical={false}
                  />
                  <XAxis dataKey="name" stroke="var(--text-secondary)" />
                  <YAxis stroke="var(--text-secondary)" />
                  <RechartsTooltip
                    contentStyle={{
                      backgroundColor: '#fff',
                      border: '1px solid var(--border-color)',
                      borderRadius: '8px',
                    }}
                    itemStyle={{ color: 'var(--text-primary)' }}
                  />
                  <Area
                    type="monotone"
                    dataKey="消纳量"
                    stroke="var(--primary)"
                    fillOpacity={1}
                    fill="url(#colorPvLight)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col span={10}>
          <Card
            title={
              <span style={{ color: 'var(--text-primary)' }}>
                重点项目告警与进度
              </span>
            }
            className="glass-panel h-[400px]"
            headStyle={{ borderBottom: '1px solid var(--border-color)' }}
            bodyStyle={{ padding: 0 }}
          >
            <Table
              columns={columns}
              dataSource={tableData}
              pagination={false}
              className="bg-transparent"
              rowClassName="hover:bg-[#fafafa]"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
