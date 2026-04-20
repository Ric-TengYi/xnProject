import React, { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Modal, Select, Space, Switch, Table, Tabs, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import {
  createAlertFence,
  createAlertPushRule,
  createAlertRule,
  fetchAlertFences,
  fetchAlertPushRules,
  fetchAlertRules,
  updateAlertFence,
  updateAlertFenceStatus,
  updateAlertPushRule,
  updateAlertPushRuleStatus,
  updateAlertRule,
  updateAlertRuleStatus,
  type AlertFenceRecord,
  type AlertPushRuleRecord,
  type AlertRuleRecord,
} from '../utils/alertConfigApi';

type EditingKind = 'rule' | 'fence' | 'push' | null;

const sceneOptions = [
  { label: '场地', value: 'SITE' },
  { label: '项目', value: 'PROJECT' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '人员', value: 'USER' },
  { label: '合同', value: 'CONTRACT' },
];

const levelOptions = [
  { label: 'L1', value: 'L1' },
  { label: 'L2', value: 'L2' },
  { label: 'L3', value: 'L3' },
];

const AlertConfig: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isAdmin = userInfo?.userType === 'TENANT_ADMIN' || userInfo?.userType === 'SYSTEM_ADMIN';
  const [submitLoading, setSubmitLoading] = useState(false);
  const [rules, setRules] = useState<AlertRuleRecord[]>([]);
  const [fences, setFences] = useState<AlertFenceRecord[]>([]);
  const [pushRules, setPushRules] = useState<AlertPushRuleRecord[]>([]);
  const [editingKind, setEditingKind] = useState<EditingKind>(null);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [ruleForm] = Form.useForm();
  const [fenceForm] = Form.useForm();
  const [pushForm] = Form.useForm();

  const loadData = async () => {
    setLoading(true);
    try {
      const [ruleList, fenceList, pushList] = await Promise.all([
        fetchAlertRules(),
        fetchAlertFences(),
        fetchAlertPushRules(),
      ]);
      setRules(ruleList);
      setFences(fenceList);
      setPushRules(pushList);
    } catch (error) {
      console.error(error);
      message.error('获取预警配置失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const openRuleModal = (record?: AlertRuleRecord) => {
    setEditingKind('rule');
    setEditingRecord(record || null);
    ruleForm.setFieldsValue(record || { ruleScene: 'VEHICLE', level: 'L2', scopeType: 'GLOBAL', status: 'ENABLED' });
  };

  const openFenceModal = (record?: AlertFenceRecord) => {
    setEditingKind('fence');
    setEditingRecord(record || null);
    fenceForm.setFieldsValue(record || { fenceType: 'ENTRY', status: 'ENABLED', bufferMeters: 0 });
  };

  const openPushModal = (record?: AlertPushRuleRecord) => {
    setEditingKind('push');
    setEditingRecord(record || null);
    pushForm.setFieldsValue(record || { level: 'L2', receiverType: 'ROLE', status: 'ENABLED', escalationMinutes: 0 });
  };

  const closeModal = () => {
    setEditingKind(null);
    setEditingRecord(null);
    ruleForm.resetFields();
    fenceForm.resetFields();
    pushForm.resetFields();
  };

  const handleSubmit = async () => {
    try {
      setSubmitLoading(true);
      if (editingKind === 'rule') {
        const values = await ruleForm.validateFields();
        if (editingRecord) {
          await updateAlertRule(editingRecord.id, values);
          message.success('阈值规则已更新');
        } else {
          await createAlertRule(values);
          message.success('阈值规则已新增');
        }
      }
      if (editingKind === 'fence') {
        const values = await fenceForm.validateFields();
        if (editingRecord) {
          await updateAlertFence(editingRecord.id, values);
          message.success('围栏配置已更新');
        } else {
          await createAlertFence(values);
          message.success('围栏配置已新增');
        }
      }
      if (editingKind === 'push') {
        const values = await pushForm.validateFields();
        if (editingRecord) {
          await updateAlertPushRule(editingRecord.id, values);
          message.success('推送规则已更新');
        } else {
          await createAlertPushRule(values);
          message.success('推送规则已新增');
        }
      }
      closeModal();
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('保存预警配置失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const ruleColumns: ColumnsType<AlertRuleRecord> = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', render: (value) => <span className="font-mono">{value}</span> },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName' },
    { title: '场景', dataIndex: 'ruleScene', key: 'ruleScene', render: (value) => <Tag color="blue">{value}</Tag> },
    { title: '阈值配置', dataIndex: 'thresholdJson', key: 'thresholdJson', render: (value) => value || '-' },
    { title: '等级', dataIndex: 'level', key: 'level', render: (value) => <Tag color={value === 'L3' ? 'red' : value === 'L2' ? 'orange' : 'default'}>{value}</Tag> },
    {
      title: '启用',
      key: 'status',
      render: (_, record) => <Switch disabled={!isAdmin} checked={record.status === 'ENABLED'} onChange={(checked) => void updateAlertRuleStatus(record.id, checked ? 'ENABLED' : 'DISABLED').then(loadData)} />,
    },
    { title: '操作', key: 'action', render: (_, record) => isAdmin ? <Button type="link" onClick={() => openRuleModal(record)}>编辑</Button> : null },
  ];

  const fenceColumns: ColumnsType<AlertFenceRecord> = [
    { title: '围栏编码', dataIndex: 'fenceCode', key: 'fenceCode', render: (value) => <span className="font-mono">{value}</span> },
    { title: '围栏名称', dataIndex: 'fenceName', key: 'fenceName' },
    { title: '类型', dataIndex: 'fenceType', key: 'fenceType', render: (value) => <Tag color="green">{value}</Tag> },
    { title: '作用范围', dataIndex: 'bizScope', key: 'bizScope', render: (value) => value || '-' },
    {
      title: '启用',
      key: 'status',
      render: (_, record) => <Switch disabled={!isAdmin} checked={record.status === 'ENABLED'} onChange={(checked) => void updateAlertFenceStatus(record.id, checked ? 'ENABLED' : 'DISABLED').then(loadData)} />,
    },
    { title: '操作', key: 'action', render: (_, record) => isAdmin ? <Button type="link" onClick={() => openFenceModal(record)}>编辑</Button> : null },
  ];

  const pushColumns: ColumnsType<AlertPushRuleRecord> = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', render: (value) => <span className="font-mono">{value}</span> },
    { title: '等级', dataIndex: 'level', key: 'level', render: (value) => <Tag color={value === 'L3' ? 'red' : value === 'L2' ? 'orange' : 'default'}>{value}</Tag> },
    { title: '推送方式', dataIndex: 'channelTypes', key: 'channelTypes' },
    { title: '推送对象', dataIndex: 'receiverExpr', key: 'receiverExpr' },
    {
      title: '启用',
      key: 'status',
      render: (_, record) => <Switch disabled={!isAdmin} checked={record.status === 'ENABLED'} onChange={(checked) => void updateAlertPushRuleStatus(record.id, checked ? 'ENABLED' : 'DISABLED').then(loadData)} />,
    },
    { title: '操作', key: 'action', render: (_, record) => isAdmin ? <Button type="link" onClick={() => openPushModal(record)}>编辑</Button> : null },
  ];

  const modelCards = sceneOptions.map((scene) => {
    const sceneRules = rules.filter((item) => item.ruleScene === scene.value);
    const sceneRuleCodes = new Set(sceneRules.map((item) => item.ruleCode));
    const linkedFenceCount = fences.filter((item) => item.ruleCode && sceneRuleCodes.has(item.ruleCode)).length;
    const linkedPushCount = pushRules.filter((item) => sceneRuleCodes.has(item.ruleCode)).length;
    return {
      ...scene,
      ruleCount: sceneRules.length,
      enabledCount: sceneRules.filter((item) => item.status === 'ENABLED').length,
      linkedFenceCount,
      linkedPushCount,
      ruleNames: sceneRules.slice(0, 4).map((item) => item.ruleName || item.ruleCode),
    };
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold g-text-primary m-0">预警配置</h1>
        <p className="g-text-secondary mt-1">统一维护阈值规则、电子围栏与推送对象</p>
      </div>
      <Tabs
        items={[
          {
            key: 'rules',
            label: '阈值规则',
            children: (
              <Card className="glass-panel g-border-panel border" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => openRuleModal()}>新增规则</Button>}>
                <Table rowKey="id" loading={loading} columns={ruleColumns} dataSource={rules} pagination={false} scroll={{ x: 'max-content' }} />
              </Card>
            ),
          },
          {
            key: 'fences',
            label: '预警围栏',
            children: (
              <Card className="glass-panel g-border-panel border" extra={isAdmin ? <Button type="primary" icon={<PlusOutlined />} onClick={() => openFenceModal()}>新增围栏</Button> : null}>
                <Table rowKey="id" loading={loading} columns={fenceColumns} dataSource={fences} pagination={false} scroll={{ x: 'max-content' }} />
              </Card>
            ),
          },
          {
            key: 'push',
            label: '推送规则',
            children: (
              <Card className="glass-panel g-border-panel border" extra={isAdmin ? <Button type="primary" icon={<PlusOutlined />} onClick={() => openPushModal()}>新增推送规则</Button> : null}>
                <Table rowKey="id" loading={loading} columns={pushColumns} dataSource={pushRules} pagination={false} scroll={{ x: 'max-content' }} />
              </Card>
            ),
          },
          {
            key: 'models',
            label: '研判模型',
            children: (
              <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                {modelCards.map((item) => (
                  <Card
                    key={item.value}
                    className="glass-panel g-border-panel border"
                    title={`${item.label}模型`}
                    extra={
                      <Button
                        type="link"
                        onClick={() => {
                          setEditingKind('rule');
                          setEditingRecord(null);
                          ruleForm.setFieldsValue({
                            ruleCode: `${item.value}_MODEL_${Date.now()}`,
                            ruleName: `${item.label}研判规则`,
                            ruleScene: item.value,
                            metricCode: '',
                            thresholdJson: '',
                            level: 'L2',
                            status: 'ENABLED',
                            scopeType: 'GLOBAL',
                            remark: '',
                          });
                        }}
                      >
                        新增规则
                      </Button>
                    }
                  >
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <div className="g-text-secondary">规则总数</div>
                        <div className="text-xl font-semibold">{item.ruleCount}</div>
                      </div>
                      <div>
                        <div className="g-text-secondary">启用规则</div>
                        <div className="text-xl font-semibold">{item.enabledCount}</div>
                      </div>
                      <div>
                        <div className="g-text-secondary">围栏挂接</div>
                        <div className="text-xl font-semibold">{item.linkedFenceCount}</div>
                      </div>
                      <div>
                        <div className="g-text-secondary">推送挂接</div>
                        <div className="text-xl font-semibold">{item.linkedPushCount}</div>
                      </div>
                    </div>
                    <div className="mt-4">
                      <div className="g-text-secondary mb-2">当前规则</div>
                      <Space wrap>
                        {item.ruleNames.length ? item.ruleNames.map((name) => <Tag key={name}>{name}</Tag>) : <span style={{ color: 'var(--text-secondary)' }}>暂无规则</span>}
                      </Space>
                    </div>
                  </Card>
                ))}
              </div>
            ),
          },
        ]}
      />

      <Modal title={editingRecord ? '编辑阈值规则' : '新增阈值规则'} open={editingKind === 'rule'} onCancel={closeModal} onOk={() => void handleSubmit()} confirmLoading={submitLoading}>
        <Form form={ruleForm} layout="vertical">
          <Form.Item name="ruleCode" label="规则编码" rules={[{ required: true, message: '请输入规则编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名称" rules={[{ required: true, message: '请输入规则名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="ruleScene" label="场景" rules={[{ required: true, message: '请选择场景' }]}>
            <Select options={sceneOptions} />
          </Form.Item>
          <Form.Item name="metricCode" label="指标编码">
            <Input />
          </Form.Item>
          <Form.Item name="thresholdJson" label="阈值配置(JSON)">
            <Input.TextArea rows={3} placeholder='如 {"threshold":80,"unit":"%"}' />
          </Form.Item>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="level" label="等级">
              <Select options={levelOptions} />
            </Form.Item>
            <Form.Item name="scopeType" label="作用域">
              <Select options={[{ label: '全局', value: 'GLOBAL' }, { label: '局部', value: 'PARTIAL' }]} />
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingRecord ? '编辑预警围栏' : '新增预警围栏'} open={editingKind === 'fence'} onCancel={closeModal} onOk={() => void handleSubmit()} confirmLoading={submitLoading}>
        <Form form={fenceForm} layout="vertical">
          <Form.Item name="ruleCode" label="关联规则编码">
            <Select allowClear showSearch options={rules.map((item) => ({ label: item.ruleCode, value: item.ruleCode }))} />
          </Form.Item>
          <Form.Item name="fenceCode" label="围栏编码" rules={[{ required: true, message: '请输入围栏编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="fenceName" label="围栏名称" rules={[{ required: true, message: '请输入围栏名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="fenceType" label="围栏类型">
            <Select options={[{ label: '入场', value: 'ENTRY' }, { label: '禁行', value: 'FORBIDDEN' }, { label: '停留', value: 'STAY' }]} />
          </Form.Item>
          <Form.Item name="bizScope" label="作用范围">
            <Input placeholder="如 SITE:1 / ROAD:RING_SOUTH" />
          </Form.Item>
          <Form.Item name="activeTimeRange" label="生效时间">
            <Input placeholder="如 06:00-22:00" />
          </Form.Item>
          <Form.Item name="geoJson" label="GeoJSON / 圆形描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingRecord ? '编辑推送规则' : '新增推送规则'} open={editingKind === 'push'} onCancel={closeModal} onOk={() => void handleSubmit()} confirmLoading={submitLoading}>
        <Form form={pushForm} layout="vertical">
          <Form.Item name="ruleCode" label="关联规则编码" rules={[{ required: true, message: '请选择规则编码' }]}>
            <Select showSearch options={rules.map((item) => ({ label: item.ruleCode, value: item.ruleCode }))} />
          </Form.Item>
          <Form.Item name="level" label="预警等级">
            <Select options={levelOptions} />
          </Form.Item>
          <Form.Item name="channelTypes" label="推送方式" rules={[{ required: true, message: '请输入推送方式' }]}>
            <Input placeholder="如 INBOX,SMS,WEBHOOK" />
          </Form.Item>
          <Form.Item name="receiverExpr" label="推送对象" rules={[{ required: true, message: '请输入推送对象' }]}>
            <Input placeholder="如 admin,leader" />
          </Form.Item>
          <Form.Item name="pushTimeRule" label="推送时机">
            <Input placeholder="如 IMMEDIATE" />
          </Form.Item>
          <Form.Item name="escalationMinutes" label="升级提醒分钟">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default AlertConfig;
