import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
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
import {
  CarOutlined,
  DashboardOutlined,
  FilterOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import dayjs, { type Dayjs } from 'dayjs';
import {
  batchDeleteVehicles,
  batchUpdateVehicleStatus,
  createVehicle,
  deleteVehicle,
  exportVehicles,
  fetchVehicleCompanyCapacity,
  fetchVehicleDetail,
  fetchVehicleStats,
  fetchVehicles,
  updateVehicle,
  type VehicleCompanyCapacityRecord,
  type VehicleDetailRecord,
  type VehicleRecord,
  type VehicleStatsRecord,
  type VehicleUpsertPayload,
} from '../utils/vehicleApi';
import { fetchVehicleModels, type VehicleModelRecord } from '../utils/vehicleModelApi';

type VehicleFormValues = Omit<
  VehicleUpsertPayload,
  'orgId' | 'nextMaintainDate' | 'annualInspectionExpireDate' | 'insuranceExpireDate'
> & {
  orgId?: string;
  vehicleModelCode?: string;
  nextMaintainDate?: Dayjs;
  annualInspectionExpireDate?: Dayjs;
  insuranceExpireDate?: Dayjs;
};

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '在用', value: 1 },
  { label: '维修', value: 2 },
  { label: '禁用', value: 3 },
  { label: '待命', value: 4 },
];

const runningStatusColorMap: Record<string, string> = {
  行驶中: 'processing',
  静止: 'warning',
  离线: 'default',
};

const statusColorMap: Record<string, string> = {
  在用: 'success',
  维修: 'warning',
  禁用: 'error',
  待命: 'processing',
  停用: 'default',
};

const warningColorMap: Record<string, string> = {
  正常: 'default',
  '30日内到期': 'warning',
  '7日内到期': 'orange',
  已到期: 'error',
};

const formatTons = (value?: number | null) => Number(value || 0).toFixed(2);

const VehiclesManagement: React.FC = () => {
  const [listLoading, setListLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [capacityLoading, setCapacityLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [vehicles, setVehicles] = useState<VehicleRecord[]>([]);
  const [stats, setStats] = useState<VehicleStatsRecord>({
    totalVehicles: 0,
    activeVehicles: 0,
    maintenanceVehicles: 0,
    disabledVehicles: 0,
    warningVehicles: 0,
    activeRate: 0,
    totalLoadTons: 0,
  });
  const [companyCapacity, setCompanyCapacity] = useState<VehicleCompanyCapacityRecord[]>([]);
  const [vehicleModels, setVehicleModels] = useState<VehicleModelRecord[]>([]);
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleDetailRecord | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [filterVisible, setFilterVisible] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [editingVehicleId, setEditingVehicleId] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | 'all'>('all');
  const [orgId, setOrgId] = useState<string | undefined>(undefined);
  const [vehicleType, setVehicleType] = useState<string | undefined>(undefined);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [filterForm] = Form.useForm();
  const [createForm] = Form.useForm<VehicleFormValues>();

  const companyOptions = useMemo(
    () =>
      companyCapacity.map((item) => ({
        label: item.orgName,
        value: item.orgId || '',
      })),
    [companyCapacity]
  );

  const vehicleTypeOptions = useMemo(() => {
    const values = [
      ...vehicleModels.map((item) => item.vehicleType),
      ...vehicles.map((item) => item.vehicleType),
    ].filter(Boolean) as string[];
    return Array.from(new Set(values));
  }, [vehicleModels, vehicles]);

  const vehicleModelOptions = useMemo(
    () =>
      vehicleModels.map((item) => ({
        value: item.modelCode,
        label: `${item.brand} / ${item.modelName}`,
      })),
    [vehicleModels]
  );

  const refreshOverview = async () => {
    setStatsLoading(true);
    setCapacityLoading(true);
    try {
      const [statsData, capacityData] = await Promise.all([
        fetchVehicleStats(),
        fetchVehicleCompanyCapacity(),
      ]);
      setStats(statsData);
      setCompanyCapacity(capacityData);
    } catch (error) {
      console.error(error);
      message.error('获取运力概览失败');
    } finally {
      setStatsLoading(false);
      setCapacityLoading(false);
    }
  };

  useEffect(() => {
    void refreshOverview();
  }, []);

  useEffect(() => {
    const loadVehicleModels = async () => {
      try {
        const rows = await fetchVehicleModels({ status: 'ENABLED' });
        setVehicleModels(rows);
      } catch (error) {
        console.error(error);
        message.error('获取车型字典失败');
        setVehicleModels([]);
      }
    };
    void loadVehicleModels();
  }, []);

  useEffect(() => {
    const loadVehicles = async () => {
      setListLoading(true);
      try {
        const page = await fetchVehicles({
          keyword: keyword.trim() || undefined,
          status: status === 'all' ? undefined : status,
          orgId: orgId || undefined,
          vehicleType: vehicleType || undefined,
          pageNo,
          pageSize,
        });
        setVehicles(page.records || []);
        setTotal(page.total || 0);
      } catch (error) {
        console.error(error);
        message.error('获取车辆列表失败');
        setVehicles([]);
        setTotal(0);
      } finally {
        setListLoading(false);
      }
    };

    void loadVehicles();
  }, [keyword, status, orgId, vehicleType, pageNo, pageSize]);

  const summaryCards = useMemo(
    () => [
      {
        title: '全网注册车辆 (台)',
        value: stats.totalVehicles,
        prefix: <CarOutlined className="g-text-primary-link" />,
        color: 'var(--text-primary)',
      },
      {
        title: '在用活跃率',
        value: stats.activeRate,
        suffix: '%',
        prefix: <DashboardOutlined />,
        color: 'var(--success)',
      },
      {
        title: '证照预警/禁运 (台)',
        value: stats.warningVehicles + stats.disabledVehicles,
        prefix: <SafetyCertificateOutlined />,
        color: 'var(--error)',
      },
      {
        title: '平台核载总量 (吨)',
        value: Number(stats.totalLoadTons || 0).toFixed(2),
        prefix: <CarOutlined />,
        color: 'var(--primary)',
      },
    ],
    [stats]
  );

  const openDetail = async (id: string) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const detail = await fetchVehicleDetail(id);
      setSelectedVehicle(detail);
    } catch (error) {
      console.error(error);
      message.error('获取车辆详情失败');
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const resetFilters = () => {
    filterForm.resetFields();
    setStatus('all');
    setOrgId(undefined);
    setVehicleType(undefined);
    setPageNo(1);
  };

  const resetVehicleForm = () => {
    createForm.resetFields();
    createForm.setFieldsValue({
      status: 1,
      useStatus: 'ACTIVE',
      runningStatus: 'STOPPED',
      energyType: 'DIESEL',
    });
  };

  const handleVehicleModelSelect = (modelCode?: string) => {
    if (!modelCode) {
      return;
    }
    const model = vehicleModels.find((item) => item.modelCode === modelCode);
    if (!model) {
      return;
    }
    createForm.setFieldsValue({
      vehicleType: model.vehicleType || undefined,
      brand: model.brand,
      model: model.modelName,
      energyType: model.energyType || undefined,
      axleCount: model.axleCount || undefined,
      deadWeight: model.deadWeight || undefined,
      loadWeight: model.loadWeight || undefined,
    });
  };

  const openCreate = () => {
    setEditingVehicleId(null);
    resetVehicleForm();
    setCreateVisible(true);
  };

  const openEdit = async (id: string) => {
    try {
      setSubmitLoading(true);
      const detail = await fetchVehicleDetail(id);
      const matchedModel = vehicleModels.find(
        (item) =>
          item.vehicleType === detail.vehicleType &&
          item.brand === detail.brand &&
          item.modelName === detail.model
      );
      createForm.setFieldsValue({
        plateNo: detail.plateNo,
        vin: detail.vin || undefined,
        orgId: detail.orgId || undefined,
        vehicleModelCode: matchedModel?.modelCode,
        vehicleType: detail.vehicleType || undefined,
        brand: detail.brand || undefined,
        model: detail.model || undefined,
        energyType: detail.energyType || undefined,
        axleCount: detail.axleCount || undefined,
        deadWeight: detail.deadWeight || undefined,
        loadWeight: detail.loadWeight || undefined,
        driverName: detail.driverName || undefined,
        driverPhone: detail.driverPhone || undefined,
        fleetName: detail.fleetName || undefined,
        captainName: detail.captainName || undefined,
        captainPhone: detail.captainPhone || undefined,
        status: detail.status || 1,
        nextMaintainDate: detail.nextMaintainDate ? dayjs(detail.nextMaintainDate) : undefined,
        annualInspectionExpireDate: detail.annualInspectionExpireDate ? dayjs(detail.annualInspectionExpireDate) : undefined,
        insuranceExpireDate: detail.insuranceExpireDate ? dayjs(detail.insuranceExpireDate) : undefined,
        currentMileage: detail.currentMileage || undefined,
        remark: detail.remark || undefined,
      });
      setEditingVehicleId(id);
      setCreateVisible(true);
    } catch (error) {
      console.error(error);
      message.error('获取车辆档案失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const reloadVehicleTable = async (nextPage = pageNo) => {
    const page = await fetchVehicles({
      keyword: keyword.trim() || undefined,
      status: status === 'all' ? undefined : status,
      orgId: orgId || undefined,
      vehicleType: vehicleType || undefined,
      pageNo: nextPage,
      pageSize,
    });
    setVehicles(page.records || []);
    setTotal(page.total || 0);
  };

  const handleSubmit = async () => {
    try {
      const values = await createForm.validateFields();
      const payload: VehicleUpsertPayload = {
        plateNo: values.plateNo,
        vin: values.vin,
        vehicleType: values.vehicleType,
        brand: values.brand,
        model: values.model,
        energyType: values.energyType,
        axleCount: values.axleCount,
        deadWeight: values.deadWeight,
        loadWeight: values.loadWeight,
        driverName: values.driverName,
        driverPhone: values.driverPhone,
        fleetName: values.fleetName,
        captainName: values.captainName,
        captainPhone: values.captainPhone,
        status: values.status,
        useStatus: values.useStatus,
        runningStatus: values.runningStatus,
        currentMileage: values.currentMileage,
        remark: values.remark,
        orgId: values.orgId ? Number(values.orgId) : undefined,
        nextMaintainDate: values.nextMaintainDate?.format('YYYY-MM-DD'),
        annualInspectionExpireDate: values.annualInspectionExpireDate?.format('YYYY-MM-DD'),
        insuranceExpireDate: values.insuranceExpireDate?.format('YYYY-MM-DD'),
      };
      setSubmitLoading(true);
      if (editingVehicleId) {
        await updateVehicle(editingVehicleId, payload);
        message.success('车辆档案已更新');
      } else {
        await createVehicle(payload);
        message.success('车辆已新增');
      }
      setCreateVisible(false);
      setEditingVehicleId(null);
      resetVehicleForm();
      setPageNo(1);
      await Promise.all([
        refreshOverview(),
        reloadVehicleTable(1),
      ]);
    } catch (error) {
      if (error instanceof Error) {
        console.error(error);
      }
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      message.error(editingVehicleId ? '更新车辆失败' : '新增车辆失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteVehicle(id);
      message.success('车辆已删除');
      await Promise.all([refreshOverview(), reloadVehicleTable(1)]);
      setPageNo(1);
    } catch (error) {
      console.error(error);
      message.error('删除车辆失败');
    }
  };

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleExport = async () => {
    try {
      const blob = await exportVehicles({
        keyword: keyword.trim() || undefined,
        status: status === 'all' ? undefined : status,
        orgId: orgId || undefined,
        vehicleType: vehicleType || undefined,
      });
      downloadBlob(blob, 'vehicles.csv');
      message.success('车辆台账已导出');
    } catch (error) {
      console.error(error);
      message.error('导出车辆台账失败');
    }
  };

  const handleBatchStatus = async (nextStatus: number, label: string) => {
    try {
      const ids = selectedRowKeys.map((item) => Number(item)).filter((item) => Number.isFinite(item));
      const result = await batchUpdateVehicleStatus({ ids, status: nextStatus });
      message.success(`已批量更新 ${result.updated} 台车辆为${label}`);
      setSelectedRowKeys([]);
      await Promise.all([refreshOverview(), reloadVehicleTable(1)]);
      setPageNo(1);
    } catch (error) {
      console.error(error);
      message.error('批量更新车辆状态失败');
    }
  };

  const handleBatchDelete = async () => {
    try {
      const ids = selectedRowKeys.map((item) => Number(item)).filter((item) => Number.isFinite(item));
      const result = await batchDeleteVehicles(ids);
      message.success(`已批量删除 ${result.deleted} 台车辆`);
      setSelectedRowKeys([]);
      await Promise.all([refreshOverview(), reloadVehicleTable(1)]);
      setPageNo(1);
    } catch (error) {
      console.error(error);
      message.error('批量删除车辆失败');
    }
  };

  const vehicleColumns: ColumnsType<VehicleRecord> = [
    {
      title: '车牌号',
      dataIndex: 'plateNo',
      key: 'plateNo',
      render: (value, record) => (
        <a className="font-bold text-lg" style={{ color: 'var(--primary)' }} onClick={() => openDetail(record.id)}>
          {value}
        </a>
      ),
    },
    {
      title: '车型',
      key: 'vehicleType',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{record.vehicleType || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>
            {[record.brand, record.model].filter(Boolean).join(' / ') || '-'}
          </span>
        </div>
      ),
    },
    {
      title: '所属运输单位',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value?: string | null) => <span style={{ color: 'var(--text-secondary)' }}>{value || '-'}</span>,
    },
    {
      title: '司机/车队',
      key: 'driver',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{record.driverName || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.fleetName || '-'}</span>
        </div>
      ),
    },
    {
      title: '核定载重',
      dataIndex: 'loadWeight',
      key: 'loadWeight',
      render: (value?: number | null) => <span style={{ color: 'var(--text-secondary)' }}>{formatTons(value)} 吨</span>,
    },
    {
      title: '运行状态',
      key: 'status',
      render: (_, record) => (
        <Space size={[4, 4]} wrap>
          <Tag color={statusColorMap[record.statusLabel || ''] || 'default'} className="border-none">
            {record.statusLabel || '未知'}
          </Tag>
          <Tag color={runningStatusColorMap[record.runningStatusLabel || ''] || 'default'} className="border-none">
            {record.runningStatusLabel || '未上报'}
          </Tag>
        </Space>
      ),
    },
    {
      title: '保养/预警',
      key: 'warning',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-secondary)' }}>{record.nextMaintainDate || '-'}</span>
          <Tag color={warningColorMap[record.warningLabel || '正常'] || 'default'} className="border-none w-fit mt-1">
            {record.warningLabel || '正常'}
          </Tag>
        </div>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <a style={{ color: 'var(--primary)' }} onClick={() => openDetail(record.id)}>
            详情
          </a>
          <a onClick={() => void openEdit(record.id)}>
            编辑
          </a>
          <Popconfirm title="确认删除当前车辆档案？" onConfirm={() => void handleDelete(record.id)}>
            <a style={{ color: 'var(--error)' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const companyColumns: ColumnsType<VehicleCompanyCapacityRecord> = [
    {
      title: '运输单位名称',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value) => <strong style={{ color: 'var(--primary)' }}>{value}</strong>,
    },
    {
      title: '入网车辆',
      dataIndex: 'totalVehicles',
      key: 'totalVehicles',
      render: (value) => <span className="text-lg font-bold text-blue-400">{value}</span>,
    },
    {
      title: '在用/行驶',
      key: 'activeVehicles',
      render: (_, record) => (
        <span style={{ color: 'var(--text-secondary)' }}>
          {record.activeVehicles} / {record.movingVehicles}
        </span>
      ),
    },
    {
      title: '预警/禁用',
      key: 'warningVehicles',
      render: (_, record) => (
        <span style={{ color: record.warningVehicles + record.disabledVehicles > 0 ? 'var(--error)' : 'var(--text-secondary)' }}>
          {record.warningVehicles} / {record.disabledVehicles}
        </span>
      ),
    },
    {
      title: '核载总量',
      dataIndex: 'totalLoadTons',
      key: 'totalLoadTons',
      render: (value) => <span style={{ color: 'var(--text-secondary)' }}>{formatTons(value)} 吨</span>,
    },
    {
      title: '活跃率',
      dataIndex: 'activeRate',
      key: 'activeRate',
      render: (value) => (
        <span style={{ color: Number(value || 0) >= 70 ? 'var(--success)' : 'var(--warning)' }}>
          {Number(value || 0).toFixed(1)}%
        </span>
      ),
    },
    {
      title: '负责人',
      key: 'captain',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{record.captainName || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.captainPhone || '-'}</span>
        </div>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.3 }} className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">车辆与运力资源</h1>
          <p className="g-text-secondary mt-1">接入真实车辆主数据，统一查看车辆档案、证照预警与运输单位运力能力</p>
        </div>
      </div>

      <Row gutter={[24, 24]}>
        {summaryCards.map((item) => (
          <Col span={6} key={item.title}>
            <Card className="glass-panel g-border-panel border" loading={statsLoading}>
              <Statistic
                title={<span className="g-text-secondary">{item.title}</span>}
                value={item.value}
                suffix={item.suffix}
                valueStyle={{ color: item.color }}
                prefix={item.prefix}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 flex flex-wrap justify-between gap-4 g-bg-toolbar border-b g-border-panel">
          <div className="flex gap-4 flex-wrap">
            <Input
              placeholder="搜索车牌号/司机/车队/单位"
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="w-72 bg-white g-border-panel border g-text-primary"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPageNo(1);
              }}
            />
            <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary" onClick={() => setFilterVisible(true)}>
              高级筛选
            </Button>
          </div>
          <Space>
            <Button onClick={() => void handleExport()}>导出台账</Button>
            <Button
              disabled={selectedRowKeys.length === 0}
              onClick={() => void handleBatchStatus(1, '在用')}
            >
              批量设为在用
            </Button>
            <Button
              disabled={selectedRowKeys.length === 0}
              onClick={() => void handleBatchStatus(2, '维修')}
            >
              批量设为维修
            </Button>
            <Button
              disabled={selectedRowKeys.length === 0}
              onClick={() => void handleBatchStatus(3, '禁用')}
            >
              批量设为禁用
            </Button>
            <Popconfirm
              title={`确认批量删除已选 ${selectedRowKeys.length} 台车辆？`}
              disabled={selectedRowKeys.length === 0}
              onConfirm={() => void handleBatchDelete()}
            >
              <Button danger disabled={selectedRowKeys.length === 0}>
                批量删除
              </Button>
            </Popconfirm>
            <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={openCreate}>
              新增车辆
            </Button>
          </Space>
        </div>

        <div className="p-4 pb-0">
          <Table
            columns={vehicleColumns}
            dataSource={vehicles}
            rowKey="id"
            loading={listLoading}
            rowSelection={{
              selectedRowKeys,
              onChange: (keys) => setSelectedRowKeys(keys),
            }}
            locale={{ emptyText: <Empty description="暂无车辆数据" /> }}
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
        </div>

        <div className="px-4 pb-4">
          <div className="mb-4">
            <h3 className="text-lg font-semibold m-0" style={{ color: 'var(--text-primary)' }}>承运单位运力概览</h3>
            <p className="mt-1 mb-0" style={{ color: 'var(--text-secondary)' }}>
              按运输单位汇总车辆数量、活跃率、预警情况和核载能力
            </p>
          </div>
          <Table
            columns={companyColumns}
            dataSource={companyCapacity}
            rowKey={(record) => record.orgId || record.orgName}
            loading={capacityLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无运力汇总数据" /> }}
            className="bg-transparent"
            rowClassName="hover:bg-white transition-colors"
          />
        </div>
      </Card>

      <Drawer
        title="高级筛选"
        placement="right"
        onClose={() => setFilterVisible(false)}
        open={filterVisible}
        extra={
          <Space>
            <Button onClick={resetFilters}>重置</Button>
            <Button
              type="primary"
              onClick={() => {
                const values = filterForm.getFieldsValue();
                setStatus(values.status ?? 'all');
                setOrgId(values.orgId || undefined);
                setVehicleType(values.vehicleType || undefined);
                setPageNo(1);
                setFilterVisible(false);
              }}
            >
              应用
            </Button>
          </Space>
        }
      >
        <Form
          form={filterForm}
          layout="vertical"
          initialValues={{
            status,
            orgId,
            vehicleType,
          }}
        >
          <Form.Item name="vehicleType" label="车型">
            <Select
              allowClear
              placeholder="请选择车型"
              options={vehicleTypeOptions.map((item) => ({ value: item, label: item }))}
            />
          </Form.Item>
          <Form.Item name="orgId" label="运输单位">
            <Select allowClear placeholder="请选择运输单位" options={companyOptions} />
          </Form.Item>
          <Form.Item name="status" label="车辆状态">
            <Select options={statusOptions} />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={selectedVehicle?.plateNo ? selectedVehicle.plateNo + ' 车辆档案' : '车辆档案'}
        width={720}
        onClose={() => {
          setDetailOpen(false);
          setSelectedVehicle(null);
        }}
        open={detailOpen}
      >
        {selectedVehicle ? (
          <Descriptions
            bordered
            column={2}
            size="middle"
            items={[
              { key: 'plateNo', label: '车牌号', children: selectedVehicle.plateNo },
              { key: 'orgName', label: '所属运输单位', children: selectedVehicle.orgName || '-' },
              { key: 'vehicleType', label: '车辆类型', children: selectedVehicle.vehicleType || '-' },
              { key: 'brandModel', label: '品牌型号', children: [selectedVehicle.brand, selectedVehicle.model].filter(Boolean).join(' / ') || '-' },
              { key: 'energyType', label: '能源类型', children: selectedVehicle.energyType || '-' },
              { key: 'axle', label: '轴数 / 载重', children: `${selectedVehicle.axleCount || '-'} 轴 / ${formatTons(selectedVehicle.loadWeight)} 吨` },
              { key: 'driver', label: '司机', children: [selectedVehicle.driverName, selectedVehicle.driverPhone].filter(Boolean).join(' / ') || '-' },
              { key: 'fleet', label: '车队', children: selectedVehicle.fleetName || '-' },
              { key: 'captain', label: '队长', children: [selectedVehicle.captainName, selectedVehicle.captainPhone].filter(Boolean).join(' / ') || '-' },
              { key: 'status', label: '车辆状态', children: <Space><Tag color={statusColorMap[selectedVehicle.statusLabel || ''] || 'default'}>{selectedVehicle.statusLabel || '未知'}</Tag><Tag color={runningStatusColorMap[selectedVehicle.runningStatusLabel || ''] || 'default'}>{selectedVehicle.runningStatusLabel || '未上报'}</Tag></Space> },
              { key: 'speedMileage', label: '速度 / 里程', children: `${Number(selectedVehicle.currentSpeed || 0).toFixed(1)} km/h / ${Number(selectedVehicle.currentMileage || 0).toFixed(1)} km` },
              { key: 'warning', label: '证照预警', children: <Tag color={warningColorMap[selectedVehicle.warningLabel || '正常'] || 'default'}>{selectedVehicle.warningLabel || '正常'}</Tag> },
              { key: 'maintain', label: '保养到期', children: selectedVehicle.nextMaintainDate || '-' },
              { key: 'annual', label: '年检到期', children: selectedVehicle.annualInspectionExpireDate || '-' },
              { key: 'insurance', label: '保险到期', children: selectedVehicle.insuranceExpireDate || '-' },
              { key: 'location', label: '位置坐标', span: 2, children: selectedVehicle.lng != null && selectedVehicle.lat != null ? `${selectedVehicle.lng}, ${selectedVehicle.lat}` : '-' },
              { key: 'gpsTime', label: '最近定位时间', span: 2, children: selectedVehicle.gpsTime || '-' },
              { key: 'remark', label: '备注', span: 2, children: selectedVehicle.remark || '-' },
            ]}
          />
        ) : (
          <Empty description={detailLoading ? '加载中...' : '暂无详情'} />
        )}
      </Drawer>

      <Modal
        title={editingVehicleId ? '编辑车辆档案' : '新增车辆'}
        open={createVisible}
        onCancel={() => {
          setCreateVisible(false);
          setEditingVehicleId(null);
          resetVehicleForm();
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
        width={760}
      >
        <Form
          form={createForm}
          layout="vertical"
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="plateNo" label="车牌号" rules={[{ required: true, message: '请输入车牌号' }]}>
                <Input placeholder="如：浙A55555" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="vin" label="VIN 码">
                <Input placeholder="请输入车辆识别代号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgId" label="所属运输单位" rules={[{ required: true, message: '请选择运输单位' }]}>
                <Select placeholder="请选择运输单位" options={companyOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="vehicleModelCode" label="车型模板">
                <Select
                  allowClear
                  showSearch
                  placeholder="选择已建车型自动带出参数"
                  options={vehicleModelOptions}
                  onChange={(value) => handleVehicleModelSelect(value)}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="vehicleType" label="车型" rules={[{ required: true, message: '请选择车型' }]}>
                <Select options={vehicleTypeOptions.map((item) => ({ value: item, label: item }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="loadWeight" label="核定载重(吨)" rules={[{ required: true, message: '请输入核定载重' }]}>
                <InputNumber min={1} precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="brand" label="品牌">
                <Input placeholder="如：中国重汽" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="model" label="型号">
                <Input placeholder="如：ZZ3257N3647A" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="energyType" label="能源类型">
                <Input placeholder="如：DIESEL / ELECTRIC" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="axleCount" label="轴数">
                <InputNumber min={1} precision={0} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deadWeight" label="车辆自重(吨)">
                <InputNumber min={0} precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="driverName" label="司机姓名">
                <Input placeholder="请输入司机姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="driverPhone" label="司机电话">
                <Input placeholder="请输入司机电话" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="fleetName" label="所属车队">
                <Input placeholder="如：宏基第一先锋车队" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="车辆状态">
                <Select options={statusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="captainName" label="车队负责人">
                <Input placeholder="请输入负责人姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="captainPhone" label="负责人电话">
                <Input placeholder="请输入负责人电话" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="annualInspectionExpireDate" label="年检到期日">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="nextMaintainDate" label="保养到期日">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="insuranceExpireDate" label="保险到期日">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="currentMileage" label="当前里程(km)">
                <InputNumber min={0} precision={1} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={3} placeholder="补充车辆运营说明" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </motion.div>
  );
};

export default VehiclesManagement;
