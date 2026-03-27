import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined, PlusOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  cancelContractReceipt,
  createContractReceipt,
  listContractReceipts,
  type ContractReceipt,
} from '../utils/contractReceipts';

const { RangePicker } = DatePicker;

const ContractsPayments: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const defaultContractId = searchParams.get('contractId') || '';
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [records, setRecords] = useState<ContractReceipt[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('all');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [dateRange, setDateRange] = useState<[string | undefined, string | undefined]>([
    undefined,
    undefined,
  ]);
  const [open, setOpen] = useState(false);

  const loadReceipts = async () => {
    setLoading(true);
    try {
      const page = await listContractReceipts({
        contractId: defaultContractId || undefined,
        keyword: keyword.trim() || undefined,
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
      message.error('获取合同入账记录失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadReceipts();
  }, [keyword, status, pageNo, pageSize, dateRange, defaultContractId]);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await createContractReceipt({
        contractId: values.contractId,
        amount: Number(values.amount),
        receiptDate: values.receiptDate.format('YYYY-MM-DD'),
        receiptType: values.receiptType,
        voucherNo: values.voucherNo,
        bankFlowNo: values.bankFlowNo,
        remark: values.remark,
      });
      message.success('入账记录已提交');
      setOpen(false);
      form.resetFields();
      await loadReceipts();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '提交入账记录失败');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelReceipt = async (receipt: ContractReceipt) => {
    try {
      await cancelContractReceipt(receipt.id, '页面手动冲销');
      message.success('冲销完成');
      await loadReceipts();
    } catch (error) {
      console.error(error);
      message.error('冲销失败');
    }
  };

  const columns: ColumnsType<ContractReceipt> = [
    {
      title: '入账流水号',
      dataIndex: 'receiptNo',
      key: 'receiptNo',
      render: (value, record) => <span className="font-mono g-text-secondary">{value || record.id}</span>,
    },
    {
      title: '合同编号',
      dataIndex: 'contractNo',
      key: 'contractNo',
      render: (value, record) => (
        <span className="g-text-primary-link">{value || record.contractId || '-'}</span>
      ),
    },
    {
      title: '合同名称',
      dataIndex: 'contractName',
      key: 'contractName',
      render: (value?: string) => value || '-',
    },
    {
      title: '入账金额',
      dataIndex: 'amount',
      key: 'amount',
      render: (value?: number) => <span className="g-text-success">{Number(value || 0).toLocaleString()}</span>,
    },
    { title: '入账日期', dataIndex: 'receiptDate', key: 'receiptDate' },
    { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value?: string) => (
        <Tag color={String(value || '').toUpperCase() === 'CANCELLED' ? 'error' : 'green'}>
          {value || '未知'}
        </Tag>
      ),
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
            disabled={!record.contractId}
            onClick={() => navigate('/contracts/' + record.contractId + '?tab=payment')}
          >
            查看合同
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<StopOutlined />}
            disabled={String(record.status || '').toUpperCase() === 'CANCELLED'}
            onClick={() => void handleCancelReceipt(record)}
          >
            冲销
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold g-text-primary m-0">合同入账管理</h1>
      </div>
      <Card className="glass-panel g-border-panel border">
        <div className="flex justify-between mb-4 flex-wrap gap-4">
          <Space wrap>
            <Input
              placeholder="搜索合同/凭证号/流水号"
              prefix={<SearchOutlined />}
              className="bg-white g-border-panel border g-text-primary w-64"
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setPageNo(1);
              }}
            />
            <Select
              value={status}
              className="w-32 bg-white"
              onChange={(value) => {
                setStatus(value);
                setPageNo(1);
              }}
              options={[
                { label: '全部状态', value: 'all' },
                { label: 'NORMAL', value: 'NORMAL' },
                { label: 'CANCELLED', value: 'CANCELLED' },
              ]}
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
            <Button type="primary" onClick={() => void loadReceipts()}>
              刷新
            </Button>
          </Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.setFieldValue('contractId', defaultContractId || undefined);
              setOpen(true);
            }}
          >
            新增入账
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
        title="入账登记"
        open={open}
        onOk={() => void handleCreate()}
        confirmLoading={submitting}
        onCancel={() => {
          setOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical" initialValues={{ contractId: defaultContractId || undefined }}>
          <Form.Item
            name="contractId"
            label="合同ID"
            rules={[{ required: true, message: '请输入合同ID' }]}
          >
            <Input placeholder="请输入后端合同ID" />
          </Form.Item>
          <Form.Item
            name="amount"
            label="本次入账金额"
            rules={[{ required: true, message: '请输入入账金额' }]}
          >
            <InputNumber className="w-full" min={0.01} />
          </Form.Item>
          <Form.Item
            name="receiptDate"
            label="入账日期"
            rules={[{ required: true, message: '请选择入账日期' }]}
          >
            <DatePicker className="w-full" />
          </Form.Item>
          <Form.Item name="receiptType" label="入账类型" initialValue="MANUAL">
            <Select
              options={[
                { label: 'MANUAL', value: 'MANUAL' },
                { label: 'REVERSAL', value: 'REVERSAL' },
              ]}
            />
          </Form.Item>
          <Form.Item name="voucherNo" label="凭证号">
            <Input />
          </Form.Item>
          <Form.Item name="bankFlowNo" label="银行流水号">
            <Input />
          </Form.Item>
          <Form.Item name="remark" label="备注说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
  );
};
export default ContractsPayments;
