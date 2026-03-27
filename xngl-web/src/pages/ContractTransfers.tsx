import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SendOutlined, SwapOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import {
  approveContractTransfer,
  createContractTransfer,
  fetchContractList,
  fetchContractTransferDetail,
  fetchTransfers,
  rejectContractTransfer,
  submitContractTransfer,
  type ContractRecord,
  type ContractTransferCreatePayload,
  type ContractTransferRecord,
} from '../utils/contractApi';

const statusMeta: Record<string, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '草稿' },
  APPROVING: { color: 'processing', label: '审批中' },
  APPROVED: { color: 'green', label: '已通过' },
  REJECTED: { color: 'red', label: '已驳回' },
};

const formatMoney = (value?: number | null) => '¥ ' + Number(value || 0).toLocaleString();

const ContractTransfers: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [createForm] = Form.useForm<ContractTransferCreatePayload>();
  const [rejectForm] = Form.useForm<{ reason?: string }>();
  const [records, setRecords] = useState<ContractTransferRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<string>('all');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [contractOptions, setContractOptions] = useState<ContractRecord[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<ContractTransferRecord | null>(null);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [queryHandledId, setQueryHandledId] = useState<string | null>(null);

  const loadList = async () => {
    setLoading(true);
    try {
      const page = await fetchTransfers({
        approvalStatus: status === 'all' ? undefined : status,
        pageNo,
        pageSize,
      } as { approvalStatus?: string; pageNo?: number; pageSize?: number });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取内拨申请列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadList();
  }, [status, pageNo, pageSize]);

  useEffect(() => {
    const transferId = searchParams.get('transferId');
    if (!transferId || queryHandledId === transferId) {
      return;
    }
    setQueryHandledId(transferId);
    void openDetail(transferId);
  }, [queryHandledId, searchParams]);

  const loadContracts = async () => {
    try {
      const page = await fetchContractList({ contractStatus: 'EFFECTIVE', pageNo: 1, pageSize: 200 });
      setContractOptions(page.records || []);
    } catch (error) {
      console.error(error);
      message.error('加载合同选项失败');
    }
  };

  const openCreate = async () => {
    setCreateOpen(true);
    createForm.resetFields();
    await loadContracts();
  };

  const openDetail = async (id: string) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const data = await fetchContractTransferDetail(id);
      setDetail(data);
    } catch (error) {
      console.error(error);
      message.error('获取内拨申请详情失败');
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshAfterAction = async (keepDetail = true) => {
    await loadList();
    if (keepDetail && detail?.id) {
      const data = await fetchContractTransferDetail(detail.id);
      setDetail(data);
    }
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      if (!Number(values.transferAmount || 0) && !Number(values.transferVolume || 0)) {
        message.warning('调拨金额和调拨方量至少填写一项');
        return;
      }
      setSubmitting(true);
      const id = await createContractTransfer({
        sourceContractId: values.sourceContractId,
        targetContractId: values.targetContractId,
        transferAmount: Number(values.transferAmount || 0) || undefined,
        transferVolume: Number(values.transferVolume || 0) || undefined,
        reason: values.reason?.trim(),
      });
      message.success('内拨申请已创建');
      setCreateOpen(false);
      await loadList();
      await openDetail(String(id));
    } catch (error) {
      if (error instanceof Error) {
        console.error(error);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const runAction = async (runner: () => Promise<void>, successText: string) => {
    setSubmitting(true);
    try {
      await runner();
      message.success(successText);
      await refreshAfterAction();
    } catch (error) {
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!detail?.id) {
      return;
    }
    try {
      const values = await rejectForm.validateFields();
      await runAction(() => rejectContractTransfer(detail.id!, values.reason), '内拨申请已驳回');
      setRejectOpen(false);
      rejectForm.resetFields();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      setSubmitting(false);
    }
  };

  const summary = useMemo(
    () => ({
      draft: records.filter((item) => item.approvalStatus === 'DRAFT').length,
      approving: records.filter((item) => item.approvalStatus === 'APPROVING').length,
      approved: records.filter((item) => item.approvalStatus === 'APPROVED').length,
    }),
    [records]
  );

  const columns: ColumnsType<ContractTransferRecord> = [
    {
      title: '内拨单号',
      dataIndex: 'transferNo',
      key: 'transferNo',
      render: (value, record) => (
        <a onClick={() => void openDetail(record.id)}>{value}</a>
      ),
    },
    {
      title: '源合同',
      dataIndex: 'sourceContractNo',
      key: 'sourceContractNo',
      render: (value) => value || '-',
    },
    {
      title: '目标合同',
      dataIndex: 'targetContractNo',
      key: 'targetContractNo',
      render: (value) => value || '-',
    },
    {
      title: '调拨金额',
      dataIndex: 'transferAmount',
      key: 'transferAmount',
      render: (value) => formatMoney(value),
    },
    {
      title: '调拨方量',
      dataIndex: 'transferVolume',
      key: 'transferVolume',
      render: (value) => `${Number(value || 0).toLocaleString()} m³`,
    },
    {
      title: '状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (value?: string | null) => {
        const meta = statusMeta[String(value || 'DRAFT').toUpperCase()] || { color: 'default', label: value || '-' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          {record.approvalStatus === 'DRAFT' ? (
            <Button type="link" size="small" onClick={() => void runAction(() => submitContractTransfer(record.id), '内拨申请已提交')}>
              提交
            </Button>
          ) : null}
          {record.approvalStatus === 'APPROVING' ? (
            <Button type="link" size="small" onClick={() => void runAction(() => approveContractTransfer(record.id), '内拨申请已通过')}>
              通过
            </Button>
          ) : null}
          <Button type="link" size="small" onClick={() => void openDetail(record.id)}>
            详情
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6 pb-10">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">内拨申请</h1>
          <p className="g-text-secondary mt-1">场地间短距离土方调拨申请、详情和审批流转</p>
        </div>
        <Button type="primary" icon={<SwapOutlined />} onClick={() => void openCreate()}>
          发起内拨申请
        </Button>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="草稿" value={summary.draft} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="审批中" value={summary.approving} valueStyle={{ color: '#1677ff' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已通过" value={summary.approved} valueStyle={{ color: '#52c41a' }} /></Card>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap items-center gap-3 mb-4">
          <Select
            value={status}
            style={{ width: 220 }}
            onChange={(value) => {
              setStatus(value);
              setPageNo(1);
            }}
            options={[
              { label: '全部状态', value: 'all' },
              { label: '草稿', value: 'DRAFT' },
              { label: '审批中', value: 'APPROVING' },
              { label: '已通过', value: 'APPROVED' },
              { label: '已驳回', value: 'REJECTED' },
            ]}
          />
          <Button icon={<SendOutlined />} onClick={() => void loadList()}>
            刷新
          </Button>
        </div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={records}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            onChange: (current, size) => {
              setPageNo(current);
              setPageSize(size);
            },
          }}
        />
      </Card>

      <Modal
        title="发起内拨申请"
        open={createOpen}
        confirmLoading={submitting}
        onOk={() => void handleCreate()}
        onCancel={() => setCreateOpen(false)}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="sourceContractId" label="源合同" rules={[{ required: true, message: '请选择源合同' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={contractOptions.map((item) => ({
                label: `${item.contractNo} / ${item.projectName || '项目'} / ${item.siteName || '场地'}`,
                value: Number(item.id),
              }))}
            />
          </Form.Item>
          <Form.Item name="targetContractId" label="目标合同" rules={[{ required: true, message: '请选择目标合同' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={contractOptions.map((item) => ({
                label: `${item.contractNo} / ${item.projectName || '项目'} / ${item.siteName || '场地'}`,
                value: Number(item.id),
              }))}
            />
          </Form.Item>
          <Form.Item name="transferAmount" label="调拨金额">
            <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="可选" />
          </Form.Item>
          <Form.Item name="transferVolume" label="调拨方量">
            <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="可选" />
          </Form.Item>
          <Form.Item name="reason" label="调拨原因">
            <Input.TextArea rows={4} maxLength={200} showCount placeholder="请输入内拨申请说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={detail ? `内拨申请详情 · ${detail.transferNo}` : '内拨申请详情'}
        width={720}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        extra={
          detail ? (
            <Space>
              {detail.approvalStatus === 'DRAFT' ? (
                <Button loading={submitting} onClick={() => void runAction(() => submitContractTransfer(detail.id), '内拨申请已提交')}>
                  提交
                </Button>
              ) : null}
              {detail.approvalStatus === 'APPROVING' ? (
                <Button loading={submitting} onClick={() => void runAction(() => approveContractTransfer(detail.id), '内拨申请已通过')}>
                  通过
                </Button>
              ) : null}
              {detail.approvalStatus === 'APPROVING' ? (
                <Button danger loading={submitting} onClick={() => setRejectOpen(true)}>
                  驳回
                </Button>
              ) : null}
            </Space>
          ) : null
        }
      >
        <Card loading={detailLoading} bordered={false}>
          {detail ? (
            <Descriptions column={2}>
              <Descriptions.Item label="内拨单号">{detail.transferNo}</Descriptions.Item>
              <Descriptions.Item label="审批状态">
                <Tag color={statusMeta[String(detail.approvalStatus || 'DRAFT').toUpperCase()]?.color || 'default'}>
                  {statusMeta[String(detail.approvalStatus || 'DRAFT').toUpperCase()]?.label || detail.approvalStatus || '-'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="源合同">{detail.sourceContractNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="目标合同">{detail.targetContractNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="调拨金额">{formatMoney(detail.transferAmount)}</Descriptions.Item>
              <Descriptions.Item label="调拨方量">{Number(detail.transferVolume || 0).toLocaleString()} m³</Descriptions.Item>
              <Descriptions.Item label="申请人">{detail.applicantId || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{detail.createTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="调拨原因" span={2}>{detail.reason || '-'}</Descriptions.Item>
            </Descriptions>
          ) : null}
        </Card>
      </Drawer>

      <Modal
        title={detail ? `驳回内拨申请 · ${detail.transferNo}` : '驳回内拨申请'}
        open={rejectOpen}
        confirmLoading={submitting}
        onOk={() => void handleReject()}
        onCancel={() => {
          setRejectOpen(false);
          rejectForm.resetFields();
        }}
      >
        <Form form={rejectForm} layout="vertical">
          <Form.Item name="reason" label="驳回原因" rules={[{ required: true, message: '请输入驳回原因' }]}>
            <Input.TextArea rows={4} maxLength={200} showCount placeholder="请输入驳回说明" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
    </div>
    </div>
  );
};

export default ContractTransfers;
