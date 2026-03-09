import React, { useState } from 'react';
import {
  Table,
  Tag,
  Input,
  Button,
  Card,
  Progress,
  Space,
  Dropdown,
  Drawer,
  Form,
  Select,
  DatePicker,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  PlusOutlined,
  MoreOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

const { RangePicker } = DatePicker;

const projectsData = [
  {
    id: 'PRJ-24001',
    name: '滨海新区基础建设B标段',
    builder: '中建八局',
    transporter: '顺达土方工程队',
    status: '在建',
    startDate: '2024-01-15',
    totalAmount: 1500000,
    usedAmount: 1250000,
  },
  {
    id: 'PRJ-24005',
    name: '老旧小区改造工程综合包',
    builder: '市建工集团',
    transporter: '宏基渣土运输公司',
    status: '在建',
    startDate: '2024-02-10',
    totalAmount: 200000,
    usedAmount: 45000,
  },
  {
    id: 'PRJ-24012',
    name: '市中心地铁延长线三期工程',
    builder: '中铁十四局',
    transporter: '联运物流、新思路运输',
    status: '预警',
    startDate: '2023-11-05',
    totalAmount: 3800000,
    usedAmount: 3750000,
  },
  {
    id: 'PRJ-24028',
    name: '科创园四期土地平整项目',
    builder: '高新产投',
    transporter: '顺达土方工程队',
    status: '立项',
    startDate: '2024-04-01',
    totalAmount: 850000,
    usedAmount: 0,
  },
  {
    id: 'PRJ-23190',
    name: '环城高速南段拓宽工程',
    builder: '省交投集团',
    transporter: '捷安运输',
    status: '完工',
    startDate: '2023-05-12',
    totalAmount: 5200000,
    usedAmount: 5198000,
  },
];

const ProjectsManagement: React.FC = () => {
  const [searchText, setSearchText] = useState('');
  const [filterVisible, setFilterVisible] = useState(false);
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const actionItems = (id: string): MenuProps['items'] => [
    { key: 'detail', label: '项目详情', onClick: () => navigate(`/projects/${id}`) },
    { key: 'contract', label: '关联合同' },
    { key: 'alert', label: '违规清单' },
    { type: 'divider' },
    { key: 'delay', label: '申请延期' },
  ];

  const columns = [
    {
      title: '项目编号',
      dataIndex: 'id',
      key: 'id',
      render: (text: string) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)' }}>
          {text}
        </span>
      ),
    },
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => (
        <a
          className="font-bold hover:opacity-80"
          style={{ color: 'var(--primary)' }}
        >
          {text}
        </a>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'default';
        if (status === '在建') color = 'processing';
        if (status === '完工') color = 'success';
        if (status === '预警') color = 'error';
        if (status === '立项') color = 'warning';
        return <Tag color={color} className="border-none">{status}</Tag>;
      },
    },
    {
      title: '施工与运输单位',
      key: 'companies',
      render: (_: unknown, record: (typeof projectsData)[0]) => (
        <div className="flex flex-col gap-1">
          <span className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            施: {record.builder}
          </span>
          <span className="text-xs" style={{ color: 'var(--text-secondary)', opacity: 0.9 }}>
            运: {record.transporter}
          </span>
        </div>
      ),
    },
    {
      title: '消纳进度',
      key: 'progress',
      width: 250,
      render: (_: unknown, record: (typeof projectsData)[0]) => {
        const percent =
          record.totalAmount === 0
            ? 0
            : Math.round((record.usedAmount / record.totalAmount) * 100);
        let status: 'normal' | 'exception' | 'success' | 'active' = 'normal';
        if (percent >= 100) status = 'success';
        else if (record.status === '预警') status = 'exception';
        else if (record.status === '在建') status = 'active';

        const remaining = record.totalAmount - record.usedAmount;

        return (
          <div className="flex flex-col gap-1 w-full">
            <Progress
              percent={percent}
              size="small"
              status={status}
              format={() => (
                <span style={{ color: 'var(--text-secondary)' }}>{percent}%</span>
              )}
              trailColor="rgba(0,0,0,0.06)"
            />
            <div
              className="flex justify-between text-xs mt-1"
              style={{ color: 'var(--text-secondary)' }}
            >
              <span>已消纳: {record.usedAmount / 10000}万方</span>
              <span style={{ color: 'var(--primary)' }}>
                剩余: {remaining / 10000}万方
              </span>
            </div>
          </div>
        );
      },
    },
    {
      title: '开工日期',
      dataIndex: 'startDate',
      key: 'startDate',
      render: (date: string) => (
        <span
          className="flex items-center gap-1 text-sm"
          style={{ color: 'var(--text-secondary)' }}
        >
          <ClockCircleOutlined /> {date}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: (typeof projectsData)[0]) => (
        <Space size="middle">
          <a
            style={{ color: 'var(--primary)' }}
            className="hover:opacity-80"
            onClick={() => navigate(`/projects/${record.id}?tab=config`)}
          >
            配置
          </a>
          <Dropdown menu={{ items: actionItems(record.id) }}>
            <a
              style={{ color: 'var(--text-secondary)' }}
              className="hover:opacity-80"
              onClick={(e) => e.preventDefault()}
            >
              更多 <MoreOutlined />
            </a>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1
            className="text-2xl font-bold m-0"
            style={{ color: 'var(--text-primary)' }}
          >
            消纳项目清单
          </h1>
          <p
            className="mt-1"
            style={{ color: 'var(--text-secondary)' }}
          >
            管理城市各类出土项目的审批进度、消纳情况与参建单位
          </p>
        </div>
      </div>

      <Card
        className="glass-panel"
        style={{ borderColor: 'var(--border-color)' }}
        bodyStyle={{ padding: 0 }}
      >
        <div
          className="p-4 border-b flex justify-between items-center"
          style={{
            borderColor: 'var(--border-color)',
            background: '#fafafa',
          }}
        >
          <div className="flex gap-4">
            <Input
              placeholder="搜索项目名称/编号/单位"
              prefix={<SearchOutlined style={{ color: 'var(--text-secondary)' }} />}
              className="w-72 bg-white hover:border-[var(--primary)] focus:border-[var(--primary)]"
              style={{
                borderColor: 'var(--border-color)',
                color: 'var(--text-primary)',
              }}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
            <Button
              icon={<FilterOutlined />}
              onClick={() => setFilterVisible(true)}
              style={{
                color: 'var(--text-secondary)',
                borderColor: 'var(--border-color)',
              }}
              className="hover:border-[var(--text-primary)]"
            >
              高级筛选
            </Button>
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            style={{ background: 'var(--primary)', border: 'none' }}
            className="hover:opacity-90"
          >
            新建项目立项
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={projectsData.filter(
            (item) =>
              item.name.includes(searchText) ||
              item.builder.includes(searchText) ||
              item.id.includes(searchText)
          )}
          rowKey="id"
          pagination={{
            defaultPageSize: 10,
            showSizeChanger: true,
            className: 'pr-4 pb-2',
          }}
          className="bg-transparent"
          rowClassName="hover:bg-[#fafafa] transition-colors"
        />
      </Card>

      <Drawer
        title="高级筛选"
        placement="right"
        onClose={() => setFilterVisible(false)}
        open={filterVisible}
        extra={
          <Space>
            <Button onClick={() => form.resetFields()}>重置</Button>
            <Button
              type="primary"
              onClick={() => setFilterVisible(false)}
              style={{ background: 'var(--primary)', border: 'none' }}
            >
              查询
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="status" label="项目状态">
            <Select
              placeholder="请选择项目状态"
              options={[
                { value: '立项', label: '立项' },
                { value: '在建', label: '在建' },
                { value: '完工', label: '完工' },
                { value: '预警', label: '预警' },
              ]}
            />
          </Form.Item>
          <Form.Item name="dateRange" label="开工日期范围">
            <RangePicker className="w-full" />
          </Form.Item>
          <Form.Item name="builder" label="建设单位">
            <Input placeholder="请输入建设单位名称" />
          </Form.Item>
          <Form.Item name="transporter" label="运输单位">
            <Input placeholder="请输入运输单位名称" />
          </Form.Item>
        </Form>
      </Drawer>
    </motion.div>
  );
};

export default ProjectsManagement;
