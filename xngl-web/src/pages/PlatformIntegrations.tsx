import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CopyOutlined,
  LinkOutlined,
  MonitorOutlined,
  ReloadOutlined,
  SaveOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import {
  createSsoTicket,
  fetchDamMonitorRecords,
  fetchGovSyncLogs,
  fetchPlatformIntegrationConfigs,
  fetchPlatformIntegrationOverview,
  fetchPlatformSyncLogs,
  fetchPlatformVideoChannels,
  fetchWeighbridgeRecords,
  issueWeighbridgeControlCommand,
  mockSyncDamMonitorRecord,
  mockSyncGovPermits,
  mockSyncWeighbridgeRecord,
  updatePlatformIntegrationConfig,
  type DamMonitorRecord,
  type DamMonitorRecordPayload,
  type GovPermitSyncPayload,
  type GovPermitSyncResult,
  type PlatformIntegrationConfig,
  type PlatformIntegrationConfigPayload,
  type PlatformIntegrationOverview,
  type PlatformSyncLogRecord,
  type PlatformVideoChannel,
  type SsoTicketRecord,
  type WeighbridgeControlPayload,
  type WeighbridgeRecord,
  type WeighbridgeRecordPayload,
} from '../utils/platformApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';

const { Paragraph, Text } = Typography;

type ConfigFormState = Record<string, PlatformIntegrationConfigPayload>;

const integrationDescriptions: Record<string, string> = {
  SSO: '统一身份认证与跨平台单点登录配置，支持免密票据生成和换取平台 JWT。',
  VIDEO: '项目监控与场地监控平台接入配置，当前自动聚合场地设备视频通道。',
  DAM_MONITOR: '坝体监测设备接入预留，支持本地 mock 同步与在线状态校验。',

const statusColorMap: Record<string, string> = {
  ONLINE: 'green',
  OFFLINE: 'default',
  ACTIVE: 'blue',
  ENABLED: 'green',
  DISABLED: 'default',
  WARNING: 'orange',
  ALERT: 'red',
  NORMAL: 'green',

const PlatformIntegrations: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submittingCode, setSubmittingCode] = useState<string>();
  const [overview, setOverview] = useState<PlatformIntegrationOverview>();
  const [configs, setConfigs] = useState<PlatformIntegrationConfig[]>([]);
  const [drafts, setDrafts] = useState<ConfigFormState>({});
  const [videoChannels, setVideoChannels] = useState<PlatformVideoChannel[]>([]);
  const [damRecords, setDamRecords] = useState<DamMonitorRecord[]>([]);
  const [syncLogs, setSyncLogs] = useState<PlatformSyncLogRecord[]>([]);
  const [weighbridgeRecords, setWeighbridgeRecords] = useState<WeighbridgeRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);
  const [selectedSiteId, setSelectedSiteId] = useState<string>();
  const [selectedWeighbridgeSiteId, setSelectedWeighbridgeSiteId] = useState<string>();
  const [ticketLoading, setTicketLoading] = useState(false);
  const [damModalOpen, setDamModalOpen] = useState(false);
  const [damSubmitting, setDamSubmitting] = useState(false);
  const [govModalOpen, setGovModalOpen] = useState(false);
  const [govSubmitting, setGovSubmitting] = useState(false);
  const [latestGovSync, setLatestGovSync] = useState<GovPermitSyncResult>();
  const [weighbridgeModalOpen, setWeighbridgeModalOpen] = useState(false);
  const [weighbridgeSubmitting, setWeighbridgeSubmitting] = useState(false);
  const [controlModalOpen, setControlModalOpen] = useState(false);
  const [controlSubmitting, setControlSubmitting] = useState(false);
  const [ticketForm] = Form.useForm<{ targetPlatform?: string; redirectUri?: string }>();
  const [damForm] = Form.useForm<DamMonitorRecordPayload>();
  const [govForm] = Form.useForm<GovPermitSyncPayload>();
  const [weighbridgeForm] = Form.useForm<WeighbridgeRecordPayload>();
  const [controlForm] = Form.useForm<WeighbridgeControlPayload>();
  const [latestTicket, setLatestTicket] = useState<SsoTicketRecord>();

  const loadAll = async (siteId?: string, weighbridgeSiteId?: string) => {
    setLoading(true);
    try {
      const [overviewRes, configRes, channelRes, damRes, siteRes, syncLogRes, govLogRes, weighbridgeRes] = await Promise.all([
        fetchPlatformIntegrationOverview(),
        fetchPlatformIntegrationConfigs(),
        fetchPlatformVideoChannels(),
        fetchDamMonitorRecords(siteId),
        fetchSites(),
        fetchPlatformSyncLogs(),
        fetchGovSyncLogs(),
        fetchWeighbridgeRecords(weighbridgeSiteId),
      ]);
      setOverview(overviewRes);
      setConfigs(configRes);
      setDrafts(
        configRes.reduce<ConfigFormState>((acc, item) => {
          acc[item.integrationCode] = {
            enabled: item.enabled,
            vendorName: item.vendorName || '',
            baseUrl: item.baseUrl || '',
            apiVersion: item.apiVersion || '',
            clientId: item.clientId || '',
            clientSecret: item.clientSecret || '',
            accessKey: item.accessKey || '',
            accessSecret: item.accessSecret || '',
            callbackPath: item.callbackPath || '',
            extJson: item.extJson || '',
            remark: item.remark || '',
          };
          return acc;
        }, {}),
      );
      setVideoChannels(channelRes);
      setDamRecords(damRes);
      setSites(siteRes);
      setSyncLogs([...govLogRes, ...syncLogRes.filter((item) => item.integrationCode !== 'GOV_PORTAL')]);
      setWeighbridgeRecords(weighbridgeRes);
    } catch (error) {
      console.error(error);
      message.error('加载平台对接中心失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadAll();
  }, []);

  const configCards = useMemo(() => {
    return configs.map((config) => ({
      config,
      draft: drafts[config.integrationCode] || {},
    }));
  }, [configs, drafts]);

  const updateDraft = (
    integrationCode: string,
    field: keyof PlatformIntegrationConfigPayload,
    value: string | boolean | undefined,
  ) => {
    setDrafts((current) => ({
      ...current,
      [integrationCode]: {
        ...current[integrationCode],
        [field]: value,
      },
    }));
  };

  const handleSaveConfig = async (integrationCode: string) => {
    setSubmittingCode(integrationCode);
    try {
      await updatePlatformIntegrationConfig(integrationCode, drafts[integrationCode] || {});
      message.success(`${integrationCode} 配置已保存`);
      await loadAll(selectedSiteId);
    } catch (error) {
      console.error(error);
      message.error('保存配置失败');
    } finally {
      setSubmittingCode(undefined);
    }
  };

  const handleCreateTicket = async () => {
    try {
      const values = await ticketForm.validateFields();
      setTicketLoading(true);
      const ticket = await createSsoTicket(values);
      setLatestTicket(ticket);
      message.success('单点登录票据已生成');
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('生成 SSO 票据失败');
    } finally {
      setTicketLoading(false);
    }
  };

  const handleCopy = async (value?: string | null) => {
    if (!value) {
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      message.success('已复制到剪贴板');
    } catch (error) {
      console.error(error);
      message.error('复制失败');
    }
  };

  const handleDamFilter = async (value?: string) => {
    setSelectedSiteId(value);
    try {
      setLoading(true);
      setDamRecords(await fetchDamMonitorRecords(value));
    } catch (error) {
      console.error(error);
      message.error('加载坝体记录失败');
    } finally {
      setLoading(false);
    }
  };

  const handleWeighbridgeFilter = async (value?: string) => {
    setSelectedWeighbridgeSiteId(value);
    try {
      setLoading(true);
      setWeighbridgeRecords(await fetchWeighbridgeRecords(value));
    } catch (error) {
      console.error(error);
      message.error('加载地磅记录失败');
    } finally {
      setLoading(false);
    }
  };

  const openDamModal = () => {
    damForm.resetFields();
    damForm.setFieldsValue({
      siteId: selectedSiteId ? Number(selectedSiteId) : undefined,
      onlineStatus: 'ONLINE',
      safetyLevel: 'NORMAL',
      alarmFlag: false,
    });
    setDamModalOpen(true);
  };

  const openGovModal = () => {
    govForm.resetFields();
    govForm.setFieldsValue({
      syncMode: 'MANUAL',
      includeTransportPermits: true,
      siteId: selectedSiteId ? Number(selectedSiteId) : undefined,
    });
    setGovModalOpen(true);
  };

  const openWeighbridgeModal = () => {
    weighbridgeForm.resetFields();
    weighbridgeForm.setFieldsValue({
      siteId: selectedWeighbridgeSiteId ? Number(selectedWeighbridgeSiteId) : undefined,
      sourceType: 'DEVICE',
      syncStatus: 'SYNCED',
    });
    setWeighbridgeModalOpen(true);
  };

  const openControlModal = () => {
    controlForm.resetFields();
    controlForm.setFieldsValue({
      siteId: selectedWeighbridgeSiteId ? Number(selectedWeighbridgeSiteId) : undefined,
      command: 'OPEN_GATE',
    });
    setControlModalOpen(true);
  };

  const handleSubmitDamRecord = async () => {
    try {
      const values = await damForm.validateFields();
      setDamSubmitting(true);
      await mockSyncDamMonitorRecord(values);
      message.success('坝体监测 mock 数据已同步');
      setDamModalOpen(false);
      await handleDamFilter(selectedSiteId);
      const refreshedOverview = await fetchPlatformIntegrationOverview();
      setOverview(refreshedOverview);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('同步坝体记录失败');
    } finally {
      setDamSubmitting(false);
    }
  };

  const handleSubmitGovSync = async () => {
    try {
      const values = await govForm.validateFields();
      setGovSubmitting(true);
      const result = await mockSyncGovPermits(values);
      setLatestGovSync(result);
      message.success(`政务网同步完成，批次 ${result.batchNo}`);
      setGovModalOpen(false);
      await loadAll(selectedSiteId, selectedWeighbridgeSiteId);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('执行政务网同步失败');
    } finally {
      setGovSubmitting(false);
    }
  };

  const handleSubmitWeighbridge = async () => {
    try {
      const values = await weighbridgeForm.validateFields();
      setWeighbridgeSubmitting(true);
      await mockSyncWeighbridgeRecord(values);
      message.success('地磅记录已同步');
      setWeighbridgeModalOpen(false);
      await loadAll(selectedSiteId, selectedWeighbridgeSiteId);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('同步地磅记录失败');
    } finally {
      setWeighbridgeSubmitting(false);
    }
  };

  const handleSubmitControl = async () => {
    try {
      const values = await controlForm.validateFields();
      setControlSubmitting(true);
      await issueWeighbridgeControlCommand(values);
      message.success('地磅控制命令已下发');
      setControlModalOpen(false);
      await loadAll(selectedSiteId, selectedWeighbridgeSiteId);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('下发控制命令失败');
    } finally {
      setControlSubmitting(false);
    }
  };

  const channelColumns: ColumnsType<PlatformVideoChannel> = [
    {
      title: '场地',
      dataIndex: 'siteName',
      key: 'siteName',
      render: (value) => value || '-',
    },
    {
      title: '设备名称',
      dataIndex: 'deviceName',
      key: 'deviceName',
      render: (value, record) => value || record.deviceCode || '-',
    },
    {
      title: '设备类型',
      dataIndex: 'deviceType',
      key: 'deviceType',
      render: (value) => <Tag>{value || '-'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value) => <Tag color={statusColorMap[String(value || '').toUpperCase()] || 'blue'}>{value || 'UNKNOWN'}</Tag>,
    },
    {
      title: '播放地址',
      dataIndex: 'playUrl',
      key: 'playUrl',
      render: (value) =>
        value ? (
          <Space size={4}>
            <Text className="font-mono">{value}</Text>
            <Button type="link" icon={<CopyOutlined />} onClick={() => void handleCopy(value)}>
              复制
            </Button>
          </Space>
        ) : (
          '-'
        ),
    },
  ];

  const damColumns: ColumnsType<DamMonitorRecord> = [
    {
      title: '场地',
      dataIndex: 'siteName',
      key: 'siteName',
      render: (value) => value || '-',
    },
    {
      title: '监测点',
      dataIndex: 'deviceName',
      key: 'deviceName',
      render: (value) => value || '-',
    },
    {
      title: '监测时间',
      dataIndex: 'monitorTime',
      key: 'monitorTime',
      render: (value) => value || '-',
    },
    {
      title: '在线状态',
      dataIndex: 'onlineStatus',
      key: 'onlineStatus',
      render: (value) => <Tag color={statusColorMap[String(value || '').toUpperCase()] || 'blue'}>{value || 'UNKNOWN'}</Tag>,
    },
    {
      title: '安全等级',
      dataIndex: 'safetyLevel',
      key: 'safetyLevel',
      render: (value) => <Tag color={statusColorMap[String(value || '').toUpperCase()] || 'orange'}>{value || 'UNKNOWN'}</Tag>,
    },
    {
      title: '指标',
      key: 'metrics',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>位移 {record.displacementValue ?? 0}</Text>
          <Text>水位 {record.waterLevel ?? 0}</Text>
          <Text>降雨 {record.rainfall ?? 0}</Text>
        </Space>
      ),
    },
    {
      title: '告警',
      dataIndex: 'alarmFlag',
      key: 'alarmFlag',
      render: (value) => <Tag color={value ? 'red' : 'green'}>{value ? '是' : '否'}</Tag>,
    },
  ];

  const syncLogColumns: ColumnsType<PlatformSyncLogRecord> = [
    { title: '对接类型', dataIndex: 'integrationCode', key: 'integrationCode', render: (value) => <Tag>{value || '-'}</Tag> },
    { title: '业务类型', dataIndex: 'bizType', key: 'bizType', render: (value) => value || '-' },
    { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', render: (value) => <span className="font-mono">{value || '-'}</span> },
    { title: '同步方式', dataIndex: 'syncMode', key: 'syncMode', render: (value) => value || '-' },
    { title: '结果', key: 'result', render: (_, record) => `${record.successCount ?? 0} / ${record.totalCount ?? 0}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value) => <Tag color={statusColorMap[String(value || '').toUpperCase()] || 'blue'}>{value || '-'}</Tag> },
    { title: '同步时间', dataIndex: 'syncTime', key: 'syncTime', render: (value) => value || '-' },
  ];

  const weighbridgeColumns: ColumnsType<WeighbridgeRecord> = [
    { title: '场地', dataIndex: 'siteName', key: 'siteName', render: (value) => value || '-' },
    { title: '设备', dataIndex: 'deviceName', key: 'deviceName', render: (value) => value || '-' },
    { title: '车牌号', dataIndex: 'vehicleNo', key: 'vehicleNo', render: (value) => value || '-' },
    { title: '过磅单号', dataIndex: 'ticketNo', key: 'ticketNo', render: (value) => <span className="font-mono">{value || '-'}</span> },
    {
      title: '重量/方量',
      key: 'weight',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>毛/皮/净：{record.grossWeight ?? 0} / {record.tareWeight ?? 0} / {record.netWeight ?? 0}</Text>
          <Text>折算方量：{record.estimatedVolume ?? 0}</Text>
        </Space>
      ),
    },
    { title: '控制命令', dataIndex: 'controlCommand', key: 'controlCommand', render: (value) => value || '-' },
    { title: '过磅时间', dataIndex: 'weighTime', key: 'weighTime', render: (value) => value || '-' },
    { title: '状态', dataIndex: 'syncStatus', key: 'syncStatus', render: (value) => <Tag color={statusColorMap[String(value || '').toUpperCase()] || 'blue'}>{value || '-'}</Tag> },
  ];

  return (
    <div className="space-y-6">
        <Button icon={<ReloadOutlined />} onClick={() => void loadAll(selectedSiteId)} loading={loading}>
          刷新数据
        </Button>

        <Row gutter={[16, 16]}>
          <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="已启用对接" value={overview?.enabledCount ?? 0} suffix={`/ ${overview?.totalCount ?? 0}`} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="视频通道" value={overview?.videoChannelCount ?? 0} prefix={<MonitorOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="在线坝体场地" value={overview?.onlineDamSiteCount ?? 0} prefix={<SafetyOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="有效 SSO 票据" value={overview?.activeSsoTicketCount ?? 0} prefix={<LinkOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="政务网同步批次" value={overview?.govSyncCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic title="地磅同步记录" value={overview?.weighbridgeRecordCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {configCards.map(({ config, draft }) => (
          <Col xs={24} xl={8} key={config.integrationCode}>
            <Card
              className="glass-panel g-border-panel border h-full"
              title={
                <Space>
                  <span>{config.integrationName}</span>
                  <Tag color={draft.enabled ? 'green' : 'default'}>{draft.enabled ? '启用中' : '未启用'}</Tag>
                </Space>
              }
              extra={
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  loading={submittingCode === config.integrationCode}
                  onClick={() => void handleSaveConfig(config.integrationCode)}
                >
                  保存
                </Button>
              }
            >
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {integrationDescriptions[config.integrationCode] || '平台对接基础配置'}
                </Paragraph>
                <Input
                  value={draft.vendorName}
                  placeholder="厂商/平台名称"
                  onChange={(event) => updateDraft(config.integrationCode, 'vendorName', event.target.value)}
                />
                <Input
                  value={draft.baseUrl}
                  placeholder="基础地址"
                  onChange={(event) => updateDraft(config.integrationCode, 'baseUrl', event.target.value)}
                />
                <Input
                  value={draft.apiVersion}
                  placeholder="API 版本"
                  onChange={(event) => updateDraft(config.integrationCode, 'apiVersion', event.target.value)}
                />
                <Input
                  value={draft.clientId}
                  placeholder="Client ID"
                  onChange={(event) => updateDraft(config.integrationCode, 'clientId', event.target.value)}
                />
                <Input.Password
                  value={draft.clientSecret}
                  placeholder="Client Secret"
                  onChange={(event) => updateDraft(config.integrationCode, 'clientSecret', event.target.value)}
                />
                <Input
                  value={draft.accessKey}
                  placeholder="Access Key"
                  onChange={(event) => updateDraft(config.integrationCode, 'accessKey', event.target.value)}
                />
                <Input.Password
                  value={draft.accessSecret}
                  placeholder="Access Secret"
                  onChange={(event) => updateDraft(config.integrationCode, 'accessSecret', event.target.value)}
                />
                <Input
                  value={draft.callbackPath}
                  placeholder="回调地址"
                  onChange={(event) => updateDraft(config.integrationCode, 'callbackPath', event.target.value)}
                />
                <Input.TextArea
                  rows={4}
                  value={draft.extJson}
                  placeholder="扩展 JSON"
                  onChange={(event) => updateDraft(config.integrationCode, 'extJson', event.target.value)}
                />
                <Input.TextArea
                  rows={2}
                  value={draft.remark}
                  placeholder="备注"
                  onChange={(event) => updateDraft(config.integrationCode, 'remark', event.target.value)}
                />
                <Text type="secondary">最近更新时间：{config.updateTime || '未配置'}</Text>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={10}>
          <Card className="glass-panel g-border-panel border" title="统一身份认证 SSO 票据">
            <Form form={ticketForm} layout="vertical">
              <Form.Item name="targetPlatform" label="目标平台">
                <Input placeholder="如 OA、监管平台、政务网" />
              </Form.Item>
              <Form.Item name="redirectUri" label="回跳地址">
                <Input placeholder="https://example.com/welcome" />
              </Form.Item>
              <Space>
                <Button type="primary" onClick={() => void handleCreateTicket()} loading={ticketLoading}>
                  生成票据
                </Button>
                {latestTicket?.loginUrl ? (
                  <Button icon={<CopyOutlined />} onClick={() => void handleCopy(latestTicket.loginUrl)}>
                    复制登录地址
                  </Button>
                ) : null}
              </Space>
            </Form>

            {latestTicket ? (
              <Card size="small" style={{ marginTop: 16, background: 'rgba(255,255,255,0.6)' }}>
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  <Text>票据：{latestTicket.ticket}</Text>
                  <Text>目标平台：{latestTicket.targetPlatform || '-'}</Text>
                  <Text>失效时间：{latestTicket.expiresAt || '-'}</Text>
                  <Paragraph copyable={{ text: latestTicket.loginUrl }} style={{ marginBottom: 0 }}>
                    登录地址：{latestTicket.loginUrl}
                  </Paragraph>
                </Space>
              </Card>
            ) : null}
          </Card>
        </Col>
        <Col xs={24} xl={14}>
          <Card className="glass-panel g-border-panel border" title="视频通道映射">
            <Table
              rowKey={(record) => record.deviceId || record.deviceCode || `${record.siteId}-${record.deviceName}`}
              columns={channelColumns}
              dataSource={videoChannels}
              loading={loading}
              pagination={{ pageSize: 5 }}
              scroll={{ x: 900 }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        className="glass-panel g-border-panel border"
        title="坝体监测记录"
        extra={
          <Space>
            <Select
              allowClear
              value={selectedSiteId}
              placeholder="按场地筛选"
              style={{ width: 220 }}
              options={sites.map((site) => ({ label: site.name, value: site.id }))}
              onChange={(value) => void handleDamFilter(value)}
            />
            <Button type="primary" onClick={openDamModal}>
              mock 同步
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={damColumns}
          dataSource={damRecords}
          loading={loading}
          pagination={{ pageSize: 6 }}
          scroll={{ x: 900 }}
        />
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card
            className="glass-panel g-border-panel border"
            title="政务网处置证/准运证同步"
            extra={
              <Button type="primary" onClick={openGovModal}>
                发起同步
              </Button>
            }
          >
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Text type="secondary">支持手动/自动同步模式，统一记录处置证与准运证同步批次。</Text>
              {latestGovSync ? (
                <Card size="small" style={{ background: 'rgba(255,255,255,0.6)' }}>
                  <Space direction="vertical" size={4}>
                    <Text>最近批次：{latestGovSync.batchNo}</Text>
                    <Text>同步结果：{latestGovSync.successCount ?? 0} / {latestGovSync.totalCount ?? 0}</Text>
                    <Text>同步时间：{latestGovSync.syncTime || '-'}</Text>
                  </Space>
                </Card>
              ) : null}
              <Table
                rowKey="id"
                columns={syncLogColumns}
                dataSource={syncLogs.filter((item) => item.integrationCode === 'GOV_PORTAL')}
                pagination={{ pageSize: 5 }}
                scroll={{ x: 760 }}
              />
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card
            className="glass-panel g-border-panel border"
            title="地磅数据对接与本地控制"
            extra={
              <Space>
                <Button onClick={openControlModal}>下发控制命令</Button>
                <Button type="primary" onClick={openWeighbridgeModal}>
                  同步地磅记录
                </Button>
              </Space>
            }
          >
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <div className="flex gap-3">
                <Select
                  allowClear
                  value={selectedWeighbridgeSiteId}
                  placeholder="按场地筛选"
                  style={{ width: 220 }}
                  options={sites.map((site) => ({ label: site.name, value: site.id }))}
                  onChange={(value) => void handleWeighbridgeFilter(value)}
                />
              </div>
              <Table
                rowKey="id"
                columns={weighbridgeColumns}
                dataSource={weighbridgeRecords}
                pagination={{ pageSize: 5 }}
                scroll={{ x: 980 }}
              />
            </Space>
          </Card>
        </Col>
      </Row>

      <Modal
        title="新增坝体监测记录"
        open={damModalOpen}
        onCancel={() => setDamModalOpen(false)}
        onOk={() => void handleSubmitDamRecord()}
        confirmLoading={damSubmitting}
      >
        <Form form={damForm} layout="vertical">
          <Form.Item name="siteId" label="场地" rules={[{ required: true, message: '请选择场地' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={sites.map((site) => ({ label: site.name, value: Number(site.id) }))}
            />
          </Form.Item>
          <Form.Item name="deviceName" label="监测点名称">
            <Input placeholder="默认自动带出场地监测点名称" />
          </Form.Item>
          <Form.Item name="monitorTime" label="监测时间">
            <Input placeholder="2026-03-22T21:00:00" />
          </Form.Item>
          <Form.Item name="onlineStatus" label="在线状态">
            <Select options={[{ label: 'ONLINE', value: 'ONLINE' }, { label: 'OFFLINE', value: 'OFFLINE' }]} />
          </Form.Item>
          <Form.Item name="safetyLevel" label="安全等级">
            <Select
              options={[
                { label: 'NORMAL', value: 'NORMAL' },
                { label: 'WARNING', value: 'WARNING' },
                { label: 'ALERT', value: 'ALERT' },
              ]}
            />
          </Form.Item>
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="displacementValue" label="位移值">
                <InputNumber style={{ width: '100%' }} min={0} precision={4} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="waterLevel" label="水位">
                <InputNumber style={{ width: '100%' }} min={0} precision={4} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="rainfall" label="降雨量">
                <InputNumber style={{ width: '100%' }} min={0} precision={4} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="alarmFlag" label="触发告警" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="发起政务网同步"
        open={govModalOpen}
        onCancel={() => setGovModalOpen(false)}
        onOk={() => void handleSubmitGovSync()}
        confirmLoading={govSubmitting}
      >
        <Form form={govForm} layout="vertical">
          <Form.Item name="syncMode" label="同步方式">
            <Select options={[{ label: 'MANUAL', value: 'MANUAL' }, { label: 'AUTO', value: 'AUTO' }]} />
          </Form.Item>
          <Form.Item name="includeTransportPermits" label="同步准运证" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="siteId" label="指定场地">
            <Select allowClear options={sites.map((site) => ({ label: site.name, value: Number(site.id) }))} />
          </Form.Item>
          <Form.Item name="vehicleNo" label="指定车牌">
            <Input placeholder="可选，默认自动生成" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="如：每日例行同步、人工补拉政务网证件" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="同步地磅记录"
        open={weighbridgeModalOpen}
        onCancel={() => setWeighbridgeModalOpen(false)}
        onOk={() => void handleSubmitWeighbridge()}
        confirmLoading={weighbridgeSubmitting}
      >
        <Form form={weighbridgeForm} layout="vertical">
          <Form.Item name="siteId" label="场地" rules={[{ required: true, message: '请选择场地' }]}>
            <Select options={sites.map((site) => ({ label: site.name, value: Number(site.id) }))} />
          </Form.Item>
          <Form.Item name="vehicleNo" label="车牌号" rules={[{ required: true, message: '请输入车牌号' }]}>
            <Input placeholder="如 浙A10001" />
          </Form.Item>
          <Form.Item name="ticketNo" label="过磅单号">
            <Input placeholder="默认自动生成" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="grossWeight" label="毛重">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="tareWeight" label="皮重">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="estimatedVolume" label="折算方量">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="weighTime" label="过磅时间">
            <Input placeholder="2026-03-23T09:20:00" />
          </Form.Item>
          <Form.Item name="controlCommand" label="控制命令">
            <Select allowClear options={[{ label: 'OPEN_GATE', value: 'OPEN_GATE' }, { label: 'CAPTURE', value: 'CAPTURE' }, { label: 'RESET_SCALE', value: 'RESET_SCALE' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="下发地磅控制命令"
        open={controlModalOpen}
        onCancel={() => setControlModalOpen(false)}
        onOk={() => void handleSubmitControl()}
        confirmLoading={controlSubmitting}
      >
        <Form form={controlForm} layout="vertical">
          <Form.Item name="siteId" label="场地" rules={[{ required: true, message: '请选择场地' }]}>
            <Select options={sites.map((site) => ({ label: site.name, value: Number(site.id) }))} />
          </Form.Item>
          <Form.Item name="command" label="控制命令" rules={[{ required: true, message: '请选择命令' }]}>
            <Select options={[{ label: 'OPEN_GATE', value: 'OPEN_GATE' }, { label: 'CAPTURE', value: 'CAPTURE' }, { label: 'RESET_SCALE', value: 'RESET_SCALE' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="如：地磅复位、补抓拍、开闸放行" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default PlatformIntegrations;
