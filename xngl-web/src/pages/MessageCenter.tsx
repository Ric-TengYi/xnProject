import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, DatePicker, Input, Modal, Select, Space, Statistic, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { BellOutlined, ExportOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import {
  exportMessages,
  fetchMessages,
  fetchMessageSummary,
  markAllMessagesRead,
  markMessageRead,
  type MessageRecord,
} from '../utils/messageApi';

const { RangePicker } = DatePicker;

type RangeValue = [Dayjs | null, Dayjs | null] | null;

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '未读', value: 'UNREAD' },
  { label: '已读', value: 'READ' },
];

const MessageCenter: React.FC = () => {
  const navigate = useNavigate();
  const [records, setRecords] = useState<MessageRecord[]>([]);
  const [summary, setSummary] = useState({ total: 0, unread: 0, read: 0 });
  const [loading, setLoading] = useState(false);
  const [submitLoadingId, setSubmitLoadingId] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string>('all');
  const [range, setRange] = useState<RangeValue>(null);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedRecord, setSelectedRecord] = useState<MessageRecord | null>(null);
  const [exporting, setExporting] = useState(false);
  const [markAllLoading, setMarkAllLoading] = useState(false);

  const buildParams = () => ({
    keyword: keyword.trim() || undefined,
    status: status === 'all' ? undefined : status,
    startTime: range?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endTime: range?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    pageNo,
    pageSize,
  });

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  const loadSummary = async () => {
    try {
      const data = await fetchMessageSummary();
      setSummary(data || { total: 0, unread: 0, read: 0 });
    } catch (error) {
      console.error(error);
      message.error('获取消息汇总失败');
    }
  };

  const loadRecords = async () => {
    setLoading(true);
    try {
      const page = await fetchMessages(buildParams());
      setRecords(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取消息列表失败');
      setRecords([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadSummary();
  }, []);

  useEffect(() => {
    void loadRecords();
  }, [keyword, status, range, pageNo, pageSize]);

  const currentPageUnread = useMemo(
    () => records.filter((item) => item.status === 'UNREAD').length,
    [records],
  );

  const resolveMessageTarget = (record: MessageRecord) => {
    const direct = record.linkUrl?.trim();
    if (direct) {
      if (direct === '/contracts/settlements' && record.bizId) {
        return `/contracts/settlements?settlementId=${record.bizId}`;
      }
      if (direct === '/contracts/transfers' && record.bizId) {
        return `/contracts/transfers?transferId=${record.bizId}`;
      }
      return direct;
    }
    if (!record.bizType || !record.bizId) {
      return null;
    }
    switch (record.bizType) {
      case 'MANUAL_EVENT':
        return `/alerts/events?eventId=${record.bizId}`;
      case 'SECURITY_INSPECTION':
        return `/alerts/security?inspectionId=${record.bizId}`;
      case 'SETTLEMENT':
        return `/contracts/settlements?settlementId=${record.bizId}`;
      case 'CONTRACT_TRANSFER':
        return `/contracts/transfers?transferId=${record.bizId}`;
      case 'CONTRACT':
        return `/contracts/${record.bizId}`;
      default:
        return null;
    }
  };

  const handleRead = async (record: MessageRecord) => {
    try {
      setSubmitLoadingId(record.id);
      await markMessageRead(record.id);
      message.success('消息已标记为已读');
      await Promise.all([loadSummary(), loadRecords()]);
    } catch (error) {
      console.error(error);
      message.error('标记已读失败');
    } finally {
      setSubmitLoadingId(null);
    }
  };

  const handleOpenBusiness = async (record: MessageRecord) => {
    const target = resolveMessageTarget(record);
    if (!target) {
      message.info('当前消息未配置业务跳转链接');
      return;
    }
    try {
      if (record.status !== 'READ') {
        setSubmitLoadingId(record.id);
        await markMessageRead(record.id);
      }
      setSelectedRecord(null);
      await Promise.all([loadSummary(), loadRecords()]);
      navigate(target);
    } catch (error) {
      console.error(error);
      message.error('跳转业务页面失败');
    } finally {
      setSubmitLoadingId(null);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      setMarkAllLoading(true);
      const updated = await markAllMessagesRead();
      message.success(updated > 0 ? `已批量标记 ${updated} 条消息` : '当前没有未读消息');
      await Promise.all([loadSummary(), loadRecords()]);
    } catch (error) {
      console.error(error);
      message.error('批量标记已读失败');
    } finally {
      setMarkAllLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      setExporting(true);
      downloadBlob(await exportMessages(buildParams()), 'messages.csv');
      message.success('消息导出成功');
    } catch (error) {
      console.error(error);
      message.error('消息导出失败');
    } finally {
      setExporting(false);
    }
  };

  const columns: ColumnsType<MessageRecord> = [
    {
      title: '消息标题',
      key: 'title',
      render: (_, record) => (
        <div className="flex flex-col">
          <Button
            type="link"
            className="!px-0 text-left"
            onClick={() => setSelectedRecord(record)}
          >
            {record.title || '-'}
          </Button>
          <span className="g-text-secondary text-sm">{record.content || '-'}</span>
        </div>
      ),
    },
    {
      title: '分类 / 渠道',
      key: 'category',
      render: (_, record) => (
        <Space size={[4, 4]} wrap>
          <Tag color="blue" className="border-none">{record.category || '系统通知'}</Tag>
          <Tag color="default" className="border-none">{record.channel || 'SYSTEM'}</Tag>
        </Space>
      ),
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) => (
        <Space size={[4, 4]} wrap>
          <Tag color={record.status === 'READ' ? 'success' : 'processing'} className="border-none">
            {record.statusLabel || '未读'}
          </Tag>
          <Tag color={record.priority === 'HIGH' ? 'error' : 'default'} className="border-none">
            {record.priorityLabel || '普通'}
          </Tag>
        </Space>
      ),
    },
    {
      title: '发送人',
      dataIndex: 'senderName',
      key: 'senderName',
      render: (value) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '发送时间',
      dataIndex: 'sendTime',
      key: 'sendTime',
      render: (value) => <span className="g-text-secondary font-mono">{value || '-'}</span>,
    },
    {
      title: '已读时间',
      dataIndex: 'readTime',
      key: 'readTime',
      render: (value) => <span className="g-text-secondary font-mono">{value || '-'}</span>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            disabled={record.status === 'READ'}
            loading={submitLoadingId === record.id}
            onClick={() => void handleRead(record)}
          >
            标记已读
          </Button>
          {resolveMessageTarget(record) ? (
            <Button type="link" onClick={() => void handleOpenBusiness(record)}>
              查看业务
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card className="glass-panel g-border-panel border"><Statistic title="消息总数" value={summary.total} prefix={<BellOutlined />} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="未读消息" value={summary.unread} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="已读消息" value={summary.read} /></Card>
        <Card className="glass-panel g-border-panel border"><Statistic title="当前页未读" value={currentPageUnread} /></Card>
      </div>

      <Card className="glass-panel g-border-panel border" styles={{ body: { padding: 0 } }}>
        <div className="p-4 flex flex-wrap justify-between gap-4 g-bg-toolbar border-b g-border-panel">
          <Space wrap>
            <Input
              placeholder="搜索标题/内容/分类/发送人"
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="w-72 bg-white g-border-panel border g-text-primary"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPageNo(1);
              }}
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
          <Space>
            <Button onClick={() => void handleMarkAllRead()} loading={markAllLoading}>
              全部标记已读
            </Button>
            <Button icon={<ExportOutlined />} onClick={() => void handleExport()} loading={exporting}>
              导出
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
          locale={{ emptyText: '暂无消息数据' }}
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
        title={selectedRecord?.title || '消息详情'}
        open={Boolean(selectedRecord)}
        onCancel={() => setSelectedRecord(null)}
        footer={
          selectedRecord?.status !== 'READ'
            ? [
                <Button key="close" onClick={() => setSelectedRecord(null)}>
                  关闭
                </Button>,
                selectedRecord && resolveMessageTarget(selectedRecord) ? (
                  <Button key="jump" onClick={() => void handleOpenBusiness(selectedRecord)}>
                    前往业务
                  </Button>
                ) : null,
                <Button
                  key="read"
                  type="primary"
                  loading={submitLoadingId === selectedRecord?.id}
                  onClick={() => selectedRecord && void handleRead(selectedRecord)}
                >
                  标记已读
                </Button>,
              ]
            : null
        }
      >
        <div className="space-y-3">
          <div>
            <div className="g-text-secondary text-sm">分类 / 渠道</div>
            <div className="mt-1">
              <Space size={[4, 4]} wrap>
                <Tag color="blue" className="border-none">
                  {selectedRecord?.category || '系统通知'}
                </Tag>
                <Tag color="default" className="border-none">
                  {selectedRecord?.channel || 'SYSTEM'}
                </Tag>
                <Tag
                  color={selectedRecord?.status === 'READ' ? 'success' : 'processing'}
                  className="border-none"
                >
                  {selectedRecord?.statusLabel || '未读'}
                </Tag>
              </Space>
            </div>
          </div>
          <div>
            <div className="g-text-secondary text-sm">发送人</div>
            <div className="mt-1 g-text-primary">{selectedRecord?.senderName || '-'}</div>
          </div>
          <div>
            <div className="g-text-secondary text-sm">业务跳转</div>
            <div className="mt-1 g-text-primary">
              {selectedRecord && resolveMessageTarget(selectedRecord)
                ? resolveMessageTarget(selectedRecord)
                : '当前消息未配置跳转链接'}
            </div>
          </div>
          <div>
            <div className="g-text-secondary text-sm">消息内容</div>
            <div className="mt-1 whitespace-pre-wrap g-text-primary">
              {selectedRecord?.content || '-'}
            </div>
          </div>
        </div>
      </Modal>
  );

export default MessageCenter;
