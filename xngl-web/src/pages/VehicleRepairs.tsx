import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
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
import { PlusOutlined, SearchOutlined, ToolFilled } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import {
  approveVehicleRepair,
  completeVehicleRepair,
  createVehicleRepair,
  deleteVehicleRepair,
  exportVehicleRepairs,
  fetchVehicleRepairDetail,
  fetchVehicleRepairSummary,
  fetchVehicleRepairs,
  rejectVehicleRepair,
  updateVehicleRepair,
  type VehicleRepairCompletePayload,
  type VehicleRepairOrderRecord,
  type VehicleRepairSummaryRecord,
  type VehicleRepairUpsertPayload,
} from '../utils/vehicleRepairApi';
import { fetchVehicleCompanyCapacity, fetchVehicles } from '../utils/vehicleApi';

const { RangePicker } = DatePicker;

type RepairFormValues = {
  vehicleId: string;
  urgencyLevel?: string;
  repairReason: string;
  repairContent?: string;
  diagnosisResult?: string;
  safetyImpact?: string;
  budgetAmount?: number;
  applyDate?: Dayjs;
  applicantName?: string;
  status?: string;
  remark?: string;

type AuditFormValues = {
  comment?: string;

type CompleteFormValues = {
  completedDate: Dayjs;
  vendorName?: string;
  repairManager?: string;
  technicianName?: string;
  acceptanceResult?: string;
  signoffStatus?: string;
  attachmentUrls?: string;
  actualAmount?: number;
  partsCost?: number;
  laborCost?: number;
  otherCost?: number;
  remark?: string;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '已批准', value: 'APPROVED' },
  { label: '维修中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '草稿', value: 'DRAFT' },
];

const urgencyOptions = [
  { label: '全部紧急度', value: 'all' },
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];

const statusColorMap: Record<string, string> = {
  PENDING_APPROVAL: 'warning',
  APPROVED: 'processing',
  IN_PROGRESS: 'processing',
  COMPLETED: 'success',
  REJECTED: 'error',
  DRAFT: 'default',

const urgencyColorMap: Record<string, string> = {
  HIGH: 'error',
  MEDIUM: 'warning',
  LOW: 'default',

const defaultSummary: VehicleRepairSummaryRecord = {
  totalOrders: 0,
  pendingOrders: 0,
  approvedOrders: 0,
  inProgressOrders: 0,
  completedOrders: 0,
  totalBudgetAmount: 0,
  totalActualAmount: 0,

const VehicleRepairs: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<VehicleRepairOrderRecord[]>([]);
  const [summary, setSummary] = useState<VehicleRepairSummaryRecord>(defaultSummary);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('all');
  const [urgencyLevel, setUrgencyLevel] = useState('all');

  const queryParams = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      status: status === 'all' ? undefined : status,
      urgencyLevel: urgencyLevel === 'all' ? undefined : urgencyLevel,
      orgId,
      vehicleId,
      applyDateFrom: applyDateRange?.[0]?.format('YYYY-MM-DD'),
      applyDateTo: applyDateRange?.[1]?.format('YYYY-MM-DD'),
      completedDateFrom: completedDateRange?.[0]?.format('YYYY-MM-DD'),
      completedDateTo: completedDateRange?.[1]?.format('YYYY-MM-DD'),
    }),
    [keyword, status, urgencyLevel, orgId, vehicleId, applyDateRange, completedDateRange]
  );

  const loadList = async () => {
    setLoading(true);
    try {
      const page = await fetchVehicleRepairs({ ...queryParams, pageNo, pageSize });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取维修单失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchVehicleRepairSummary(queryParams));
    } catch (error) {
      console.error(error);
      message.error('获取维修统计失败');
      setSummary(defaultSummary);
    } finally {
      setSummaryLoading(false);
    }
  };

  useEffect(() => {
    void loadSummary();
  }, [queryParams]);

  useEffect(() => {
    void loadList();
  }, [pageNo, pageSize, queryParams]);

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [companies, vehiclesPage] = await Promise.all([
          fetchVehicleCompanyCapacity(),
          fetchVehicles({ pageNo: 1, pageSize: 200 }),
        ]);
        setCompanyOptions(
          companies
            .filter((item) => item.orgId)
            .map((item) => ({ label: item.orgName, value: item.orgId as string }))
        );
        setVehicleOptions(
          (vehiclesPage.records || []).map((item) => ({
            label: `${item.plateNo} / ${item.orgName || '未归属单位'}`,
            value: item.id,
          }))
        );
      } catch (error) {
        console.error(error);
      }
    };

    void loadOptions();
  }, []);

  const handleReset = () => {
    setKeyword('');
    setStatus('all');
    setUrgencyLevel('all');
    setOrgId(undefined);
    setVehicleId(undefined);
    setApplyDateRange(null);
    setCompletedDateRange(null);
    setPageNo(1);
  };

  const handleExport = async () => {
    try {
      const blob = await exportVehicleRepairs(queryParams);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'vehicle_repairs.csv';
      link.click();
      window.URL.revokeObjectURL(url);
      message.success('维修台账已导出');
    } catch (error) {
      console.error(error);
    }
  };

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      urgencyLevel: 'MEDIUM',
      applyDate: dayjs(),
      budgetAmount: 0,
      status: 'PENDING_APPROVAL',
    });
    setEditorOpen(true);
  };

  const openEdit = (record: VehicleRepairOrderRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      vehicleId: record.vehicleId || undefined,
      urgencyLevel: record.urgencyLevel || 'MEDIUM',
      repairReason: record.repairReason || undefined,
      repairContent: record.repairContent || undefined,
      diagnosisResult: record.diagnosisResult || undefined,
      safetyImpact: record.safetyImpact || undefined,
      budgetAmount: record.budgetAmount,
      applyDate: record.applyDate ? dayjs(record.applyDate) : undefined,
      applicantName: record.applicantName || undefined,
      status: record.status,
      remark: record.remark || undefined,
    });
    setEditorOpen(true);
  };

  const openAudit = (record: VehicleRepairOrderRecord, action: 'approve' | 'reject') => {
    setCurrentRecord(record);
    setAuditAction(action);
    auditForm.resetFields();
    auditForm.setFieldsValue({
      comment: action === 'approve' ? '同意维修' : '请补充维修说明后重新提交',
    });
    setAuditOpen(true);
  };

  const openComplete = (record: VehicleRepairOrderRecord) => {
    setCurrentRecord(record);
    completeForm.resetFields();
    completeForm.setFieldsValue({
      completedDate: dayjs(),
      vendorName: record.vendorName || undefined,
      actualAmount: record.actualAmount || record.budgetAmount,
      repairManager: record.repairManager || record.applicantName || undefined,
      technicianName: record.technicianName || undefined,
      acceptanceResult: record.acceptanceResult || undefined,
      signoffStatus: record.signoffStatus || 'SIGNED',
      attachmentUrls: record.attachmentUrls || undefined,
      partsCost: record.partsCost || 0,
      laborCost: record.laborCost || 0,
      otherCost: record.otherCost || 0,
      remark: record.remark || undefined,
    });
    setCompleteOpen(true);
  };

  const openDetail = async (record: VehicleRepairOrderRecord) => {
    try {
      setDetailRecord(await fetchVehicleRepairDetail(record.id));
      setDetailOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取维修单详情失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: VehicleRepairUpsertPayload = {
        vehicleId: Number(values.vehicleId),
        urgencyLevel: values.urgencyLevel,
        repairReason: values.repairReason,
        repairContent: values.repairContent,
        diagnosisResult: values.diagnosisResult,
        safetyImpact: values.safetyImpact,
        budgetAmount: values.budgetAmount,
        applyDate: values.applyDate?.format('YYYY-MM-DD'),
        applicantName: values.applicantName,
        status: values.status,
        remark: values.remark,
      };
      setSubmitLoading(true);
      if (editingRecord) {
        await updateVehicleRepair(editingRecord.id, payload);
        message.success('维修单已更新');
      } else {
        await createVehicleRepair(payload);
        message.success('维修单已新增');
      }
      setEditorOpen(false);
      setPageNo(1);
      await Promise.all([loadSummary(), loadList()]);
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleAudit = async () => {
    if (!currentRecord) {
      return;
    }
    try {
      const values = await auditForm.validateFields();
      setSubmitLoading(true);
      if (auditAction === 'approve') {
        await approveVehicleRepair(currentRecord.id, { comment: values.comment });
        message.success('维修单已批准');
      } else {
        await rejectVehicleRepair(currentRecord.id, { comment: values.comment });
        message.success('维修单已驳回');
      }
      setAuditOpen(false);
      await Promise.all([loadSummary(), loadList()]);
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleComplete = async () => {
    if (!currentRecord) {
      return;
    }
    try {
      const values = await completeForm.validateFields();
      const payload: VehicleRepairCompletePayload = {
        completedDate: values.completedDate?.format('YYYY-MM-DD'),
        vendorName: values.vendorName,
        repairManager: values.repairManager,
        technicianName: values.technicianName,
        acceptanceResult: values.acceptanceResult,
        signoffStatus: values.signoffStatus,
        attachmentUrls: values.attachmentUrls,
        actualAmount: values.actualAmount,
        partsCost: values.partsCost,
        laborCost: values.laborCost,
        otherCost: values.otherCost,
        remark: values.remark,
      };
      setSubmitLoading(true);
      await completeVehicleRepair(currentRecord.id, payload);
      message.success('维修单已完成');
      setCompleteOpen(false);
      await Promise.all([loadSummary(), loadList()]);
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteVehicleRepair(id);
      message.success('维修单已删除');
      setPageNo(1);
      await Promise.all([loadSummary(), loadList()]);
    } catch (error) {
      console.error(error);
      message.error((error as any)?.response?.data?.message || '删除维修单失败');
    }
  };

  const columns: ColumnsType<VehicleRepairOrderRecord> = [
    {
      title: '维修单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      render: (value: string) => <span className="font-mono g-text-secondary">{value}</span>,
    },
    {
      title: '车牌号',
      dataIndex: 'plateNo',
      key: 'plateNo',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '所属单位',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value?: string | null) => value || '未归属单位',
    },
    {
      title: '紧急度',
      dataIndex: 'urgencyLabel',
      key: 'urgencyLabel',
      render: (_, record) => <Tag color={urgencyColorMap[record.urgencyLevel || ''] || 'default'}>{record.urgencyLabel || '-'}</Tag>,
    },
    {
      title: '维修原因',
      dataIndex: 'repairReason',
      key: 'repairReason',
      ellipsis: true,
      render: (value?: string | null) => value || '-',
    },
    {
      title: '申请日期',
      dataIndex: 'applyDate',
      key: 'applyDate',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '预算/实际(元)',
      key: 'amount',
      render: (_, record) => `${record.budgetAmount.toFixed(2)} / ${record.actualAmount.toFixed(2)}`,
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (_, record) => <Tag color={statusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => void openDetail(record)}>
            详情
          </Button>
          {record.status !== 'COMPLETED' ? (
            <Button type="link" size="small" onClick={() => openEdit(record)}>
              编辑
            </Button>
          ) : null}
          {['PENDING_APPROVAL', 'DRAFT'].includes(record.status) ? (
            <>
              <Button type="link" size="small" onClick={() => openAudit(record, 'approve')}>
                批准
              </Button>
              <Button type="link" size="small" danger onClick={() => openAudit(record, 'reject')}>
                驳回
              </Button>
            </>
          ) : null}
          {!['COMPLETED', 'REJECTED'].includes(record.status) ? (
            <Button type="link" size="small" onClick={() => openComplete(record)}>
              完成维修
            </Button>
          ) : null}
          {record.status !== 'COMPLETED' ? (
            <Popconfirm title="确认删除当前维修单？" onConfirm={() => void handleDelete(record.id)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">维修管理</h1>
          <p className="g-text-secondary mt-1">覆盖车辆维修申请、审批、完工和费用回填。</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增维修单
        </Button>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="维修单总数" value={summary.totalOrders} prefix={<ToolFilled />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="待审批" value={summary.pendingOrders} prefix={<ToolFilled />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="已完成" value={summary.completedOrders} prefix={<ToolFilled />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="实际费用(元)" value={summary.totalActualAmount} precision={2} prefix={<ToolFilled />} />
          </Card>
        </Col>
      </Row>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap gap-4 items-center mb-4">
          <Input
            allowClear
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPageNo(1);
            }}
            placeholder="搜索维修单号/车牌/单位/申请人"
            prefix={<SearchOutlined />}
            style={{ width: 260 }}
          />
          <Select value={status} onChange={(value) => { setStatus(value); setPageNo(1); }} options={statusOptions} style={{ width: 160 }} />
          <Select value={urgencyLevel} onChange={(value) => { setUrgencyLevel(value); setPageNo(1); }} options={urgencyOptions} style={{ width: 160 }} />
          <Select allowClear placeholder="所属单位" value={orgId} onChange={(value) => { setOrgId(value); setPageNo(1); }} options={companyOptions} style={{ width: 220 }} />
          <Select allowClear placeholder="车辆" value={vehicleId} onChange={(value) => { setVehicleId(value); setPageNo(1); }} options={vehicleOptions} showSearch optionFilterProp="label" style={{ width: 260 }} />
          <RangePicker
            value={applyDateRange}
            onChange={(value) => { setApplyDateRange(value as [Dayjs, Dayjs] | null); setPageNo(1); }}
            placeholder={['申请开始', '申请结束']}
          />
          <RangePicker
            value={completedDateRange}
            onChange={(value) => { setCompletedDateRange(value as [Dayjs, Dayjs] | null); setPageNo(1); }}
            placeholder={['完工开始', '完工结束']}
          />
          <Button onClick={handleReset}>重置</Button>
          <Button onClick={() => void handleExport()}>导出台账</Button>
        </div>
        <Table<VehicleRepairOrderRecord>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={records}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => {
              setPageNo(nextPage);
              setPageSize(nextSize);
            },
          }}
        />
      </Card>

      <Drawer
        title={detailRecord ? `维修单详情 - ${detailRecord.orderNo}` : '维修单详情'}
        width={860}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
      >
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="维修单号">{detailRecord?.orderNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="车牌号">{detailRecord?.plateNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="所属单位">{detailRecord?.orgName || '-'}</Descriptions.Item>
          <Descriptions.Item label="紧急度">
            {detailRecord ? <Tag color={urgencyColorMap[detailRecord.urgencyLevel || ''] || 'default'}>{detailRecord.urgencyLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="维修原因">{detailRecord?.repairReason || '-'}</Descriptions.Item>
          <Descriptions.Item label="申请人">{detailRecord?.applicantName || '-'}</Descriptions.Item>
          <Descriptions.Item label="申请日期">{detailRecord?.applyDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {detailRecord ? <Tag color={statusColorMap[detailRecord.status] || 'default'}>{detailRecord.statusLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="维修内容" span={2}>{detailRecord?.repairContent || '-'}</Descriptions.Item>
          <Descriptions.Item label="诊断结果" span={2}>{detailRecord?.diagnosisResult || '-'}</Descriptions.Item>
          <Descriptions.Item label="安全影响">{detailRecord?.safetyImpact || '-'}</Descriptions.Item>
          <Descriptions.Item label="审批意见">{detailRecord?.auditRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="审批人 / 时间">{`${detailRecord?.approvedBy || '-'} / ${detailRecord?.approvedTime || '-'}`}</Descriptions.Item>
          <Descriptions.Item label="完工日期">{detailRecord?.completedDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="维修单位">{detailRecord?.vendorName || '-'}</Descriptions.Item>
          <Descriptions.Item label="维修负责人">{detailRecord?.repairManager || '-'}</Descriptions.Item>
          <Descriptions.Item label="维修技师">{detailRecord?.technicianName || '-'}</Descriptions.Item>
          <Descriptions.Item label="验收结果" span={2}>{detailRecord?.acceptanceResult || '-'}</Descriptions.Item>
          <Descriptions.Item label="预算 / 实际">
            {(detailRecord?.budgetAmount ?? 0).toFixed(2)} / {(detailRecord?.actualAmount ?? 0).toFixed(2)}
          </Descriptions.Item>
          <Descriptions.Item label="配件 / 人工 / 其他">
            {(detailRecord?.partsCost ?? 0).toFixed(2)} / {(detailRecord?.laborCost ?? 0).toFixed(2)} / {(detailRecord?.otherCost ?? 0).toFixed(2)}
          </Descriptions.Item>
          <Descriptions.Item label="预算偏差">{(detailRecord?.costVariance ?? 0).toFixed(2)}</Descriptions.Item>
          <Descriptions.Item label="签字状态">
            {detailRecord?.signoffStatusLabel ? <Tag color={detailRecord.signoffStatus === 'SIGNED' ? 'success' : detailRecord.signoffStatus === 'WAIVED' ? 'default' : 'warning'}>{detailRecord.signoffStatusLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="附件" span={2}>{detailRecord?.attachmentUrls || '-'}</Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>{detailRecord?.remark || '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Modal
        title={editingRecord ? '编辑维修单' : '新增维修单'}
        open={editorOpen}
        onCancel={() => setEditorOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitLoading}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="vehicleId" label="车辆" rules={[{ required: true, message: '请选择车辆' }]}>
                <Select options={vehicleOptions} showSearch optionFilterProp="label" placeholder="请选择车辆" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="urgencyLevel" label="紧急度">
                <Select options={urgencyOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="repairReason" label="维修原因" rules={[{ required: true, message: '请输入维修原因' }]}>
                <Input placeholder="请输入维修原因" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="applicantName" label="申请人">
                <Input placeholder="请输入申请人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="applyDate" label="申请日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="budgetAmount" label="预算金额(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="diagnosisResult" label="诊断结果">
                <Input.TextArea rows={2} placeholder="请输入故障诊断结果" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="safetyImpact" label="安全影响">
                <Select
                  options={[
                    { label: '高', value: '高' },
                    { label: '中', value: '中' },
                    { label: '低', value: '低' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select options={statusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="repairContent" label="维修内容">
                <Input.TextArea rows={3} placeholder="请输入维修内容" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={3} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title={auditAction === 'approve' ? '批准维修单' : '驳回维修单'}
        open={auditOpen}
        onCancel={() => setAuditOpen(false)}
        onOk={handleAudit}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={auditForm} layout="vertical">
          <Form.Item name="comment" label="审批意见">
            <Input.TextArea rows={4} placeholder="请输入审批意见" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={currentRecord ? `完成维修 - ${currentRecord.orderNo}` : '完成维修'}
        open={completeOpen}
        onCancel={() => setCompleteOpen(false)}
        onOk={handleComplete}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={completeForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="completedDate" label="完工日期" rules={[{ required: true, message: '请选择完工日期' }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="actualAmount" label="实际金额(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="partsCost" label="配件费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="laborCost" label="人工费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="otherCost" label="其他费用(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="vendorName" label="维修单位">
                <Input placeholder="请输入维修单位" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="repairManager" label="维修负责人">
                <Input placeholder="请输入维修负责人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="technicianName" label="维修技师">
                <Input placeholder="请输入维修技师" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="acceptanceResult" label="验收结果">
                <Input.TextArea rows={2} placeholder="请输入验收结果" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="signoffStatus" label="签字状态">
                <Select
                  options={[
                    { label: '未签字', value: 'UNSIGNED' },
                    { label: '已签字', value: 'SIGNED' },
                    { label: '免签', value: 'WAIVED' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="attachmentUrls" label="附件地址">
                <Input.TextArea rows={2} placeholder="多个地址可用换行分隔" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="完工备注">
                <Input.TextArea rows={4} placeholder="请输入完工备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
  );
};
export default VehicleRepairs;
