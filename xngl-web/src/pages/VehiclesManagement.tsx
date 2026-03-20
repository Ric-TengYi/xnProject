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
import type { Dayjs } from 'dayjs';
import {
  createVehicle,
  fetchVehicleCompanyCapacity,
  fetchVehicleDetail,
  fetchVehicleStats,
  fetchVehicles,
  type VehicleCompanyCapacityRecord,
  type VehicleDetailRecord,
  type VehicleRecord,
  type VehicleStatsRecord,
  type VehicleUpsertPayload,
} from '../utils/vehicleApi';

type VehicleFormValues = Omit<
  VehicleUpsertPayload,
  'orgId' | 'nextMaintainDate' | 'annualInspectionExpireDate' | 'insuranceExpireDate'
> & {
  orgId?: string;
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

const vehicleTypeOptions = [
  '重型自卸货车',
  '中型自卸货车',
  '轻型自卸货车',
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
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleDetailRecord | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [filterVisible, setFilterVisible] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | 'all'>('all');
  const [orgId, setOrgId] = useState<string | undefined>(undefined);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
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
    const loadVehicles = async () => {
      setListLoading(true);
      try {
        const page = await fetchVehicles({
          keyword: keyword.trim() || undefined,
          status: status === 'all' ? undefined : status,
          orgId: orgId || undefined,
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
  }, [keyword, status, orgId, pageNo, pageSize]);

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
    setPageNo(1);
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      const payload: VehicleUpsertPayload = {
        ...values,
        orgId: values.orgId ? Number(values.orgId) : undefined,
        nextMaintainDate: values.nextMaintainDate?.format('YYYY-MM-DD'),
        annualInspectionExpireDate: values.annualInspectionExpireDate?.format('YYYY-MM-DD'),
        insuranceExpireDate: values.insuranceExpireDate?.format('YYYY-MM-DD'),
      };
      setSubmitLoading(true);
      await createVehicle(payload);
      message.success('车辆已新增');
      setCreateVisible(false);
      createForm.resetFields();
      setPageNo(1);
      await Promise.all([
        refreshOverview(),
        (async () => {
          const page = await fetchVehicles({
            keyword: keyword.trim() || undefined,
            status: status === 'all' ? undefined : status,
            orgId: orgId || undefined,
            pageNo: 1,
            pageSize,
          });
          setVehicles(page.records || []);
          setTotal(page.total || 0);
        })(),
      ]);
    } catch (error) {
      if (error instanceof Error) {
        console.error(error);
      }
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      message.error('新增车辆失败');
    } finally {
      setSubmitLoading(false);
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
            <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={() => setCreateVisible(true)}>
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
          }}
        >
          <Form.Item name="vehicleType" label="车型">
            <Select
              allowClear
              placeholder="请选择车型"
              options={vehicleTypeOptions.map((item) => ({ value: item, label: item }))}
              disabled
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
        title="新增车辆"
        open={createVisible}
        onCancel={() => {
          setCreateVisible(false);
          createForm.resetFields();
        }}
        onOk={() => void handleCreate()}
        confirmLoading={submitLoading}
        width={760}
      >
        <Form
          form={createForm}
          layout="vertical"
          initialValues={{
            status: 1,
            useStatus: 'ACTIVE',
            runningStatus: 'STOPPED',
            energyType: 'DIESEL',
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="plateNo" label="车牌号" rules={[{ required: true, message: '请输入车牌号' }]}>
                <Input placeholder="如：浙A55555" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgId" label="所属运输单位" rules={[{ required: true, message: '请选择运输单位' }]}>
                <Select placeholder="请选择运输单位" options={companyOptions} />
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
              <Form.Item name="nextMaintainDate" label="保养到期日">
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="insuranceExpireDate" label="保险到期日">
                <DatePicker className="w-full" />
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
