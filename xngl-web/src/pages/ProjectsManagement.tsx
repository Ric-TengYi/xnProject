import React, { useEffect, useMemo, useState } from 'react';
import {
  Card,
  Empty,
  Input,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Button,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { FilterOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '立项', value: 0 },
  { label: '在建', value: 1 },
  { label: '预警', value: 2 },
  { label: '完工', value: 3 },
];

const statusColorMap: Record<string, string> = {
  立项: 'warning',
  在建: 'processing',
  预警: 'error',
  完工: 'success',
};

const formatAmount = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const ProjectsManagement: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | 'all'>('all');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);

  useEffect(() => {
    const loadProjects = async () => {
      setLoading(true);
      try {
        const page = await fetchProjects({
          keyword: keyword.trim() || undefined,
          status: status === 'all' ? undefined : status,
          pageNo,
          pageSize,
        });
        setProjects(page.records || []);
        setTotal(page.total || 0);
      } catch (error) {
        console.error(error);
        message.error('获取项目列表失败');
        setProjects([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };

    void loadProjects();
  }, [keyword, status, pageNo, pageSize]);

  const summary = useMemo(
    () => ({
      total,
      active: projects.filter((item) => item.statusLabel === '在建').length,
      warning: projects.filter((item) => item.statusLabel === '预警').length,
      amount: projects.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
    }),
    [projects, total]
  );

  const columns: ColumnsType<ProjectRecord> = [
    {
      title: '项目编号',
      dataIndex: 'code',
      key: 'code',
      render: (value, record) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)' }}>
          {value || 'PRJ-' + record.id}
        </span>
      ),
    },
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
      render: (value, record) => (
        <a
          className="font-bold hover:opacity-80"
          style={{ color: 'var(--primary)' }}
          onClick={() => navigate('/projects/' + record.id)}
        >
          {value}
        </a>
      ),
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (value?: string | null) => (
        <Tag color={statusColorMap[value || ''] || 'default'} className="border-none">
          {value || '未知'}
        </Tag>
      ),
    },
    {
      title: '所属组织',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value?: string | null) => (
        <span style={{ color: 'var(--text-secondary)' }}>{value || '-'}</span>
      ),
    },
    {
      title: '合同/场地',
      key: 'relations',
      render: (_, record) => (
        <div className="flex flex-col gap-1">
          <span style={{ color: 'var(--text-secondary)' }}>
            合同 {record.contractCount || 0} 份
          </span>
          <span style={{ color: 'var(--text-secondary)' }}>
            场地 {record.siteCount || 0} 个
          </span>
        </div>
      ),
    },
    {
      title: '交款进度',
      key: 'payment',
      width: 260,
      render: (_, record) => {
        const totalAmount = Number(record.totalAmount || 0);
        const paidAmount = Number(record.paidAmount || 0);
        const percent =
          totalAmount > 0 ? Math.min(100, Math.round((paidAmount / totalAmount) * 100)) : 0;
        return (
          <div className="flex flex-col gap-1">
            <Progress
              percent={percent}
              size="small"
              showInfo={false}
              strokeColor="var(--success)"
              trailColor="rgba(0,0,0,0.06)"
            />
            <div className="flex justify-between text-xs" style={{ color: 'var(--text-secondary)' }}>
              <span>已交 {formatAmount(record.paidAmount)}</span>
              <span>欠款 {formatAmount(record.debtAmount)}</span>
            </div>
          </div>
        );
      },
    },
    {
      title: '最后交款日',
      dataIndex: 'lastPaymentDate',
      key: 'lastPaymentDate',
      render: (value?: string | null) => (
        <span style={{ color: 'var(--text-secondary)' }}>{value || '-'}</span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <a style={{ color: 'var(--primary)' }} onClick={() => navigate('/projects/' + record.id)}>
            详情
          </a>
          <a
            style={{ color: 'var(--primary)' }}
            onClick={() => navigate('/projects/' + record.id + '?tab=config')}
          >
            配置
          </a>
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
          <h1 className="text-2xl font-bold m-0" style={{ color: 'var(--text-primary)' }}>
            消纳项目清单
          </h1>
          <p className="mt-1" style={{ color: 'var(--text-secondary)' }}>
            管理项目主档、交款进度、关联合同与场地投放情况
          </p>
        </div>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="glass-panel" style={{ borderColor: 'var(--border-color)' }}>
          <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>项目总数</div>
          <div className="text-2xl font-bold">{summary.total}</div>
        </Card>
        <Card className="glass-panel" style={{ borderColor: 'var(--border-color)' }}>
          <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>在建项目</div>
          <div className="text-2xl font-bold">{summary.active}</div>
        </Card>
        <Card className="glass-panel" style={{ borderColor: 'var(--border-color)' }}>
          <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>预警项目</div>
          <div className="text-2xl font-bold">{summary.warning}</div>
        </Card>
        <Card className="glass-panel" style={{ borderColor: 'var(--border-color)' }}>
          <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>当前页合同额</div>
          <div className="text-2xl font-bold">{formatAmount(summary.amount)}</div>
        </Card>

      <Card
        className="glass-panel"
        style={{ borderColor: 'var(--border-color)' }}
        bodyStyle={{ padding: 0 }}
      >
        <div
          className="p-4 border-b flex flex-wrap justify-between gap-4 items-center"
          style={{ borderColor: 'var(--border-color)', background: '#fafafa' }}
        >
          <div className="flex gap-4 flex-wrap">
            <Input
              placeholder="搜索项目名称/编号/地址"
              prefix={<SearchOutlined style={{ color: 'var(--text-secondary)' }} />}
              className="w-72 bg-white"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPageNo(1);
              }}
            />
            <Select
              value={status}
              options={statusOptions}
              onChange={(value) => {
                setStatus(value as number | 'all');
                setPageNo(1);
              }}
              style={{ width: 140 }}
            />
            <Button icon={<FilterOutlined />}>筛选条件</Button>
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            style={{ background: 'var(--primary)', border: 'none' }}
          >
            新建项目立项
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={projects}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: <Empty description="暂无项目数据" /> }}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPageNo(nextPage);
              setPageSize(nextPageSize);
            },
            className: 'pr-4 pb-2',
          }}
          className="bg-transparent"
          rowClassName="hover:bg-[#fafafa] transition-colors"
        />
      </Card>
    </motion.div>
  );    </div>
  );
};

export default ProjectsManagement;
