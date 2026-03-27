import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined, PlusOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  fetchProjectPaymentSummary,
  fetchProjects,
  type ProjectPaymentSummary,
  type ProjectRecord,
} from '../utils/projectApi';
import {
  cancelProjectPayment,
  createProjectPayment,
  listProjectPayments,
  type ProjectPaymentRecord,
} from '../utils/projectPaymentApi';

const { RangePicker } = DatePicker;

const paymentTypeOptions = [
  { label: '全部类型', value: 'all' },
  { label: 'MANUAL', value: 'MANUAL' },
];

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: 'NORMAL', value: 'NORMAL' },
  { label: 'CANCELLED', value: 'CANCELLED' },
];

const sourceTypeOptions = [
  { label: 'MANUAL', value: 'MANUAL' },
  { label: 'BANK', value: 'BANK' },
  { label: 'CONTRACT', value: 'CONTRACT' },
];

const formatAmount = (value?: number) => Number(value || 0).toLocaleString();

const ProjectsPayments: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultProjectId = searchParams.get('projectId') || '';
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [records, setRecords] = useState<ProjectPaymentRecord[]>([]);
  const [projectOptions, setProjectOptions] = useState<ProjectRecord[]>([]);
  const [summary, setSummary] = useState<ProjectPaymentSummary>();
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [projectId, setProjectId] = useState(defaultProjectId);
  const [paymentType, setPaymentType] = useState('all');
  const [status, setStatus] = useState('all');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [dateRange, setDateRange] = useState<[string | undefined, string | undefined]>([
    undefined,
    undefined,
  ]);

  const loadProjects = async () => {
    setProjectsLoading(true);
    try {
      const page = await fetchProjects({ pageNo: 1, pageSize: 200 });
      setProjectOptions(page.records || []);
    } catch (error) {
      console.error(error);
      message.error('获取项目选项失败');
      setProjectOptions([]);
    } finally {
      setProjectsLoading(false);
    }
  };

  const loadPayments = async () => {
    setLoading(true);
    try {
      const page = await listProjectPayments({
        projectId: projectId || undefined,
        keyword: keyword.trim() || undefined,
        paymentType: paymentType === 'all' ? undefined : paymentType,
        status: status === 'all' ? undefined : status,
        startDate: dateRange[0],
        endDate: dateRange[1],
        pageNo,
        pageSize,
      });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取项目交款记录失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async (currentProjectId: string) => {
    if (!currentProjectId) {
      setSummary(undefined);
      return;
    }
    try {
      const data = await fetchProjectPaymentSummary(currentProjectId);
      setSummary(data);
    } catch (error) {
      console.error(error);
      message.error('获取项目交款汇总失败');
      setSummary(undefined);
    }
  };

  useEffect(() => {
    void loadProjects();
  }, []);

  useEffect(() => {
    void loadPayments();
  }, [projectId, keyword, paymentType, status, pageNo, pageSize, dateRange]);

  useEffect(() => {
    void loadSummary(projectId);
  }, [projectId]);

  const stats = useMemo(() => {
    const totalAmount = records.reduce((sum, item) => sum + Number(item.amount || 0), 0);
    const normalCount = records.filter((item) => item.status === 'NORMAL').length;
    const cancelledCount = records.filter((item) => item.status === 'CANCELLED').length;
    return {
      total,
      totalAmount,
      normalCount,
      cancelledCount,
    };
  }, [records, total]);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const targetProjectId = values.projectId;
      const result = await createProjectPayment(targetProjectId, {
        paymentNo: values.paymentNo,
        paymentType: values.paymentType,
        amount: Number(values.amount),
        paymentDate: values.paymentDate.format('YYYY-MM-DD'),
        voucherNo: values.voucherNo,
        sourceType: values.sourceType,
        remark: values.remark,
      });
      message.success('交款登记成功: ' + (result.paymentNo || '已生成流水'));
      setOpen(false);
      form.resetFields();
      if (!projectId) {
        setProjectId(targetProjectId);
        setSearchParams(targetProjectId ? { projectId: targetProjectId } : {});
      }
      await Promise.all([loadPayments(), loadSummary(targetProjectId)]);
    } catch (error) {
      if (error instanceof Error && error.message) {
        message.error(error.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (record: ProjectPaymentRecord) => {
    try {
      await cancelProjectPayment(record.id, '页面手动撤销');
      message.success('交款记录已撤销');
      await Promise.all([loadPayments(), loadSummary(record.projectId)]);
    } catch (error) {
      console.error(error);
      message.error('撤销失败');
    }
  };

  const columns: ColumnsType<ProjectPaymentRecord> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (_, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary-link font-medium">{record.projectName || '-'}</span>
          <span className="text-xs g-text-secondary">{record.projectCode || '-'}</span>
        </div>
      ),
    },
    {
      title: '交款单号',
      dataIndex: 'paymentNo',
      key: 'paymentNo',
      render: (value?: string) => <span className="font-mono g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '交款类型',
      dataIndex: 'paymentType',
      key: 'paymentType',
      render: (value?: string) => value || '-',
    },
    {
      title: '交款金额 (元)',
      dataIndex: 'amount',
      key: 'amount',
      render: (value?: number) => <span className="g-text-success">{formatAmount(value)}</span>,
    },
    {
      title: '交款日期',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value?: string) => value || '-',
    },
    {
      title: '凭证号',
      dataIndex: 'voucherNo',
      key: 'voucherNo',
      render: (value?: string) => value || '-',
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      key: 'sourceType',
      render: (value?: string) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value?: string) => (
        <Tag color={value === 'CANCELLED' ? 'error' : 'green'}>{value || '未知'}</Tag>
      ),
    },
    {
      title: '备注/撤销原因',
      key: 'remark',
      render: (_, record) => record.cancelReason || record.remark || '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate('/projects/' + record.projectId + '?tab=payment')}
          >
            查看
          </Button>
          <Popconfirm
            title="确认撤销该交款记录吗？"
            okText="确认"
            cancelText="取消"
            disabled={record.status === 'CANCELLED'}
            onConfirm={() => void handleCancel(record)}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<StopOutlined />}
              disabled={record.status === 'CANCELLED'}
            >
              撤销
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold g-text-primary m-0">项目交款管理</h1>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="glass-panel g-border-panel border">
          <Statistic title="当前结果数" value={stats.total} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="当前页交款额" value={stats.totalAmount} precision={2} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="正常记录" value={stats.normalCount} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="已撤销记录" value={stats.cancelledCount} />
        </Card>

      <Card className="glass-panel g-border-panel border" title="所选项目结算概览">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Statistic title="项目名称" value={summary.projectName || '-'} valueStyle={{ fontSize: 18 }} />
            <Statistic title="应收总额" value={Number(summary.totalAmount || 0)} precision={2} />
            <Statistic title="累计已交" value={Number(summary.paidAmount || 0)} precision={2} />
            <Statistic
              title="剩余欠款"
              value={Number(summary.debtAmount || 0)}
              precision={2}
              valueStyle={{ color: Number(summary.debtAmount || 0) > 0 ? 'var(--error)' : 'var(--success)' }}
            />
          </div>
          <div className="mt-3 text-sm g-text-secondary">
            最近交款日期: {summary.lastPaymentDate || '-'} / 状态: {summary.status || '-'}
          </div>
        </Card>
      ) : null}

      <Card className="glass-panel g-border-panel border">
        <div className="flex justify-between mb-4 flex-wrap gap-4">
          <Space wrap>
            <Input
              placeholder="搜索项目名称/编号/交款单号"
              prefix={<SearchOutlined />}
              className="bg-white g-border-panel border g-text-primary w-64"
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setPageNo(1);
              }}
            />
            <Select
              showSearch
              allowClear
              placeholder="筛选项目"
              loading={projectsLoading}
              value={projectId || undefined}
              style={{ width: 240 }}
              optionFilterProp="label"
              options={projectOptions.map((item) => ({
                label: (item.name || '-') + (item.code ? ' (' + item.code + ')' : ''),
                value: item.id,
              }))}
              onChange={(value) => {
                const nextValue = value || '';
                setProjectId(nextValue);
                setPageNo(1);
                setSearchParams(nextValue ? { projectId: nextValue } : {});
              }}
            />
            <Select
              value={paymentType}
              style={{ width: 140 }}
              options={paymentTypeOptions}
              onChange={(value) => {
                setPaymentType(value);
                setPageNo(1);
              }}
            />
            <Select
              value={status}
              style={{ width: 140 }}
              options={statusOptions}
              onChange={(value) => {
                setStatus(value);
                setPageNo(1);
              }}
            />
            <RangePicker
              className="bg-white g-border-panel border"
              onChange={(values) => {
                setDateRange([
                  values?.[0] ? dayjs(values[0]).format('YYYY-MM-DD') : undefined,
                  values?.[1] ? dayjs(values[1]).format('YYYY-MM-DD') : undefined,
                ]);
                setPageNo(1);
              }}
            />
            <Button
              onClick={() => {
                setKeyword('');
                setProjectId('');
                setPaymentType('all');
                setStatus('all');
                setDateRange([undefined, undefined]);
                setPageNo(1);
                setSearchParams({});
              }}
            >
              重置
            </Button>
          </Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.setFieldsValue({
                projectId: projectId || undefined,
                paymentDate: dayjs(),
                paymentType: 'MANUAL',
                sourceType: 'MANUAL',
              });
              setOpen(true);
            }}
          >
            交款登记
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
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
        />
      </Card>

      <Modal
        title="项目交款登记"
        open={open}
        confirmLoading={submitting}
        onOk={() => void handleCreate()}
        onCancel={() => {
          setOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="projectId"
            label="关联项目"
            rules={[{ required: true, message: '请选择项目' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={projectOptions.map((item) => ({
                label: (item.name || '-') + (item.code ? ' (' + item.code + ')' : ''),
                value: item.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="paymentNo" label="交款单号">
            <Input placeholder="不填则系统自动生成" />
          </Form.Item>
          <Form.Item
            name="amount"
            label="交款金额 (元)"
            rules={[{ required: true, message: '请输入交款金额' }]}
          >
            <InputNumber className="w-full" min={0.01} precision={2} />
          </Form.Item>
          <Form.Item
            name="paymentDate"
            label="交款日期"
            rules={[{ required: true, message: '请选择交款日期' }]}
          >
            <DatePicker className="w-full" />
          </Form.Item>
          <Form.Item name="paymentType" label="交款类型">
            <Select options={paymentTypeOptions.slice(1)} />
          </Form.Item>
          <Form.Item name="sourceType" label="来源类型">
            <Select options={sourceTypeOptions} />
          </Form.Item>
          <Form.Item name="voucherNo" label="凭证号">
            <Input placeholder="请输入银行流水号或收据号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注信息" />
          </Form.Item>
        </Form>
      </Modal>
  );
};
export default ProjectsPayments;
