import React, { useEffect, useMemo, useState } from 'react';
import { Card, DatePicker, Input, Select, Space, Statistic, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { fetchDisposals, type DisposalRecord } from '../utils/disposalApi';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';

const { RangePicker } = DatePicker;

type RangeValue = [Dayjs | null, Dayjs | null] | null;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '正常', value: '正常' },
  { label: '异常', value: '异常' },
];

const DisposalRecords: React.FC = () => {
  const [records, setRecords] = useState<DisposalRecord[]>([]);
  const [projects, setProjects] = useState<ProjectRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [projectId, setProjectId] = useState<string>('all');
  const [siteId, setSiteId] = useState<string>('all');
  const [status, setStatus] = useState<string>('all');
  const [range, setRange] = useState<RangeValue>(null);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

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
        const page = await fetchDisposals({
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
        message.error('获取消纳信息失败');
        setRecords([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };
    void loadData();
  }, [keyword, projectId, siteId, status, range, pageNo, pageSize]);

  const summary = useMemo(
    () => ({
      total,
      normal: records.filter((item) => item.statusLabel === '正常').length,
      abnormal: records.filter((item) => item.statusLabel === '异常').length,
      volume: records.reduce((sum, item) => sum + Number(item.volume || 0), 0),
    }),
    [records, total],
  );

  const columns: ColumnsType<DisposalRecord> = [
    {
      title: '消纳编号',
      dataIndex: 'ticketNo',
      key: 'ticketNo',
      render: (value, record) => <span className="font-mono g-text-secondary">{value || record.id}</span>,
    },
    {
      title: '消纳时间',
      dataIndex: 'disposalTime',
      key: 'disposalTime',
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
      title: '运输单位',
      dataIndex: 'transportOrgName',
      key: 'transportOrgName',
      render: (value) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '方量(方)',
      dataIndex: 'volume',
      key: 'volume',
      render: (value) => <span className="g-text-success font-bold">{Number(value || 0).toFixed(2)}</span>,
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (value) => (
        <Tag color={value === '异常' ? 'error' : 'success'} className="border-none">
          {value || '正常'}
        </Tag>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold g-text-primary m-0">消纳信息</h1>
        <p className="g-text-secondary mt-1">查询全平台消纳记录，支持按项目、场地、状态和时间范围组合检索。</p>
      </div>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="记录总数" value={summary.total} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="正常记录" value={summary.normal} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="异常记录" value={summary.abnormal} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="当前页方量" value={summary.volume.toFixed(2)} /></Card>
      </div>

      <Card className="glass-panel g-border-panel border" styles={{ body: { padding: 0 } }}>
        <div className="p-4 flex flex-wrap justify-between gap-4 g-bg-toolbar border-b g-border-panel">
          <Space wrap>
            <Input
              placeholder="搜索消纳编号/项目/场地/合同/车牌"
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
          locale={{ emptyText: '暂无消纳记录' }}
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
    </div>
  );
};
export default DisposalRecords;
