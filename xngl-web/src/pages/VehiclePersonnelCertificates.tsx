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
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { IdcardOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import {
  createVehiclePersonnelCertificate,
  exportVehiclePersonnelCertificates,
  fetchVehiclePersonnelCertificates,
  fetchVehiclePersonnelCertificateSummary,
  updateVehiclePersonnelCertificate,
  type VehiclePersonnelCertificateRecord,
  type VehiclePersonnelCertificateSummaryRecord,
  type VehiclePersonnelCertificateUpsertPayload,
} from '../utils/vehiclePersonnelApi';
import { fetchVehicleCompanyCapacity, fetchVehicles } from '../utils/vehicleApi';

type PersonnelFormValues = {
  orgId?: string;
  vehicleId?: string;
  personName: string;
  mobile?: string;
  roleType?: string;
  idCardNo?: string;
  driverLicenseNo?: string;
  driverLicenseExpireDate?: Dayjs;
  transportLicenseNo?: string;
  transportLicenseExpireDate?: Dayjs;
  feeAmount?: number;
  paidAmount?: number;
  feeDueDate?: Dayjs;
  status?: string;
  remark?: string;
};

const roleOptions = [
  { label: '全部角色', value: 'all' },
  { label: '司机', value: 'DRIVER' },
  { label: '队长', value: 'CAPTAIN' },
  { label: '调度员', value: 'DISPATCHER' },
  { label: '安全员', value: 'SAFETY_OFFICER' },
  { label: '后勤', value: 'LOGISTICS' },
];

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '有效', value: 'ACTIVE' },
  { label: '即将到期', value: 'EXPIRING' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已停用', value: 'DISABLED' },
];

const statusColorMap: Record<string, string> = {
  ACTIVE: 'success',
  EXPIRING: 'warning',
  EXPIRED: 'error',
  DISABLED: 'default',
};

const defaultSummary: VehiclePersonnelCertificateSummaryRecord = {
  totalPersons: 0,
  activeCertificates: 0,
  expiringCertificates: 0,
  expiredCertificates: 0,
  totalFeeAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0,
};

const { RangePicker } = DatePicker;

const VehiclePersonnelCertificates: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<VehiclePersonnelCertificateRecord[]>([]);
  const [summary, setSummary] = useState<VehiclePersonnelCertificateSummaryRecord>(defaultSummary);
  const [keyword, setKeyword] = useState('');
  const [roleType, setRoleType] = useState('all');
  const [status, setStatus] = useState('all');
  const [orgId, setOrgId] = useState<string | undefined>(undefined);
  const [vehicleId, setVehicleId] = useState<string | undefined>(undefined);
  const [feeDueRange, setFeeDueRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [expireWithinDays, setExpireWithinDays] = useState<number | undefined>(undefined);
  const [unpaidOnly, setUnpaidOnly] = useState(false);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [companyOptions, setCompanyOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [vehicleOptions, setVehicleOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<VehiclePersonnelCertificateRecord | null>(null);
  const [form] = Form.useForm<PersonnelFormValues>();

  const queryParams = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      roleType: roleType === 'all' ? undefined : roleType,
      status: status === 'all' ? undefined : status,
      orgId,
      vehicleId,
      feeDueDateFrom: feeDueRange?.[0]?.format('YYYY-MM-DD'),
      feeDueDateTo: feeDueRange?.[1]?.format('YYYY-MM-DD'),
      expireWithinDays,
      unpaidOnly: unpaidOnly || undefined,
    }),
    [keyword, roleType, status, orgId, vehicleId, feeDueRange, expireWithinDays, unpaidOnly]
  );

  const loadList = async () => {
    setLoading(true);
    try {
      const page = await fetchVehiclePersonnelCertificates({
        ...queryParams,
        pageNo,
        pageSize,
      });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取人证管理列表失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchVehiclePersonnelCertificateSummary(queryParams));
    } catch (error) {
      console.error(error);
      message.error('获取人证统计失败');
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
    setRoleType('all');
    setStatus('all');
    setOrgId(undefined);
    setVehicleId(undefined);
    setFeeDueRange(null);
    setExpireWithinDays(undefined);
    setUnpaidOnly(false);
    setPageNo(1);
  };

  const handleExport = async () => {
    try {
      const blob = await exportVehiclePersonnelCertificates(queryParams);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'vehicle_personnel_certificates.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('人证台账导出成功');
    } catch (error) {
      console.error(error);
      message.error('人证台账导出失败');
    }
  };

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      roleType: 'DRIVER',
      status: 'ACTIVE',
      feeAmount: 0,
      paidAmount: 0,
      driverLicenseExpireDate: dayjs().add(1, 'year'),
      transportLicenseExpireDate: dayjs().add(8, 'month'),
      feeDueDate: dayjs().add(3, 'month'),
    });
    setModalOpen(true);
  };

  const openEdit = (record: VehiclePersonnelCertificateRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      orgId: record.orgId || undefined,
      vehicleId: record.vehicleId || undefined,
      personName: record.personName,
      mobile: record.mobile || undefined,
      roleType: record.roleType || 'DRIVER',
      idCardNo: record.idCardNo || undefined,
      driverLicenseNo: record.driverLicenseNo || undefined,
      driverLicenseExpireDate: record.driverLicenseExpireDate
        ? dayjs(record.driverLicenseExpireDate)
        : undefined,
      transportLicenseNo: record.transportLicenseNo || undefined,
      transportLicenseExpireDate: record.transportLicenseExpireDate
        ? dayjs(record.transportLicenseExpireDate)
        : undefined,
      feeAmount: record.feeAmount,
      paidAmount: record.paidAmount,
      feeDueDate: record.feeDueDate ? dayjs(record.feeDueDate) : undefined,
      status: record.status,
      remark: record.remark || undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: VehiclePersonnelCertificateUpsertPayload = {
        orgId: values.orgId ? Number(values.orgId) : undefined,
        vehicleId: values.vehicleId ? Number(values.vehicleId) : undefined,
        personName: values.personName,
        mobile: values.mobile,
        roleType: values.roleType,
        idCardNo: values.idCardNo,
        driverLicenseNo: values.driverLicenseNo,
        driverLicenseExpireDate: values.driverLicenseExpireDate?.format('YYYY-MM-DD'),
        transportLicenseNo: values.transportLicenseNo,
        transportLicenseExpireDate: values.transportLicenseExpireDate?.format('YYYY-MM-DD'),
        feeAmount: values.feeAmount,
        paidAmount: values.paidAmount,
        feeDueDate: values.feeDueDate?.format('YYYY-MM-DD'),
        status: values.status,
        remark: values.remark,
      };
      setSubmitLoading(true);
      if (editingRecord) {
        await updateVehiclePersonnelCertificate(editingRecord.id, payload);
        message.success('人证记录已更新');
      } else {
        await createVehiclePersonnelCertificate(payload);
        message.success('人证记录已新增');
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

  const columns: ColumnsType<VehiclePersonnelCertificateRecord> = [
    {
      title: '人员',
      key: 'person',
      render: (_, record) => (
        <div>
          <div className="font-semibold g-text-primary">{record.personName}</div>
          <div className="text-xs g-text-secondary">{record.mobile || '-'}</div>
        </div>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roleTypeLabel',
      key: 'roleTypeLabel',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '所属单位/车辆',
      key: 'orgVehicle',
      render: (_, record) => (
        <div>
          <div>{record.orgName || '未归属单位'}</div>
          <div className="text-xs g-text-secondary">{record.plateNo || '未关联车辆'}</div>
        </div>
      ),
    },
    {
      title: '驾驶证',
      key: 'driverLicense',
      render: (_, record) => (
        <div>
          <div>{record.driverLicenseNo || '-'}</div>
          <div className="text-xs g-text-secondary">到期：{record.driverLicenseExpireDate || '-'}</div>
        </div>
      ),
    },
    {
      title: '营运证',
      key: 'transportLicense',
      render: (_, record) => (
        <div>
          <div>{record.transportLicenseNo || '-'}</div>
          <div className="text-xs g-text-secondary">到期：{record.transportLicenseExpireDate || '-'}</div>
        </div>
      ),
    },
    {
      title: '费用(元)',
      key: 'fee',
      render: (_, record) => (
        <div>
          <div>应缴：{record.feeAmount.toFixed(2)}</div>
          <div className="text-xs g-text-secondary">已缴：{record.paidAmount.toFixed(2)}</div>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (_, record) => (
        <Space size={6}>
          <Tag color={statusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>
          {record.remainingDays != null ? (
            <span className="text-xs g-text-secondary">{record.remainingDays} 天</span>
          ) : null}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => openEdit(record)}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">人证管理</h1>
          <p className="g-text-secondary mt-1">
            管理司机、队长等人员证件有效期及相关费用台账。
          </p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增人证
        </Button>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="人员总数" value={summary.totalPersons} prefix={<IdcardOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="有效证件" value={summary.activeCertificates} prefix={<IdcardOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="即将到期" value={summary.expiringCertificates} prefix={<IdcardOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="未缴费用(元)" value={summary.unpaidAmount} precision={2} prefix={<IdcardOutlined />} />
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
            placeholder="搜索姓名/手机号/证件号/车牌"
            prefix={<SearchOutlined />}
            style={{ width: 260 }}
          />
          <Select
            value={roleType}
            onChange={(value) => {
              setRoleType(value);
              setPageNo(1);
            }}
            options={roleOptions}
            style={{ width: 160 }}
          />
          <Select
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPageNo(1);
            }}
            options={statusOptions}
            style={{ width: 160 }}
          />
          <Select
            allowClear
            placeholder="所属单位"
            value={orgId}
            onChange={(value) => {
              setOrgId(value);
              setPageNo(1);
            }}
            options={companyOptions}
            style={{ width: 220 }}
          />
          <Select
            allowClear
            placeholder="车辆"
            value={vehicleId}
            onChange={(value) => {
              setVehicleId(value);
              setPageNo(1);
            }}
            options={vehicleOptions}
            showSearch
            optionFilterProp="label"
            style={{ width: 260 }}
          />
          <RangePicker
            value={feeDueRange}
            onChange={(value) => {
              setFeeDueRange(value);
              setPageNo(1);
            }}
            placeholder={['费用到期开始', '费用到期结束']}
          />
          <Select
            allowClear
            placeholder="到期预警"
            value={expireWithinDays}
            onChange={(value) => {
              setExpireWithinDays(value);
              setPageNo(1);
            }}
            options={[
              { label: '7天内到期', value: 7 },
              { label: '30天内到期', value: 30 },
              { label: '60天内到期', value: 60 },
            ]}
            style={{ width: 160 }}
          />
          <Select
            value={unpaidOnly ? 'YES' : 'ALL'}
            onChange={(value) => {
              setUnpaidOnly(value === 'YES');
              setPageNo(1);
            }}
            options={[
              { label: '全部费用状态', value: 'ALL' },
              { label: '仅看未缴费用', value: 'YES' },
            ]}
            style={{ width: 160 }}
          />
          <Button onClick={() => void handleExport()}>导出台账</Button>
          <Button onClick={handleReset}>重置</Button>
        </div>
        <Table<VehiclePersonnelCertificateRecord>
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

      <Modal
        title={editingRecord ? '编辑人证记录' : '新增人证记录'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitLoading}
        width={820}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="personName" label="人员姓名" rules={[{ required: true, message: '请输入人员姓名' }]}>
                <Input placeholder="请输入人员姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="mobile" label="手机号">
                <Input placeholder="请输入手机号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="roleType" label="角色类型">
                <Select options={roleOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select options={statusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgId" label="所属单位">
                <Select options={companyOptions} allowClear showSearch optionFilterProp="label" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="vehicleId" label="关联车辆">
                <Select options={vehicleOptions} allowClear showSearch optionFilterProp="label" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="idCardNo" label="身份证号">
                <Input placeholder="请输入身份证号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="driverLicenseNo" label="驾驶证号">
                <Input placeholder="请输入驾驶证号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="driverLicenseExpireDate" label="驾驶证到期日">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transportLicenseNo" label="营运证号">
                <Input placeholder="请输入营运证号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transportLicenseExpireDate" label="营运证到期日">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="feeDueDate" label="费用到期日">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="feeAmount" label="应缴费用(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="paidAmount" label="已缴费用(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
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
    </div>
  );
};

export default VehiclePersonnelCertificates;
