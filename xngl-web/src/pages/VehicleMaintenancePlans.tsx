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
import { PlusOutlined, SearchOutlined, ToolOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import {
  createVehicleMaintenancePlan,
  deleteVehicleMaintenancePlan,
  executeVehicleMaintenance,
  exportVehicleMaintenancePlans,
  exportVehicleMaintenanceRecords,
  fetchVehicleMaintenancePlanDetail,
  fetchVehicleMaintenancePlanRecords,
  fetchVehicleMaintenanceRecordDetail,
  fetchVehicleMaintenancePlans,
  fetchVehicleMaintenanceRecords,
  fetchVehicleMaintenanceSummary,
  updateVehicleMaintenancePlan,
  type VehicleMaintenanceExecutePayload,
  type VehicleMaintenancePlanRecord,
  type VehicleMaintenancePlanUpsertPayload,
  type VehicleMaintenanceRecord,
  type VehicleMaintenanceSummaryRecord,
} from '../utils/vehicleMaintenanceApi';
import { fetchVehicleCompanyCapacity, fetchVehicles } from '../utils/vehicleApi';

const { RangePicker } = DatePicker;

type PlanFormValues = {
  vehicleId: string;
  planType: string;
  cycleType: string;
  cycleValue: number;
  lastMaintainDate?: Dayjs;
  nextMaintainDate?: Dayjs;
  lastOdometer?: number;
  nextOdometer?: number;
  responsibleName?: string;
  status?: string;
  remark?: string;

type ExecuteFormValues = {
  serviceDate: Dayjs;
  odometer?: number;
  vendorName?: string;
  costAmount?: number;
  laborCost?: number;
  materialCost?: number;
  externalCost?: number;
  items?: string;
  issueDescription?: string;
  resultSummary?: string;
  operatorName?: string;
  technicianName?: string;
  checkerName?: string;
  signoffStatus?: string;
  attachmentUrls?: string;
  status?: string;
  remark?: string;
  nextMaintainDate?: Dayjs;
  nextOdometer?: number;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '执行中', value: 'ACTIVE' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完成', value: 'COMPLETED' },
];

const planStatusColorMap: Record<string, string> = {
  ACTIVE: 'processing',
  PAUSED: 'warning',
  COMPLETED: 'success',

const recordStatusColorMap: Record<string, string> = {
  DONE: 'success',
  PENDING: 'warning',
  CANCELLED: 'default',

const cycleTypeOptions = [
  { label: '按天', value: 'DAY' },
  { label: '按月', value: 'MONTH' },
  { label: '按里程', value: 'KM' },
];

const overdueOptions = [
  { label: '全部计划', value: 'all' },
  { label: '仅逾期计划', value: 'overdue' },
];

const planDefaultSummary: VehicleMaintenanceSummaryRecord = {
  totalPlans: 0,
  activePlans: 0,
  overduePlans: 0,
  pausedPlans: 0,
  recordCount: 0,
  totalCostAmount: 0,

const VehicleMaintenancePlans: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [plans, setPlans] = useState<VehicleMaintenancePlanRecord[]>([]);
  const [recordDetailOpen, setRecordDetailOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState<VehicleMaintenancePlanRecord | null>(null);
  const [currentPlan, setCurrentPlan] = useState<VehicleMaintenancePlanRecord | null>(null);
  const [detailPlan, setDetailPlan] = useState<VehicleMaintenancePlanRecord | null>(null);
  const [detailPlanRecords, setDetailPlanRecords] = useState<VehicleMaintenanceRecord[]>([]);
  const [detailRecord, setDetailRecord] = useState<VehicleMaintenanceRecord | null>(null);
  const [planForm] = Form.useForm<PlanFormValues>();
  const [executeForm] = Form.useForm<ExecuteFormValues>();

  const queryParams = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      status: status === 'all' ? undefined : status,
      orgId,
      vehicleId,
      nextMaintainDateFrom: planDateRange?.[0]?.format('YYYY-MM-DD'),
      nextMaintainDateTo: planDateRange?.[1]?.format('YYYY-MM-DD'),
      serviceDateFrom: recordDateRange?.[0]?.format('YYYY-MM-DD'),
      serviceDateTo: recordDateRange?.[1]?.format('YYYY-MM-DD'),
      overdueOnly: overdueFilter === 'overdue' ? true : undefined,
    }),
    [keyword, status, orgId, vehicleId, planDateRange, recordDateRange, overdueFilter]
  );

  const loadPlans = async () => {
    setLoading(true);
    try {
      const page = await fetchVehicleMaintenancePlans({ ...queryParams, pageNo, pageSize });
      setPlans(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取维保计划失败');
      setPlans([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadRecords = async () => {
    setRecordsLoading(true);
    try {
      const page = await fetchVehicleMaintenanceRecords({
        ...queryParams,
        pageNo: recordPageNo,
        pageSize: recordPageSize,
      });
      setRecords(page.records || []);
      setRecordTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取维保记录失败');
      setRecords([]);
      setRecordTotal(0);
    } finally {
      setRecordsLoading(false);
    }
  };

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchVehicleMaintenanceSummary(queryParams));
    } catch (error) {
      console.error(error);
      message.error('获取维保统计失败');
      setSummary(planDefaultSummary);
    } finally {
      setSummaryLoading(false);
    }
  };

  useEffect(() => {
    void loadSummary();
  }, [queryParams]);

  useEffect(() => {
    void loadPlans();
  }, [pageNo, pageSize, queryParams]);

  useEffect(() => {
    void loadRecords();
  }, [recordPageNo, recordPageSize, queryParams]);

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
    setOrgId(undefined);
    setVehicleId(undefined);
    setPlanDateRange(null);
    setRecordDateRange(null);
    setOverdueFilter('all');
    setPageNo(1);
    setRecordPageNo(1);
  };

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  const handleExportPlans = async () => {
    try {
      downloadBlob(await exportVehicleMaintenancePlans(queryParams), 'vehicle_maintenance_plans.csv');
      message.success('维保计划已导出');
    } catch (error) {
      console.error(error);
    }
  };

  const handleExportRecords = async () => {
    try {
      downloadBlob(await exportVehicleMaintenanceRecords(queryParams), 'vehicle_maintenance_records.csv');
      message.success('维保记录已导出');
    } catch (error) {
      console.error(error);
    }
  };

  const openCreate = () => {
    setEditingPlan(null);
    planForm.resetFields();
    planForm.setFieldsValue({
      cycleType: 'MONTH',
      cycleValue: 3,
      status: 'ACTIVE',
      lastMaintainDate: dayjs().subtract(1, 'month'),
      nextMaintainDate: dayjs().add(2, 'month'),
      lastOdometer: 0,
    });
    setEditorOpen(true);
  };

  const openEdit = (record: VehicleMaintenancePlanRecord) => {
    setEditingPlan(record);
    planForm.setFieldsValue({
      vehicleId: record.vehicleId || undefined,
      planType: record.planType || undefined,
      cycleType: record.cycleType || 'MONTH',
      cycleValue: record.cycleValue || 1,
      lastMaintainDate: record.lastMaintainDate ? dayjs(record.lastMaintainDate) : undefined,
      nextMaintainDate: record.nextMaintainDate ? dayjs(record.nextMaintainDate) : undefined,
      lastOdometer: record.lastOdometer,
      nextOdometer: record.nextOdometer || undefined,
      responsibleName: record.responsibleName || undefined,
      status: record.status,
      remark: record.remark || undefined,
    });
    setEditorOpen(true);
  };

  const openExecute = (record: VehicleMaintenancePlanRecord) => {
    setCurrentPlan(record);
    executeForm.resetFields();
    executeForm.setFieldsValue({
      serviceDate: dayjs(),
      odometer: record.nextOdometer || record.lastOdometer || 0,
      costAmount: 0,
      laborCost: 0,
      materialCost: 0,
      externalCost: 0,
      operatorName: record.responsibleName || undefined,
      technicianName: record.responsibleName || undefined,
      signoffStatus: 'UNSIGNED',
      status: 'DONE',
      nextMaintainDate: record.nextMaintainDate ? dayjs(record.nextMaintainDate) : undefined,
      nextOdometer: record.nextOdometer || undefined,
    });
    setExecuteOpen(true);
  };

  const openPlanDetail = async (record: VehicleMaintenancePlanRecord) => {
    try {
      const [planDetail, planRecords] = await Promise.all([
        fetchVehicleMaintenancePlanDetail(record.id),
        fetchVehicleMaintenancePlanRecords(record.id),
      ]);
      setDetailPlan(planDetail);
      setDetailPlanRecords(planRecords);
      setPlanDetailOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取维保计划详情失败');
    }
  };

  const openRecordDetail = async (record: VehicleMaintenanceRecord) => {
    try {
      setDetailRecord(await fetchVehicleMaintenanceRecordDetail(record.id));
      setRecordDetailOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取维保记录详情失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await planForm.validateFields();
      const payload: VehicleMaintenancePlanUpsertPayload = {
        vehicleId: Number(values.vehicleId),
        planType: values.planType,
        cycleType: values.cycleType,
        cycleValue: values.cycleValue,
        lastMaintainDate: values.lastMaintainDate?.format('YYYY-MM-DD'),
        nextMaintainDate: values.nextMaintainDate?.format('YYYY-MM-DD'),
        lastOdometer: values.lastOdometer,
        nextOdometer: values.nextOdometer,
        responsibleName: values.responsibleName,
        status: values.status,
        remark: values.remark,
      };
      setSubmitLoading(true);
      if (editingPlan) {
        await updateVehicleMaintenancePlan(editingPlan.id, payload);
        message.success('维保计划已更新');
      } else {
        await createVehicleMaintenancePlan(payload);
        message.success('维保计划已新增');
      }
      setEditorOpen(false);
      setPageNo(1);
      await Promise.all([loadSummary(), loadPlans(), loadRecords()]);
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleExecute = async () => {
    if (!currentPlan) {
      return;
    }
    try {
      const values = await executeForm.validateFields();
      const payload: VehicleMaintenanceExecutePayload = {
        serviceDate: values.serviceDate?.format('YYYY-MM-DD'),
        odometer: values.odometer,
        vendorName: values.vendorName,
        costAmount: values.costAmount,
        laborCost: values.laborCost,
        materialCost: values.materialCost,
        externalCost: values.externalCost,
        items: values.items,
        issueDescription: values.issueDescription,
        resultSummary: values.resultSummary,
        operatorName: values.operatorName,
        technicianName: values.technicianName,
        checkerName: values.checkerName,
        signoffStatus: values.signoffStatus,
        attachmentUrls: values.attachmentUrls,
        status: values.status,
        remark: values.remark,
        nextMaintainDate: values.nextMaintainDate?.format('YYYY-MM-DD'),
        nextOdometer: values.nextOdometer,
      };
      setSubmitLoading(true);
      await executeVehicleMaintenance(currentPlan.id, payload);
      message.success('维保执行记录已登记');
      setExecuteOpen(false);
      setRecordPageNo(1);
      await Promise.all([loadSummary(), loadPlans(), loadRecords()]);
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
      await deleteVehicleMaintenancePlan(id);
      message.success('维保计划已删除');
      setPageNo(1);
      await Promise.all([loadSummary(), loadPlans(), loadRecords()]);
    } catch (error) {
      console.error(error);
      message.error((error as any)?.response?.data?.message || '删除维保计划失败');
    }
  };

  const planColumns: ColumnsType<VehicleMaintenancePlanRecord> = [
    {
      title: '计划编号',
      dataIndex: 'planNo',
      key: 'planNo',
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
      title: '维保类型',
      dataIndex: 'planType',
      key: 'planType',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '周期',
      key: 'cycle',
      render: (_, record) => `${record.cycleValue || '-'} ${record.cycleType || ''}`,
    },
    {
      title: '下次维保',
      dataIndex: 'nextMaintainDate',
      key: 'nextMaintainDate',
      render: (_, record) => (
        <Space size={6}>
          <span>{record.nextMaintainDate || '-'}</span>
          {record.overdue ? <Tag color="error">逾期</Tag> : null}
        </Space>
      ),
    },
    {
      title: '负责人',
      dataIndex: 'responsibleName',
      key: 'responsibleName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (_, record) => <Tag color={planStatusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => void openPlanDetail(record)}>
            详情
          </Button>
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => openExecute(record)}>
            执行维保
          </Button>
          <Popconfirm title="确认删除当前维保计划？" onConfirm={() => void handleDelete(record.id)}>
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const recordColumns: ColumnsType<VehicleMaintenanceRecord> = [
    {
      title: '执行编号',
      dataIndex: 'recordNo',
      key: 'recordNo',
      render: (value: string) => <span className="font-mono g-text-secondary">{value}</span>,
    },
    {
      title: '车牌号',
      dataIndex: 'plateNo',
      key: 'plateNo',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '维保项目',
      dataIndex: 'maintainType',
      key: 'maintainType',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '执行日期',
      dataIndex: 'serviceDate',
      key: 'serviceDate',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '费用(元)',
      dataIndex: 'costAmount',
      key: 'costAmount',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '供应商',
      dataIndex: 'vendorName',
      key: 'vendorName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (_, record) => <Tag color={recordStatusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => void openRecordDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">维保计划</h1>
          <p className="g-text-secondary mt-1">统一管理车辆维保计划、执行记录和到期预警。</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增计划
        </Button>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="维保计划" value={summary.totalPlans} prefix={<ToolOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="执行中" value={summary.activePlans} prefix={<ToolOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="逾期计划" value={summary.overduePlans} prefix={<ToolOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="累计费用(元)" value={summary.totalCostAmount} precision={2} prefix={<ToolOutlined />} />
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
              setRecordPageNo(1);
            }}
            placeholder="搜索计划编号/车牌/单位/维保类型"
            prefix={<SearchOutlined />}
            style={{ width: 260 }}
          />
          <Select value={status} onChange={(value) => { setStatus(value); setPageNo(1); setRecordPageNo(1); }} options={statusOptions} style={{ width: 160 }} />
          <Select value={overdueFilter} onChange={(value) => { setOverdueFilter(value); setPageNo(1); }} options={overdueOptions} style={{ width: 160 }} />
          <Select allowClear placeholder="所属单位" value={orgId} onChange={(value) => { setOrgId(value); setPageNo(1); setRecordPageNo(1); }} options={companyOptions} style={{ width: 220 }} />
          <Select allowClear placeholder="车辆" value={vehicleId} onChange={(value) => { setVehicleId(value); setPageNo(1); setRecordPageNo(1); }} options={vehicleOptions} style={{ width: 260 }} showSearch optionFilterProp="label" />
          <RangePicker
            value={planDateRange}
            onChange={(value) => { setPlanDateRange(value as [Dayjs, Dayjs] | null); setPageNo(1); }}
            placeholder={['下次维保开始', '下次维保结束']}
          />
          <RangePicker
            value={recordDateRange}
            onChange={(value) => { setRecordDateRange(value as [Dayjs, Dayjs] | null); setRecordPageNo(1); }}
            placeholder={['执行开始', '执行结束']}
          />
          <Button onClick={handleReset}>重置</Button>
          <Button onClick={() => void handleExportPlans()}>导出计划</Button>
          <Button onClick={() => void handleExportRecords()}>导出记录</Button>
        </div>
        <Table<VehicleMaintenancePlanRecord>
          rowKey="id"
          loading={loading}
          columns={planColumns}
          dataSource={plans}
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

      <Card className="glass-panel g-border-panel border" title="维保执行记录">
        <Table<VehicleMaintenanceRecord>
          rowKey="id"
          loading={recordsLoading}
          columns={recordColumns}
          dataSource={records}
          pagination={{
            current: recordPageNo,
            pageSize: recordPageSize,
            total: recordTotal,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => {
              setRecordPageNo(nextPage);
              setRecordPageSize(nextSize);
            },
          }}
        />
      </Card>

      <Drawer
        title={detailPlan ? `维保计划详情 - ${detailPlan.planNo}` : '维保计划详情'}
        width={860}
        open={planDetailOpen}
        onClose={() => setPlanDetailOpen(false)}
      >
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="计划编号">{detailPlan?.planNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="车牌号">{detailPlan?.plateNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="所属单位">{detailPlan?.orgName || '-'}</Descriptions.Item>
          <Descriptions.Item label="维保类型">{detailPlan?.planType || '-'}</Descriptions.Item>
          <Descriptions.Item label="周期">{`${detailPlan?.cycleValue || '-'} ${detailPlan?.cycleType || ''}`}</Descriptions.Item>
          <Descriptions.Item label="负责人">{detailPlan?.responsibleName || '-'}</Descriptions.Item>
          <Descriptions.Item label="上次维保">{detailPlan?.lastMaintainDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="下次维保">{detailPlan?.nextMaintainDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="执行次数">{detailPlan?.recordCount ?? 0}</Descriptions.Item>
          <Descriptions.Item label="累计费用">{detailPlan?.totalRecordCost?.toFixed(2) || '0.00'}</Descriptions.Item>
          <Descriptions.Item label="最近执行日期">{detailPlan?.lastServiceDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {detailPlan ? <Tag color={planStatusColorMap[detailPlan.status] || 'default'}>{detailPlan.statusLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>{detailPlan?.remark || '-'}</Descriptions.Item>
        </Descriptions>
        <Card className="glass-panel g-border-panel border mt-4" title="执行历史">
          <Table<VehicleMaintenanceRecord>
            rowKey="id"
            pagination={false}
            columns={recordColumns.filter((item) => item.key !== 'action')}
            dataSource={detailPlanRecords}
          />
        </Card>
      </Drawer>

      <Drawer
        title={detailRecord ? `维保记录详情 - ${detailRecord.recordNo}` : '维保记录详情'}
        width={820}
        open={recordDetailOpen}
        onClose={() => setRecordDetailOpen(false)}
      >
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="执行编号">{detailRecord?.recordNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="计划编号">{detailRecord?.planNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="车牌号">{detailRecord?.plateNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="所属单位">{detailRecord?.orgName || '-'}</Descriptions.Item>
          <Descriptions.Item label="维保项目">{detailRecord?.maintainType || '-'}</Descriptions.Item>
          <Descriptions.Item label="执行日期">{detailRecord?.serviceDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="执行里程">{detailRecord?.odometer?.toFixed(2) || '0.00'}</Descriptions.Item>
          <Descriptions.Item label="服务商">{detailRecord?.vendorName || '-'}</Descriptions.Item>
          <Descriptions.Item label="总费用">{detailRecord?.costAmount?.toFixed(2) || '0.00'}</Descriptions.Item>
          <Descriptions.Item label="人工/材料/外协">
            {(detailRecord?.laborCost ?? 0).toFixed(2)} / {(detailRecord?.materialCost ?? 0).toFixed(2)} / {(detailRecord?.externalCost ?? 0).toFixed(2)}
          </Descriptions.Item>
          <Descriptions.Item label="经办人">{detailRecord?.operatorName || '-'}</Descriptions.Item>
          <Descriptions.Item label="维修技师">{detailRecord?.technicianName || '-'}</Descriptions.Item>
          <Descriptions.Item label="验收人">{detailRecord?.checkerName || '-'}</Descriptions.Item>
          <Descriptions.Item label="签字状态">
            {detailRecord?.signoffStatusLabel ? <Tag color={detailRecord.signoffStatus === 'SIGNED' ? 'success' : detailRecord.signoffStatus === 'WAIVED' ? 'default' : 'warning'}>{detailRecord.signoffStatusLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="执行状态">
            {detailRecord ? <Tag color={recordStatusColorMap[detailRecord.status] || 'default'}>{detailRecord.statusLabel}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="维保项目明细" span={2}>{detailRecord?.items || '-'}</Descriptions.Item>
          <Descriptions.Item label="异常描述" span={2}>{detailRecord?.issueDescription || '-'}</Descriptions.Item>
          <Descriptions.Item label="处理结果" span={2}>{detailRecord?.resultSummary || '-'}</Descriptions.Item>
          <Descriptions.Item label="附件" span={2}>{detailRecord?.attachmentUrls || '-'}</Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>{detailRecord?.remark || '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Modal
        title={editingPlan ? '编辑维保计划' : '新增维保计划'}
        open={editorOpen}
        onCancel={() => setEditorOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitLoading}
        width={720}
        destroyOnClose
      >
        <Form form={planForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="vehicleId" label="车辆" rules={[{ required: true, message: '请选择车辆' }]}>
                <Select options={vehicleOptions} showSearch optionFilterProp="label" placeholder="请选择车辆" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="planType" label="维保类型" rules={[{ required: true, message: '请输入维保类型' }]}>
                <Input placeholder="如：常规保养 / 轮胎检查" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="cycleType" label="周期类型" rules={[{ required: true, message: '请选择周期类型' }]}>
                <Select options={cycleTypeOptions} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="cycleValue" label="周期值" rules={[{ required: true, message: '请输入周期值' }]}>
                <InputNumber min={1} precision={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="状态">
                <Select options={statusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lastMaintainDate" label="上次维保日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="nextMaintainDate" label="下次维保日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lastOdometer" label="上次里程(km)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="nextOdometer" label="下次里程(km)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="responsibleName" label="负责人">
                <Input placeholder="请输入负责人" />
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
        title={currentPlan ? `执行维保 - ${currentPlan.planNo}` : '执行维保'}
        open={executeOpen}
        onCancel={() => setExecuteOpen(false)}
        onOk={handleExecute}
        confirmLoading={submitLoading}
        width={720}
        destroyOnClose
      >
        <Form form={executeForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="serviceDate" label="执行日期" rules={[{ required: true, message: '请选择执行日期' }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="执行状态">
                <Select
                  options={[
                    { label: '已执行', value: 'DONE' },
                    { label: '待确认', value: 'PENDING' },
                    { label: '已取消', value: 'CANCELLED' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="odometer" label="当前里程(km)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="costAmount" label="费用(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="laborCost" label="人工费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="materialCost" label="材料费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="externalCost" label="外协费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="vendorName" label="服务商">
                <Input placeholder="请输入服务商" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="operatorName" label="经办人">
                <Input placeholder="请输入经办人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="technicianName" label="维修技师">
                <Input placeholder="请输入维修技师" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="checkerName" label="验收人">
                <Input placeholder="请输入验收人" />
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
            <Col span={12}>
              <Form.Item name="nextMaintainDate" label="下次维保日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="nextOdometer" label="下次维保里程(km)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="items" label="维保项目">
                <Input.TextArea rows={2} placeholder="如：机油更换/滤芯保养" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="issueDescription" label="异常描述">
                <Input.TextArea rows={2} placeholder="请输入发现的问题或异常情况" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="resultSummary" label="处理结果">
                <Input.TextArea rows={2} placeholder="请输入处理结果和恢复情况" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="attachmentUrls" label="附件地址">
                <Input.TextArea rows={2} placeholder="多个地址可用换行分隔" />
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
  );
};
export default VehicleMaintenancePlans;
