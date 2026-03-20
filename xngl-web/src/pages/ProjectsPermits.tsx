import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
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
import { EditOutlined, EyeOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import {
  createDisposalPermit,
  fetchDisposalPermitDetail,
  fetchDisposalPermits,
  updateDisposalPermit,
  type DisposalPermitRecord,
  type DisposalPermitUpsertPayload,
} from '../utils/permitApi';
import { fetchContractList, type ContractRecord } from '../utils/contractApi';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';
import { fetchVehicles, type VehicleRecord } from '../utils/vehicleApi';

const permitTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '排放证', value: 'DISPOSAL' },
  { label: '准运证', value: 'TRANSPORT' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '有效', value: 'ACTIVE' },
  { label: '即将到期', value: 'EXPIRING' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '作废', value: 'VOID' },
];

const resolvePermitType = (value?: string | null) => {
  if (value === 'DISPOSAL') return '排放证';
  if (value === 'TRANSPORT') return '准运证';
  return value || '-';
};

const resolveStatus = (value?: string | null) => {
  if (value === 'ACTIVE') return '有效';
  if (value === 'EXPIRING') return '即将到期';
  if (value === 'EXPIRED') return '已过期';
  if (value === 'VOID') return '作废';
  return value || '-';
};

const resolveBindStatus = (value?: string | null) => (value === 'BOUND' ? '已绑定' : '未绑定');

const statusColorMap: Record<string, string> = {
  ACTIVE: 'success',
  EXPIRING: 'warning',
  EXPIRED: 'error',
  VOID: 'default',
};

const ProjectsPermits: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [records, setRecords] = useState<DisposalPermitRecord[]>([]);
  const [contracts, setContracts] = useState<ContractRecord[]>([]);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);
  const [vehicles, setVehicles] = useState<VehicleRecord[]>([]);
  const [keyword, setKeyword] = useState('');
  const [permitType, setPermitType] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const [detailOpen, setDetailOpen] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<DisposalPermitRecord | null>(null);
  const [form] = Form.useForm<DisposalPermitUpsertPayload>();

  const projectNameMap = useMemo(
    () => Object.fromEntries(projects.map((item) => [String(item.id), item.name])),
    [projects],
  );
  const contractNameMap = useMemo(
    () =>
      Object.fromEntries(
        contracts.map((item) => [
          String(item.id),
          `${item.contractNo}${item.name ? ` / ${item.name}` : ''}`,
        ]),
      ),
    [contracts],
  );
  const siteNameMap = useMemo(
    () => Object.fromEntries(sites.map((item) => [String(item.id), item.name])),
    [sites],
  );

  const loadData = async () => {
    setLoading(true);
    try {
      const [permitList, contractPage, projectPage, siteList, vehiclePage] = await Promise.all([
        fetchDisposalPermits(),
        fetchContractList({ pageNo: 1, pageSize: 200 }),
        fetchProjects({ pageNo: 1, pageSize: 200 }),
        fetchSites(),
        fetchVehicles({ pageNo: 1, pageSize: 200 }),
      ]);
      setRecords(permitList);
      setContracts(contractPage.records || []);
      setProjects(projectPage.records || []);
      setSites(siteList || []);
      setVehicles(vehiclePage.records || []);
    } catch (error) {
      console.error(error);
      message.error('获取处置证数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const filteredRecords = useMemo(() => {
    const keywordValue = keyword.trim();
    return records.filter((item) => {
      const matchedKeyword =
        !keywordValue
        || item.permitNo.includes(keywordValue)
        || (item.vehicleNo || '').includes(keywordValue)
        || (projectNameMap[String(item.projectId || '')] || '').includes(keywordValue)
        || (contractNameMap[String(item.contractId || '')] || '').includes(keywordValue);
      const matchedType = permitType === 'ALL' || item.permitType === permitType;
      const matchedStatus = status === 'ALL' || item.status === status;
      return matchedKeyword && matchedType && matchedStatus;
    });
  }, [keyword, permitType, projectNameMap, records, status]);

  const openDetail = async (id: string | number) => {
    setDetailLoading(true);
    setDetailOpen(true);
    try {
      setCurrentRecord(await fetchDisposalPermitDetail(id));
    } catch (error) {
      console.error(error);
      message.error('获取处置证详情失败');
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const openCreate = () => {
    setCurrentRecord(null);
    form.resetFields();
    form.setFieldsValue({ permitType: 'DISPOSAL', status: 'ACTIVE', approvedVolume: 0, usedVolume: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: DisposalPermitRecord) => {
    setCurrentRecord(record);
    form.setFieldsValue({
      permitNo: record.permitNo,
      permitType: record.permitType || 'DISPOSAL',
      projectId: record.projectId ? Number(record.projectId) : undefined,
      contractId: record.contractId ? Number(record.contractId) : undefined,
      siteId: record.siteId ? Number(record.siteId) : undefined,
      vehicleNo: record.vehicleNo || undefined,
      issueDate: record.issueDate || undefined,
      expireDate: record.expireDate || undefined,
      approvedVolume: Number(record.approvedVolume || 0),
      usedVolume: Number(record.usedVolume || 0),
      status: record.status || 'ACTIVE',
      remark: record.remark || undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (currentRecord) {
        await updateDisposalPermit(currentRecord.id, values);
        message.success('处置证已更新');
      } else {
        await createDisposalPermit(values);
        message.success('处置证已新增');
      }
      setModalOpen(false);
      form.resetFields();
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('保存处置证失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const columns: ColumnsType<DisposalPermitRecord> = [
    {
      title: '处置证号',
      dataIndex: 'permitNo',
      key: 'permitNo',
      render: (value) => <span className="font-mono g-text-secondary">{value}</span>,
    },
    {
      title: '证件类型',
      dataIndex: 'permitType',
      key: 'permitType',
      render: (value) => <Tag color="blue">{resolvePermitType(value)}</Tag>,
    },
    {
      title: '关联项目 / 场地',
      key: 'relation',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{projectNameMap[String(record.projectId || '')] || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{contractNameMap[String(record.contractId || '')] || '未关联合同'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{siteNameMap[String(record.siteId || '')] || '-'}</span>
        </div>
      ),
    },
    {
      title: '绑定车牌',
      dataIndex: 'vehicleNo',
      key: 'vehicleNo',
      render: (value) => value || '-',
    },
    {
      title: '核准 / 已用方量',
      key: 'volume',
      render: (_, record) => `${Number(record.approvedVolume || 0)} / ${Number(record.usedVolume || 0)}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value) => <Tag color={statusColorMap[value || ''] || 'default'}>{resolveStatus(value)}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => void openDetail(record.id)}>
            查看
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">处置证清单</h1>
          <p className="g-text-secondary mt-1">支持手工新增、关联项目/场地/车辆并维护有效期</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增处置证
        </Button>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap gap-3 mb-4">
          <Input
            allowClear
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            prefix={<SearchOutlined />}
            placeholder="搜索证号 / 项目 / 车牌"
            className="w-72"
          />
          <Select value={permitType} options={permitTypeOptions} onChange={setPermitType} className="w-40" />
          <Select value={status} options={statusOptions} onChange={setStatus} className="w-40" />
        </div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={filteredRecords}
          locale={{ emptyText: <Empty description="暂无处置证数据" /> }}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Drawer title="处置证详情" open={detailOpen} onClose={() => setDetailOpen(false)} width={520}>
        {detailLoading ? (
          <div className="py-12 text-center g-text-secondary">处置证详情加载中...</div>
        ) : (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="处置证号">{currentRecord?.permitNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="证件类型">{resolvePermitType(currentRecord?.permitType)}</Descriptions.Item>
            <Descriptions.Item label="关联项目">{projectNameMap[String(currentRecord?.projectId || '')] || '-'}</Descriptions.Item>
            <Descriptions.Item label="关联合同">{contractNameMap[String(currentRecord?.contractId || '')] || '-'}</Descriptions.Item>
            <Descriptions.Item label="指定场地">{siteNameMap[String(currentRecord?.siteId || '')] || '-'}</Descriptions.Item>
            <Descriptions.Item label="绑定车辆">{currentRecord?.vehicleNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="有效期">{currentRecord?.issueDate || '-'} ~ {currentRecord?.expireDate || '-'}</Descriptions.Item>
            <Descriptions.Item label="核准 / 已用方量">{Number(currentRecord?.approvedVolume || 0)} / {Number(currentRecord?.usedVolume || 0)}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusColorMap[currentRecord?.status || ''] || 'default'}>{resolveStatus(currentRecord?.status)}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="绑定状态">{resolveBindStatus(currentRecord?.bindStatus)}</Descriptions.Item>
            <Descriptions.Item label="备注">{currentRecord?.remark || '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      <Modal
        title={currentRecord ? '编辑处置证' : '新增处置证'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
        width={720}
      >
        <Form form={form} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="permitNo" label="处置证号" rules={[{ required: true, message: '请输入处置证号' }]}>
              <Input placeholder="如 PZ-2026-003" />
            </Form.Item>
            <Form.Item name="permitType" label="证件类型" rules={[{ required: true, message: '请选择证件类型' }]}>
              <Select options={permitTypeOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="projectId" label="关联项目">
              <Select allowClear options={projects.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="contractId" label="关联合同">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                options={contracts.map((item) => ({
                  label: `${item.contractNo}${item.name ? ` / ${item.name}` : ''}`,
                  value: Number(item.id),
                }))}
              />
            </Form.Item>
            <Form.Item name="siteId" label="指定场地">
              <Select allowClear options={sites.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="vehicleNo" label="绑定车辆">
              <Select allowClear showSearch options={vehicles.map((item) => ({ label: item.plateNo, value: item.plateNo }))} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="issueDate" label="签发日期">
              <Input type="date" />
            </Form.Item>
            <Form.Item name="expireDate" label="到期日期">
              <Input type="date" />
            </Form.Item>
            <Form.Item name="approvedVolume" label="核准方量">
              <InputNumber min={0} precision={2} className="w-full" />
            </Form.Item>
            <Form.Item name="usedVolume" label="已用方量">
              <InputNumber min={0} precision={2} className="w-full" />
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="补充说明证件来源、关联说明等" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ProjectsPermits;
