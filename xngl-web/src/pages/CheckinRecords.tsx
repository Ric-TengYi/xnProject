import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ExclamationCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { fetchCheckins, voidCheckin, type CheckinRecord } from '../utils/checkinApi';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';

const { RangePicker } = DatePicker;

type RangeValue = [Dayjs | null, Dayjs | null] | null;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '正常', value: '正常' },
  { label: '已作废', value: '已作废' },
];

const CheckinRecords: React.FC = () => {
  const [records, setRecords] = useState<CheckinRecord[]>([]);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [projectId, setProjectId] = useState<string>('all');
  const [siteId, setSiteId] = useState<string>('all');
  const [status, setStatus] = useState<string>('all');
  const [range, setRange] = useState<RangeValue>(null);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [voidOpen, setVoidOpen] = useState(false);
  const [activeRecord, setActiveRecord] = useState<CheckinRecord | null>(null);
  const [voidForm] = Form.useForm<{ reason: string }>();

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [projectPage, siteRows] = await Promise.all([
          fetchProjects({ pageNo: 1, pageSize: 200 }),
          fetchSites(),
        ]);
        setProjects(projectPage.records || []);
        setSites(siteRows || []);
      } catch (error) {
        console.error(error);
        message.error('获取筛选项失败');
      }
    };
    void loadOptions();
  }, []);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const page = await fetchCheckins({
          keyword: keyword.trim() || undefined,
          projectId: projectId === 'all' ? undefined : projectId,
          siteId: siteId === 'all' ? undefined : siteId,
          status: status === 'all' ? undefined : status,
          startDate: range?.[0]?.format('YYYY-MM-DD'),
          endDate: range?.[1]?.format('YYYY-MM-DD'),
          pageNo,
          pageSize,
        });
        setRecords(page.records || []);
        setTotal(page.total || 0);
      } catch (error) {
        console.error(error);
        message.error('获取打卡数据失败');
        setRecords([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };
    void loadData();
  }, [keyword, projectId, siteId, status, range, pageNo, pageSize]);

  const summary = useMemo(() => ({
    total: total,
    normal: records.filter((item) => item.statusLabel === '正常').length,
    cancelled: records.filter((item) => item.statusLabel === '已作废').length,
    volume: records.reduce((sum, item) => sum + Number(item.volume || 0), 0),
  }), [records, total]);

  const openVoid = (record: CheckinRecord) => {
    setActiveRecord(record);
    voidForm.resetFields();
    setVoidOpen(true);
  };

  const handleVoid = async () => {
    try {
      const values = await voidForm.validateFields();
      if (!activeRecord) {
        return;
      }
      setSubmitLoading(true);
      await voidCheckin(activeRecord.id, values.reason);
      message.success('打卡记录已作废');
      setVoidOpen(false);
      setActiveRecord(null);
      voidForm.resetFields();
      setPageNo(1);
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('作废打卡记录失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const columns: ColumnsType<CheckinRecord> = [
    {
      title: '打卡编号',
      dataIndex: 'ticketNo',
      key: 'ticketNo',
      render: (value, record) => <span className="font-mono g-text-secondary">{value || record.id}</span>,
    },
    {
      title: '打卡时间',
      dataIndex: 'punchTime',
      key: 'punchTime',
    },
    {
      title: '项目 / 场地',
      key: 'projectSite',
      render: (_, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary">{record.projectName || '-'}</span>
          <span className="g-text-secondary">{record.siteName || '-'}</span>
        </div>
      ),
    },
    {
      title: '合同 / 来源',
      key: 'contract',
      render: (_, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary">{record.contractNo || record.contractName || '-'}</span>
          <span className="g-text-secondary">{record.sourceType || '-'}</span>
        </div>
      ),
    },
    {
      title: '车辆 / 司机',
      key: 'vehicle',
      render: (_, record) => (
        <div className="flex flex-col">
          <span className="g-text-primary">{record.plateNo || '-'}</span>
          <span className="g-text-secondary">{record.driverName || '-'}</span>
        </div>
      ),
    },
    {
      title: '方量(方)',
      dataIndex: 'volume',
      key: 'volume',
      render: (value) => <span className="g-text-success font-bold">{Number(value || 0).toFixed(2)}</span>,
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) => (
        <Space size={[4, 4]} wrap>
          <Tag color={record.statusLabel === '已作废' ? 'error' : 'success'} className="border-none">
            {record.statusLabel || '正常'}
          </Tag>
          <Tag color={record.exceptionType === '异常打卡' ? 'orange' : 'blue'} className="border-none">
            {record.exceptionType || '正常打卡'}
          </Tag>
        </Space>
      ),
    },
    {
      title: '异常说明',
      dataIndex: 'voidReason',
      key: 'voidReason',
      render: (value) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="link"
          danger
          disabled={record.statusLabel === '已作废'}
          icon={<ExclamationCircleOutlined />}
          onClick={() => openVoid(record)}
        >
          作废
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold g-text-primary m-0">打卡数据</h1>
        <p className="g-text-secondary mt-1">查询项目打卡记录，支持多条件组合筛选和异常打卡在线作废。</p>
      </div>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="记录总数" value={summary.total} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="正常记录" value={summary.normal} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已作废" value={summary.cancelled} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="当前页方量" value={summary.volume.toFixed(2)} /></Card>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 flex flex-wrap justify-between gap-4 g-bg-toolbar border-b g-border-panel">
          <Space wrap>
            <Input
              placeholder="搜索打卡编号/项目/合同/车牌/司机"
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="w-72 bg-white g-border-panel border g-text-primary"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPageNo(1);
              }}
            />
            <Select
              value={projectId}
              className="w-48"
              onChange={(value) => {
                setProjectId(value);
                setPageNo(1);
              }}
              options={[{ label: '全部项目', value: 'all' }, ...projects.map((item) => ({ label: item.name, value: item.id }))]}
            />
            <Select
              value={siteId}
              className="w-48"
              onChange={(value) => {
                setSiteId(value);
                setPageNo(1);
              }}
              options={[{ label: '全部场地', value: 'all' }, ...sites.map((item) => ({ label: item.name, value: item.id }))]}
            />
            <Select
              value={status}
              className="w-36"
              onChange={(value) => {
                setStatus(value);
                setPageNo(1);
              }}
              options={statusOptions}
            />
            <RangePicker
              value={range}
              onChange={(value) => {
                setRange(value as RangeValue);
                setPageNo(1);
              }}
            />
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
          locale={{ emptyText: '暂无打卡数据' }}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPageNo(nextPage);
              setPageSize(nextPageSize);
            },
          }}
        />
      </Card>

      <Modal
        title="作废打卡记录"
        open={voidOpen}
        onCancel={() => {
          setVoidOpen(false);
          setActiveRecord(null);
          voidForm.resetFields();
        }}
        onOk={() => void handleVoid()}
        confirmLoading={submitLoading}
      >
        <p className="g-text-secondary">记录编号：{activeRecord?.ticketNo || activeRecord?.id || '-'}</p>
        <Form form={voidForm} layout="vertical">
          <Form.Item name="reason" label="作废原因" rules={[{ required: true, message: '请输入作废原因' }]}> 
            <Input.TextArea rows={4} placeholder="请输入异常打卡作废原因" />
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default CheckinRecords;
