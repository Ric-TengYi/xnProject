import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  BlockOutlined,
  DollarOutlined,
  DownloadOutlined,
  FileTextOutlined,
  FilterOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  fetchContractList,
  fetchContractStats,
  type ContractRecord,
  type ContractStats,
} from '../utils/contractApi';

const { RangePicker } = DatePicker;

const statusLabelMap: Record<string, { color: string; text: string }> = {
  EFFECTIVE: { color: 'green', text: '生效' },
  EXECUTING: { color: 'green', text: '生效' },
  APPROVING: { color: 'processing', text: '审批中' },
  PENDING: { color: 'processing', text: '审批中' },
  TERMINATED: { color: 'default', text: '终止' },
  CANCELLED: { color: 'error', text: '作废' },
  VOID: { color: 'error', text: '作废' },
};

const formatMoney = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const ContractsManagement: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<ContractStats | null>(null);
  const [records, setRecords] = useState<ContractRecord[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string>('all');
  const [range, setRange] = useState<[string | undefined, string | undefined]>([
    undefined,
    undefined,
  ]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [statsData, listData] = await Promise.all([
          fetchContractStats(),
          fetchContractList({
            keyword: keyword.trim() || undefined,
            contractStatus: status === 'all' ? undefined : status,
            startDate: range[0],
            endDate: range[1],
            pageNo,
            pageSize,
          }),
        ]);
        setStats(statsData);
        setRecords(listData.records || []);
        setTotal(listData.total || 0);
      } catch (error) {
        console.error(error);
        message.error('获取合同数据失败');
        setStats(null);
        setRecords([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [keyword, status, range, pageNo, pageSize]);

  const summaryAmount = useMemo(
    () => records.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0),
    [records]
  );

  const columns: ColumnsType<ContractRecord> = [
    {
      title: '合同编号',
      dataIndex: 'contractNo',
      key: 'contractNo',
      render: (value, record) => (
        <a
          className="g-text-primary-link font-mono tracking-wide"
          onClick={() => navigate('/contracts/' + record.id)}
        >
          {value || 'HT-' + record.id}
        </a>
      ),
    },
    {
      title: '合同类型',
      dataIndex: 'contractType',
      key: 'contractType',
      render: (value?: string) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '关联项目',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (value, record) => (
        <strong className="g-text-primary">{value || '项目#' + String(record.projectId || '-')}</strong>
      ),
    },
    {
      title: '约定消纳场',
      dataIndex: 'siteName',
      key: 'siteName',
      render: (value, record) => (
        <span className="g-text-secondary">{value || '场地#' + String(record.siteId || '-')}</span>
      ),
    },
    {
      title: '总金额/已入账',
      key: 'money',
      render: (_, record) => {
        const totalAmount = Number(record.contractAmount || 0);
        const receivedAmount = Number(record.receivedAmount || 0);
        const percent =
          totalAmount > 0 ? Math.min(100, Math.round((receivedAmount / totalAmount) * 100)) : 0;
        return (
          <div className="flex flex-col gap-1 w-40">
            <span className="g-text-success font-bold">{formatMoney(totalAmount)}</span>
            <Progress
              percent={percent}
              size="small"
              showInfo={false}
              strokeColor="var(--success)"
              trailColor="rgba(0,0,0,0.06)"
            />
            <span className="text-xs g-text-secondary">
              已入账 {formatMoney(receivedAmount)}
            </span>
          </div>
        );
      },
    },
    {
      title: '约定方量(m³)',
      dataIndex: 'agreedVolume',
      key: 'agreedVolume',
      render: (value?: number) => (
        <span className="g-text-secondary">{Number(value || 0).toLocaleString()}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'contractStatus',
      key: 'contractStatus',
      render: (value?: string) => {
        const normalized = String(value || '').toUpperCase();
        const matched = statusLabelMap[normalized];
        return <Tag color={matched?.color || 'default'}>{matched?.text || value || '未知'}</Tag>;
      },
    },
    {
      title: '签订日期',
      dataIndex: 'signDate',
      key: 'signDate',
      render: (value?: string) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <a onClick={() => navigate('/contracts/' + record.id)}>详情</a>
          <a onClick={() => navigate('/contracts/payments?contractId=' + record.id)}>入账</a>
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">合同与财务结算</h1>
          <p className="g-text-secondary mt-1">
            管理合同主档、审批状态、分次入账及财务结算进度
          </p>
        </div>
        <div className="text-right">
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            className="g-btn-primary border-none mr-3 text-white"
          >
            导出月度报表
          </Button>
          <Button type="primary" className="g-btn-primary border-none">
            发起新合同审批
          </Button>
        </div>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">本月累计入账</span>}
              value={stats?.monthlyReceiptAmount || 0}
              valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }}
              prefix={<DollarOutlined />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              已入账 {stats?.monthlyReceiptCount || 0} 笔
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">待入账金额</span>}
              value={stats?.pendingReceiptAmount || 0}
              valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }}
              prefix={<FileTextOutlined />}
            />
            <div className="mt-2 text-xs g-text-secondary">含审批中合同</div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">生效合同数</span>}
              value={stats?.effectiveContracts || 0}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={<BlockOutlined className="g-text-primary-link" />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              总合同 {stats?.totalContracts || 0} 份
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">当前页合同额</span>}
              value={summaryAmount}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={<FileTextOutlined className="g-text-primary-link" />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              结算单 {stats?.totalSettlementOrders || 0} 张
            </div>
          </Card>
        </Col>
      </Row>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
          <Input
            placeholder="搜索合同编号或项目名称"
            prefix={<SearchOutlined className="g-text-secondary" />}
            className="w-64 bg-white g-border-panel border g-text-primary"
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPageNo(1);
            }}
          />
          <RangePicker
            className="bg-white g-border-panel border"
            onChange={(values) => {
              setRange([
                values?.[0] ? dayjs(values[0]).format('YYYY-MM-DD') : undefined,
                values?.[1] ? dayjs(values[1]).format('YYYY-MM-DD') : undefined,
              ]);
              setPageNo(1);
            }}
          />
          <Select
            value={status}
            style={{ width: 160 }}
            options={[
              { label: '全部状态', value: 'all' },
              { label: '生效', value: 'EFFECTIVE' },
              { label: '审批中', value: 'APPROVING' },
              { label: '终止', value: 'TERMINATED' },
              { label: '作废', value: 'CANCELLED' },
            ]}
            onChange={(value) => {
              setStatus(value);
              setPageNo(1);
            }}
          />
          <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">
            状态筛选
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPageNo(nextPage);
              setPageSize(nextPageSize);
            },
          }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>
    </motion.div>
  );
};

export default ContractsManagement;
