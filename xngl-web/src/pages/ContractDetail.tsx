import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Timeline,
  message,
} from 'antd';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import {
  approveContract,
  downloadContractMaterial,
  fetchApprovalRecords,
  fetchContractDetail,
  fetchContractInvoices,
  fetchContractMaterials,
  fetchContractTickets,
  rejectContract,
  submitContract,
  type ApprovalRecord,
  type ContractDetail as ContractDetailType,
  type ContractInvoice,
  type ContractMaterial,
  type ContractTicket,
} from '../utils/contractApi';
import {
  listContractReceiptsByContract,
  type ContractReceipt,
} from '../utils/contractReceipts';

const formatMoney = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const statusColorMap: Record<string, string> = {
  EFFECTIVE: 'green',
  EXECUTING: 'green',
  APPROVING: 'processing',
  PENDING: 'processing',
  TERMINATED: 'default',
  CANCELLED: 'error',
  VOID: 'error',
};

const ContractDetail: React.FC = () => {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<ContractDetailType | null>(null);
  const [approvals, setApprovals] = useState<ApprovalRecord[]>([]);
  const [materials, setMaterials] = useState<ContractMaterial[]>([]);
  const [invoices, setInvoices] = useState<ContractInvoice[]>([]);
  const [tickets, setTickets] = useState<ContractTicket[]>([]);
  const [receipts, setReceipts] = useState<ContractReceipt[]>([]);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectForm] = Form.useForm<{ reason?: string }>();

  const loadData = async (contractId: string) => {
    setLoading(true);
    try {
      const [detailData, approvalData, materialData, invoiceData, ticketData, receiptData] =
        await Promise.all([
          fetchContractDetail(contractId),
          fetchApprovalRecords(contractId),
          fetchContractMaterials(contractId),
          fetchContractInvoices(contractId),
          fetchContractTickets(contractId),
          listContractReceiptsByContract(contractId),
        ]);
      setDetail(detailData);
      setApprovals(approvalData || []);
      setMaterials(materialData || []);
      setInvoices(invoiceData || []);
      setTickets(ticketData || []);
      setReceipts(receiptData || []);
    } catch (error) {
      console.error(error);
      message.error('获取合同详情失败');
      setDetail(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!id) {
      return;
    }
    void loadData(id);
  }, [id]);

  const paymentTab = new URLSearchParams(location.search).get('tab');
  const statusText = detail?.contractStatus || detail?.approvalStatus || '未知';
  const canSubmit =
    detail &&
    ['DRAFT', 'REJECTED'].includes(String(detail.contractStatus || '').toUpperCase());
  const canApprove = detail && String(detail.contractStatus || '').toUpperCase() === 'APPROVING';

  const refreshCurrent = async () => {
    if (!id) {
      return;
    }
    await loadData(id);
  };

  const handleSubmitApproval = async () => {
    if (!id) {
      return;
    }
    setActionLoading(true);
    try {
      await submitContract(id);
      message.success('合同已提交审批');
      await refreshCurrent();
    } catch (error) {
      console.error(error);
      message.error('提交审批失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!id) {
      return;
    }
    setActionLoading(true);
    try {
      await approveContract(id);
      message.success('合同审批通过');
      await refreshCurrent();
    } catch (error) {
      console.error(error);
      message.error('合同审批失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!id) {
      return;
    }
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      await rejectContract(id, values.reason);
      message.success('合同已驳回');
      setRejectOpen(false);
      rejectForm.resetFields();
      await refreshCurrent();
    } catch (error) {
      if (error instanceof Error) {
        console.error(error);
      }
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      message.error('合同驳回失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handlePreviewMaterial = async (item: ContractMaterial) => {
    if (!id) {
      return;
    }
    try {
      const blob = await downloadContractMaterial(id, item.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
      console.error(error);
      message.error('预览办事材料失败');
    }
  };

  const handleDownloadMaterial = async (item: ContractMaterial) => {
    if (!id) {
      return;
    }
    try {
      const blob = await downloadContractMaterial(id, item.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${item.materialName || 'contract-material'}.txt`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
      console.error(error);
      message.error('下载办事材料失败');
    }
  };

  const headerActions = (
    <Space>
      {canSubmit ? (
        <Button type="primary" loading={actionLoading} onClick={() => void handleSubmitApproval()}>
          提交审批
        </Button>
      ) : null}
      {canApprove ? (
        <Button loading={actionLoading} onClick={() => void handleApprove()}>
          审批通过
        </Button>
      ) : null}
      {canApprove ? (
        <Button danger loading={actionLoading} onClick={() => setRejectOpen(true)}>
          驳回
        </Button>
      ) : null}
      <Button type="link" icon={<EditOutlined />}>
        编辑草稿
      </Button>
    </Space>
  );

  const items = [
    {
      key: 'info',
      label: '基础信息',
      children: (
        <div className="space-y-6">
          <Card
            title="合同基础信息"
            className="glass-panel g-border-panel border"
            extra={headerActions}
          >
            <Descriptions column={3} className="g-text-secondary">
              <Descriptions.Item label="合同编号">
                {detail?.contractNo || 'HT-' + String(detail?.id || '')}
              </Descriptions.Item>
              <Descriptions.Item label="合同类型">{detail?.contractType || '-'}</Descriptions.Item>
              <Descriptions.Item label="合同状态">
                <Tag color={statusColorMap[String(statusText).toUpperCase()] || 'default'}>
                  {statusText}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="关联项目">{detail?.projectName || '-'}</Descriptions.Item>
              <Descriptions.Item label="约定消纳场">{detail?.siteName || '-'}</Descriptions.Item>
              <Descriptions.Item label="建设单位">
                {detail?.constructionOrgName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="运输单位">
                {detail?.transportOrgName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="合同总金额">
                {formatMoney(detail?.contractAmount || detail?.amount)}
              </Descriptions.Item>
              <Descriptions.Item label="约定方量">
                {Number(detail?.agreedVolume || 0).toLocaleString()} m³
              </Descriptions.Item>
              <Descriptions.Item label="单价配置">
                区内 {formatMoney(detail?.unitPriceInside)} / 区外 {formatMoney(detail?.unitPriceOutside)}
              </Descriptions.Item>
              <Descriptions.Item label="签订日期">{detail?.signDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="生效日期">{detail?.effectiveDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="失效日期">{detail?.expireDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="备注" span={3}>
                {detail?.remark || '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="资金流向" className="glass-panel g-border-panel border">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">合同总金额</div>
                <div className="text-2xl font-bold g-text-primary">
                  {formatMoney(detail?.contractAmount || detail?.amount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">累计已入账</div>
                <div className="text-2xl font-bold g-text-success">
                  {formatMoney(detail?.receivedAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">剩余待入账</div>
                <div className="text-2xl font-bold g-text-warning">
                  {formatMoney(detail?.pendingAmount)}
                </div>
              </Card>
            </div>
          </Card>
        </div>
      ),
    },
    {
      key: 'approval',
      label: '审批流程',
      children: (
        <Card className="glass-panel g-border-panel border">
          {approvals.length > 0 ? (
            <Timeline
              items={approvals.map((item, index) => ({
                color: index === approvals.length - 1 ? 'blue' : 'green',
                dot:
                  index === approvals.length - 1 ? (
                    <ClockCircleOutlined className="text-lg" />
                  ) : (
                    <CheckCircleOutlined className="text-lg" />
                  ),
                children: (
                  <div className="mb-4">
                    <p className="g-text-primary font-bold mb-1">
                      {item.actionName || item.actionType || '审批动作'}
                    </p>
                    <p className="g-text-secondary text-sm">
                      {item.operatorName || '系统'}{' '}
                      <span className="ml-4">{item.operateTime || '-'}</span>
                    </p>
                    <div className="bg-white p-2 rounded mt-2 g-text-secondary text-sm border g-border-panel border">
                      {item.remark || '无审批意见'}
                    </div>
                  </div>
                ),
              }))}
            />
          ) : (
            <Empty description="暂无审批记录" />
          )}
        </Card>
      ),
    },
    {
      key: 'payments',
      label: '入账与发票',
      children: (
        <div className="space-y-6">
          <Card
            title="入账记录"
            className="glass-panel g-border-panel border"
            extra={<Button type="primary" size="small" onClick={() => navigate('/contracts/payments?contractId=' + id)}>新增入账</Button>}
          >
            <Table
              dataSource={receipts}
              rowKey="id"
              locale={{ emptyText: <Empty description="暂无入账记录" /> }}
              columns={[
                { title: '入账流水号', dataIndex: 'receiptNo', key: 'receiptNo' },
                { title: '入账日期', dataIndex: 'receiptDate', key: 'receiptDate' },
                {
                  title: '入账金额',
                  dataIndex: 'amount',
                  key: 'amount',
                  render: (value) => <span className="g-text-success font-bold">{formatMoney(value)}</span>,
                },
                { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo' },
                { title: '银行流水', dataIndex: 'bankFlowNo', key: 'bankFlowNo' },
                { title: '状态', dataIndex: 'status', key: 'status', render: (value) => <Tag>{value || '未知'}</Tag> },
              ]}
              pagination={false}
            />
          </Card>

          <Card title="发票记录" className="glass-panel g-border-panel border">
            <Table
              dataSource={invoices}
              rowKey="id"
              locale={{ emptyText: <Empty description="暂无发票记录" /> }}
              columns={[
                { title: '发票编号', dataIndex: 'invoiceNo', key: 'invoiceNo' },
                { title: '发票代码', dataIndex: 'invoiceCode', key: 'invoiceCode' },
                { title: '发票号码', dataIndex: 'invoiceNumber', key: 'invoiceNumber' },
                {
                  title: '金额',
                  dataIndex: 'amount',
                  key: 'amount',
                  render: (value) => formatMoney(value),
                },
                { title: '发票类型', dataIndex: 'invoiceType', key: 'invoiceType' },
                { title: '开票日期', dataIndex: 'invoiceDate', key: 'invoiceDate' },
              ]}
              pagination={false}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'docs',
      label: '办事材料',
      children: (
        <div className="space-y-6">
          <Card className="glass-panel g-border-panel border">
            <List
              locale={{ emptyText: <Empty description="暂无办事材料" /> }}
              dataSource={materials}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <a key="preview" onClick={() => void handlePreviewMaterial(item)}>预览</a>,
                    <a key="download" onClick={() => void handleDownloadMaterial(item)}>
                      <DownloadOutlined /> 下载
                    </a>,
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <span className="g-text-primary">{item.materialName}</span>
                        <Tag>{item.materialType || '材料'}</Tag>
                      </Space>
                    }
                    description={
                      <span className="g-text-secondary">
                        上传时间: {item.uploadTime || '-'} {item.remark ? '| ' + item.remark : ''}
                      </span>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card title="历史领票记录" className="glass-panel g-border-panel border">
            <Table
              dataSource={tickets}
              rowKey="id"
              locale={{ emptyText: <Empty description="暂无票据记录" /> }}
              columns={[
                { title: '票据编号', dataIndex: 'ticketNo', key: 'ticketNo' },
                { title: '票据类型', dataIndex: 'ticketType', key: 'ticketType' },
                { title: '票据日期', dataIndex: 'ticketDate', key: 'ticketDate' },
                { title: '方量', dataIndex: 'volume', key: 'volume' },
                {
                  title: '金额',
                  dataIndex: 'amount',
                  key: 'amount',
                  render: (value) => formatMoney(value),
                },
                { title: '状态', dataIndex: 'status', key: 'status', render: (value) => <Tag>{value || '未知'}</Tag> },
              ]}
              pagination={false}
            />
          </Card>
        </div>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6 pb-10"
    >
      <div className="flex items-center gap-4 mb-6">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/contracts')}
          className="g-text-secondary hover:g-text-primary"
        />
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">
            合同详情: {detail?.contractNo || 'HT-' + String(detail?.id || '')}
          </h1>
          <p className="g-text-secondary mt-1">
            {(detail?.projectName || '-') + ' - ' + (detail?.siteName || '-')}
          </p>
        </div>

      <Spin spinning={loading}>
        {detail ? (
          <Tabs
            defaultActiveKey={paymentTab === 'payment' ? 'payments' : 'info'}
            items={items}
            className="custom-tabs"
          />
        ) : (
          <Card className="glass-panel g-border-panel border">
            <Empty description="合同不存在或暂无数据" />
          </Card>
        )}
      </Spin>
      <Modal
        title={detail ? `驳回合同 · ${detail.contractNo}` : '驳回合同'}
        open={rejectOpen}
        confirmLoading={actionLoading}
        onOk={() => void handleReject()}
        onCancel={() => {
          setRejectOpen(false);
          rejectForm.resetFields();
        }}
      >
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="reason"
            label="驳回原因"
            rules={[{ required: true, message: '请输入驳回原因' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入需要退回修改的说明" maxLength={200} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </motion.div>
    </div>
  );
};

export default ContractDetail;
