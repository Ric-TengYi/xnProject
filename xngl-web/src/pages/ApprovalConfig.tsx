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
import { DeleteOutlined, EditOutlined, ExportOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import {
  createApprovalFlowConfig,
  createApprovalMaterialConfig,
  createApprovalRule,
  deleteApprovalFlowConfig,
  deleteApprovalMaterialConfig,
  deleteApprovalRule,
  exportApprovalFlowConfigs,
  exportApprovalMaterialConfigs,
  exportApprovalRules,
  fetchApprovalFlowConfigDetail,
  fetchApprovalFlowConfigs,
  fetchApprovalMaterialConfigDetail,
  fetchApprovalMaterialConfigs,
  fetchApprovalRuleDetail,
  fetchApprovalRules,
  updateApprovalFlowConfig,
  updateApprovalFlowConfigStatus,
  updateApprovalMaterialConfig,
  updateApprovalMaterialConfigStatus,
  updateApprovalRule,
  updateApprovalRuleStatus,
  type ApprovalFlowConfigPayload,
  type ApprovalFlowConfigRecord,
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

const approvalTypeOptions = [
  { label: '串行审批', value: 'SERIAL' },
  { label: '并行审批', value: 'PARALLEL' },
  { label: '自动通过', value: 'AUTO' },
];

const ApprovalConfig: React.FC = () => {
  const [activeTab, setActiveTab] = useState('rules');
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [records, setRecords] = useState<ApprovalRuleRecord[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ApprovalRuleRecord | null>(null);
  const [rulesKeyword, setRulesKeyword] = useState('');
  const [rulesProcessKey, setRulesProcessKey] = useState<string | undefined>(undefined);
  const [rulesStatus, setRulesStatus] = useState<string>('all');
  const [form] = Form.useForm<ApprovalRulePayload>();

  const [materialsLoading, setMaterialsLoading] = useState(false);
  const [materialSubmitLoading, setMaterialSubmitLoading] = useState(false);
  const [materialRecords, setMaterialRecords] = useState<ApprovalMaterialConfigRecord[]>([]);
  const [materialModalOpen, setMaterialModalOpen] = useState(false);
  const [editingMaterial, setEditingMaterial] = useState<ApprovalMaterialConfigRecord | null>(null);
  const [materialsKeyword, setMaterialsKeyword] = useState('');
  const [materialsProcessKey, setMaterialsProcessKey] = useState<string | undefined>(undefined);
  const [materialsStatus, setMaterialsStatus] = useState<string>('all');
  const [materialForm] = Form.useForm<ApprovalMaterialConfigPayload>();
  const [flowLoading, setFlowLoading] = useState(false);
  const [flowSubmitLoading, setFlowSubmitLoading] = useState(false);
  const [flowRecords, setFlowRecords] = useState<ApprovalFlowConfigRecord[]>([]);
  const [flowModalOpen, setFlowModalOpen] = useState(false);
  const [editingFlow, setEditingFlow] = useState<ApprovalFlowConfigRecord | null>(null);
  const [flowsKeyword, setFlowsKeyword] = useState('');
  const [flowsProcessKey, setFlowsProcessKey] = useState<string | undefined>(undefined);
  const [flowsStatus, setFlowsStatus] = useState<string>('all');
  const [exportingTab, setExportingTab] = useState<string | null>(null);
  const [flowForm] = Form.useForm<ApprovalFlowConfigPayload>();

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
      setRecords(
        await fetchApprovalRules({
          tenantId,
          keyword: rulesKeyword.trim() || undefined,
          processKey: rulesProcessKey || undefined,
          status: rulesStatus === 'all' ? undefined : rulesStatus,
        }),
      );
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
      setMaterialRecords(
        await fetchApprovalMaterialConfigs({
          keyword: materialsKeyword.trim() || undefined,
          processKey: materialsProcessKey || undefined,
          status: materialsStatus === 'all' ? undefined : materialsStatus,
        }),
      );
    } catch (error) {
      console.error(error);
      message.error('获取办事材料配置失败');
    } finally {
      setMaterialsLoading(false);
    }
  };

  const loadFlows = async () => {
    setFlowLoading(true);
    try {
      setFlowRecords(
        await fetchApprovalFlowConfigs({
          keyword: flowsKeyword.trim() || undefined,
          processKey: flowsProcessKey || undefined,
          status: flowsStatus === 'all' ? undefined : flowsStatus,
        }),
      );
    } catch (error) {
      console.error(error);
      message.error('获取审批流程配置失败');
    } finally {
      setFlowLoading(false);
    }
  };

  useEffect(() => {
    void Promise.all([loadRules(), loadMaterials(), loadFlows()]);
  }, [
    tenantId,
    rulesKeyword,
    rulesProcessKey,
    rulesStatus,
    materialsKeyword,
    materialsProcessKey,
    materialsStatus,
    flowsKeyword,
    flowsProcessKey,
    flowsStatus,
  ]);

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  const handleExport = async (tab: 'rules' | 'materials' | 'flows') => {
    try {
      setExportingTab(tab);
      if (tab === 'rules') {
        downloadBlob(
          await exportApprovalRules({
            tenantId,
            keyword: rulesKeyword.trim() || undefined,
            processKey: rulesProcessKey || undefined,
            status: rulesStatus === 'all' ? undefined : rulesStatus,
          }),
          'approval_actor_rules.csv',
        );
      } else if (tab === 'materials') {
        downloadBlob(
          await exportApprovalMaterialConfigs({
            keyword: materialsKeyword.trim() || undefined,
            processKey: materialsProcessKey || undefined,
            status: materialsStatus === 'all' ? undefined : materialsStatus,
          }),
          'approval_material_configs.csv',
        );
      } else {
        downloadBlob(
          await exportApprovalFlowConfigs({
            keyword: flowsKeyword.trim() || undefined,
            processKey: flowsProcessKey || undefined,
            status: flowsStatus === 'all' ? undefined : flowsStatus,
          }),
          'approval_configs.csv',
        );
      }
      message.success('审批配置导出成功');
    } catch (error) {
      console.error(error);
      message.error('审批配置导出失败');
    } finally {
      setExportingTab(null);
    }
  };

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

  const openCreateFlow = () => {
    setEditingFlow(null);
    flowForm.resetFields();
    flowForm.setFieldsValue({
      processKey: 'CONTRACT_APPROVAL',
      configName: '合同审批标准流',
      approvalType: 'SERIAL',
      nodeCode: 'NODE_01',
      nodeName: '流程节点',
      timeoutHours: 24,
      sortOrder: 10,
      status: 'ENABLED',
    });
    setFlowModalOpen(true);
  };

  const openEditFlow = async (record: ApprovalFlowConfigRecord) => {
    try {
      const detail = await fetchApprovalFlowConfigDetail(record.id);
      setEditingFlow(detail);
      flowForm.setFieldsValue({
        processKey: detail.processKey,
        configName: detail.configName,
        approvalType: detail.approvalType || 'SERIAL',
        nodeCode: detail.nodeCode,
        nodeName: detail.nodeName,
        approvers: detail.approvers || undefined,
        conditions: detail.conditions || undefined,
        formTemplateCode: detail.formTemplateCode || undefined,
        timeoutHours: detail.timeoutHours || 24,
        sortOrder: detail.sortOrder || 0,
        status: detail.status || 'ENABLED',
        remark: detail.remark || undefined,
      });
      setFlowModalOpen(true);
    } catch (error) {
      console.error(error);
      message.error('获取流程配置详情失败');
    }
  };

  const handleFlowSubmit = async () => {
    try {
      const values = await flowForm.validateFields();
      setFlowSubmitLoading(true);
      if (editingFlow) {
        await updateApprovalFlowConfig(editingFlow.id, values);
        message.success('流程配置已更新');
      } else {
        await createApprovalFlowConfig(values);
        message.success('流程配置已新增');
      }
      setFlowModalOpen(false);
      await loadFlows();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存流程配置失败');
    } finally {
      setFlowSubmitLoading(false);
    }
  };

  const toggleFlowStatus = async (record: ApprovalFlowConfigRecord, checked: boolean) => {
    try {
      await updateApprovalFlowConfigStatus(record.id, checked ? 'ENABLED' : 'DISABLED');
      message.success('状态已更新');
      await loadFlows();
    } catch (error) {
      console.error(error);
      message.error('更新流程状态失败');
    }
  };

  const handleDeleteFlow = async (id: string) => {
    try {
      await deleteApprovalFlowConfig(id);
      message.success('流程配置已删除');
      await loadFlows();
    } catch (error) {
      console.error(error);
      message.error('删除流程配置失败');
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

  const flowColumns: ColumnsType<ApprovalFlowConfigRecord> = [
    {
      title: '流程 / 节点',
      key: 'configName',
      render: (_, record) => (
        <div className="flex flex-col">
          <span style={{ color: 'var(--text-primary)' }}>{record.configName}</span>
          <span style={{ color: 'var(--text-secondary)' }}>
            {record.processKey} / {record.nodeName}
          </span>
        </div>
      ),
    },
    {
      title: '审批方式',
      dataIndex: 'approvalType',
      key: 'approvalType',
      render: (value) => (
        <Tag color={value === 'PARALLEL' ? 'geekblue' : value === 'AUTO' ? 'green' : 'blue'}>
          {approvalTypeOptions.find((item) => item.value === value)?.label || value || '-'}
        </Tag>
      ),
    },
    {
      title: '审批人表达式',
      dataIndex: 'approvers',
      key: 'approvers',
      render: (value) => value || '-',
    },
    {
      title: '条件表达式',
      dataIndex: 'conditions',
      key: 'conditions',
      render: (value) => value || '-',
    },
    {
      title: '超时/排序',
      key: 'meta',
      render: (_, record) => (
        <div className="flex flex-col">
          <span>{record.timeoutHours || 0} 小时</span>
          <span style={{ color: 'var(--text-secondary)' }}>排序 {record.sortOrder || 0}</span>
        </div>
      ),
    },
    {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          onChange={(checked) => void toggleFlowStatus(record, checked)}
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
            onClick={() => void openEditFlow(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除当前流程节点？"
            onConfirm={() => void handleDeleteFlow(record.id)}
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

  const flowStats = useMemo(() => {
    const enabledCount = flowRecords.filter((item) => item.status === 'ENABLED').length;
    const processCount = new Set(flowRecords.map((item) => item.processKey)).size;
    return {
      total: flowRecords.length,
      enabledCount,
      processCount,
    };
  }, [flowRecords]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold m-0">审批配置</h1>
        </div>
        {activeTab === 'rules' ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增规则
          </Button>
        ) : activeTab === 'materials' ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateMaterial}>
            新增材料
          </Button>
        ) : (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateFlow}>
            新增流程
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
                  <div className="mb-4 flex flex-wrap gap-3">
                    <Input
                      allowClear
                      value={rulesKeyword}
                      onChange={(event) => setRulesKeyword(event.target.value)}
                      prefix={<SearchOutlined />}
                      placeholder="搜索规则名称 / 表达式"
                      className="w-72"
                    />
                    <Select
                      allowClear
                      value={rulesProcessKey}
                      options={processOptions}
                      placeholder="选择流程"
                      className="w-48"
                      onChange={setRulesProcessKey}
                    />
                    <Select
                      value={rulesStatus}
                      className="w-36"
                      onChange={setRulesStatus}
                      options={[
                        { label: '全部状态', value: 'all' },
                        { label: '启用', value: 'ENABLED' },
                        { label: '停用', value: 'DISABLED' },
                      ]}
                    />
                    <div className="flex-1 flex justify-end">
                      <Button
                        icon={<ExportOutlined />}
                        onClick={() => void handleExport('rules')}
                        loading={exportingTab === 'rules'}
                      >
                        导出
                      </Button>
                    </div>
                  </div>
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
                  <div className="mb-4 flex flex-wrap gap-3">
                    <Input
                      allowClear
                      value={materialsKeyword}
                      onChange={(event) => setMaterialsKeyword(event.target.value)}
                      prefix={<SearchOutlined />}
                      placeholder="搜索材料编码 / 名称"
                      className="w-72"
                    />
                    <Select
                      allowClear
                      value={materialsProcessKey}
                      options={processOptions}
                      placeholder="选择流程"
                      className="w-48"
                      onChange={setMaterialsProcessKey}
                    />
                    <Select
                      value={materialsStatus}
                      className="w-36"
                      onChange={setMaterialsStatus}
                      options={[
                        { label: '全部状态', value: 'all' },
                        { label: '启用', value: 'ENABLED' },
                        { label: '停用', value: 'DISABLED' },
                      ]}
                    />
                    <div className="flex-1 flex justify-end">
                      <Button
                        icon={<ExportOutlined />}
                        onClick={() => void handleExport('materials')}
                        loading={exportingTab === 'materials'}
                      >
                        导出
                      </Button>
                    </div>
                  </div>
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
          {
            key: 'flows',
            label: '流程配置',
            children: (
              <Space direction="vertical" size={16} className="w-full">
                <Card className="glass-panel g-border-panel border">
                  <div className="flex gap-8 flex-wrap">
                    <div>
                      <div className="text-sm g-text-secondary">节点总数</div>
                      <div className="text-2xl font-bold g-text-primary">{flowStats.total}</div>
                    </div>
                    <div>
                      <div className="text-sm g-text-secondary">已启用节点</div>
                      <div className="text-2xl font-bold g-text-success">{flowStats.enabledCount}</div>
                    </div>
                    <div>
                      <div className="text-sm g-text-secondary">流程数</div>
                      <div className="text-2xl font-bold g-text-warning">{flowStats.processCount}</div>
                    </div>
                  </div>
                </Card>
              </Space>
            ),
          },
        ]}
      />
    <Modal
      title={editingFlow ? '编辑流程配置' : '新增流程配置'}
      open={flowModalOpen}
      onCancel={() => setFlowModalOpen(false)}
      onOk={() => void handleFlowSubmit()}
      confirmLoading={flowSubmitLoading}
    >
      <Form form={flowForm} layout="vertical">
        <Form.Item
          name="processKey"
          label="业务流程"
          rules={[{ required: true, message: '请选择业务流程' }]}
        >
          <Select options={processOptions} />
        </Form.Item>
        <Form.Item
          name="configName"
          label="流程名称"
          rules={[{ required: true, message: '请输入流程名称' }]}
        >
          <Input placeholder="如 合同审批标准流" />
        </Form.Item>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item
            name="approvalType"
            label="审批方式"
            rules={[{ required: true, message: '请选择审批方式' }]}
          >
            <Select options={approvalTypeOptions} />
          </Form.Item>
          <Form.Item name="formTemplateCode" label="文书模板编码">
            <Input placeholder="如 CONTRACT_APPLY_DOC" />
          </Form.Item>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item
            name="nodeCode"
            label="节点编码"
            rules={[{ required: true, message: '请输入节点编码' }]}
          >
            <Input placeholder="如 APPLY_AUDIT" />
          </Form.Item>
          <Form.Item
            name="nodeName"
            label="节点名称"
            rules={[{ required: true, message: '请输入节点名称' }]}
          >
            <Input placeholder="如 申请审核" />
          </Form.Item>
        </div>
        <Form.Item name="approvers" label="审批人表达式">
          <Input.TextArea rows={2} placeholder="如 role:project_manager,user:1001" />
        </Form.Item>
        <Form.Item name="conditions" label="流转条件">
          <Input.TextArea rows={2} placeholder="如 amount>500000" />
        </Form.Item>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item name="timeoutHours" label="超时小时数">
            <InputNumber min={0} className="w-full" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} className="w-full" />
          </Form.Item>
        </div>
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
