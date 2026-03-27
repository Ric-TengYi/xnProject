import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
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
import { PlusOutlined, SafetyCertificateOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import {
  createVehicleInsurance,
  deleteVehicleInsurance,
  exportVehicleInsurances,
  fetchVehicleInsurances,
  fetchVehicleInsuranceSummary,
  updateVehicleInsurance,
  type VehicleInsuranceRecord,
  type VehicleInsuranceSummaryRecord,
  type VehicleInsuranceUpsertPayload,
} from '../utils/vehicleInsuranceApi';
import { fetchVehicleCompanyCapacity, fetchVehicles } from '../utils/vehicleApi';

const { RangePicker } = DatePicker;

type InsuranceFormValues = {
  vehicleId: string;
  policyNo: string;
  insuranceType?: string;
  insurerName?: string;
  coverageAmount?: number;
  premiumAmount?: number;
  claimAmount?: number;
  startDate: Dayjs;
  endDate: Dayjs;
  remark?: string;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '有效', value: 'ACTIVE' },
  { label: '即将到期', value: 'EXPIRING' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已失效', value: 'CANCELLED' },
];

const statusColorMap: Record<string, string> = {
  ACTIVE: 'success',
  EXPIRING: 'warning',
  EXPIRED: 'error',
  CANCELLED: 'default',

const expiringOptions = [
  { label: '全部到期范围', value: 'all' },
  { label: '7天内到期', value: '7' },
  { label: '30天内到期', value: '30' },
  { label: '60天内到期', value: '60' },
];

const defaultSummary: VehicleInsuranceSummaryRecord = {
  totalPolicies: 0,
  activePolicies: 0,
  expiringPolicies: 0,
  expiredPolicies: 0,
  totalCoverageAmount: 0,
  totalPremiumAmount: 0,
  totalClaimAmount: 0,

const VehicleInsurances: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<VehicleInsuranceRecord[]>([]);
  const [summary, setSummary] = useState<VehicleInsuranceSummaryRecord>(defaultSummary);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('all');
  const [orgId, setOrgId] = useState<string | undefined>(undefined);
  const [vehicleId, setVehicleId] = useState<string | undefined>(undefined);
  const [effectiveRange, setEffectiveRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [expiringWithinDays, setExpiringWithinDays] = useState('all');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [companyOptions, setCompanyOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [vehicleOptions, setVehicleOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<VehicleInsuranceRecord | null>(null);
  const [form] = Form.useForm<InsuranceFormValues>();

  const queryParams = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      status: status === 'all' ? undefined : status,
      orgId,
      vehicleId,
      endDateFrom: effectiveRange?.[0]?.format('YYYY-MM-DD'),
      endDateTo: effectiveRange?.[1]?.format('YYYY-MM-DD'),
      expiringWithinDays: expiringWithinDays === 'all' ? undefined : Number(expiringWithinDays),
    }),
    [effectiveRange, expiringWithinDays, keyword, orgId, status, vehicleId]
  );

  const loadList = async () => {
    setLoading(true);
    try {
      const page = await fetchVehicleInsurances({ ...queryParams, pageNo, pageSize });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取保险记录失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchVehicleInsuranceSummary(queryParams));
    } catch (error) {
      console.error(error);
      message.error('获取保险统计失败');
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
    setOrgId(undefined);
    setVehicleId(undefined);
    setEffectiveRange(null);
    setExpiringWithinDays('all');
    setPageNo(1);
  };

  const handleExport = async () => {
    try {
      const blob = await exportVehicleInsurances(queryParams);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'vehicle_insurances.csv';
      link.click();
      window.URL.revokeObjectURL(url);
      message.success('保险台账已导出');
    } catch (error) {
      console.error(error);
    }
  };

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      startDate: dayjs(),
      endDate: dayjs().add(1, 'year'),
      coverageAmount: 0,
      premiumAmount: 0,
      claimAmount: 0,
    });
    setModalOpen(true);
  };

  const openEdit = (record: VehicleInsuranceRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      vehicleId: record.vehicleId || undefined,
      policyNo: record.policyNo,
      insuranceType: record.insuranceType || undefined,
      insurerName: record.insurerName || undefined,
      coverageAmount: record.coverageAmount,
      premiumAmount: record.premiumAmount,
      claimAmount: record.claimAmount,
      startDate: record.startDate ? dayjs(record.startDate) : dayjs(),
      endDate: record.endDate ? dayjs(record.endDate) : dayjs(),
      remark: record.remark || undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: VehicleInsuranceUpsertPayload = {
        vehicleId: Number(values.vehicleId),
        policyNo: values.policyNo,
        insuranceType: values.insuranceType,
        insurerName: values.insurerName,
        coverageAmount: values.coverageAmount,
        premiumAmount: values.premiumAmount,
        claimAmount: values.claimAmount,
        startDate: values.startDate.format('YYYY-MM-DD'),
        endDate: values.endDate.format('YYYY-MM-DD'),
        remark: values.remark,
      };
      setSubmitLoading(true);
      if (editingRecord) {
        await updateVehicleInsurance(editingRecord.id, payload);
        message.success('保险记录已更新');
      } else {
        await createVehicleInsurance(payload);
        message.success('保险记录已新增');
      }
      setModalOpen(false);
      form.resetFields();
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

  const handleDelete = async (id: string) => {
    try {
      await deleteVehicleInsurance(id);
      message.success('保险记录已删除');
      setPageNo(1);
      await Promise.all([loadSummary(), loadList()]);
    } catch (error) {
      console.error(error);
      message.error('删除保险记录失败');
    }
  };

  const columns: ColumnsType<VehicleInsuranceRecord> = [
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
      title: '保单号',
      dataIndex: 'policyNo',
      key: 'policyNo',
      render: (value: string) => <span className="font-mono g-text-secondary">{value}</span>,
    },
    {
      title: '险种',
      dataIndex: 'insuranceType',
      key: 'insuranceType',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '承保公司',
      dataIndex: 'insurerName',
      key: 'insurerName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '保额(元)',
      dataIndex: 'coverageAmount',
      key: 'coverageAmount',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '保费(元)',
      dataIndex: 'premiumAmount',
      key: 'premiumAmount',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '到期日',
      dataIndex: 'endDate',
      key: 'endDate',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '剩余天数',
      dataIndex: 'remainingDays',
      key: 'remainingDays',
      render: (value?: number | null) => (value == null ? '-' : value),
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
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除当前保单记录？" onConfirm={() => void handleDelete(record.id)}>
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">保险管理</h1>
          <p className="g-text-secondary mt-1">集中维护车辆保险台账，并回写车辆证照到期预警数据。</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增保单
        </Button>
      </div>
      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="保单总数" value={summary.totalPolicies} prefix={<SafetyCertificateOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="有效保单" value={summary.activePolicies} prefix={<SafetyCertificateOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="即将到期" value={summary.expiringPolicies} prefix={<SafetyCertificateOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="总保费(元)" value={summary.totalPremiumAmount} precision={2} prefix={<SafetyCertificateOutlined />} />
          </Card>
        </Col>
      </Row>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap justify-between gap-4 mb-4">
          <Space wrap>
            <Input
              allowClear
              placeholder="搜索车牌/单位/保单号/险种"
              prefix={<SearchOutlined className="g-text-secondary" />}
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setPageNo(1);
              }}
              className="w-72"
            />
            <Select
              value={status}
              options={statusOptions}
              onChange={(value) => {
                setStatus(value);
                setPageNo(1);
              }}
              className="w-36"
            />
            <Select
              allowClear
              value={orgId}
              options={companyOptions}
              placeholder="所属单位"
              onChange={(value) => {
                setOrgId(value);
                setVehicleId(undefined);
                setPageNo(1);
              }}
              className="w-56"
            />
            <Select
              allowClear
              value={vehicleId}
              options={vehicleOptions}
              placeholder="车辆筛选"
              showSearch
              optionFilterProp="label"
              onChange={(value) => {
                setVehicleId(value);
                setPageNo(1);
              }}
              className="w-72"
            />
            <RangePicker
              value={effectiveRange}
              onChange={(value) => {
                setEffectiveRange(value as [Dayjs, Dayjs] | null);
                setPageNo(1);
              }}
              placeholder={['到期开始', '到期结束']}
            />
            <Select
              value={expiringWithinDays}
              options={expiringOptions}
              onChange={(value) => {
                setExpiringWithinDays(value);
                setPageNo(1);
              }}
              className="w-36"
            />
          </Space>
          <Space>
            <Button onClick={handleReset}>重置</Button>
            <Button onClick={() => void handleExport()}>导出台账</Button>
          </Space>
        </div>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={records}
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
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑保险记录' : '新增保险记录'}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="vehicleId" label="车辆" rules={[{ required: true, message: '请选择车辆' }]}>
            <Select showSearch optionFilterProp="label" options={vehicleOptions} placeholder="请选择车辆" />
          </Form.Item>
          <Form.Item name="policyNo" label="保单号" rules={[{ required: true, message: '请输入保单号' }]}>
            <Input placeholder="请输入保单号" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="insuranceType" label="险种">
                <Input placeholder="如交强险/商业险" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="insurerName" label="承保公司">
                <Input placeholder="请输入承保公司" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="coverageAmount" label="保额(元)">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="premiumAmount" label="保费(元)">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="claimAmount" label="赔付(元)">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="startDate" label="开始日期" rules={[{ required: true, message: '请选择开始日期' }]}>
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="endDate" label="结束日期" rules={[{ required: true, message: '请选择结束日期' }]}>
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="补充说明" />
          </Form.Item>
        </Form>
      </Modal>
  );
};
export default VehicleInsurances;
