import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CheckCircleOutlined, CloseCircleOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  approveManualEvent,
  closeManualEvent,
  createManualEvent,
  exportManualEvents,
  fetchManualEventDetail,
  fetchManualEvents,
  fetchManualEventSummary,
  rejectManualEvent,
  submitManualEvent,
  updateManualEvent,
  type ManualEventDetailRecord,
  type ManualEventPayload,
  type ManualEventRecord,
  type ManualEventSummaryRecord,
} from '../utils/manualEventApi';
import { fetchProjects } from '../utils/projectApi';
import { fetchSites } from '../utils/siteApi';
import { fetchVehicles } from '../utils/vehicleApi';
import type { Dayjs } from 'dayjs';
import { useSearchParams } from 'react-router-dom';

const { RangePicker } = DatePicker;

const eventTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '延期申报', value: 'DELAY' },
  { label: '场地事件', value: 'SITE' },
  { label: '违规举报', value: 'REPORT' },
  { label: '其他', value: 'OTHER' },
];

const sourceOptions = [
  { label: '全部来源', value: 'ALL' },
  { label: 'WEB', value: 'WEB' },
  { label: 'APP', value: 'APP' },
  { label: 'MINI_PROGRAM', value: 'MINI_PROGRAM' },
  { label: 'MANUAL', value: 'MANUAL' },
];

const priorityOptions = [
  { label: '全部优先级', value: 'ALL' },
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_AUDIT' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已退回', value: 'REJECTED' },
  { label: '已关闭', value: 'CLOSED' },
];

const overdueOptions = [
  { label: '全部时效', value: 'ALL' },
  { label: '仅超期', value: 'Y' },
];

const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  PENDING_AUDIT: 'warning',
  PROCESSING: 'processing',
  REJECTED: 'error',
  CLOSED: 'success',
};

const emptySummary: ManualEventSummaryRecord = {
  total: 0,
  draftCount: 0,
  pendingAuditCount: 0,
  processingCount: 0,
  rejectedCount: 0,
  closedCount: 0,
  highPriorityCount: 0,
  overdueCount: 0,
  todayCount: 0,
  typeBuckets: [],
  sourceBuckets: [],
};

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
};

const formatRangeParams = (
  params: Record<string, string>,
  keyFrom: string,
  keyTo: string,
  range: [Dayjs, Dayjs] | null,
) => {
  if (!range) {
    return;
  }
  params[keyFrom] = range[0].format('YYYY-MM-DDTHH:mm:ss');
  params[keyTo] = range[1].format('YYYY-MM-DDTHH:mm:ss');
};

const parseAttachmentUrls = (value?: string | null) =>
  (value || '')
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean);

const EventsManagement: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [summary, setSummary] = useState<ManualEventSummaryRecord>(emptySummary);
  const [records, setRecords] = useState<ManualEventRecord[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<ManualEventDetailRecord | null>(null);
  const [queryHandledId, setQueryHandledId] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ManualEventRecord | null>(null);
  const [projects, setProjects] = useState<any[]>([]);
  const [sites, setSites] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [filters, setFilters] = useState({
    keyword: '',
    eventType: 'ALL',
    status: 'ALL',
    priority: 'ALL',
    sourceChannel: 'ALL',
    projectId: 'ALL',
    overdueOnly: 'ALL',
  });
  const [reportRange, setReportRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [deadlineRange, setDeadlineRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [form] = Form.useForm<ManualEventPayload>();

  const loadMasters = async () => {
    const [projectPage, siteList, vehiclePage] = await Promise.all([
      fetchProjects({ pageNo: 1, pageSize: 200 }),
      fetchSites(),
      fetchVehicles({ pageNo: 1, pageSize: 200 }),
    ]);
    setProjects(projectPage.records || []);
    setSites(siteList || []);
    setVehicles(vehiclePage.records || []);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (filters.keyword.trim()) params.keyword = filters.keyword.trim();
      if (filters.eventType !== 'ALL') params.eventType = filters.eventType;
      if (filters.status !== 'ALL') params.status = filters.status;
      if (filters.priority !== 'ALL') params.priority = filters.priority;
      if (filters.sourceChannel !== 'ALL') params.sourceChannel = filters.sourceChannel;
      if (filters.projectId !== 'ALL') params.projectId = filters.projectId;
      if (filters.overdueOnly === 'Y') params.overdueOnly = 'true';
      formatRangeParams(params, 'reportTimeFrom', 'reportTimeTo', reportRange);
      formatRangeParams(params, 'deadlineTimeFrom', 'deadlineTimeTo', deadlineRange);
      const [summaryData, listData] = await Promise.all([
        fetchManualEventSummary(params),
        fetchManualEvents(params),
      ]);
      setSummary(summaryData);
      setRecords(listData);
    } catch (error) {
      console.error(error);
      message.error('获取事件管理数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadMasters().catch((error) => {
      console.error(error);
      message.error('加载基础数据失败');
    });
  }, []);

  useEffect(() => {
    void loadData();
  }, [
    filters.keyword,
    filters.eventType,
    filters.status,
    filters.priority,
    filters.sourceChannel,
    filters.projectId,
    filters.overdueOnly,
    reportRange,
    deadlineRange,
  ]);

  useEffect(() => {
    const eventId = searchParams.get('eventId');
    if (!eventId || queryHandledId === eventId) {
      return;
    }
    setQueryHandledId(eventId);
    setDetailOpen(true);
    void fetchManualEventDetail(eventId)
      .then((data) => setDetail(data))
      .catch((error) => {
        console.error(error);
        message.error('获取事件详情失败');
      });
  }, [queryHandledId, searchParams]);

  const openCreateModal = () => {
    setEditingRecord(null);
    form.setFieldsValue({
      eventType: 'DELAY',
      priority: 'MEDIUM',
      sourceChannel: 'WEB',
      contactPhone: '13800001111',
      status: 'DRAFT',
      currentAuditNode: 'MANUAL_EVENT_AUDIT',
    });
    setModalOpen(true);
  };

  const openEditModal = (record: ManualEventRecord) => {
    setEditingRecord(record);
    form.setFieldsValue({
      eventType: record.eventType,
      title: record.title,
      content: record.content || undefined,
      sourceChannel: record.sourceChannel || 'WEB',
      reportAddress: record.reportAddress || undefined,
      projectId: record.projectId ? Number(record.projectId) : undefined,
      siteId: record.siteId ? Number(record.siteId) : undefined,
      vehicleId: record.vehicleId ? Number(record.vehicleId) : undefined,
      contactPhone: record.contactPhone || undefined,
      priority: record.priority || 'MEDIUM',
      status: record.status || 'DRAFT',
      currentAuditNode: record.currentAuditNode || 'MANUAL_EVENT_AUDIT',
      occurTime: record.occurTime || undefined,
      deadlineTime: record.deadlineTime || undefined,
      attachmentUrls: record.attachmentUrls || undefined,
      assigneeName: record.assigneeName || undefined,
      assigneePhone: record.assigneePhone || undefined,
      dispatchRemark: record.dispatchRemark || undefined,
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingRecord(null);
    form.resetFields();
  };

  const openDetail = async (record: ManualEventRecord) => {
    setDetailOpen(true);
    try {
      setDetail(await fetchManualEventDetail(record.id));
    } catch (error) {
      console.error(error);
      message.error('获取事件详情失败');
    }
  };

  const refreshDetail = async (id: string) => {
    const [detailData] = await Promise.all([fetchManualEventDetail(id), loadData()]);
    setDetail(detailData);
  };

  const saveEvent = async (submitAfterSave: boolean) => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      const payload: ManualEventPayload = {
        ...values,
        status: values.status || 'DRAFT',
        currentAuditNode: values.currentAuditNode || 'MANUAL_EVENT_AUDIT',
        sourceChannel: values.sourceChannel || 'WEB',
        priority: values.priority || 'MEDIUM',
      };
      const saved = editingRecord
        ? await updateManualEvent(editingRecord.id, payload)
        : await createManualEvent(payload);
      if (submitAfterSave) {
        await submitManualEvent(saved.record.id);
      }
      message.success(submitAfterSave ? '事件已保存并提交审核' : editingRecord ? '事件已更新' : '事件草稿已保存');
      closeModal();
      await loadData();
      if (detailOpen && detail?.record.id === saved.record.id) {
        setDetail(await fetchManualEventDetail(saved.record.id));
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error(submitAfterSave ? '提交事件失败' : '保存事件失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const performDetailAction = async (action: () => Promise<void>, successText: string) => {
    if (!detail) {
      return;
    }
    try {
      await action();
      message.success(successText);
      await refreshDetail(detail.record.id);
    } catch (error) {
      console.error(error);
      message.error(successText.replace('成功', '失败'));
    }
  };

  const performRowAction = async (id: string, action: () => Promise<void>, successText: string) => {
    try {
      await action();
      message.success(successText);
      await loadData();
      if (detailOpen && detail?.record.id === id) {
        setDetail(await fetchManualEventDetail(id));
      }
    } catch (error) {
      console.error(error);
      message.error(successText.replace('成功', '失败'));
    }
  };

  const buildParams = () => {
    const params: Record<string, string> = {};
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim();
    if (filters.eventType !== 'ALL') params.eventType = filters.eventType;
    if (filters.status !== 'ALL') params.status = filters.status;
    if (filters.priority !== 'ALL') params.priority = filters.priority;
    if (filters.sourceChannel !== 'ALL') params.sourceChannel = filters.sourceChannel;
    if (filters.projectId !== 'ALL') params.projectId = filters.projectId;
    if (filters.overdueOnly === 'Y') params.overdueOnly = 'true';
    formatRangeParams(params, 'reportTimeFrom', 'reportTimeTo', reportRange);
    formatRangeParams(params, 'deadlineTimeFrom', 'deadlineTimeTo', deadlineRange);
    return params;
  };

  const handleExport = async () => {
    try {
      downloadBlob(await exportManualEvents(buildParams()), 'manual_events.csv');
      message.success('事件导出成功');
    } catch (error) {
      console.error(error);
      message.error('事件导出失败');
    }
  };

  const columns: ColumnsType<ManualEventRecord> = useMemo(
    () => [
      {
        title: '事件编号',
        dataIndex: 'eventNo',
        key: 'eventNo',
        render: (value) => <span className="font-mono">{value || '-'}</span>,
      },
      {
        title: '事件信息',
        key: 'info',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <span>{record.title}</span>
            <Space size={6} wrap>
              <Tag color="blue">{record.eventType}</Tag>
              {record.priority ? <Tag color={record.priority === 'HIGH' ? 'red' : record.priority === 'MEDIUM' ? 'orange' : 'default'}>{record.priority}</Tag> : null}
              {record.isOverdue ? <Tag color="red">超期</Tag> : null}
              <span style={{ color: 'var(--text-secondary)' }}>{record.sourceChannel || '-'}</span>
            </Space>
          </div>
        ),
      },
      {
        title: '项目 / 场地 / 车辆',
        key: 'relation',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <span>{record.projectName || '-'}</span>
            <span style={{ color: 'var(--text-secondary)' }}>{record.siteName || '-'}</span>
            <span style={{ color: 'var(--text-secondary)' }}>{record.vehicleNo || '-'}</span>
          </div>
        ),
      },
      {
        title: '当前节点',
        dataIndex: 'currentAuditNode',
        key: 'currentAuditNode',
        render: (value) => value || '-',
      },
      {
        title: '最近处理',
        key: 'audit',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <span>{record.lastAuditAction || '-'}</span>
            <span style={{ color: 'var(--text-secondary)' }}>{record.lastAuditTime || '-'}</span>
          </div>
        ),
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (value) => <Tag color={statusColorMap[value || ''] || 'default'}>{value || '-'}</Tag>,
      },
      {
        title: '操作',
        key: 'action',
        render: (_, record) => (
          <Space>
            <Button type="link" onClick={() => void openDetail(record)}>
              {record.status === 'PENDING_AUDIT' ? '审核' : '详情'}
            </Button>
            {(record.status === 'DRAFT' || record.status === 'REJECTED') ? (
              <Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>
                编辑
              </Button>
            ) : null}
            {(record.status === 'DRAFT' || record.status === 'REJECTED') ? (
              <Button type="link" onClick={() => void performRowAction(record.id, () => submitManualEvent(record.id), '事件已提交审核')}>
                提交
              </Button>
            ) : null}
          </Space>
        ),
      },
    ],
    [detail],
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">事件管理</h1>
          <p className="g-text-secondary mt-1">支持人工事件上报、审核流转、退回修改和闭环归档</p>
        </div>
        <Space>
          <Button onClick={() => void handleExport()}>
            导出
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            新建事件
          </Button>
        </Space>
      </div>
      <Card className="glass-panel g-border-panel border">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4 xl:grid-cols-8">
          <Input
            allowClear
            placeholder="搜索编号 / 标题 / 申报人 / 内容"
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <Select value={filters.eventType} options={eventTypeOptions} onChange={(value) => setFilters((prev) => ({ ...prev, eventType: value }))} />
          <Select value={filters.status} options={statusOptions} onChange={(value) => setFilters((prev) => ({ ...prev, status: value }))} />
          <Select value={filters.priority} options={priorityOptions} onChange={(value) => setFilters((prev) => ({ ...prev, priority: value }))} />
          <Select value={filters.sourceChannel} options={sourceOptions} onChange={(value) => setFilters((prev) => ({ ...prev, sourceChannel: value }))} />
          <Select
            value={filters.projectId}
            options={[{ label: '全部项目', value: 'ALL' }, ...projects.map((item) => ({ label: item.name, value: String(item.id) }))]}
            onChange={(value) => setFilters((prev) => ({ ...prev, projectId: value }))}
          />
          <Select value={filters.overdueOnly} options={overdueOptions} onChange={(value) => setFilters((prev) => ({ ...prev, overdueOnly: value }))} />
          <RangePicker
            showTime
            value={reportRange}
            onChange={(value) => setReportRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['上报开始', '上报结束']}
          />
          <RangePicker
            showTime
            value={deadlineRange}
            onChange={(value) => setDeadlineRange((value as [Dayjs, Dayjs] | null) || null)}
            placeholder={['截止开始', '截止结束']}
          />
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card className="glass-panel g-border-panel border"><Statistic title="事件总数" value={summary.total} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="今日上报" value={summary.todayCount} valueStyle={{ color: '#1677ff' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="待审核" value={summary.pendingAuditCount} valueStyle={{ color: '#faad14' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="处理中" value={summary.processingCount} valueStyle={{ color: '#13c2c2' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已退回" value={summary.rejectedCount} valueStyle={{ color: '#ff4d4f' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已关闭" value={summary.closedCount} valueStyle={{ color: '#52c41a' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="高优先级" value={summary.highPriorityCount} valueStyle={{ color: '#cf1322' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="超期待办" value={summary.overdueCount} valueStyle={{ color: '#d4380d' }} /></Card>

      <Card
        className="glass-panel g-border-panel border"
        title="事件类型与来源分布"
        extra={
          <Space wrap>
            {summary.typeBuckets.map((item) => (
              <Tag key={`type-${item.code}`}>{item.code}: {item.count}</Tag>
            ))}
            {summary.sourceBuckets.map((item) => (
              <Tag key={`source-${item.code}`} color="blue">{item.code}: {item.count}</Tag>
            ))}
          </Space>
        }
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={{ pageSize: 10 }} />
      </Card>

      <Drawer
        title="事件详情"
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={640}
        extra={
          detail?.record.status === 'PENDING_AUDIT' ? (
            <Space>
              <Button
                icon={<CloseCircleOutlined />}
                danger
                onClick={() => void performDetailAction(() => rejectManualEvent(detail.record.id, '退回补充说明'), '事件已退回')}
              >
                退回
              </Button>
              <Button
                icon={<CheckCircleOutlined />}
                type="primary"
                onClick={() => void performDetailAction(() => approveManualEvent(detail.record.id, '审核通过'), '事件审核成功')}
              >
                通过
              </Button>
            </Space>
          ) : detail?.record.status === 'PROCESSING' ? (
            <Button type="primary" onClick={() => void performDetailAction(() => closeManualEvent(detail.record.id, '已完成处置'), '事件关闭成功')}>
              关闭事件
            </Button>
          ) : detail?.record.status === 'DRAFT' || detail?.record.status === 'REJECTED' ? (
            <Space>
              <Button onClick={() => detail && openEditModal(detail.record)}>编辑</Button>
              <Button type="primary" onClick={() => detail && void performDetailAction(() => submitManualEvent(detail.record.id), '事件已提交审核')}>
                提交审核
              </Button>
            </Space>
          ) : null
        }
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="事件编号">{detail?.record.eventNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="事件类型">{detail?.record.eventType || '-'}</Descriptions.Item>
          <Descriptions.Item label="优先级">{detail?.record.priority || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Space>
              <Tag color={statusColorMap[detail?.record.status || ''] || 'default'}>{detail?.record.status || '-'}</Tag>
              <span>{detail?.record.currentAuditNode || '-'}</span>
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="项目 / 场地 / 车辆">
            {detail?.record.projectName || '-'} / {detail?.record.siteName || '-'} / {detail?.record.vehicleNo || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="来源渠道">{detail?.record.sourceChannel || '-'}</Descriptions.Item>
          <Descriptions.Item label="申报人">{detail?.record.reporterName || '-'} ({detail?.record.reporterId || '-'})</Descriptions.Item>
          <Descriptions.Item label="联系电话">{detail?.record.contactPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="上报地点">{detail?.record.reportAddress || '-'}</Descriptions.Item>
          <Descriptions.Item label="发生时间">{detail?.record.occurTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="要求完成时间">
            <Space>
              <span>{detail?.record.deadlineTime || '-'}</span>
              {detail?.record.isOverdue ? <Tag color="red">已超期</Tag> : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="上报时间">{detail?.record.reportTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="处置责任人">
            {detail?.record.assigneeName || '-'} / {detail?.record.assigneePhone || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="派单说明">{detail?.record.dispatchRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="关闭时间">{detail?.record.closeTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="关闭说明">{detail?.record.closeRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="附件">
            <Space wrap>
              {parseAttachmentUrls(detail?.record.attachmentUrls).length
                ? parseAttachmentUrls(detail?.record.attachmentUrls).map((item) => (
                    <a key={item} href={item} target="_blank" rel="noreferrer">
                      {item}
                    </a>
                  ))
                : '-'}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="审核次数">{detail?.record.auditCount ?? 0}</Descriptions.Item>
          <Descriptions.Item label="最近审核动作">{detail?.record.lastAuditAction || '-'} / {detail?.record.lastAuditTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="内容">{detail?.record.content || '-'}</Descriptions.Item>
        </Descriptions>

        <div className="mt-6">
          <h3 className="g-text-primary">审核记录</h3>
          <Timeline
            items={(detail?.auditLogs || []).map((item) => ({
              color: item.resultStatus === 'REJECTED' ? 'red' : item.resultStatus === 'CLOSED' ? 'green' : 'blue',
              children: (
                <div>
                  <div>{item.action || '-'} / {item.resultStatus || '-'}</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{item.auditorName || '-'} · {item.auditTime || '-'}</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{item.comment || '-'}</div>
                </div>
              ),
            }))}
          />
        </div>
      </Drawer>

      <Modal
        title={editingRecord ? '编辑事件' : '新建事件'}
        open={modalOpen}
        onCancel={closeModal}
        confirmLoading={submitLoading}
        footer={[
          <Button key="cancel" onClick={closeModal}>取消</Button>,
          <Button key="draft" loading={submitLoading} onClick={() => void saveEvent(false)}>
            {editingRecord ? '保存修改' : '保存草稿'}
          </Button>,
          <Button key="submit" type="primary" loading={submitLoading} onClick={() => void saveEvent(true)}>
            保存并提交
          </Button>,
        ]}
      >
        <Form form={form} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="eventType" label="事件类型" rules={[{ required: true, message: '请选择事件类型' }]}>
              <Select options={eventTypeOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="priority" label="优先级">
              <Select options={priorityOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
          </div>
          <Form.Item name="title" label="事件标题" rules={[{ required: true, message: '请输入事件标题' }]}>
            <Input />
          </Form.Item>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="sourceChannel" label="来源渠道">
              <Select options={sourceOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="occurTime" label="发生时间">
              <Input placeholder="2026-03-20T08:30:00" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="contactPhone" label="联系电话">
              <Input placeholder="13800001111" />
            </Form.Item>
            <Form.Item name="deadlineTime" label="要求完成时间">
              <Input placeholder="2026-03-20T18:00:00" />
            </Form.Item>
          </div>
          <Form.Item name="reportAddress" label="上报地点">
            <Input placeholder="请输入事件发生地点" />
          </Form.Item>
          <div className="grid grid-cols-3 gap-4">
            <Form.Item name="projectId" label="关联项目">
              <Select allowClear options={projects.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="siteId" label="关联场地">
              <Select allowClear options={sites.map((item) => ({ label: item.name, value: Number(item.id) }))} />
            </Form.Item>
            <Form.Item name="vehicleId" label="关联车辆">
              <Select allowClear options={vehicles.map((item) => ({ label: item.plateNo, value: Number(item.id) }))} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="assigneeName" label="处置责任人">
              <Input />
            </Form.Item>
            <Form.Item name="assigneePhone" label="责任人电话">
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="dispatchRemark" label="派单说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="attachmentUrls" label="附件地址">
            <Input.TextArea rows={2} placeholder="多个附件可用逗号分隔" />
          </Form.Item>
          <Form.Item name="content" label="事件描述">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EventsManagement;
