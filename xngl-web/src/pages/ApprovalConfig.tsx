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
  Steps,
  Switch,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  createApprovalMaterialConfig,
  createApprovalRule,
  deleteApprovalMaterialConfig,
  deleteApprovalRule,
  fetchApprovalMaterialConfigDetail,
  fetchApprovalMaterialConfigs,
  fetchApprovalRuleDetail,
  fetchApprovalRules,
  updateApprovalMaterialConfig,
  updateApprovalMaterialConfigStatus,
  updateApprovalRule,
  updateApprovalRuleStatus,
  type ApprovalMaterialConfigPayload,
  type ApprovalMaterialConfigRecord,
  type ApprovalRulePayload,
  type ApprovalRuleRecord,
} from '../utils/approvalApi';

const processOptions = [
  { label: '消纳合同审批', value: 'CONTRACT_APPROVAL' },
  { label: '项目延期审批', value: 'PROJECT_DELAY' },
  { label: '人工事件审核', value: 'MANUAL_EVENT_AUDIT' },
  { label: '场地结算审批', value: 'SITE_SETTLEMENT' },
];

const ruleTypeOptions = [
  { label: '角色', value: 'ROLE' },
  { label: '指定人', value: 'USER' },
  { label: '组织负责人', value: 'ORG_LEADER' },
];

const materialTypeOptions = [
  { label: 'PDF', value: 'PDF' },
  { label: '图片', value: 'IMAGE' },
  { label: '压缩包', value: 'ZIP' },
  { label: '文档', value: 'DOC' },
  { label: '其他', value: 'OTHER' },
];

const ApprovalConfig: React.FC = () => {
  const [activeTab, setActiveTab] = useState('rules');
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<ApprovalRuleRecord[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ApprovalRuleRecord | null>(null);
  const [form] = Form.useForm<ApprovalRulePayload>();

  const [materialsLoading, setMaterialsLoading] = useState(false);
  const [materialSubmitLoading, setMaterialSubmitLoading] = useState(false);
  const [materialRecords, setMaterialRecords] = useState<ApprovalMaterialConfigRecord[]>([]);
  const [materialModalOpen, setMaterialModalOpen] = useState(false);
  const [editingMaterial, setEditingMaterial] = useState<ApprovalMaterialConfigRecord | null>(null);
  const [materialForm] = Form.useForm<ApprovalMaterialConfigPayload>();

  const tenantId = useMemo(() => {
    try {
      const raw = localStorage.getItem('userInfo');
      const parsed = raw ? JSON.parse(raw) : {};
      return String(parsed.tenantId || '1');
    } catch {
      return '1';
    }
  }, []);

  const loadRules = async () => {
    setLoading(true);
    try {
      setRecords(await fetchApprovalRules({ tenantId }));
    } catch (error) {
      console.error(error);
      message.error('获取审批配置失败');
    } finally {
      setLoading(false);
    }
  };

  const loadMaterials = async () => {
    setMaterialsLoading(true);
    try {
      setMaterialRecords(await fetchApprovalMaterialConfigs());
    } catch (error) {
      console.error(error);
      message.error('获取办事材料配置失败');
    } finally {
      setMaterialsLoading(false);
    }
  };

  useEffect(() => {
    void Promise.all([loadRules(), loadMaterials()]);
  }, [tenantId]);

  const openCreate = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      tenantId,
      processKey: 'CONTRACT_APPROVAL',
      ruleType: 'ROLE',
      priority: 10,
    });
    setModalOpen(true);
  };

  const openEdit = async (record: ApprovalRuleRecord) => {
    try {
      const detail = await fetchApprovalRuleDetail(record.id);
      setEditingRecord(detail);
      form.setFieldsValue({
        tenantId,
        processKey: detail.processKey,
        ruleName: detail.ruleName,
        ruleType: detail.ruleType || 'ROLE',
        ruleExpression: detail.ruleExpression || undefined,
        priority: detail.priority || 0,
      });
      setModalOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取审批规则详情失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (editingRecord) {
        await updateApprovalRule(editingRecord.id, values);
        message.success('审批规则已更新');
      } else {
        await createApprovalRule(values);
        message.success('审批规则已新增');
      }
      setModalOpen(false);
      await loadRules();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存审批规则失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const toggleStatus = async (record: ApprovalRuleRecord, checked: boolean) => {
    try {
      await updateApprovalRuleStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await loadRules();
    } catch (error) {
      console.error(error);
      message.error('更新状态失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteApprovalRule(id);
      message.success('审批规则已删除');
      await loadRules();
    } catch (error) {
      console.error(error);
      message.error('删除审批规则失败');
    }
  };

  const openCreateMaterial = () => {
    setEditingMaterial(null);
    materialForm.resetFields();
    materialForm.setFieldsValue({
      processKey: 'CONTRACT_APPROVAL',
      materialType: 'PDF',
      required: true,
      sortOrder: 10,
      status: 'ENABLED',
    });
    setMaterialModalOpen(true);
  };

  const openEditMaterial = async (record: ApprovalMaterialConfigRecord) => {
    try {
      const detail = await fetchApprovalMaterialConfigDetail(record.id);
      setEditingMaterial(detail);
      materialForm.setFieldsValue({
        processKey: detail.processKey,
        materialCode: detail.materialCode,
        materialName: detail.materialName,
        materialType: detail.materialType || undefined,
        required: detail.required,
        sortOrder: detail.sortOrder,
        status: detail.status,
        remark: detail.remark || undefined,
      });
      setMaterialModalOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取材料配置详情失败');
    }
  };

  const handleMaterialSubmit = async () => {
    try {
      const values = await materialForm.validateFields();
      setMaterialSubmitLoading(true);
      if (editingMaterial) {
        await updateApprovalMaterialConfig(editingMaterial.id, values);
        message.success('材料配置已更新');
      } else {
        await createApprovalMaterialConfig(values);
        message.success('材料配置已新增');
      }
      setMaterialModalOpen(false);
      await loadMaterials();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存材料配置失败');
    } finally {
      setMaterialSubmitLoading(false);
    }
  };

  const toggleMaterialStatus = async (
    record: ApprovalMaterialConfigRecord,
    checked: boolean
  ) => {
    try {
      await updateApprovalMaterialConfigStatus(
        record.id,
        checked ? 'ENABLED' : 'DISABLED'
      );
      message.success('状态已更新');
      await loadMaterials();
    } catch (error) {
      console.error(error);
      message.error('更新材料状态失败');
    }
  };

  const handleDeleteMaterial = async (id: string) => {
    try {
      await deleteApprovalMaterialConfig(id);
      message.success('材料配置已删除');
      await loadMaterials();
    } catch (error) {
      console.error(error);
      message.error('删除材料配置失败');
    }
  };

  const columns: ColumnsType<ApprovalRuleRecord> = [
    {
      title: '审批事项',
      dataIndex: 'ruleName',
      key: 'ruleName',
      render: (value, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{value}</span>
          <span style={{ color: 'var(--text-secondary)' }}>{record.processKey}</span>
        </div>
      ),
    },
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      render: (value) => <Tag color="blue">{value || '-'}</Tag>,
    },
    {
      title: '审批表达式',
      dataIndex: 'ruleExpression',
      key: 'ruleExpression',
      render: (value) => value || '-',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (value) => Number(value || 0),
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
          <Popconfirm title="确认删除当前规则？" onConfirm={() => void handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const materialColumns: ColumnsType<ApprovalMaterialConfigRecord> = [
    {
      title: '业务流程',
      dataIndex: 'processKey',
      key: 'processKey',
      render: (value) =>
        processOptions.find((item) => item.value === value)?.label || value || '-',
    },
    {
      title: '材料编码',
      dataIndex: 'materialCode',
      key: 'materialCode',
      render: (value) => <span className="font-mono">{value}</span>,
    },
    {
      title: '材料名称',
      dataIndex: 'materialName',
      key: 'materialName',
    },
    {
      title: '材料类型',
      dataIndex: 'materialType',
      key: 'materialType',
      render: (value) => <Tag color="blue">{value || '-'}</Tag>,
    },
    {
      title: '必填',
      dataIndex: 'required',
      key: 'required',
      render: (value) => <Tag color={value ? 'red' : 'default'}>{value ? '必填' : '选填'}</Tag>,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
    },
    {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          onChange={(checked) => void toggleMaterialStatus(record, checked)}
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
            onClick={() => void openEditMaterial(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除当前材料配置？"
            onConfirm={() => void handleDeleteMaterial(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const previewItems = [
    { title: '发起申请', description: '申请人提交业务单据' },
    { title: '审核节点', description: records[0]?.ruleName || '按配置规则匹配审批人' },
    { title: '结果通知', description: '审批完成后站内消息 / 短信通知' },
  ];

  const materialStats = useMemo(() => {
    const requiredCount = materialRecords.filter((item) => item.required).length;
    const enabledCount = materialRecords.filter((item) => item.status === 'ENABLED').length;
    return {
      total: materialRecords.length,
      requiredCount,
      enabledCount,
    };
  }, [materialRecords]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">审核审批配置</h1>
          <p className="g-text-secondary mt-1">
            维护审批人规则、流程材料模板与启停状态。
          </p>
        </div>
        {activeTab === 'rules' ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增规则
          </Button>
        ) : (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateMaterial}>
            新增材料
          </Button>
        )}
      </div>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'rules',
            label: '审批人规则',
            children: (
              <Space direction="vertical" size={16} className="w-full">
                <Card className="glass-panel g-border-panel border" title="当前审批流预览">
                  <Steps current={-1} items={previewItems} />
                </Card>
                <Card className="glass-panel g-border-panel border">
                  <Table
                    rowKey="id"
                    loading={loading}
                    columns={columns}
                    dataSource={records}
                    pagination={false}
                  />
                </Card>
              </Space>
            ),
          },
          {
            key: 'materials',
            label: '办事材料配置',
            children: (
              <Space direction="vertical" size={16} className="w-full">
                <Card className="glass-panel g-border-panel border">
                  <div className="flex gap-8 flex-wrap">
                    <div>
                      <div className="text-sm g-text-secondary">材料总数</div>
                      <div className="text-2xl font-bold g-text-primary">{materialStats.total}</div>
                    </div>
                    <div>
                      <div className="text-sm g-text-secondary">必填材料</div>
                      <div className="text-2xl font-bold g-text-error">{materialStats.requiredCount}</div>
                    </div>
                    <div>
                      <div className="text-sm g-text-secondary">已启用</div>
                      <div className="text-2xl font-bold g-text-success">{materialStats.enabledCount}</div>
                    </div>
                  </div>
                </Card>
                <Card className="glass-panel g-border-panel border">
                  <Table
                    rowKey="id"
                    loading={materialsLoading}
                    columns={materialColumns}
                    dataSource={materialRecords}
                    pagination={false}
                  />
                </Card>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={editingRecord ? '编辑审批规则' : '新增审批规则'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="tenantId" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="processKey"
            label="业务流程"
            rules={[{ required: true, message: '请选择业务流程' }]}
          >
            <Select options={processOptions} />
          </Form.Item>
          <Form.Item
            name="ruleName"
            label="规则名称"
            rules={[{ required: true, message: '请输入规则名称' }]}
          >
            <Input placeholder="如 项目经理审核" />
          </Form.Item>
          <Form.Item
            name="ruleType"
            label="规则类型"
            rules={[{ required: true, message: '请选择规则类型' }]}
          >
            <Select options={ruleTypeOptions} />
          </Form.Item>
          <Form.Item
            name="ruleExpression"
            label="审批表达式"
            rules={[{ required: true, message: '请输入审批表达式' }]}
          >
            <Input.TextArea rows={3} placeholder="如 role:project_manager 或 user:1001" />
          </Form.Item>
          <Form.Item name="priority" label="优先级">
            <InputNumber min={0} className="w-full" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingMaterial ? '编辑材料配置' : '新增材料配置'}
        open={materialModalOpen}
        onCancel={() => setMaterialModalOpen(false)}
        onOk={() => void handleMaterialSubmit()}
        confirmLoading={materialSubmitLoading}
      >
        <Form form={materialForm} layout="vertical">
          <Form.Item
            name="processKey"
            label="业务流程"
            rules={[{ required: true, message: '请选择业务流程' }]}
          >
            <Select options={processOptions} />
          </Form.Item>
          <Form.Item
            name="materialCode"
            label="材料编码"
            rules={[{ required: true, message: '请输入材料编码' }]}
          >
            <Input placeholder="如 APPLY_FORM" />
          </Form.Item>
          <Form.Item
            name="materialName"
            label="材料名称"
            rules={[{ required: true, message: '请输入材料名称' }]}
          >
            <Input placeholder="如 合同申请表" />
          </Form.Item>
          <Form.Item name="materialType" label="材料类型">
            <Select allowClear options={materialTypeOptions} />
          </Form.Item>
          <Form.Item name="required" label="是否必填" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} className="w-full" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { label: '启用', value: 'ENABLED' },
                { label: '停用', value: 'DISABLED' },
              ]}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ApprovalConfig;
