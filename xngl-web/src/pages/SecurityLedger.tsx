import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import {
  createSecurityInspection,
  fetchSecurityInspectionDetail,
  fetchSecurityInspections,
  fetchSecuritySummary,
  rectifySecurityInspection,
  type SecurityInspectionPayload,
  type SecurityInspectionRecord,
  type SecuritySummaryRecord,
} from '../utils/securityApi';
import { fetchProjects } from '../utils/projectApi';
import { fetchSites } from '../utils/siteApi';
import { fetchVehicles } from '../utils/vehicleApi';

const objectTypeOptions = [
  { label: '全部对象', value: 'ALL' },
  { label: '场地', value: 'SITE' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '人员', value: 'PERSON' },
];

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '待整改', value: 'OPEN' },
  { label: '整改中', value: 'RECTIFYING' },
  { label: '已关闭', value: 'CLOSED' },
];

const resultOptions = [
  { label: '全部结果', value: 'ALL' },
  { label: '合格', value: 'PASS' },
  { label: '不合格', value: 'FAIL' },
  { label: '需复查', value: 'RECTIFYING' },
];

const emptySummary: SecuritySummaryRecord = {
  monthInspectionCount: 0,
  issueCount: 0,
  closedIssueCount: 0,
  openInspectionCount: 0,
  failCount: 0,
  passCount: 0,
  rectifyingCount: 0,
  overdueRectifyCount: 0,
  objectTypeBuckets: {},
  dangerLevelBuckets: [],
  hazardCategoryBuckets: [],
};

const SecurityLedger: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [rectifyLoading, setRectifyLoading] = useState(false);
  const [summary, setSummary] = useState<SecuritySummaryRecord>(emptySummary);
  const [records, setRecords] = useState<SecurityInspectionRecord[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<SecurityInspectionRecord | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [rectifyOpen, setRectifyOpen] = useState(false);
  const [rectifyRecord, setRectifyRecord] = useState<SecurityInspectionRecord | null>(null);
  const [projects, setProjects] = useState<any[]>([]);
  const [sites, setSites] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [filters, setFilters] = useState({
    keyword: '',
    objectType: 'ALL',
    status: 'ALL',
    resultLevel: 'ALL',
    checkScene: '',
  });
  const [createForm] = Form.useForm<SecurityInspectionPayload>();
  const [rectifyForm] = Form.useForm();

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
      if (filters.objectType !== 'ALL') params.objectType = filters.objectType;
      if (filters.status !== 'ALL') params.status = filters.status;
      if (filters.resultLevel !== 'ALL') params.resultLevel = filters.resultLevel;
      if (filters.checkScene.trim()) params.checkScene = filters.checkScene.trim();
      const [summaryData, inspectionList] = await Promise.all([
        fetchSecuritySummary(),
        fetchSecurityInspections(params),
      ]);
      setSummary(summaryData);
      setRecords(inspectionList);
    } catch (error) {
      console.error(error);
      message.error('获取安全台账失败');
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
  }, [filters.keyword, filters.objectType, filters.status, filters.resultLevel, filters.checkScene]);

  const openDetail = async (record: SecurityInspectionRecord) => {
    setDetailOpen(true);
    try {
      setDetail(await fetchSecurityInspectionDetail(record.id));
    } catch (error) {
      console.error(error);
      message.error('获取检查详情失败');
    }
  };

  const refreshDetail = async (id: string) => {
    const [detailData] = await Promise.all([fetchSecurityInspectionDetail(id), loadData()]);
    setDetail(detailData);
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      setSubmitLoading(true);
      await createSecurityInspection(values);
      message.success('安全检查记录已新增');
      setCreateOpen(false);
      createForm.resetFields();
      await loadData();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('保存安全检查失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const openRectifyModal = (record: SecurityInspectionRecord) => {
    setRectifyRecord(record);
    rectifyForm.setFieldsValue({
      status: record.status === 'OPEN' ? 'RECTIFYING' : record.status || 'CLOSED',
      resultLevel: record.resultLevel || 'PASS',
      rectifyRemark: record.rectifyRemark || undefined,
      nextCheckTime: record.nextCheckTime || undefined,
    });
    setRectifyOpen(true);
  };

  const handleRectify = async () => {
    if (!rectifyRecord) {
      return;
    }
    try {
      const values = await rectifyForm.validateFields();
      setRectifyLoading(true);
      await rectifySecurityInspection(rectifyRecord.id, values);
      message.success('整改信息已更新');
      setRectifyOpen(false);
      setRectifyRecord(null);
      rectifyForm.resetFields();
      if (detailOpen && detail?.id === rectifyRecord.id) {
        await refreshDetail(rectifyRecord.id);
      } else {
        await loadData();
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('更新整改失败');
    } finally {
      setRectifyLoading(false);
    }
  };

  const columns: ColumnsType<SecurityInspectionRecord> = useMemo(
    () => [
      {
        title: '检查编号',
        dataIndex: 'inspectionNo',
        key: 'inspectionNo',
        render: (value) => <span className="font-mono">{value || '-'}</span>,
      },
      {
        title: '检查信息',
        key: 'info',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <span>{record.title}</span>
            <Space size={6} wrap>
              <Tag color="blue">{record.objectType}</Tag>
              {record.dangerLevel ? <Tag color={record.dangerLevel === 'HIGH' ? 'red' : record.dangerLevel === 'MEDIUM' ? 'orange' : 'green'}>{record.dangerLevel}</Tag> : null}
              <span style={{ color: 'var(--text-secondary)' }}>{record.checkScene || '-'}</span>
              {record.isOverdue ? <Tag color="red">已超期</Tag> : null}
            </Space>
          </div>
        ),
      },
      {
        title: '关联对象',
        key: 'relation',
        render: (_, record) => (
          <div className="flex flex-col gap-1">
            <span>{record.projectName || '-'}</span>
            <span style={{ color: 'var(--text-secondary)' }}>{record.siteName || '-'}</span>
            <span style={{ color: 'var(--text-secondary)' }}>{record.vehicleNo || '-'}</span>
          </div>
        ),
      },
      { title: '问题数', dataIndex: 'issueCount', key: 'issueCount' },
      {
        title: '结果',
        dataIndex: 'resultLevel',
        key: 'resultLevel',
        render: (value) => <Tag color={value === 'FAIL' ? 'red' : value === 'RECTIFYING' ? 'orange' : 'green'}>{value || '-'}</Tag>,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (value) => <Tag color={value === 'CLOSED' ? 'green' : value === 'RECTIFYING' ? 'orange' : 'red'}>{value || '-'}</Tag>,
      },
      {
        title: '操作',
        key: 'action',
        render: (_, record) => (
          <Space>
            <Button type="link" onClick={() => void openDetail(record)}>
              详情
            </Button>
            {record.status !== 'CLOSED' ? (
              <Button type="link" onClick={() => openRectifyModal(record)}>
                整改
              </Button>
            ) : null}
          </Space>
        ),
      },
    ],
    [],
  );

  const bucketEntries = Object.entries(summary.objectTypeBuckets || {});

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">安全台账管理</h1>
          <p className="g-text-secondary mt-1">扩展场地、车辆、人员安全检查字段与台账明细闭环能力</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            createForm.setFieldsValue({
              objectType: 'SITE',
              resultLevel: 'PASS',
              dangerLevel: 'LOW',
              issueCount: 0,
              status: 'CLOSED',
            });
            setCreateOpen(true);
          }}
        >
          新增检查记录
        </Button>
      </div>

      <Card className="glass-panel g-border-panel border">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
          <Input
            allowClear
            placeholder="搜索编号 / 标题 / 检查人 / 描述"
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <Select value={filters.objectType} options={objectTypeOptions} onChange={(value) => setFilters((prev) => ({ ...prev, objectType: value }))} />
          <Select value={filters.status} options={statusOptions} onChange={(value) => setFilters((prev) => ({ ...prev, status: value }))} />
          <Select value={filters.resultLevel} options={resultOptions} onChange={(value) => setFilters((prev) => ({ ...prev, resultLevel: value }))} />
          <Input placeholder="检查场景，如 FIRE" value={filters.checkScene} onChange={(event) => setFilters((prev) => ({ ...prev, checkScene: event.target.value }))} />
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card className="glass-panel g-border-panel border"><Statistic title="本月检查" value={summary.monthInspectionCount} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="隐患问题" value={summary.issueCount} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已整改问题" value={summary.closedIssueCount} valueStyle={{ color: '#16a34a' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="待闭环" value={summary.openInspectionCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="不合格" value={summary.failCount} valueStyle={{ color: '#dc2626' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="合格" value={summary.passCount} valueStyle={{ color: '#16a34a' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="整改中" value={summary.rectifyingCount} valueStyle={{ color: '#f59e0b' }} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="超期整改" value={summary.overdueRectifyCount} valueStyle={{ color: '#ef4444' }} /></Card>
      </div>

      <Card
        className="glass-panel g-border-panel border"
        title="对象分布"
        extra={
          <Space wrap>
            {bucketEntries.map(([key, value]) => <Tag key={key}>{key}: {value}</Tag>)}
            {summary.dangerLevelBuckets.map((item) => <Tag key={`danger-${item.code}`} color={item.code === 'HIGH' ? 'red' : item.code === 'MEDIUM' ? 'orange' : 'green'}>{item.code}: {item.count}</Tag>)}
            {summary.hazardCategoryBuckets.map((item) => <Tag key={`hazard-${item.code}`} color="blue">{item.code}: {item.count}</Tag>)}
          </Space>
        }
      >
        <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={{ pageSize: 10 }} />
      </Card>

      <Drawer
        title="检查详情"
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={640}
        extra={detail && detail.status !== 'CLOSED' ? <Button type="primary" onClick={() => openRectifyModal(detail)}>整改处理</Button> : null}
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="检查编号">{detail?.inspectionNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="检查标题">{detail?.title || '-'}</Descriptions.Item>
          <Descriptions.Item label="对象类型">{detail?.objectType || '-'}</Descriptions.Item>
          <Descriptions.Item label="关联对象ID">{detail?.objectId || '-'}</Descriptions.Item>
          <Descriptions.Item label="项目 / 场地 / 车辆">
            {detail?.projectName || '-'} / {detail?.siteName || '-'} / {detail?.vehicleNo || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查场景 / 类型">{detail?.checkScene || '-'} / {detail?.checkType || '-'}</Descriptions.Item>
          <Descriptions.Item label="隐患类别 / 等级">
            {detail?.hazardCategory || '-'} / {detail?.dangerLevel || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查人">{detail?.inspectorName || '-'} ({detail?.inspectorId || '-'})</Descriptions.Item>
          <Descriptions.Item label="整改责任人">
            {detail?.rectifyOwner || '-'} / {detail?.rectifyOwnerPhone || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="检查结果">
            <Space>
              <Tag color={detail?.resultLevel === 'FAIL' ? 'red' : detail?.resultLevel === 'RECTIFYING' ? 'orange' : 'green'}>{detail?.resultLevel || '-'}</Tag>
              <Tag color={detail?.status === 'CLOSED' ? 'green' : detail?.status === 'RECTIFYING' ? 'orange' : 'red'}>{detail?.status || '-'}</Tag>
              {detail?.isOverdue ? <Tag color="red">已超期</Tag> : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="问题数量">{detail?.issueCount ?? 0}</Descriptions.Item>
          <Descriptions.Item label="检查时间">{detail?.checkTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改截止">{detail?.rectifyDeadline || '-'}</Descriptions.Item>
          <Descriptions.Item label="下次复查">{detail?.nextCheckTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改时间">{detail?.rectifyTime || '-'}</Descriptions.Item>
          <Descriptions.Item label="整改说明">{detail?.rectifyRemark || '-'}</Descriptions.Item>
          <Descriptions.Item label="预估整改费用">{detail?.estimatedCost ?? 0}</Descriptions.Item>
          <Descriptions.Item label="附件">{detail?.attachmentUrls || '-'}</Descriptions.Item>
          <Descriptions.Item label="检查说明">{detail?.description || '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Modal title="新增安全检查" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => void handleCreate()} confirmLoading={submitLoading}>
        <Form form={createForm} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="objectType" label="对象类型" rules={[{ required: true, message: '请选择对象类型' }]}>
              <Select options={objectTypeOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="objectId" label="对象ID">
              <InputNumber className="w-full" min={1} />
            </Form.Item>
          </div>
          <Form.Item name="title" label="检查标题" rules={[{ required: true, message: '请输入检查标题' }]}>
            <Input />
          </Form.Item>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="checkScene" label="检查场景">
              <Input placeholder="如 FIRE / VEHICLE_OPERATION" />
            </Form.Item>
            <Form.Item name="checkType" label="检查类型">
              <Input placeholder="如 DAILY / SPECIAL" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="hazardCategory" label="隐患类别">
              <Input placeholder="如 FIRE / OPERATION / PERSONNEL" />
            </Form.Item>
            <Form.Item name="dangerLevel" label="隐患等级">
              <Select options={[{ label: '高', value: 'HIGH' }, { label: '中', value: 'MEDIUM' }, { label: '低', value: 'LOW' }]} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="resultLevel" label="检查结果">
              <Select options={resultOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="issueCount" label="问题数量">
              <InputNumber className="w-full" min={0} />
            </Form.Item>
            <Form.Item name="checkTime" label="检查时间">
              <Input placeholder="2026-03-20T09:30:00" />
            </Form.Item>
          </div>
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
            <Form.Item name="rectifyOwner" label="整改责任人">
              <Input />
            </Form.Item>
            <Form.Item name="rectifyOwnerPhone" label="责任人电话">
              <Input />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="rectifyDeadline" label="整改截止">
              <Input placeholder="2026-03-21T18:00:00" />
            </Form.Item>
            <Form.Item name="nextCheckTime" label="下次复查">
              <Input placeholder="2026-03-22T09:00:00" />
            </Form.Item>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="estimatedCost" label="预估整改费用">
              <InputNumber className="w-full" min={0} />
            </Form.Item>
            <Form.Item name="attachmentUrls" label="附件地址">
              <Input placeholder="https://..." />
            </Form.Item>
          </div>
          <Form.Item name="description" label="检查说明">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="整改处理" open={rectifyOpen} onCancel={() => setRectifyOpen(false)} onOk={() => void handleRectify()} confirmLoading={rectifyLoading}>
        <Form form={rectifyForm} layout="vertical">
          <Form.Item name="status" label="整改状态" rules={[{ required: true, message: '请选择整改状态' }]}>
            <Select options={statusOptions.filter((item) => item.value !== 'ALL')} />
          </Form.Item>
          <Form.Item name="resultLevel" label="整改结果">
            <Select options={resultOptions.filter((item) => item.value !== 'ALL')} />
          </Form.Item>
          <Form.Item name="nextCheckTime" label="下次复查时间">
            <Input placeholder="2026-03-22T09:00:00" />
          </Form.Item>
          <Form.Item name="rectifyRemark" label="整改说明">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SecurityLedger;
