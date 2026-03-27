import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import {
  createVehicleModel,
  deleteVehicleModel,
  exportVehicleModels,
  fetchVehicleModelDetail,
  fetchVehicleModels,
  updateVehicleModel,
  updateVehicleModelStatus,
  type VehicleModelPayload,
  type VehicleModelRecord,
} from '../utils/vehicleModelApi';

const energyTypeOptions = [
  { label: '柴油', value: 'DIESEL' },
  { label: '电动', value: 'ELECTRIC' },
  { label: '混动', value: 'HYBRID' },
  { label: '天然气', value: 'GAS' },
];

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const VehicleModelsManagement: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<VehicleModelRecord[]>([]);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<VehicleModelRecord | null>(null);
  const [form] = Form.useForm<VehicleModelPayload>();

  const loadData = async () => {
    setLoading(true);
    try {
      setRecords(await fetchVehicleModels());
    } catch (error) {
      console.error(error);
      message.error('获取车型库失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const filteredRecords = useMemo(() => {
    const value = keyword.trim();
    if (!value) {
      return records;
    }
    return records.filter(
      (item) =>
        item.modelCode.includes(value) ||
        item.brand.includes(value) ||
        item.modelName.includes(value) ||
        (item.vehicleType || '').includes(value),
    );
  }, [keyword, records]);

  const summary = useMemo(() => {
    const enabledCount = records.filter((item) => item.status === 'ENABLED').length;
    const totalLoadWeight = records.reduce((sum, item) => sum + Number(item.loadWeight || 0), 0);
    return {
      total: records.length,
      enabledCount,
      electricCount: records.filter((item) => item.energyType === 'ELECTRIC').length,
      totalLoadWeight,
    };
  }, [records]);

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      energyType: 'DIESEL',
      status: 'ENABLED',
      seatCount: 2,
      axleCount: 2,
    });
    setModalOpen(true);
  };

  const openEdit = async (record: VehicleModelRecord) => {
    try {
      const detail = await fetchVehicleModelDetail(record.id);
      setEditingRecord(detail);
      form.setFieldsValue({
        modelCode: detail.modelCode,
        brand: detail.brand,
        modelName: detail.modelName,
        vehicleType: detail.vehicleType || undefined,
        axleCount: detail.axleCount || undefined,
        seatCount: detail.seatCount || undefined,
        deadWeight: detail.deadWeight || undefined,
        loadWeight: detail.loadWeight || undefined,
        energyType: detail.energyType || undefined,
        status: detail.status || 'ENABLED',
        remark: detail.remark || undefined,
      });
      setModalOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取车型详情失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (editingRecord) {
        await updateVehicleModel(editingRecord.id, values);
        message.success('车型已更新');
      } else {
        await createVehicleModel(values);
        message.success('车型已新增');
      }
      setModalOpen(false);
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存车型失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteVehicleModel(id);
      message.success('车型已删除');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('删除车型失败');
    }
  };

  const toggleStatus = async (record: VehicleModelRecord, checked: boolean) => {
    try {
      await updateVehicleModelStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('更新状态失败');
    }
  };

  const handleExport = async () => {
    try {
      const blob = await exportVehicleModels({ keyword });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'vehicle_models.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('车型库已导出');
    } catch (error) {
      console.error(error);
      message.error('导出车型库失败');
    }
  };

  const columns: ColumnsType<VehicleModelRecord> = [
    {
      title: '车型编码',
      dataIndex: 'modelCode',
      key: 'modelCode',
      render: (value) => <span className="font-mono">{value}</span>,
    },
    {
      title: '品牌 / 车型',
      key: 'model',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>
            {record.brand} / {record.modelName}
          </span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.vehicleType || '-'}</span>
        </div>
      ),
    },
    {
      title: '轴数 / 座位',
      key: 'meta',
      render: (_, record) => `${record.axleCount || '-'} 轴 / ${record.seatCount || '-'} 座`,
    },
    {
      title: '自重 / 载重',
      key: 'weight',
      render: (_, record) =>
        `${Number(record.deadWeight || 0).toFixed(2)} 吨 / ${Number(record.loadWeight || 0).toFixed(2)} 吨`,
    },
    {
      title: '能源类型',
      dataIndex: 'energyType',
      key: 'energyType',
      render: (value) => <Tag color={value === 'ELECTRIC' ? 'green' : 'blue'}>{value || '-'}</Tag>,
    },
    {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          onChange={(checked) => void toggleStatus(record, checked)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => void openEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确认删除当前车型？" onConfirm={() => void handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增车型
        </Button>
      </div>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card className="glass-panel g-border-panel border">
          <Statistic title="车型总数" value={summary.total} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="启用车型" value={summary.enabledCount} valueStyle={{ color: '#16a34a' }} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="新能源车型" value={summary.electricCount} valueStyle={{ color: '#1677ff' }} />
        </Card>
        <Card className="glass-panel g-border-panel border">
          <Statistic title="总载重(吨)" value={summary.totalLoadWeight.toFixed(2)} />
        </Card>

      <Card className="glass-panel g-border-panel border">
        <div className="flex justify-between items-center gap-4 mb-4">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索编码 / 品牌 / 车型"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="max-w-md"
          />
          <Button onClick={() => void handleExport()}>导出车型库</Button>
        </div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={filteredRecords}
          pagination={false}
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑车型' : '新增车型'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Form.Item
              name="modelCode"
              label="车型编码"
              rules={[{ required: true, message: '请输入车型编码' }]}
            >
              <Input placeholder="如 HOWO-8X4" />
            </Form.Item>
            <Form.Item
              name="brand"
              label="品牌"
              rules={[{ required: true, message: '请输入品牌' }]}
            >
              <Input placeholder="如 中国重汽" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Form.Item
              name="modelName"
              label="车型名称"
              rules={[{ required: true, message: '请输入车型名称' }]}
            >
              <Input placeholder="如 豪沃 TX 8x4" />
            </Form.Item>
            <Form.Item name="vehicleType" label="车辆类型">
              <Input placeholder="如 重型自卸货车" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Form.Item name="axleCount" label="轴数">
              <InputNumber min={1} className="w-full" />
            </Form.Item>
            <Form.Item name="seatCount" label="座位数">
              <InputNumber min={1} className="w-full" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Form.Item name="deadWeight" label="自重(吨)">
              <InputNumber min={0} precision={2} className="w-full" />
            </Form.Item>
            <Form.Item name="loadWeight" label="载重(吨)">
              <InputNumber min={0} precision={2} className="w-full" />
            </Form.Item>
          </div>
          <Form.Item name="energyType" label="能源类型">
            <Select allowClear options={energyTypeOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
  );
};
export default VehicleModelsManagement;
