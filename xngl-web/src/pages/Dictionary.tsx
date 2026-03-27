import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Input, List, Modal, Popconfirm, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, ExportOutlined, PlusOutlined, SearchOutlined, ThunderboltOutlined } from '@ant-design/icons';
import {
  createDataDict,
  deleteDataDict,
  exportDataDicts,
  fetchDataDicts,
  fetchDictTypes,
  updateDataDict,
  updateDataDictStatus,
  type DataDictPayload,
  type DataDictRecord,
  type DictTypeItem,
} from '../utils/dataDictApi';

const Dictionary: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<DataDictRecord[]>([]);
  const [currentRecords, setCurrentRecords] = useState<DataDictRecord[]>([]);
  const [dictTypes, setDictTypes] = useState<DictTypeItem[]>([]);
  const [typeKeyword, setTypeKeyword] = useState('');
  const [itemKeyword, setItemKeyword] = useState('');
  const [selectedType, setSelectedType] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<DataDictRecord | null>(null);
  const [exporting, setExporting] = useState(false);
  const [form] = Form.useForm<DataDictPayload>();

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  // 加载平台预置的字典类型
  const loadDictTypes = () => {
    const types = fetchDictTypes();
    setDictTypes(types);
    if (!selectedType && types.length > 0) {
      setSelectedType(types[0].typeCode);
    }
  };

  // 加载全部字典数据（用于左侧计数）
  const loadAllRecords = async () => {
    try {
      const data = await fetchDataDicts({ keyword: typeKeyword.trim() || undefined });
      setRecords(data);
    } catch {
      message.error('获取数据字典失败');
    }
  };

  useEffect(() => {
    loadDictTypes();
  }, []);

  useEffect(() => {
    void loadAllRecords();
  }, [typeKeyword]);

  const loadCurrentItems = async () => {
    if (!selectedType) {
      setCurrentRecords([]);
      return;
    }
    setLoading(true);
    try {
      const data = await fetchDataDicts({
        dictType: selectedType,
        keyword: itemKeyword.trim() || undefined,
      });
      setCurrentRecords(data);
    } catch {
      message.error('获取字典项失败');
      setCurrentRecords([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadCurrentItems();
  }, [selectedType, itemKeyword]);

  // 字典类型 + 中文名 + 条目数
  const typeLabelMap = useMemo(() => {
    const map: Record<string, string> = {};
    dictTypes.forEach((t) => { map[t.typeCode] = t.typeLabel; });
    return map;
  }, [dictTypes]);

  const typeCountMap = useMemo(() => {
    const map: Record<string, number> = {};
    records.forEach((r) => { map[r.dictType] = (map[r.dictType] || 0) + 1; });
    return map;
  }, [records]);

  // 过滤后的类型列表
  const filteredTypes = useMemo(() => {
    if (!typeKeyword.trim()) return dictTypes;
    const lower = typeKeyword.toLowerCase();
    return dictTypes.filter(
      (t) => t.typeCode.toLowerCase().includes(lower) || t.typeLabel.includes(lower),
    );
  }, [dictTypes, typeKeyword]);

  const selectedTypeLabel = typeLabelMap[selectedType] || selectedType;

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({ dictType: selectedType, status: 'ENABLED', sort: 0 });
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
      await Promise.all([loadAllRecords(), loadCurrentItems()]);
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
      await Promise.all([loadAllRecords(), loadCurrentItems()]);
    } catch {
      message.error('删除字典项失败');
    }
  };

  const toggleStatus = async (record: DataDictRecord, checked: boolean) => {
    try {
      await updateDataDictStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await Promise.all([loadAllRecords(), loadCurrentItems()]);
    } catch {
      message.error('更新状态失败');
    }
  };

  const handleExport = async () => {
    try {
      setExporting(true);
      downloadBlob(
        await exportDataDicts({
          dictType: selectedType || undefined,
          keyword: itemKeyword.trim() || undefined,
        }),
        'data_dicts.csv',
      );
      message.success('数据字典导出成功');
    } catch {
      message.error('数据字典导出失败');
    } finally {
      setExporting(false);
    }
  };

  const columns: ColumnsType<DataDictRecord> = [
    { title: '字典编码', dataIndex: 'dictCode', key: 'dictCode', render: (value) => <span className="font-mono g-text-secondary">{value}</span> },
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
        {/* 左侧：字典类型列表 */}
        <Card className="glass-panel g-border-panel border w-80 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div className="p-4 border-b g-border-panel border">
            <Input value={typeKeyword} onChange={(event) => setTypeKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索字典类型" allowClear />
          </div>
          <div className="flex-1 overflow-auto p-2">
            <List
              dataSource={filteredTypes}
              renderItem={(item) => (
                <List.Item
                  className={`px-4 py-3 cursor-pointer rounded mb-1 ${selectedType === item.typeCode ? 'bg-blue-600/10 border border-blue-500/50' : 'hover:bg-white'}`}
                  onClick={() => setSelectedType(item.typeCode)}
                >
                  <div className="w-full">
                    <div className="font-bold g-text-primary">{item.typeLabel}</div>
                    <div className="text-xs g-text-secondary flex justify-between">
                      <span className="font-mono">{item.typeCode}</span>
                      <span>{typeCountMap[item.typeCode] || 0} 项</span>
                    </div>
                  </div>
                </List.Item>
              )}
            />
          </div>
        </Card>

        {/* 右侧：字典项列表 */}
        <Card className="glass-panel g-border-panel border flex-1 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div className="p-4 border-b g-border-panel border flex justify-between items-center">
            <Space>
              <Input allowClear value={itemKeyword} onChange={(event) => setItemKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索标签 / 编码" className="w-64" />
              <Tag color="blue">{selectedTypeLabel || '未选择类型'}</Tag>
            </Space>
            <Space>
              <Button icon={<ExportOutlined />} onClick={() => void handleExport()} loading={exporting}>
                导出
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate} disabled={!selectedType}>
                新增字典项
              </Button>
            </Space>
          </div>
          <div className="flex-1 overflow-auto p-4">
            <Table rowKey="id" loading={loading} columns={columns} dataSource={currentRecords} pagination={false} />
          </div>
        </Card>
      </div>

      {/* 新增/编辑字典项 Modal */}
      <Modal
        title={editingRecord ? '编辑字典项' : `新增字典项 · ${selectedTypeLabel}`}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical">
          {/* dictType 只读展示，不可修改 */}
          <Form.Item label="字典类型">
            <Input value={`${selectedTypeLabel}（${selectedType}）`} disabled />
          </Form.Item>
          <Form.Item name="dictType" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="dictLabel" label="字典标签" rules={[{ required: true, message: '请输入字典标签' }]}>
            <Input placeholder="用户可见的显示名称" />
          </Form.Item>
          <Form.Item name="dictValue" label={<span>字典值 <ThunderboltOutlined className="g-text-secondary" title="不填则自动生成" /></span>}>
            <Input placeholder="不填则自动生成（与编码相同）" />
          </Form.Item>
          <Form.Item name="dictCode" label={<span>字典编码 <ThunderboltOutlined className="g-text-secondary" title="不填则自动生成" /></span>}>
            <Input placeholder="不填则自动生成" />
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
