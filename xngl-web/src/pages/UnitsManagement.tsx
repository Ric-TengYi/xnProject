import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
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
import { BankOutlined, CarOutlined, PlusOutlined, SearchOutlined, TeamOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import {
  createUnit,
  fetchUnitDetail,
  fetchUnitSummary,
  fetchUnits,
  updateUnit,
  type UnitDetailRecord,
  type UnitRecord,
  type UnitSummaryRecord,
  type UnitUpsertPayload,
} from '../utils/unitApi';

const unitTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '建设单位', value: 'CONSTRUCTION_UNIT' },
  { label: '施工单位', value: 'BUILDER_UNIT' },
  { label: '运输单位', value: 'TRANSPORT_COMPANY' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '正常', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const typeColorMap: Record<string, string> = {
  建设单位: 'blue',
  施工单位: 'gold',
  运输单位: 'green',
};

const statusColorMap: Record<string, string> = {
  正常: 'success',
  停用: 'error',
};

const UnitsManagement: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [records, setRecords] = useState<UnitRecord[]>([]);
  const [summary, setSummary] = useState<UnitSummaryRecord>({
    totalUnits: 0,
    constructionUnits: 0,
    builderUnits: 0,
    transportUnits: 0,
    totalVehicles: 0,
  });
  const [keyword, setKeyword] = useState('');
  const [unitType, setUnitType] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedUnit, setSelectedUnit] = useState<UnitDetailRecord | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUnit, setEditingUnit] = useState<UnitDetailRecord | null>(null);
  const [form] = Form.useForm<UnitUpsertPayload>();

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchUnitSummary());
    } catch (error) {
      console.error(error);
      message.error('获取单位概览失败');
    } finally {
      setSummaryLoading(false);
    }
  };

  const loadUnits = async () => {
    setLoading(true);
    try {
      const page = await fetchUnits({
        keyword: keyword.trim() || undefined,
        unitType,
        status,
        pageNo,
        pageSize,
      });
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取单位列表失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadSummary();
  }, []);

  useEffect(() => {
    void loadUnits();
  }, [keyword, unitType, status, pageNo, pageSize]);

  const openDetail = async (id: string) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setSelectedUnit(await fetchUnitDetail(id));
    } catch (error) {
      console.error(error);
      message.error('获取单位详情失败');
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const openEdit = async (record?: UnitRecord) => {
    if (!record) {
      setEditingUnit(null);
      form.resetFields();
      form.setFieldsValue({ orgType: 'CONSTRUCTION_UNIT', status: 'ENABLED' });
      setModalOpen(true);
      return;
    }
    try {
      const detail = await fetchUnitDetail(record.id);
      setEditingUnit(detail);
      form.setFieldsValue({
        orgName: detail.orgName,
        orgType: detail.orgType || 'CONSTRUCTION_UNIT',
        orgCode: detail.orgCode || undefined,
        contactPerson: detail.contactPerson || undefined,
        contactPhone: detail.contactPhone || undefined,
        address: detail.address || undefined,
        unifiedSocialCode: detail.unifiedSocialCode || undefined,
        remark: detail.remark || undefined,
        status: detail.status || 'ENABLED',
      });
      setModalOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取单位详情失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (editingUnit) {
        await updateUnit(editingUnit.id, values);
        message.success('单位信息已更新');
      } else {
        await createUnit(values);
        message.success('单位已新增');
      }
      setModalOpen(false);
      setEditingUnit(null);
      form.resetFields();
      setPageNo(1);
      await Promise.all([loadSummary(), loadUnits()]);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存单位失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const statsCards = useMemo(
    () => [
      {
        title: '单位总数',
        value: summary.totalUnits,
        prefix: <BankOutlined className="g-text-primary-link" />,
      },
      {
        title: '建设 / 施工',
        value: `${summary.constructionUnits} / ${summary.builderUnits}`,
        prefix: <TeamOutlined className="g-text-warning" />,
      },
      {
        title: '运输单位',
        value: summary.transportUnits,
        prefix: <CarOutlined className="g-text-success" />,
      },
      {
        title: '挂接车辆',
        value: summary.totalVehicles,
        prefix: <CarOutlined className="g-text-primary" />,
      },
    ],
    [summary]
  );

  const columns: ColumnsType<UnitRecord> = [
    {
      title: '单位名称',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value, record) => (
        <a style={{ color: 'var(--primary)' }} onClick={() => openDetail(record.id)}>
          {value}
        </a>
      ),
    },
    {
      title: '单位类型',
      dataIndex: 'orgTypeLabel',
      key: 'orgTypeLabel',
      render: (value?: string | null) => (
        <Tag color={typeColorMap[value || ''] || 'default'} className="border-none">
          {value || '未知'}
        </Tag>
      ),
    },
    {
      title: '联系人',
      key: 'contact',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{record.contactPerson || '-'}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.contactPhone || '-'}</span>
        </div>
      ),
    },
    {
      title: '关联业务',
      key: 'relations',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-secondary)' }}>项目 {record.projectCount || 0}</span>
          <span style={{ color: 'var(--text-secondary)' }}>合同 {record.contractCount || 0}</span>
        </div>
      ),
    },
    {
      title: '车辆挂接',
      key: 'vehicles',
      render: (_, record) => (
        <span style={{ color: 'var(--text-secondary)' }}>
          {record.activeVehicleCount || 0} / {record.vehicleCount || 0}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (value?: string | null) => (
        <Tag color={statusColorMap[value || ''] || 'default'} className="border-none">
          {value || '未知'}
        </Tag>
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
          <a style={{ color: 'var(--primary)' }} onClick={() => openEdit(record)}>
            编辑
          </a>
        </Space>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">单位管理</h1>
          <p className="g-text-secondary mt-1">统一维护建设单位、施工单位、运输单位，并关联项目、合同与车辆主数据</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={() => openEdit()}>
          新增单位
        </Button>
      </div>

      <Row gutter={[24, 24]}>
        {statsCards.map((item) => (
          <Col span={6} key={item.title}>
            <Card className="glass-panel g-border-panel border" loading={summaryLoading}>
              <Statistic title={<span className="g-text-secondary">{item.title}</span>} value={item.value} prefix={item.prefix} valueStyle={{ color: 'var(--text-primary)' }} />
            </Card>
          </Col>
        ))}
      </Row>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 justify-between g-bg-toolbar">
          <div className="flex gap-4 flex-wrap">
            <Input
              placeholder="搜索单位名称/编码/联系人"
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="w-72 bg-white g-border-panel border g-text-primary"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPageNo(1);
              }}
            />
            <Select
              value={unitType}
              options={unitTypeOptions}
              style={{ width: 160 }}
              onChange={(value) => {
                setUnitType(value);
                setPageNo(1);
              }}
            />
            <Select
              value={status}
              options={statusOptions}
              style={{ width: 140 }}
              onChange={(value) => {
                setStatus(value);
                setPageNo(1);
              }}
            />
          </div>
        </div>
        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: <Empty description="暂无单位数据" /> }}
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
        title={selectedUnit?.orgName || '单位详情'}
        width={720}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setSelectedUnit(null);
        }}
      >
        {selectedUnit ? (
          <Descriptions
            bordered
            column={2}
            items={[
              { key: 'name', label: '单位名称', children: selectedUnit.orgName },
              { key: 'type', label: '单位类型', children: selectedUnit.orgTypeLabel || '-' },
              { key: 'code', label: '单位编码', children: selectedUnit.orgCode || '-' },
              { key: 'credit', label: '统一社会信用代码', children: selectedUnit.unifiedSocialCode || '-' },
              { key: 'contact', label: '联系人', children: selectedUnit.contactPerson || '-' },
              { key: 'phone', label: '联系电话', children: selectedUnit.contactPhone || '-' },
              { key: 'status', label: '状态', children: <Tag color={statusColorMap[selectedUnit.statusLabel || ''] || 'default'}>{selectedUnit.statusLabel || '未知'}</Tag> },
              { key: 'vehicle', label: '车辆挂接', children: `${selectedUnit.activeVehicleCount || 0} / ${selectedUnit.vehicleCount || 0}` },
              { key: 'project', label: '关联项目', children: selectedUnit.projectCount || 0 },
              { key: 'contract', label: '关联合同', children: selectedUnit.contractCount || 0 },
              { key: 'address', label: '联系地址', span: 2, children: selectedUnit.address || '-' },
              { key: 'remark', label: '备注', span: 2, children: selectedUnit.remark || '-' },
            ]}
          />
        ) : (
          <Empty description={detailLoading ? '加载中...' : '暂无详情'} />
        )}
      </Drawer>

      <Modal
        title={editingUnit ? '编辑单位' : '新增单位'}
        open={modalOpen}
        width={720}
        onCancel={() => {
          setModalOpen(false);
          setEditingUnit(null);
          form.resetFields();
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ orgType: 'CONSTRUCTION_UNIT', status: 'ENABLED' }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="orgName" label="单位名称" rules={[{ required: true, message: '请输入单位名称' }]}>
                <Input placeholder="请输入单位名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgType" label="单位类型" rules={[{ required: true, message: '请选择单位类型' }]}>
                <Select options={unitTypeOptions.filter((item) => item.value !== 'ALL')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgCode" label="单位编码">
                <Input placeholder="不填则自动生成" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unifiedSocialCode" label="统一社会信用代码">
                <Input placeholder="请输入统一社会信用代码" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPerson" label="联系人">
                <Input placeholder="请输入联系人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPhone" label="联系电话">
                <Input placeholder="请输入联系电话" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="address" label="联系地址">
                <Input placeholder="请输入联系地址" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
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
    </motion.div>
  );
};

export default UnitsManagement;
