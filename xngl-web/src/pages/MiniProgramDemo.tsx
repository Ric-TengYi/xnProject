import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Row,
  Select,
  Space,
  Statistic,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import { motion } from 'framer-motion';
import {
  LinkOutlined,
  LoginOutlined,
  MobileOutlined,
  NotificationOutlined,
  PictureOutlined,
  TruckOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker, MapPoint, MapPolyline } from '../components/TiandituMap';
import {
  bindMiniAccount,
  createMiniEvent,
  createMiniFeedback,
  createMiniManualDisposal,
  createMiniPhoto,
  fetchMiniExcavationOrg,
  fetchMiniExcavationProjectDetail,
  fetchMiniExcavationProjects,
  fetchMiniProfile,
  fetchMiniSites,
  fetchMiniVehicleTrackHistory,
  fetchMiniVehicles,
  listMiniEvents,
  listMiniFeedbacks,
  listMiniManualDisposals,
  listMiniPhotos,
  loginMini,
  openIdLoginMini,
  sendMiniSmsCode,
  type MiniAccessibleSite,
  type MiniEventRecord,
  type MiniExcavationOrg,
  type MiniExcavationProject,
  type MiniFeedbackRecord,
  type MiniManualDisposalRecord,
  type MiniPhotoRecord,
  type MiniProjectContract,
  type MiniUserProfile,
  type MiniVehicleRecord,
  type MiniVehicleTrackHistory,
} from '../utils/miniApi';

const { Paragraph, Text, Title } = Typography;

const defaultAuthValues = {
  tenantId: '1',
  username: 'admin',
  password: 'admin',
  mobile: '13800000001',
  openId: 'mini-openid-demo-20260408',
};

const eventTypeOptions = [
  { label: '现场异常', value: 'SITE_EXCEPTION' },
  { label: '违规线索', value: 'VIOLATION_CLUE' },
  { label: '设备问题', value: 'DEVICE_FAILURE' },
];

const feedbackTypeOptions = [
  { label: '系统问题', value: 'SYSTEM' },
  { label: '数据问题', value: 'DATA' },
  { label: '业务建议', value: 'BUSINESS' },
];

const resolveSiteId = (site?: MiniAccessibleSite | null) => Number(site?.siteId || 0);
const resolveSiteName = (site?: MiniAccessibleSite | null) => site?.siteName || `场地#${site?.siteId || '-'}`;
const resolveProjectId = (project?: MiniExcavationProject | null) => Number(project?.projectId || 0);
const resolveProjectName = (project?: MiniExcavationProject | null) =>
  project?.projectName || `项目#${project?.projectId || '-'}`;
const resolveContractId = (contract?: MiniProjectContract | null) => Number(contract?.contractId || 0);
const resolveContractName = (contract?: MiniProjectContract | null) =>
  contract?.contractNo || contract?.contractName || `合同#${contract?.contractId || '-'}`;

const MiniProgramDemo: React.FC = () => {
  const [authForm] = Form.useForm<typeof defaultAuthValues & { smsCode?: string }>();
  const [photoForm] = Form.useForm<{ projectId?: number; siteId?: number; plateNo?: string; fileUrl?: string; remark?: string }>();
  const [disposalForm] = Form.useForm<{ siteId?: number; contractId?: number; volume?: number; vehicleId?: number; plateNo?: string; remark?: string }>();
  const [eventForm] = Form.useForm<{ siteId?: number; title?: string; eventType?: string; priority?: string; content?: string }>();
  const [feedbackForm] = Form.useForm<{ siteId?: number; title?: string; feedbackType?: string; content?: string }>();

  const [submitting, setSubmitting] = useState<string>();
  const [miniToken, setMiniToken] = useState<string>();
  const [profile, setProfile] = useState<MiniUserProfile | null>(null);
  const [currentOrg, setCurrentOrg] = useState<MiniExcavationOrg | null>(null);
  const [sites, setSites] = useState<MiniAccessibleSite[]>([]);
  const [projects, setProjects] = useState<MiniExcavationProject[]>([]);
  const [contracts, setContracts] = useState<MiniProjectContract[]>([]);
  const [photos, setPhotos] = useState<MiniPhotoRecord[]>([]);
  const [manualDisposals, setManualDisposals] = useState<MiniManualDisposalRecord[]>([]);
  const [events, setEvents] = useState<MiniEventRecord[]>([]);
  const [feedbacks, setFeedbacks] = useState<MiniFeedbackRecord[]>([]);
  const [vehicles, setVehicles] = useState<MiniVehicleRecord[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<string>();
  const [trackHistory, setTrackHistory] = useState<MiniVehicleTrackHistory | null>(null);

  useEffect(() => {
    authForm.setFieldsValue(defaultAuthValues);
  }, [authForm]);

  const currentSite = useMemo(() => sites[0] || null, [sites]);
  const currentProject = useMemo(() => projects[0] || null, [projects]);
  const currentContract = useMemo(() => contracts[0] || null, [contracts]);
  const currentVehicle = useMemo(
    () => vehicles.find((item) => String(item.vehicleId) === String(selectedVehicleId)) || vehicles[0] || null,
    [selectedVehicleId, vehicles],
  );

  const currentSiteId = resolveSiteId(currentSite);
  const currentProjectId = resolveProjectId(currentProject);
  const currentContractId = resolveContractId(currentContract);

  useEffect(() => {
    photoForm.setFieldsValue({
      projectId: currentProjectId || undefined,
      siteId: currentSiteId || undefined,
      fileUrl: 'https://example.com/demo/excavation-photo.jpg',
      plateNo: '浙A12345',
      remark: '演示端提交的出土拍照记录',
    });
  }, [currentProjectId, currentSiteId, photoForm]);

  useEffect(() => {
    disposalForm.setFieldsValue({
      siteId: currentSiteId || undefined,
      contractId: currentContractId || undefined,
      volume: 18.5,
      vehicleId: currentVehicle?.vehicleId ? Number(currentVehicle.vehicleId) : undefined,
      plateNo: currentVehicle?.plateNo || '浙A10007',
      remark: '演示端提交的人工消纳记录',
    });
  }, [currentContractId, currentSiteId, currentVehicle?.plateNo, currentVehicle?.vehicleId, disposalForm]);

  useEffect(() => {
    eventForm.setFieldsValue({
      siteId: currentSiteId || undefined,
      title: '移动端事件上报',
      eventType: 'SITE_EXCEPTION',
      priority: 'HIGH',
      content: '现场围挡破损，已通过移动端提交事件并同步后台处置。',
    });
    feedbackForm.setFieldsValue({
      siteId: currentSiteId || undefined,
      title: '移动端问题反馈',
      feedbackType: 'SYSTEM',
      content: '移动作业端已支持正式演示，可继续补充报表与安全教育入口。',
    });
  }, [currentSiteId, eventForm, feedbackForm]);

  const loadTrackHistory = async (token: string, vehicleId?: string | number | null) => {
    if (!vehicleId) {
      setTrackHistory(null);
      return;
    }
    const history = await fetchMiniVehicleTrackHistory(token, vehicleId, {
      startTime: dayjs().startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
      endTime: dayjs().endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    });
    setTrackHistory(history);
  };

  const reloadMiniWorkspace = async (token: string) => {
    const [me, accessibleSites, org, excavationProjects, realtimeVehicles] = await Promise.all([
      fetchMiniProfile(token),
      fetchMiniSites(token),
      fetchMiniExcavationOrg(token).catch(() => null),
      fetchMiniExcavationProjects(token).catch(() => []),
      fetchMiniVehicles(token, { pageNo: 1, pageSize: 12 }),
    ]);
    setProfile(me);
    setSites(accessibleSites || []);
    setCurrentOrg(org);
    setProjects(excavationProjects || []);
    setVehicles(realtimeVehicles || []);

    const firstProjectId = resolveProjectId((excavationProjects || [])[0]);
    if (firstProjectId) {
      const detail = await fetchMiniExcavationProjectDetail(token, firstProjectId);
      setContracts(Array.isArray(detail.contractDetails) ? detail.contractDetails : []);
    } else {
      setContracts([]);
    }

    const firstSiteId = resolveSiteId((accessibleSites || [])[0]);
    const [photoRecords, disposalRecords, eventRecords, feedbackRecords] = await Promise.all([
      listMiniPhotos(token, {
        projectId: firstProjectId || undefined,
        siteId: firstSiteId || undefined,
      }).catch(() => []),
      listMiniManualDisposals(token, {
        siteId: firstSiteId || undefined,
      }).catch(() => []),
      listMiniEvents(token).catch(() => []),
      listMiniFeedbacks(token).catch(() => []),
    ]);
    setPhotos(photoRecords || []);
    setManualDisposals(disposalRecords || []);
    setEvents(eventRecords || []);
    setFeedbacks(feedbackRecords || []);
    const nextVehicleId = realtimeVehicles?.[0]?.vehicleId;
    setSelectedVehicleId(nextVehicleId ? String(nextVehicleId) : undefined);
    await loadTrackHistory(token, nextVehicleId);
  };

  const withSubmitting = async (key: string, action: () => Promise<void>) => {
    setSubmitting(key);
    try {
      await action();
    } finally {
      setSubmitting(undefined);
    }
  };

  const handleSendCode = async () => {
    const values = await authForm.validateFields(['tenantId', 'username', 'password', 'mobile', 'openId']);
    await withSubmitting('send-code', async () => {
      const response = await sendMiniSmsCode({
        tenantId: values.tenantId,
        username: values.username,
        password: values.password,
        mobile: values.mobile,
        openId: values.openId,
      });
      authForm.setFieldValue('smsCode', response.mockCode || '');
      message.success('验证码已发送，请输入后完成登录');
    });
  };

  const handleLogin = async () => {
    const values = await authForm.validateFields();
    await withSubmitting('login', async () => {
      const response = await loginMini({
        tenantId: values.tenantId,
        username: values.username,
        password: values.password,
        mobile: values.mobile,
        smsCode: values.smsCode || '',
        openId: values.openId,
        deviceName: '演示移动端',
      });
      setMiniToken(response.token);
      message.success('移动端登录成功');
      await reloadMiniWorkspace(response.token);
    });
  };

  const handleOpenIdLogin = async () => {
    const values = await authForm.validateFields(['openId']);
    await withSubmitting('openid-login', async () => {
      const response = await openIdLoginMini({
        openId: values.openId,
        deviceName: '演示移动端',
      });
      setMiniToken(response.token);
      message.success('openId 免登成功');
      await reloadMiniWorkspace(response.token);
    });
  };

  const handleBindOpenId = async () => {
    if (!miniToken) {
      message.warning('请先完成移动端登录');
      return;
    }
    const values = await authForm.validateFields(['openId']);
    await withSubmitting('bind-openid', async () => {
      const result = await bindMiniAccount(miniToken, {
        openId: values.openId,
        deviceName: '演示移动端',
      });
      setProfile(result);
      message.success('账号绑定成功');
    });
  };

  const refreshAfterMutation = async () => {
    if (!miniToken) {
      return;
    }
    await reloadMiniWorkspace(miniToken);
  };

  const handleCreatePhoto = async () => {
    if (!miniToken) {
      message.warning('请先完成移动端登录');
      return;
    }
    const values = await photoForm.validateFields();
    await withSubmitting('photo', async () => {
      await createMiniPhoto(miniToken, {
        projectId: Number(values.projectId),
        siteId: values.siteId ? Number(values.siteId) : undefined,
        plateNo: values.plateNo,
        fileUrl: values.fileUrl || 'https://example.com/demo/excavation-photo.jpg',
        photoType: 'EXCAVATION',
        shootTime: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
        remark: values.remark,
      });
      message.success('出土拍照已提交');
      await refreshAfterMutation();
    });
  };

  const handleCreateDisposal = async () => {
    if (!miniToken) {
      message.warning('请先完成移动端登录');
      return;
    }
    const values = await disposalForm.validateFields();
    await withSubmitting('disposal', async () => {
      await createMiniManualDisposal(miniToken, {
        siteId: Number(values.siteId),
        contractId: Number(values.contractId),
        vehicleId: values.vehicleId ? Number(values.vehicleId) : undefined,
        plateNo: values.plateNo,
        volume: Number(values.volume),
        disposalTime: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
        remark: values.remark,
      });
      message.success('人工消纳已确认');
      await refreshAfterMutation();
    });
  };

  const handleCreateEvent = async () => {
    if (!miniToken) {
      message.warning('请先完成移动端登录');
      return;
    }
    const values = await eventForm.validateFields();
    await withSubmitting('event', async () => {
      await createMiniEvent(miniToken, {
        title: values.title || '',
        content: values.content,
        eventType: values.eventType,
        siteId: values.siteId ? Number(values.siteId) : undefined,
        priority: values.priority,
      });
      message.success('事件上报已提交');
      await refreshAfterMutation();
    });
  };

  const handleCreateFeedback = async () => {
    if (!miniToken) {
      message.warning('请先完成移动端登录');
      return;
    }
    const values = await feedbackForm.validateFields();
    await withSubmitting('feedback', async () => {
      await createMiniFeedback(miniToken, {
        feedbackType: values.feedbackType || 'SYSTEM',
        title: values.title || '',
        content: values.content || '',
        siteId: values.siteId ? Number(values.siteId) : undefined,
      });
      message.success('问题反馈已提交');
      await refreshAfterMutation();
    });
  };

  const handleSwitchVehicle = async (vehicleId?: string) => {
    setSelectedVehicleId(vehicleId);
    if (!miniToken || !vehicleId) {
      setTrackHistory(null);
      return;
    }
    await withSubmitting('track', async () => {
      await loadTrackHistory(miniToken, vehicleId);
    });
  };

  const trackMarkers = useMemo<MapMarker[]>(() => {
    const points = Array.isArray(trackHistory?.points) ? trackHistory?.points : [];
    if (!points.length) {
      return currentVehicle?.lng != null && currentVehicle?.lat != null
        ? [{ id: String(currentVehicle.vehicleId || 'vehicle'), position: [Number(currentVehicle.lng), Number(currentVehicle.lat)] }]
        : [];
    }
    const lastPoint = points[points.length - 1];
    return [{ id: String(trackHistory?.vehicleId || 'vehicle'), position: [lastPoint.lng, lastPoint.lat] }];
  }, [currentVehicle?.lat, currentVehicle?.lng, currentVehicle?.vehicleId, trackHistory?.points, trackHistory?.vehicleId]);

  const trackLines = useMemo<MapPolyline[]>(() => {
    const points = Array.isArray(trackHistory?.points) ? trackHistory?.points : [];
    if (points.length < 2) {
      return [];
    }
    return [
      {
        id: 'mini-track-line',
        path: points.map((item) => [item.lng, item.lat] as MapPoint),
        color: '#0f62ff',
        weight: 4,
        opacity: 0.88,
      },
    ];
  }, [trackHistory?.points]);

  const mapCenter = useMemo<MapPoint>(() => {
    const points = Array.isArray(trackHistory?.points) ? trackHistory?.points : [];
    if (points.length > 0) {
      return [points[0].lng, points[0].lat];
    }
    if (currentVehicle?.lng != null && currentVehicle?.lat != null) {
      return [Number(currentVehicle.lng), Number(currentVehicle.lat)];
    }
    return [120.1551, 30.2741];
  }, [currentVehicle?.lat, currentVehicle?.lng, trackHistory?.points]);

  const siteOptions = useMemo(
    () => sites.map((item) => ({ label: resolveSiteName(item), value: resolveSiteId(item) })),
    [sites],
  );
  const projectOptions = useMemo(
    () => projects.map((item) => ({ label: resolveProjectName(item), value: resolveProjectId(item) })),
    [projects],
  );
  const contractOptions = useMemo(
    () => contracts.map((item) => ({ label: resolveContractName(item), value: resolveContractId(item) })),
    [contracts],
  );
  const vehicleOptions = useMemo(
    () =>
      vehicles.map((item) => ({
        label: `${item.plateNo || item.vehicleId} / ${item.runningStatus || 'UNKNOWN'}`,
        value: String(item.vehicleId),
      })),
    [vehicles],
  );

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.28 }}
      className="space-y-6"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Title level={2} style={{ marginBottom: 4 }}>
            移动作业端演示
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            面向甲方正式演示录屏，使用现有 mini 接口完成登录、现场作业、事件留痕与车辆监管闭环。
          </Paragraph>
        </div>
        <Space wrap>
          <Tag color={miniToken ? 'success' : 'default'}>{miniToken ? '移动端已登录' : '未登录'}</Tag>
          <Tag color="blue">账号绑定</Tag>
          <Tag color="purple">现场作业</Tag>
          <Tag color="orange">车辆跟踪</Tag>
        </Space>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} xl={10}>
          <Card
            className="glass-panel g-border-panel border"
            bodyStyle={{ padding: 20, background: 'linear-gradient(180deg, rgba(15,98,255,0.04), rgba(255,255,255,0.98))' }}
          >
            <div
              style={{
                margin: '0 auto',
                maxWidth: 380,
                borderRadius: 32,
                border: '1px solid rgba(15,98,255,0.14)',
                background: '#fff',
                boxShadow: '0 20px 60px rgba(15,98,255,0.08)',
                overflow: 'hidden',
              }}
            >
              <div style={{ padding: '14px 18px', background: '#0f62ff', color: '#fff' }}>
                <div className="flex items-center justify-between">
                  <Space>
                    <MobileOutlined />
                    <Text style={{ color: '#fff' }}>移动作业端</Text>
                  </Space>
                  <Text style={{ color: 'rgba(255,255,255,0.8)' }}>{profile?.name || '演示账号'}</Text>
                </div>
              </div>

              <Tabs
                defaultActiveKey="auth"
                items={[
                  {
                    key: 'auth',
                    label: '登录',
                    children: (
                      <div className="space-y-4 p-4">
                        <Form form={authForm} layout="vertical">
                          <Form.Item name="tenantId" label="租户ID" hidden>
                            <Input />
                          </Form.Item>
                          <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
                            <Input placeholder="请输入移动端账号" />
                          </Form.Item>
                          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                            <Input.Password placeholder="请输入密码" />
                          </Form.Item>
                          <Form.Item name="mobile" label="手机号" rules={[{ required: true, message: '请输入手机号' }]}>
                            <Input placeholder="请输入手机号" />
                          </Form.Item>
                          <Form.Item name="openId" label="OpenID">
                            <Input placeholder="用于绑定和免登演示" />
                          </Form.Item>
                          <Form.Item name="smsCode" label="短信验证码" rules={[{ required: true, message: '请输入验证码' }]}>
                            <Input placeholder="先获取验证码再登录" />
                          </Form.Item>
                        </Form>
                        <Space wrap>
                          <Button loading={submitting === 'send-code'} onClick={() => void handleSendCode()}>
                            获取验证码
                          </Button>
                          <Button
                            type="primary"
                            icon={<LoginOutlined />}
                            loading={submitting === 'login'}
                            onClick={() => void handleLogin()}
                          >
                            登录移动端
                          </Button>
                          <Button
                            icon={<LinkOutlined />}
                            loading={submitting === 'bind-openid'}
                            onClick={() => void handleBindOpenId()}
                          >
                            绑定账号
                          </Button>
                          <Button loading={submitting === 'openid-login'} onClick={() => void handleOpenIdLogin()}>
                            OpenID 免登
                          </Button>
                        </Space>
                      </div>
                    ),
                  },
                  {
                    key: 'field',
                    label: '现场',
                    children: (
                      <div className="space-y-4 p-4">
                        <Card size="small" title="出土打卡 / 拍照" extra={<PictureOutlined />}>
                          <Form form={photoForm} layout="vertical">
                            <Form.Item name="projectId" label="项目" rules={[{ required: true, message: '请选择项目' }]}>
                              <Select options={projectOptions} placeholder="请选择项目" />
                            </Form.Item>
                            <Form.Item name="siteId" label="场地">
                              <Select options={siteOptions} placeholder="请选择场地" />
                            </Form.Item>
                            <Form.Item name="plateNo" label="车牌号">
                              <Input placeholder="如 浙A12345" />
                            </Form.Item>
                            <Form.Item name="fileUrl" label="照片地址" rules={[{ required: true, message: '请输入照片地址' }]}>
                              <Input placeholder="请输入照片 URL" />
                            </Form.Item>
                            <Form.Item name="remark" label="备注">
                              <Input.TextArea rows={2} />
                            </Form.Item>
                            <Button type="primary" block loading={submitting === 'photo'} onClick={() => void handleCreatePhoto()}>
                              提交出土拍照
                            </Button>
                          </Form>
                        </Card>

                        <Card size="small" title="消纳确认 / 人工消纳" extra={<TruckOutlined />}>
                          <Form form={disposalForm} layout="vertical">
                            <Form.Item name="siteId" label="场地" rules={[{ required: true, message: '请选择场地' }]}>
                              <Select options={siteOptions} placeholder="请选择场地" />
                            </Form.Item>
                            <Form.Item name="contractId" label="合同" rules={[{ required: true, message: '请选择合同' }]}>
                              <Select options={contractOptions} placeholder="请选择合同" />
                            </Form.Item>
                            <Form.Item name="vehicleId" label="车辆">
                              <Select options={vehicleOptions} placeholder="可选关联车辆" />
                            </Form.Item>
                            <Form.Item name="volume" label="消纳方量" rules={[{ required: true, message: '请输入方量' }]}>
                              <InputNumber min={0.1} precision={2} className="w-full" />
                            </Form.Item>
                            <Form.Item name="plateNo" label="车牌号">
                              <Input />
                            </Form.Item>
                            <Form.Item name="remark" label="备注">
                              <Input.TextArea rows={2} />
                            </Form.Item>
                            <Button type="primary" block loading={submitting === 'disposal'} onClick={() => void handleCreateDisposal()}>
                              确认消纳并留痕
                            </Button>
                          </Form>
                        </Card>

                        <Card size="small" title="事件上报 / 问题反馈" extra={<NotificationOutlined />}>
                          <Form form={eventForm} layout="vertical">
                            <Form.Item name="siteId" label="场地">
                              <Select options={siteOptions} placeholder="请选择场地" />
                            </Form.Item>
                            <Form.Item name="eventType" label="事件类型" rules={[{ required: true, message: '请选择事件类型' }]}>
                              <Select options={eventTypeOptions} />
                            </Form.Item>
                            <Form.Item name="title" label="事件标题" rules={[{ required: true, message: '请输入标题' }]}>
                              <Input />
                            </Form.Item>
                            <Form.Item name="content" label="事件说明">
                              <Input.TextArea rows={2} />
                            </Form.Item>
                            <Button type="primary" block loading={submitting === 'event'} onClick={() => void handleCreateEvent()}>
                              提交事件上报
                            </Button>
                          </Form>
                          <Divider />
                          <Form form={feedbackForm} layout="vertical">
                            <Form.Item name="siteId" label="场地">
                              <Select options={siteOptions} placeholder="请选择场地" />
                            </Form.Item>
                            <Form.Item name="feedbackType" label="反馈类型" rules={[{ required: true, message: '请选择反馈类型' }]}>
                              <Select options={feedbackTypeOptions} />
                            </Form.Item>
                            <Form.Item name="title" label="反馈标题" rules={[{ required: true, message: '请输入标题' }]}>
                              <Input />
                            </Form.Item>
                            <Form.Item name="content" label="反馈内容" rules={[{ required: true, message: '请输入反馈内容' }]}>
                              <Input.TextArea rows={2} />
                            </Form.Item>
                            <Button block loading={submitting === 'feedback'} onClick={() => void handleCreateFeedback()}>
                              提交问题反馈
                            </Button>
                          </Form>
                        </Card>
                      </div>
                    ),
                  },
                  {
                    key: 'vehicle',
                    label: '车辆',
                    children: (
                      <div className="space-y-4 p-4">
                        <Select
                          value={selectedVehicleId}
                          options={vehicleOptions}
                          placeholder="请选择车辆"
                          onChange={(value) => void handleSwitchVehicle(value)}
                        />
                        <div className="rounded-xl border g-border-panel overflow-hidden" style={{ height: 240 }}>
                          <TiandituMap center={mapCenter} zoom={12} markers={trackMarkers} polylines={trackLines} />
                        </div>
                        <Alert
                          type="info"
                          showIcon
                          message="轨迹与停留点来自移动端车辆监管接口"
                          description={`当前车辆：${currentVehicle?.plateNo || '-'}，轨迹点数：${trackHistory?.pointCount || 0}`}
                        />
                      </div>
                    ),
                  },
                ]}
              />
            </div>
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <div className="space-y-6">
            <Row gutter={[16, 16]}>
              <Col xs={12} md={6}>
                <Card className="glass-panel g-border-panel border">
                  <Statistic title="可访问场地" value={sites.length} />
                </Card>
              </Col>
              <Col xs={12} md={6}>
                <Card className="glass-panel g-border-panel border">
                  <Statistic title="移动项目" value={projects.length} />
                </Card>
              </Col>
              <Col xs={12} md={6}>
                <Card className="glass-panel g-border-panel border">
                  <Statistic title="已留痕事件" value={events.length} />
                </Card>
              </Col>
              <Col xs={12} md={6}>
                <Card className="glass-panel g-border-panel border">
                  <Statistic title="车辆在线数" value={vehicles.length} />
                </Card>
              </Col>
            </Row>

            <Card title="移动端当前身份" className="glass-panel g-border-panel border">
              {profile ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <div className="text-sm g-text-secondary">账号</div>
                    <div className="font-semibold">{profile.username || '-'}</div>
                  </div>
                  <div>
                    <div className="text-sm g-text-secondary">姓名</div>
                    <div className="font-semibold">{profile.name || '-'}</div>
                  </div>
                  <div>
                    <div className="text-sm g-text-secondary">组织</div>
                    <div className="font-semibold">{currentOrg?.orgName || profile.orgName || '-'}</div>
                  </div>
                  <div>
                    <div className="text-sm g-text-secondary">绑定状态</div>
                    <div>
                      <Tag color={profile.bindStatus === 'BOUND' ? 'success' : 'default'}>
                        {profile.bindStatus || '未返回'}
                      </Tag>
                      <Text type="secondary">{profile.openId || authForm.getFieldValue('openId') || '-'}</Text>
                    </div>
                  </div>
                </div>
              ) : (
                <Empty description="请先完成移动端登录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>

            <Card title="可访问场地与项目" className="glass-panel g-border-panel border">
              <Row gutter={[16, 16]}>
                <Col xs={24} md={11}>
                  <Title level={5}>场地权限</Title>
                  <List
                    dataSource={sites}
                    locale={{ emptyText: <Empty description="暂无可访问场地" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={resolveSiteName(item)}
                          description={
                            <Space wrap>
                              <Tag color="blue">{item.accountRole || '现场账号'}</Tag>
                              <Tag color={item.manualDisposalEnabled ? 'success' : 'default'}>
                                {item.manualDisposalEnabled ? '支持人工消纳' : '人工消纳关闭'}
                              </Tag>
                            </Space>
                          }
                        />
                      </List.Item>
                    )}
                  />
                </Col>
                <Col xs={24} md={13}>
                  <Title level={5}>项目与合同</Title>
                  <List
                    dataSource={projects}
                    locale={{ emptyText: <Empty description="暂无可访问项目" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={resolveProjectName(item)}
                          description={
                            <Space wrap>
                              <Tag>{item.statusLabel || '运行中'}</Tag>
                              <Text type="secondary">合同 {item.contractCount || 0} 份</Text>
                              <Text type="secondary">场地 {item.siteCount || 0} 个</Text>
                            </Space>
                          }
                        />
                      </List.Item>
                    )}
                  />
                </Col>
              </Row>
            </Card>

            <Row gutter={[16, 16]}>
              <Col xs={24} lg={12}>
                <Card title="最近出土拍照 / 消纳记录" className="glass-panel g-border-panel border">
                  <Title level={5}>出土拍照</Title>
                  <List
                    dataSource={photos.slice(0, 3)}
                    locale={{ emptyText: <Empty description="暂无拍照记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={`${item.plateNo || '未识别车牌'} / ${item.photoType || 'EXCAVATION'}`}
                          description={`${item.projectName || '-'} · ${item.siteName || '-'} · ${item.shootTime || '-'}`}
                        />
                      </List.Item>
                    )}
                  />
                  <Divider />
                  <Title level={5}>人工消纳</Title>
                  <List
                    dataSource={manualDisposals.slice(0, 3)}
                    locale={{ emptyText: <Empty description="暂无人工消纳记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={`${item.contractNo || '-'} / ${item.plateNo || '-'}`}
                          description={`${item.siteName || '-'} · ${item.volume || 0} 方 · ${item.disposalTime || '-'}`}
                        />
                        <Tag color="success">{item.status || 'CONFIRMED'}</Tag>
                      </List.Item>
                    )}
                  />
                </Card>
              </Col>
              <Col xs={24} lg={12}>
                <Card title="最近事件 / 问题反馈" className="glass-panel g-border-panel border">
                  <Title level={5}>事件上报</Title>
                  <List
                    dataSource={events.slice(0, 3)}
                    locale={{ emptyText: <Empty description="暂无事件记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={item.title || '-'}
                          description={`${item.eventNo || '-'} · ${item.siteName || '-'} · ${item.reportTime || '-'}`}
                        />
                        <Tag color={item.status === 'PENDING_AUDIT' ? 'processing' : 'success'}>
                          {item.status || 'PENDING_AUDIT'}
                        </Tag>
                      </List.Item>
                    )}
                  />
                  <Divider />
                  <Title level={5}>问题反馈</Title>
                  <List
                    dataSource={feedbacks.slice(0, 3)}
                    locale={{ emptyText: <Empty description="暂无反馈记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                    renderItem={(item) => (
                      <List.Item>
                        <List.Item.Meta
                          title={item.title || '-'}
                          description={`${item.feedbackType || '-'} · ${item.siteName || '-'} · ${item.createTime || '-'}`}
                        />
                        <Tag>{item.status || 'PENDING'}</Tag>
                      </List.Item>
                    )}
                  />
                </Card>
              </Col>
            </Row>

            <Card title="车辆监管回看" className="glass-panel g-border-panel border">
              {currentVehicle ? (
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={8}>
                    <div className="text-sm g-text-secondary">当前车辆</div>
                    <div className="text-lg font-semibold">{currentVehicle.plateNo || '-'}</div>
                    <div className="mt-3 text-sm g-text-secondary">运行状态</div>
                    <Tag color={currentVehicle.runningStatus === 'MOVING' ? 'success' : 'default'}>
                      {currentVehicle.runningStatus || 'UNKNOWN'}
                    </Tag>
                  </Col>
                  <Col xs={24} md={8}>
                    <div className="text-sm g-text-secondary">驾驶员 / 车队</div>
                    <div className="font-semibold">{currentVehicle.driverName || '-'}</div>
                    <div className="mt-3 text-sm g-text-secondary">车队</div>
                    <div>{currentVehicle.fleetName || '-'}</div>
                  </Col>
                  <Col xs={24} md={8}>
                    <div className="text-sm g-text-secondary">最新定位时间</div>
                    <div className="font-semibold">{currentVehicle.gpsTime || '-'}</div>
                    <div className="mt-3 text-sm g-text-secondary">轨迹点数</div>
                    <div>{trackHistory?.pointCount || 0}</div>
                  </Col>
                </Row>
              ) : (
                <Empty description="暂无车辆轨迹数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>
          </div>
        </Col>
      </Row>
    </motion.div>
  );
};

export default MiniProgramDemo;
