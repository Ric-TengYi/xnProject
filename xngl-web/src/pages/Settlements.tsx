import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
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
  CalculatorOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import type { Dayjs } from 'dayjs';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';
import {
  approveSettlement,
  fetchSettlementDetail,
  fetchSettlementList,
  fetchSettlementStats,
  generateProjectSettlement,
  generateSiteSettlement,
  rejectSettlement,
  submitSettlement,
  type SettlementDetail,
  type SettlementLine,
  type SettlementRecord,
  type SettlementStats,
} from '../utils/settlementApi';

const { RangePicker } = DatePicker;

type SettlementTypeValue = 'all' | 'PROJECT' | 'SITE';
type StatusValue = 'all' | 'DRAFT' | 'APPROVING' | 'SETTLED' | 'REJECTED';

interface GenerateFormValues {
  settlementType: 'PROJECT' | 'SITE';
  targetId: string;
  period: [Dayjs, Dayjs];
  remark?: string;
}

const settlementTypeLabelMap: Record<string, string> = {
  PROJECT: '按项目结算',
  SITE: '按场地结算',
};

const settlementStatusLabelMap: Record<string, string> = {
  DRAFT: '草稿',
  APPROVING: '审批中',
  SETTLED: '已结算',
  REJECTED: '已驳回',
};

const approvalStatusLabelMap: Record<string, string> = {
  NOT_SUBMITTED: '未提交',
  APPROVING: '审批中',
  APPROVED: '已审批',
  REJECTED: '已驳回',
};

const settlementStatusColorMap: Record<string, string> = {
  DRAFT: 'default',
  APPROVING: 'processing',
  SETTLED: 'success',
  REJECTED: 'error',
};

const approvalStatusColorMap: Record<string, string> = {
  NOT_SUBMITTED: 'default',
  APPROVING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
};

const settlementTypeOptions = [
  { label: '全部结算类型', value: 'all' },
  { label: '按项目结算', value: 'PROJECT' },
  { label: '按场地结算', value: 'SITE' },
];

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '草稿', value: 'DRAFT' },
  { label: '审批中', value: 'APPROVING' },
  { label: '已结算', value: 'SETTLED' },
  { label: '已驳回', value: 'REJECTED' },
];

const emptyStats: SettlementStats = {
  pendingAmount: 0,
  settledAmount: 0,
  totalOrders: 0,
  draftOrders: 0,
  pendingOrders: 0,
  settledOrders: 0,
};

const formatNumber = (value?: number | null) => Number(value || 0).toLocaleString();
const formatCurrency = (value?: number | null) => `¥ ${formatNumber(value)}`;
const formatPeriod = (start?: string | null, end?: string | null) =>
  start || end ? `${start || '-'} ~ ${end || '-'}` : '-';

const Settlements: React.FC = () => {
  const [form] = Form.useForm<GenerateFormValues>();
  const [rejectForm] = Form.useForm<{ reason?: string }>();
  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [masterLoading, setMasterLoading] = useState(false);
  const [records, setRecords] = useState<SettlementRecord[]>([]);
  const [stats, setStats] = useState<SettlementStats>(emptyStats);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [settlementType, setSettlementType] = useState<SettlementTypeValue>('all');
  const [status, setStatus] = useState<StatusValue>('all');
  const [targetId, setTargetId] = useState<string | undefined>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedDetail, setSelectedDetail] = useState<SettlementDetail | null>(null);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activeActionId, setActiveActionId] = useState<string | null>(null);
  const [rejectingRecord, setRejectingRecord] = useState<SettlementRecord | null>(null);
  const generateSettlementType = Form.useWatch('settlementType', form) || 'SITE';

  const projectNameMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [String(item.id), item.name])),
    [projects]
  );
  const siteNameMap = useMemo(
    () => Object.fromEntries(sites.map((item) => [String(item.id), item.name])),
    [sites]
  );

  const currentTargetOptions = useMemo(() => {
    if (settlementType === 'PROJECT') {
      return projects.map((item) => ({ label: item.name, value: String(item.id) }));
    }
    if (settlementType === 'SITE') {
      return sites.map((item) => ({ label: item.name, value: String(item.id) }));
    }
    return [];
  }, [projects, settlementType, sites]);

  const generateTargetOptions = useMemo(() => {
    if (generateSettlementType === 'PROJECT') {
      return projects.map((item) => ({ label: item.name, value: String(item.id) }));
    }
    return sites.map((item) => ({ label: item.name, value: String(item.id) }));
  }, [generateSettlementType, projects, sites]);

  const resolveTargetName = (record: SettlementRecord) => {
    if (record.settlementType === 'PROJECT') {
      return (
        record.targetProjectName ||
        projectNameMap[String(record.targetProjectId || '')] ||
        `项目#${record.targetProjectId || '-'}`
      );
    }
    return (
      record.targetSiteName ||
      siteNameMap[String(record.targetSiteId || '')] ||
      `场地#${record.targetSiteId || '-'}`
    );
  };

  const loadStats = async () => {
    setStatsLoading(true);
    try {
      const data = await fetchSettlementStats();
      setStats({ ...emptyStats, ...data });
    } catch (error) {
      console.error(error);
      setStats(emptyStats);
    } finally {
      setStatsLoading(false);
    }
  };

  const loadMasters = async () => {
    setMasterLoading(true);
    try {
      const [projectPage, siteList] = await Promise.all([
        fetchProjects({ pageNo: 1, pageSize: 200 }),
        fetchSites(),
      ]);
      setProjects(projectPage.records || []);
      setSites(siteList || []);
    } catch (error) {
      console.error(error);
      message.error('加载项目/场地主数据失败');
      setProjects([]);
      setSites([]);
    } finally {
      setMasterLoading(false);
    }
  };

  const loadSettlements = async () => {
    setLoading(true);
    try {
      const page = await fetchSettlementList({
        settlementType: settlementType === 'all' ? undefined : settlementType,
        status: status === 'all' ? undefined : status,
        projectId: settlementType === 'PROJECT' ? targetId : undefined,
        siteId: settlementType === 'SITE' ? targetId : undefined,
        pageNo,
        pageSize,
      });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadMasters();
    void loadStats();
  }, []);

  useEffect(() => {
    void loadSettlements();
  }, [pageNo, pageSize, settlementType, status, targetId]);

  const openDetail = async (id: string) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const detail = await fetchSettlementDetail(id);
      setSelectedDetail(detail);
    } catch (error) {
      console.error(error);
      setDetailOpen(false);
      setSelectedDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshAll = async () => {
    await Promise.all([loadSettlements(), loadStats()]);
  };

  const handleSubmitApproval = async (record: SettlementRecord) => {
    setActiveActionId(record.id);
    try {
      await submitSettlement(record.id);
      message.success('结算单已提交审批');
      await refreshAll();
      if (selectedDetail?.id === record.id) {
        const detail = await fetchSettlementDetail(record.id);
        setSelectedDetail(detail);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setActiveActionId(null);
    }
  };

  const handleApprove = async (record: SettlementRecord | SettlementDetail) => {
    setActiveActionId(record.id);
    try {
      await approveSettlement(record.id);
      message.success('结算单已审批通过');
      await refreshAll();
      if (detailOpen) {
        const detail = await fetchSettlementDetail(record.id);
        setSelectedDetail(detail);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setActiveActionId(null);
    }
  };

  const handleOpenReject = (record: SettlementRecord | SettlementDetail) => {
    setRejectingRecord({
      id: record.id,
      settlementNo: record.settlementNo,
      settlementType: record.settlementType,
      targetProjectId: record.targetProjectId,
      targetProjectName: record.targetProjectName,
      targetSiteId: record.targetSiteId,
      targetSiteName: record.targetSiteName,
      periodStart: record.periodStart,
      periodEnd: record.periodEnd,
      totalVolume: record.totalVolume,
      totalAmount: record.totalAmount,
      adjustAmount: record.adjustAmount,
      payableAmount: record.payableAmount,
      approvalStatus: record.approvalStatus,
      settlementStatus: record.settlementStatus,
      creatorId: record.creatorId,
      createTime: record.createTime,
    });
    rejectForm.resetFields();
    setRejectOpen(true);
  };

  const handleReject = async () => {
    if (!rejectingRecord) {
      return;
    }
    setSubmitting(true);
    try {
      const values = await rejectForm.validateFields();
      await rejectSettlement(rejectingRecord.id, values.reason);
      message.success('结算单已驳回');
      setRejectOpen(false);
      setRejectingRecord(null);
      await refreshAll();
      if (detailOpen) {
        const detail = await fetchSettlementDetail(rejectingRecord.id);
        setSelectedDetail(detail);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleGenerate = async () => {
    setSubmitting(true);
    try {
      const values = await form.validateFields();
      const payload = {
        targetId: Number(values.targetId),
        periodStart: values.period[0].format('YYYY-MM-DD'),
        periodEnd: values.period[1].format('YYYY-MM-DD'),
        remark: values.remark?.trim() || undefined,
      };
      const id =
        values.settlementType === 'PROJECT'
          ? await generateProjectSettlement(payload)
          : await generateSiteSettlement(payload);
      message.success('结算单已生成');
      setGenerateOpen(false);
      form.resetFields();
      setPageNo(1);
      await refreshAll();
      if (id) {
        await openDetail(String(id));
      }
    } catch (error) {
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  const lineColumns: ColumnsType<SettlementLine> = [
    { title: '业务日期', dataIndex: 'bizDate', key: 'bizDate', render: (value) => value || '-' },
    {
      title: '来源类型',
      dataIndex: 'sourceRecordType',
      key: 'sourceRecordType',
      render: (value) => value || '-',
    },
    { title: '项目ID', dataIndex: 'projectId', key: 'projectId', render: (value) => value || '-' },
    { title: '场地ID', dataIndex: 'siteId', key: 'siteId', render: (value) => value || '-' },
    { title: '车辆ID', dataIndex: 'vehicleId', key: 'vehicleId', render: (value) => value || '-' },
    {
      title: '方量(m3)',
      dataIndex: 'volume',
      key: 'volume',
      render: (value) => formatNumber(value),
    },
    {
      title: '单价(元)',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      render: (value) => formatCurrency(value),
    },
    {
      title: '金额(元)',
      dataIndex: 'amount',
      key: 'amount',
      render: (value) => formatCurrency(value),
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', render: (value) => value || '-' },
  ];

  const columns: ColumnsType<SettlementRecord> = [
    {
      title: '结算单号',
      dataIndex: 'settlementNo',
      key: 'settlementNo',
      render: (value, record) => (
        <Button type="link" className="px-0 font-mono" onClick={() => void openDetail(record.id)}>
          {value}
        </Button>
      ),
    },
    {
      title: '结算类型',
      dataIndex: 'settlementType',
      key: 'settlementType',
      render: (value?: string | null) => (
        <Tag color="blue" className="border-none">
          {settlementTypeLabelMap[value || ''] || value || '-'}
        </Tag>
      ),
    },
    {
      title: '结算对象',
      key: 'target',
      render: (_, record) => (
        <div className="flex flex-col gap-1">
          <strong style={{ color: 'var(--text-primary)' }}>{resolveTargetName(record)}</strong>
          <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
            {record.settlementType === 'PROJECT'
              ? `项目ID：${record.targetProjectId || '-'}`
              : `场地ID：${record.targetSiteId || '-'}`
            }
          </span>
        </div>
      ),
    },
    {
      title: '结算周期',
      key: 'period',
      render: (_, record) => (
        <span style={{ color: 'var(--text-secondary)' }}>
          {formatPeriod(record.periodStart, record.periodEnd)}
        </span>
      ),
    },
    {
      title: '消纳量(m3)',
      dataIndex: 'totalVolume',
      key: 'totalVolume',
      render: (value?: number | null) => formatNumber(value),
    },
    {
      title: '应付金额(元)',
      dataIndex: 'payableAmount',
      key: 'payableAmount',
      render: (value?: number | null) => (
        <span style={{ color: 'var(--success)', fontWeight: 600 }}>{formatCurrency(value)}</span>
      ),
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) => (
        <div className="flex flex-col gap-1">
          <Tag
            color={settlementStatusColorMap[record.settlementStatus || ''] || 'default'}
            className="border-none w-fit"
          >
            {settlementStatusLabelMap[record.settlementStatus || ''] ||
              record.settlementStatus ||
              '-'}
          </Tag>
          <Tag
            color={approvalStatusColorMap[record.approvalStatus || ''] || 'default'}
            className="border-none w-fit"
          >
            {approvalStatusLabelMap[record.approvalStatus || ''] || record.approvalStatus || '-'}
          </Tag>
        </div>
      ),
    },
    {
      title: '生成时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" className="px-0" onClick={() => void openDetail(record.id)}>
            查看详情
          </Button>
          {record.settlementStatus === 'DRAFT' ? (
            <Popconfirm
              title="确认提交审批？"
              onConfirm={() => void handleSubmitApproval(record)}
            >
              <Button
                type="link"
                className="px-0"
                loading={activeActionId === record.id}
              >
                提交审批
              </Button>
            </Popconfirm>
          ) : null}
          {record.approvalStatus === 'APPROVING' ? (
            <>
              <Popconfirm title="确认审批通过？" onConfirm={() => void handleApprove(record)}>
                <Button
                  type="link"
                  className="px-0"
                  loading={activeActionId === record.id}
                >
                  审批通过
                </Button>
              </Popconfirm>
              <Button type="link" danger className="px-0" onClick={() => handleOpenReject(record)}>
                驳回
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  const detailExtra =
    selectedDetail && !detailLoading ? (
      <Space>
        {selectedDetail.settlementStatus === 'DRAFT' ? (
          <Popconfirm
            title="确认提交审批？"
            onConfirm={() => void handleSubmitApproval(selectedDetail)}
          >
            <Button loading={activeActionId === selectedDetail.id}>提交审批</Button>
          </Popconfirm>
        ) : null}
        {selectedDetail.approvalStatus === 'APPROVING' ? (
          <>
            <Popconfirm title="确认审批通过？" onConfirm={() => void handleApprove(selectedDetail)}>
              <Button type="primary" loading={activeActionId === selectedDetail.id}>
                审批通过
              </Button>
            </Popconfirm>
            <Button danger onClick={() => handleOpenReject(selectedDetail)}>
              驳回
            </Button>
          </>
        ) : null}
      </Space>
    ) : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">结算管理</h1>
          <p className="g-text-secondary mt-1">
            对接真实结算单数据，支持项目/场地结算生成、审批流转与明细追踪
          </p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => void refreshAll()}>
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            loading={masterLoading}
            className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]"
            onClick={() => {
              form.setFieldsValue({ settlementType: 'SITE' });
              setGenerateOpen(true);
            }}
          >
            发起新结算
          </Button>
        </Space>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              loading={statsLoading}
              title={<span className="g-text-secondary">待结算金额 (元)</span>}
              value={Number(stats.pendingAmount || 0)}
              valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              loading={statsLoading}
              title={<span className="g-text-secondary">已结算金额 (元)</span>}
              value={Number(stats.settledAmount || 0)}
              valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              loading={statsLoading}
              title={<span className="g-text-secondary">结算单总数 (张)</span>}
              value={Number(stats.totalOrders || 0)}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={<CalculatorOutlined className="g-text-primary-link" />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              loading={statsLoading}
              title={<span className="g-text-secondary">草稿 / 审批中</span>}
              value={`${Number(stats.draftOrders || 0)} / ${Number(stats.pendingOrders || 0)}`}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
            />
          </Card>
        </Col>
      </Row>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
          <Select
            value={settlementType}
            className="w-40"
            options={settlementTypeOptions}
            onChange={(value: SettlementTypeValue) => {
              setSettlementType(value);
              setTargetId(undefined);
              setPageNo(1);
            }}
          />
          <Select
            value={status}
            className="w-40"
            options={statusOptions}
            onChange={(value: StatusValue) => {
              setStatus(value);
              setPageNo(1);
            }}
          />
          <Select
            allowClear
            value={targetId}
            className="min-w-56"
            placeholder={settlementType === 'all' ? '请先选择结算类型' : '请选择结算对象'}
            disabled={settlementType === 'all'}
            options={currentTargetOptions}
            onChange={(value) => {
              setTargetId(value);
              setPageNo(1);
            }}
          />
          <div className="flex-1 flex justify-end items-center">
            <span className="text-sm g-text-secondary">
              当前共 {total} 条，已接入真实后端结算数据
            </span>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: <Empty description="暂无结算单数据" /> }}
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
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>

      <Drawer
        title={selectedDetail ? `结算单详情 · ${selectedDetail.settlementNo}` : '结算单详情'}
        width={1080}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setSelectedDetail(null);
        }}
        extra={detailExtra}
      >
        {selectedDetail && !detailLoading ? (
          <div className="space-y-6">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={12} xl={8}>
                <Card size="small" title="结算基础信息">
                  <div className="space-y-2 text-sm">
                    <div>结算类型：{settlementTypeLabelMap[selectedDetail.settlementType] || '-'}</div>
                    <div>结算对象：{resolveTargetName(selectedDetail)}</div>
                    <div>结算周期：{formatPeriod(selectedDetail.periodStart, selectedDetail.periodEnd)}</div>
                    <div>结算日期：{selectedDetail.settlementDate || '-'}</div>
                    <div>创建时间：{selectedDetail.createTime || '-'}</div>
                  </div>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={8}>
                <Card size="small" title="金额信息">
                  <div className="space-y-2 text-sm">
                    <div>消纳量：{formatNumber(selectedDetail.totalVolume)} m3</div>
                    <div>单价：{formatCurrency(selectedDetail.unitPrice)}</div>
                    <div>结算金额：{formatCurrency(selectedDetail.totalAmount)}</div>
                    <div>调整金额：{formatCurrency(selectedDetail.adjustAmount)}</div>
                    <div>应付金额：{formatCurrency(selectedDetail.payableAmount)}</div>
                  </div>
                </Card>
              </Col>
              <Col xs={24} md={24} xl={8}>
                <Card size="small" title="审批状态">
                  <div className="space-y-3 text-sm">
                    <div>
                      结算状态：
                      <Tag
                        color={
                          settlementStatusColorMap[selectedDetail.settlementStatus || ''] || 'default'
                        }
                        className="ml-2 border-none"
                      >
                        {settlementStatusLabelMap[selectedDetail.settlementStatus || ''] ||
                          selectedDetail.settlementStatus ||
                          '-'}
                      </Tag>
                    </div>
                    <div>
                      审批状态：
                      <Tag
                        color={
                          approvalStatusColorMap[selectedDetail.approvalStatus || ''] || 'default'
                        }
                        className="ml-2 border-none"
                      >
                        {approvalStatusLabelMap[selectedDetail.approvalStatus || ''] ||
                          selectedDetail.approvalStatus ||
                          '-'}
                      </Tag>
                    </div>
                    <div>流程实例：{selectedDetail.processInstanceId || '-'}</div>
                    <div>备注：{selectedDetail.remark || '-'}</div>
                  </div>
                </Card>
              </Col>
            </Row>

            <Card size="small" title={`结算明细 (${selectedDetail.items?.length || 0})`}>
              <Table
                columns={lineColumns}
                dataSource={selectedDetail.items || []}
                rowKey="id"
                pagination={false}
                locale={{ emptyText: <Empty description="暂无结算明细行" /> }}
                scroll={{ x: 960 }}
              />
            </Card>
          </div>
        ) : (
          <div className="py-10 text-center g-text-secondary">正在加载结算单详情...</div>
        )}
      </Drawer>

      <Modal
        title="发起结算"
        open={generateOpen}
        confirmLoading={submitting}
        onOk={() => void handleGenerate()}
        onCancel={() => {
          setGenerateOpen(false);
          form.resetFields();
        }}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ settlementType: 'SITE' }}
          onValuesChange={(changedValues) => {
            if ('settlementType' in changedValues) {
              form.setFieldValue('targetId', undefined);
            }
          }}
        >
          <Form.Item
            label="结算类型"
            name="settlementType"
            rules={[{ required: true, message: '请选择结算类型' }]}
          >
            <Select
              options={settlementTypeOptions.filter((item) => item.value !== 'all')}
            />
          </Form.Item>
          <Form.Item
            label="结算对象"
            name="targetId"
            rules={[{ required: true, message: '请选择结算对象' }]}
          >
            <Select
              showSearch
              placeholder="请选择项目或场地"
              loading={masterLoading}
              options={generateTargetOptions}
              filterOption={(input, option) =>
                String(option?.label || '')
                  .toLowerCase()
                  .includes(input.toLowerCase())
              }
            />
          </Form.Item>
          <Form.Item
            label="结算周期"
            name="period"
            rules={[{ required: true, message: '请选择结算周期' }]}
          >
            <RangePicker className="w-full" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={4} placeholder="可填写结算说明、口径备注等" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          rejectingRecord ? `驳回结算单 · ${rejectingRecord.settlementNo}` : '驳回结算单'
        }
        open={rejectOpen}
        confirmLoading={submitting}
        onOk={() => void handleReject()}
        onCancel={() => {
          setRejectOpen(false);
          setRejectingRecord(null);
          rejectForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            label="驳回原因"
            name="reason"
            rules={[{ required: true, message: '请填写驳回原因' }]}
          >
            <Input.TextArea rows={4} maxLength={200} placeholder="请输入审批驳回原因" />
          </Form.Item>
        </Form>
      </Modal>
    </motion.div>
  );
};

export default Settlements;
