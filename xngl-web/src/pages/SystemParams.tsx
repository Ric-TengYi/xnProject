import React, { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, ExportOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import {
  createSysParam,
  deleteSysParam,
  exportSysParams,
  fetchSysParams,
  updateSysParam,
  updateSysParamStatus,
  type SysParamPayload,
  type SysParamRecord,
} from '../utils/sysParamApi';

const paramTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '字符串', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: 'JSON', value: 'JSON' },
];

const SystemParams: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<SysParamRecord[]>([]);
  const [keyword, setKeyword] = useState('');
  const [paramType, setParamType] = useState('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<SysParamRecord | null>(null);
  const [exporting, setExporting] = useState(false);
  const [form] = Form.useForm<SysParamPayload>();

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      setRecords(
        await fetchSysParams({
          keyword: keyword.trim() || undefined,
          paramType: paramType === 'ALL' ? undefined : paramType,
        }),
      );
    } catch (error) {
      console.error(error);
      message.error('获取系统参数失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [keyword, paramType]);

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({ paramType: 'STRING', status: 'ENABLED' });
    setModalOpen(true);
  };

  const openEdit = (record: SysParamRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      paramKey: record.paramKey,
      paramName: record.paramName,
      paramValue: record.paramValue || undefined,
      paramType: record.paramType,
      status: record.status,
      remark: record.remark || undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (editingRecord) {
        await updateSysParam(editingRecord.id, values);
        message.success('系统参数已更新');
      } else {
        await createSysParam(values);
        message.success('系统参数已新增');
      }
      setModalOpen(false);
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('保存系统参数失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      setExporting(true);
      downloadBlob(
        await exportSysParams({
          keyword: keyword.trim() || undefined,
          paramType: paramType === 'ALL' ? undefined : paramType,
        }),
        'sys_params.csv',
      );
      message.success('系统参数导出成功');
    } catch (error) {
      console.error(error);
      message.error('系统参数导出失败');
    } finally {
      setExporting(false);
    }
  };

  const toggleStatus = async (record: SysParamRecord, checked: boolean) => {
    try {
      await updateSysParamStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('更新状态失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteSysParam(id);
      message.success('系统参数已删除');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('删除系统参数失败');
    }
  };

  const columns: ColumnsType<SysParamRecord> = [
    {
      title: '参数键',
      dataIndex: 'paramKey',
      key: 'paramKey',
      render: (value) => <span className="font-mono">{value}</span>,
    },
    {
      title: '参数名称',
      dataIndex: 'paramName',
      key: 'paramName',
    },
    {
      title: '参数值',
      dataIndex: 'paramValue',
      key: 'paramValue',
      render: (value) => <span style={{ color: 'var(--text-secondary)' }}>{value || '-'}</span>,
    },
    {
      title: '类型',
      dataIndex: 'paramType',
      key: 'paramType',
      render: (value) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => <Switch checked={record.status === 'ENABLED'} onChange={(checked) => void toggleStatus(record, checked)} />,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除当前系统参数？" onConfirm={() => void handleDelete(record.id)}>
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
        <Space>
          <Button icon={<ExportOutlined />} onClick={() => void handleExport()} loading={exporting}>
            导出
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增参数
          </Button>
        </Space>

        <Card className="glass-panel g-border-panel border">
        <div className="flex flex-wrap gap-3 mb-4">
          <Input allowClear value={keyword} onChange={(event) => setKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索参数键 / 参数名称" className="w-72" />
          <Select value={paramType} options={paramTypeOptions} onChange={setParamType} className="w-40" />
        </div>
        <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={false} />
      </Card>

      <Modal
        title={editingRecord ? '编辑系统参数' : '新增系统参数'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="paramKey" label="参数键" rules={[{ required: true, message: '请输入参数键' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="paramName" label="参数名称" rules={[{ required: true, message: '请输入参数名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="paramValue" label="参数值">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="paramType" label="参数类型">
            <Select options={paramTypeOptions.filter((item) => item.value !== 'ALL')} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ label: '启用', value: 'ENABLED' }, { label: '停用', value: 'DISABLED' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default SystemParams;
