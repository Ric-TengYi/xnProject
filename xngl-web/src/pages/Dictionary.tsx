import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Input, List, Modal, Popconfirm, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import {
  createDataDict,
  deleteDataDict,
  fetchDataDicts,
  updateDataDict,
  updateDataDictStatus,
  type DataDictPayload,
  type DataDictRecord,
} from '../utils/dataDictApi';

const Dictionary: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<DataDictRecord[]>([]);
  const [typeKeyword, setTypeKeyword] = useState('');
  const [itemKeyword, setItemKeyword] = useState('');
  const [selectedType, setSelectedType] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<DataDictRecord | null>(null);
  const [form] = Form.useForm<DataDictPayload>();

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await fetchDataDicts();
      setRecords(data);
      if (!selectedType && data[0]?.dictType) {
        setSelectedType(data[0].dictType);
      }
    } catch (error) {
      console.error(error);
      message.error('获取数据字典失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const dictTypes = useMemo(() => {
    const unique = Array.from(new Set(records.map((item) => item.dictType))).filter(Boolean);
    return unique.filter((item) => item.includes(typeKeyword.trim()));
  }, [records, typeKeyword]);

  const currentRecords = useMemo(
    () => records.filter((item) => item.dictType === selectedType && (!itemKeyword.trim() || item.dictLabel.includes(itemKeyword.trim()) || item.dictCode.includes(itemKeyword.trim()))),
    [itemKeyword, records, selectedType],
  );

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({ dictType: selectedType || 'alert_level', status: 'ENABLED', sort: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: DataDictRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      dictType: record.dictType,
      dictCode: record.dictCode,
      dictLabel: record.dictLabel,
      dictValue: record.dictValue,
      sort: record.sort,
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
        await updateDataDict(editingRecord.id, values);
        message.success('字典项已更新');
      } else {
        await createDataDict(values);
        message.success('字典项已新增');
      }
      setModalOpen(false);
      await loadData();
      setSelectedType(values.dictType);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('保存字典项失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteDataDict(id);
      message.success('字典项已删除');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('删除字典项失败');
    }
  };

  const toggleStatus = async (record: DataDictRecord, checked: boolean) => {
    try {
      await updateDataDictStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await loadData();
    } catch (error) {
      console.error(error);
      message.error('更新状态失败');
    }
  };

  const columns: ColumnsType<DataDictRecord> = [
    { title: '字典编码', dataIndex: 'dictCode', key: 'dictCode', render: (value) => <span className="font-mono">{value}</span> },
    { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
    { title: '字典值', dataIndex: 'dictValue', key: 'dictValue', render: (value) => <Tag color="blue">{value}</Tag> },
    { title: '排序', dataIndex: 'sort', key: 'sort' },
    {
      title: '状态',
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
          <Popconfirm title="确认删除当前字典项？" onConfirm={() => void handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6 h-[calc(100vh-110px)] flex flex-col">
      <div>
        <h1 className="text-2xl font-bold g-text-primary m-0">数据字典</h1>
        <p className="g-text-secondary mt-1">统一维护系统通用枚举、业务类型和展示标签</p>
      </div>

      <div className="flex gap-6 flex-1 min-h-0">
        <Card className="glass-panel g-border-panel border w-80 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div className="p-4 border-b g-border-panel border">
            <Input value={typeKeyword} onChange={(event) => setTypeKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索字典类型" />
          </div>
          <div className="flex-1 overflow-auto p-2">
            <List
              loading={loading}
              dataSource={dictTypes}
              renderItem={(item) => (
                <List.Item
                  className={`px-4 py-3 cursor-pointer rounded mb-1 ${selectedType === item ? 'bg-blue-600/10 border border-blue-500/50' : 'hover:bg-white'}`}
                  onClick={() => setSelectedType(item)}
                >
                  <div className="w-full">
                    <div className="font-bold g-text-primary">{item}</div>
                    <div className="text-xs g-text-secondary">{records.filter((record) => record.dictType === item).length} 项</div>
                  </div>
                </List.Item>
              )}
            />
          </div>
        </Card>

        <Card className="glass-panel g-border-panel border flex-1 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div className="p-4 border-b g-border-panel border flex justify-between items-center">
            <Space>
              <Input allowClear value={itemKeyword} onChange={(event) => setItemKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索标签 / 编码" className="w-64" />
              <Tag color="blue">{selectedType || '未选择类型'}</Tag>
            </Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新增字典项
            </Button>
          </div>
          <div className="flex-1 overflow-auto p-4">
            <Table rowKey="id" loading={loading} columns={columns} dataSource={currentRecords} pagination={false} />
          </div>
        </Card>
      </div>

      <Modal
        title={editingRecord ? '编辑字典项' : '新增字典项'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="dictType" label="字典类型" rules={[{ required: true, message: '请输入字典类型' }]}>
            <Select
              showSearch
              allowClear
              options={dictTypes.map((item) => ({ label: item, value: item }))}
              dropdownRender={(menu) => (
                <div>
                  {menu}
                  <div className="p-2 border-t g-border-panel border g-text-secondary text-xs">可直接输入新的字典类型</div>
                </div>
              )}
              mode={undefined}
            />
          </Form.Item>
          <Form.Item name="dictCode" label="字典编码" rules={[{ required: true, message: '请输入字典编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="dictLabel" label="字典标签" rules={[{ required: true, message: '请输入字典标签' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="dictValue" label="字典值" rules={[{ required: true, message: '请输入字典值' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <Input type="number" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ label: '启用', value: 'ENABLED' }, { label: '停用', value: 'DISABLED' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Dictionary;
