import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CreditCardOutlined,
  DollarCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import {
  bindVehicleCard,
  consumeVehicleCard,
  createVehicleCard,
  exportVehicleCards,
  exportVehicleCardTransactions,
  fetchVehicleCards,
  fetchVehicleCardSummary,
  fetchVehicleCardTransactions,
  fetchVehicleCardTransactionSummary,
  rechargeVehicleCard,
  unbindVehicleCard,
  updateVehicleCard,
  type VehicleCardRecord,
  type VehicleCardSummaryRecord,
  type VehicleCardTransactionRecord,
  type VehicleCardTransactionSummaryRecord,
  type VehicleCardUpsertPayload,
} from '../utils/vehicleCardApi';
import { fetchVehicleCompanyCapacity, fetchVehicles } from '../utils/vehicleApi';

type CardFormValues = {
  cardNo: string;
  cardType: string;
  providerName?: string;
  orgId?: string;
  vehicleId?: string;
  balance?: number;
  totalRecharge?: number;
  totalConsume?: number;
  status?: string;
  remark?: string;
};

type AmountFormValues = {
  amount: number;
  remark?: string;
};

type BindFormValues = {
  vehicleId: string;
};

const { RangePicker } = DatePicker;

const cardTypeOptions = [
  { label: '全部类型', value: 'all' },
  { label: '油卡', value: 'FUEL' },
  { label: '电卡', value: 'ELECTRIC' },
];

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '正常', value: 'NORMAL' },
  { label: '余额不足', value: 'LOW_BALANCE' },
  { label: '未绑定', value: 'UNBOUND' },
  { label: '停用', value: 'DISABLED' },
];

const transactionTypeOptions = [
  { label: '全部流水', value: 'all' },
  { label: '充值', value: 'RECHARGE' },
  { label: '消费', value: 'CONSUME' },
];

const statusColorMap: Record<string, string> = {
  NORMAL: 'success',
  LOW_BALANCE: 'error',
  UNBOUND: 'default',
  DISABLED: 'warning',
};

const transactionColorMap: Record<string, string> = {
  RECHARGE: 'success',
  CONSUME: 'processing',
};

const defaultSummary: VehicleCardSummaryRecord = {
  totalCards: 0,
  fuelCards: 0,
  electricCards: 0,
  boundCards: 0,
  lowBalanceCards: 0,
  totalBalance: 0,
  fuelBalance: 0,
  electricBalance: 0,
};

const defaultTransactionSummary: VehicleCardTransactionSummaryRecord = {
  totalTransactions: 0,
  rechargeTransactions: 0,
  consumeTransactions: 0,
  totalRechargeAmount: 0,
  totalConsumeAmount: 0,
};

const VehiclesCards: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [cards, setCards] = useState<VehicleCardRecord[]>([]);
  const [summary, setSummary] = useState<VehicleCardSummaryRecord>(defaultSummary);
  const [keyword, setKeyword] = useState('');
  const [cardType, setCardType] = useState('all');
  const [status, setStatus] = useState('all');
  const [orgId, setOrgId] = useState<string | undefined>(undefined);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [transactionLoading, setTransactionLoading] = useState(false);
  const [transactionSummaryLoading, setTransactionSummaryLoading] = useState(false);
  const [transactionRows, setTransactionRows] = useState<VehicleCardTransactionRecord[]>([]);
  const [transactionSummary, setTransactionSummary] =
    useState<VehicleCardTransactionSummaryRecord>(defaultTransactionSummary);
  const [transactionKeyword, setTransactionKeyword] = useState('');
  const [transactionCardType, setTransactionCardType] = useState('all');
  const [transactionType, setTransactionType] = useState('all');
  const [transactionOrgId, setTransactionOrgId] = useState<string | undefined>(undefined);
  const [transactionVehicleId, setTransactionVehicleId] = useState<string | undefined>(undefined);
  const [transactionCardId, setTransactionCardId] = useState<string | undefined>(undefined);
  const [transactionRange, setTransactionRange] = useState<[Dayjs | null, Dayjs | null] | null>(
    null
  );
  const [transactionPageNo, setTransactionPageNo] = useState(1);
  const [transactionPageSize, setTransactionPageSize] = useState(10);
  const [transactionTotal, setTransactionTotal] = useState(0);

  const [companyOptions, setCompanyOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [vehicleOptions, setVehicleOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [cardOptions, setCardOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [editorOpen, setEditorOpen] = useState(false);
  const [rechargeOpen, setRechargeOpen] = useState(false);
  const [consumeOpen, setConsumeOpen] = useState(false);
  const [bindOpen, setBindOpen] = useState(false);
  const [editingCard, setEditingCard] = useState<VehicleCardRecord | null>(null);
  const [currentCard, setCurrentCard] = useState<VehicleCardRecord | null>(null);
  const [form] = Form.useForm<CardFormValues>();
  const [rechargeForm] = Form.useForm<AmountFormValues>();
  const [consumeForm] = Form.useForm<AmountFormValues>();
  const [bindForm] = Form.useForm<BindFormValues>();

  const queryParams = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      cardType: cardType === 'all' ? undefined : cardType,
      status: status === 'all' ? undefined : status,
      orgId,
    }),
    [cardType, keyword, orgId, status]
  );

  const transactionQueryParams = useMemo(
    () => ({
      keyword: transactionKeyword.trim() || undefined,
      cardType: transactionCardType === 'all' ? undefined : transactionCardType,
      txnType: transactionType === 'all' ? undefined : transactionType,
      orgId: transactionOrgId,
      vehicleId: transactionVehicleId,
      cardId: transactionCardId,
      dateFrom: transactionRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: transactionRange?.[1]?.format('YYYY-MM-DD'),
    }),
    [
      transactionCardId,
      transactionCardType,
      transactionKeyword,
      transactionOrgId,
      transactionRange,
      transactionType,
      transactionVehicleId,
    ]
  );

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  const loadCards = async () => {
    setLoading(true);
    try {
      const page = await fetchVehicleCards({
        ...queryParams,
        pageNo,
        pageSize,
      });
      setCards(page.records || []);
      setTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取油电卡列表失败');
      setCards([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    setSummaryLoading(true);
    try {
      setSummary(await fetchVehicleCardSummary(queryParams));
    } catch (error) {
      console.error(error);
      message.error('获取油电卡统计失败');
      setSummary(defaultSummary);
    } finally {
      setSummaryLoading(false);
    }
  };

  const loadTransactions = async () => {
    setTransactionLoading(true);
    try {
      const page = await fetchVehicleCardTransactions({
        ...transactionQueryParams,
        pageNo: transactionPageNo,
        pageSize: transactionPageSize,
      });
      setTransactionRows(page.records || []);
      setTransactionTotal(page.total || 0);
    } catch (error) {
      console.error(error);
      message.error('获取油电卡流水失败');
      setTransactionRows([]);
      setTransactionTotal(0);
    } finally {
      setTransactionLoading(false);
    }
  };

  const loadTransactionSummary = async () => {
    setTransactionSummaryLoading(true);
    try {
      setTransactionSummary(await fetchVehicleCardTransactionSummary(transactionQueryParams));
    } catch (error) {
      console.error(error);
      message.error('获取油电卡流水统计失败');
      setTransactionSummary(defaultTransactionSummary);
    } finally {
      setTransactionSummaryLoading(false);
    }
  };

  useEffect(() => {
    void loadSummary();
  }, [queryParams]);

  useEffect(() => {
    void loadCards();
  }, [pageNo, pageSize, queryParams]);

  useEffect(() => {
    void loadTransactionSummary();
  }, [transactionQueryParams]);

  useEffect(() => {
    void loadTransactions();
  }, [transactionPageNo, transactionPageSize, transactionQueryParams]);

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [companies, vehiclesPage, cardsPage] = await Promise.all([
          fetchVehicleCompanyCapacity(),
          fetchVehicles({ pageNo: 1, pageSize: 200 }),
          fetchVehicleCards({ pageNo: 1, pageSize: 200 }),
        ]);
        setCompanyOptions(
          companies
            .filter((item) => item.orgId)
            .map((item) => ({ label: item.orgName, value: item.orgId as string }))
        );
        setVehicleOptions(
          (vehiclesPage.records || []).map((item) => ({
            label: `${item.plateNo} / ${item.orgName || '未归属单位'}`,
            value: item.id,
          }))
        );
        setCardOptions(
          (cardsPage.records || []).map((item) => ({
            label: `${item.cardNo}${item.plateNo ? ` / ${item.plateNo}` : ''}`,
            value: item.id,
          }))
        );
      } catch (error) {
        console.error(error);
      }
    };

    void loadOptions();
  }, []);

  const reloadAll = async () => {
    await Promise.all([
      loadSummary(),
      loadCards(),
      loadTransactionSummary(),
      loadTransactions(),
    ]);
  };

  const handleReset = () => {
    setKeyword('');
    setCardType('all');
    setStatus('all');
    setOrgId(undefined);
    setPageNo(1);
  };

  const handleResetTransactions = () => {
    setTransactionKeyword('');
    setTransactionCardType('all');
    setTransactionType('all');
    setTransactionOrgId(undefined);
    setTransactionVehicleId(undefined);
    setTransactionCardId(undefined);
    setTransactionRange(null);
    setTransactionPageNo(1);
  };

  const handleExportCards = async () => {
    try {
      downloadBlob(await exportVehicleCards(queryParams), 'vehicle_cards.csv');
      message.success('油电卡台账导出成功');
    } catch (error) {
      console.error(error);
      message.error('油电卡台账导出失败');
    }
  };

  const handleExportTransactions = async () => {
    try {
      downloadBlob(await exportVehicleCardTransactions(transactionQueryParams), 'vehicle_card_transactions.csv');
      message.success('油电卡流水导出成功');
    } catch (error) {
      console.error(error);
      message.error('油电卡流水导出失败');
    }
  };

  const openCreate = () => {
    setEditingCard(null);
    form.resetFields();
    form.setFieldsValue({
      cardType: 'FUEL',
      status: 'NORMAL',
      balance: 0,
      totalRecharge: 0,
      totalConsume: 0,
    });
    setEditorOpen(true);
  };

  const openEdit = (record: VehicleCardRecord) => {
    setEditingCard(record);
    form.setFieldsValue({
      cardNo: record.cardNo,
      cardType: record.cardType,
      providerName: record.providerName || undefined,
      orgId: record.orgId || undefined,
      vehicleId: record.vehicleId || undefined,
      balance: record.balance,
      totalRecharge: record.totalRecharge,
      totalConsume: record.totalConsume,
      status: record.status,
      remark: record.remark || undefined,
    });
    setEditorOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: VehicleCardUpsertPayload = {
        cardNo: values.cardNo,
        cardType: values.cardType,
        providerName: values.providerName,
        orgId: values.orgId ? Number(values.orgId) : undefined,
        vehicleId: values.vehicleId ? Number(values.vehicleId) : undefined,
        balance: values.balance,
        totalRecharge: values.totalRecharge,
        totalConsume: values.totalConsume,
        status: values.status,
        remark: values.remark,
      };
      setSubmitLoading(true);
      if (editingCard) {
        await updateVehicleCard(editingCard.id, payload);
        message.success('油电卡已更新');
      } else {
        await createVehicleCard(payload);
        message.success('油电卡已新增');
      }
      setEditorOpen(false);
      form.resetFields();
      setPageNo(1);
      await reloadAll();
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const openRecharge = (record: VehicleCardRecord) => {
    setCurrentCard(record);
    rechargeForm.setFieldsValue({ amount: 1000, remark: undefined });
    setRechargeOpen(true);
  };

  const openConsume = (record: VehicleCardRecord) => {
    setCurrentCard(record);
    consumeForm.setFieldsValue({ amount: 100, remark: undefined });
    setConsumeOpen(true);
  };

  const handleRecharge = async () => {
    if (!currentCard) {
      return;
    }
    try {
      const values = await rechargeForm.validateFields();
      setSubmitLoading(true);
      await rechargeVehicleCard(currentCard.id, values);
      message.success('充值成功');
      setRechargeOpen(false);
      rechargeForm.resetFields();
      await reloadAll();
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleConsume = async () => {
    if (!currentCard) {
      return;
    }
    try {
      const values = await consumeForm.validateFields();
      setSubmitLoading(true);
      await consumeVehicleCard(currentCard.id, values);
      message.success('消费确认成功');
      setConsumeOpen(false);
      consumeForm.resetFields();
      await reloadAll();
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const openBind = (record: VehicleCardRecord) => {
    setCurrentCard(record);
    bindForm.setFieldsValue({ vehicleId: record.vehicleId || undefined });
    setBindOpen(true);
  };

  const handleBind = async () => {
    if (!currentCard) {
      return;
    }
    try {
      const values = await bindForm.validateFields();
      setSubmitLoading(true);
      await bindVehicleCard(currentCard.id, { vehicleId: Number(values.vehicleId) });
      message.success('绑定成功');
      setBindOpen(false);
      bindForm.resetFields();
      await reloadAll();
    } catch (error) {
      if (error instanceof Error && error.message) {
        console.error(error);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleUnbind = (record: VehicleCardRecord) => {
    Modal.confirm({
      title: `确认解绑 ${record.cardNo} ?`,
      content: record.plateNo ? `当前绑定车辆：${record.plateNo}` : '将解除当前车辆绑定关系。',
      okText: '确认解绑',
      cancelText: '取消',
      onOk: async () => {
        await unbindVehicleCard(record.id);
        message.success('解绑成功');
        await reloadAll();
      },
    });
  };

  const cardColumns: ColumnsType<VehicleCardRecord> = [
    {
      title: '卡号',
      dataIndex: 'cardNo',
      key: 'cardNo',
      render: (value: string) => <span className="font-mono g-text-secondary">{value}</span>,
    },
    {
      title: '卡类型',
      dataIndex: 'cardTypeLabel',
      key: 'cardTypeLabel',
      render: (_, record) => (
        <Tag color={record.cardType === 'FUEL' ? 'orange' : 'blue'}>{record.cardTypeLabel}</Tag>
      ),
    },
    {
      title: '所属单位',
      dataIndex: 'orgName',
      key: 'orgName',
      render: (value?: string | null) => value || '未归属单位',
    },
    {
      title: '绑定车辆',
      dataIndex: 'plateNo',
      key: 'plateNo',
      render: (value?: string | null) =>
        value ? <Tag>{value}</Tag> : <span className="g-text-secondary">未绑定</span>,
    },
    {
      title: '当前余额(元)',
      dataIndex: 'balance',
      key: 'balance',
      render: (value: number) => (
        <span className={value < 500 ? 'g-text-error font-semibold' : 'g-text-success font-semibold'}>
          {value.toFixed(2)}
        </span>
      ),
    },
    {
      title: '累计充值(元)',
      dataIndex: 'totalRecharge',
      key: 'totalRecharge',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '累计消费(元)',
      dataIndex: 'totalConsume',
      key: 'totalConsume',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      key: 'statusLabel',
      render: (_, record) => (
        <Tag color={statusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => openRecharge(record)}>
            充值
          </Button>
          <Button type="link" size="small" onClick={() => openConsume(record)}>
            消费
          </Button>
          {record.vehicleId ? (
            <Button type="link" size="small" danger onClick={() => handleUnbind(record)}>
              解绑
            </Button>
          ) : (
            <Button type="link" size="small" onClick={() => openBind(record)}>
              绑定
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const transactionColumns: ColumnsType<VehicleCardTransactionRecord> = [
    {
      title: '卡片 / 流水类型',
      key: 'card',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span className="font-semibold g-text-primary">{record.cardNo || '-'}</span>
          <span className="text-xs g-text-secondary">{record.cardTypeLabel || '-'}</span>
        </Space>
      ),
    },
    {
      title: '所属单位 / 车辆',
      key: 'orgVehicle',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span>{record.orgName || '未归属单位'}</span>
          <span className="text-xs g-text-secondary">{record.plateNo || '未绑定车辆'}</span>
        </Space>
      ),
    },
    {
      title: '流水类型',
      dataIndex: 'txnTypeLabel',
      key: 'txnTypeLabel',
      render: (_, record) => (
        <Tag color={transactionColorMap[record.txnType] || 'default'}>
          {record.txnTypeLabel || '未知'}
        </Tag>
      ),
    },
    {
      title: '金额(元)',
      dataIndex: 'amount',
      key: 'amount',
      render: (value: number, record) => (
        <span className={record.txnType === 'CONSUME' ? 'g-text-error' : 'g-text-success'}>
          {value.toFixed(2)}
        </span>
      ),
    },
    {
      title: '变动前余额',
      dataIndex: 'balanceBefore',
      key: 'balanceBefore',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '变动后余额',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      render: (value: number) => value.toFixed(2),
    },
    {
      title: '操作时间',
      dataIndex: 'occurredAt',
      key: 'occurredAt',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operatorName',
      key: 'operatorName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string | null) => value || '-',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">油电卡管理</h1>
          <p className="g-text-secondary mt-1">
            对接真实油卡/电卡主数据，支持余额统计、充值消费确认与流水台账。
          </p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增卡片
        </Button>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="卡片总数" value={summary.totalCards} prefix={<CreditCardOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="已绑定卡片" value={summary.boundCards} prefix={<CreditCardOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="油卡余额(元)" value={summary.fuelBalance} precision={2} prefix={<WalletOutlined />} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card loading={summaryLoading} className="glass-panel g-border-panel border">
            <Statistic title="电卡余额(元)" value={summary.electricBalance} precision={2} prefix={<WalletOutlined />} />
          </Card>
        </Col>
      </Row>

      <Tabs
        items={[
          {
            key: 'cards',
            label: '卡片台账',
            children: (
              <Card className="glass-panel g-border-panel border">
                <div className="flex flex-wrap justify-between gap-4 mb-4">
                  <Space wrap>
                    <Input
                      allowClear
                      placeholder="搜索卡号/车辆/单位/发卡方"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      value={keyword}
                      onChange={(event) => {
                        setKeyword(event.target.value);
                        setPageNo(1);
                      }}
                      className="w-72"
                    />
                    <Select
                      value={cardType}
                      options={cardTypeOptions}
                      onChange={(value) => {
                        setCardType(value);
                        setPageNo(1);
                      }}
                      className="w-36"
                    />
                    <Select
                      value={status}
                      options={statusOptions}
                      onChange={(value) => {
                        setStatus(value);
                        setPageNo(1);
                      }}
                      className="w-36"
                    />
                    <Select
                      allowClear
                      value={orgId}
                      options={companyOptions}
                      placeholder="所属单位"
                      onChange={(value) => {
                        setOrgId(value);
                        setPageNo(1);
                      }}
                      className="w-56"
                    />
                  </Space>
                  <Space>
                    <Button onClick={handleReset}>重置</Button>
                    <Button onClick={() => void handleExportCards()}>导出台账</Button>
                  </Space>
                </div>

                <Table
                  rowKey="id"
                  columns={cardColumns}
                  dataSource={cards}
                  loading={loading}
                  scroll={{ x: 1280 }}
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
            ),
          },
          {
            key: 'transactions',
            label: '流水记录',
            children: (
              <Space direction="vertical" size={16} className="w-full">
                <Row gutter={[24, 24]}>
                  <Col xs={24} md={6}>
                    <Card loading={transactionSummaryLoading} className="glass-panel g-border-panel border">
                      <Statistic
                        title="流水总数"
                        value={transactionSummary.totalTransactions}
                        prefix={<DollarCircleOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card loading={transactionSummaryLoading} className="glass-panel g-border-panel border">
                      <Statistic
                        title="充值金额(元)"
                        value={transactionSummary.totalRechargeAmount}
                        precision={2}
                        prefix={<WalletOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card loading={transactionSummaryLoading} className="glass-panel g-border-panel border">
                      <Statistic
                        title="消费金额(元)"
                        value={transactionSummary.totalConsumeAmount}
                        precision={2}
                        prefix={<WalletOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card loading={transactionSummaryLoading} className="glass-panel g-border-panel border">
                      <Statistic
                        title="充值/消费笔数"
                        value={`${transactionSummary.rechargeTransactions} / ${transactionSummary.consumeTransactions}`}
                      />
                    </Card>
                  </Col>
                </Row>

                <Card className="glass-panel g-border-panel border">
                  <div className="flex flex-wrap justify-between gap-4 mb-4">
                    <Space wrap>
                      <Input
                        allowClear
                        placeholder="搜索卡号/单位/车辆/操作人"
                        prefix={<SearchOutlined className="g-text-secondary" />}
                        value={transactionKeyword}
                        onChange={(event) => {
                          setTransactionKeyword(event.target.value);
                          setTransactionPageNo(1);
                        }}
                        className="w-72"
                      />
                      <Select
                        value={transactionCardType}
                        options={cardTypeOptions}
                        onChange={(value) => {
                          setTransactionCardType(value);
                          setTransactionPageNo(1);
                        }}
                        className="w-36"
                      />
                      <Select
                        value={transactionType}
                        options={transactionTypeOptions}
                        onChange={(value) => {
                          setTransactionType(value);
                          setTransactionPageNo(1);
                        }}
                        className="w-36"
                      />
                      <Select
                        allowClear
                        value={transactionOrgId}
                        options={companyOptions}
                        placeholder="所属单位"
                        onChange={(value) => {
                          setTransactionOrgId(value);
                          setTransactionPageNo(1);
                        }}
                        className="w-56"
                      />
                      <Select
                        allowClear
                        value={transactionVehicleId}
                        options={vehicleOptions}
                        placeholder="绑定车辆"
                        showSearch
                        optionFilterProp="label"
                        onChange={(value) => {
                          setTransactionVehicleId(value);
                          setTransactionPageNo(1);
                        }}
                        className="w-64"
                      />
                      <Select
                        allowClear
                        value={transactionCardId}
                        options={cardOptions}
                        placeholder="指定卡片"
                        showSearch
                        optionFilterProp="label"
                        onChange={(value) => {
                          setTransactionCardId(value);
                          setTransactionPageNo(1);
                        }}
                        className="w-64"
                      />
                      <RangePicker
                        value={transactionRange}
                        onChange={(value) => {
                          setTransactionRange(value);
                          setTransactionPageNo(1);
                        }}
                      />
                    </Space>
                    <Space>
                      <Button onClick={handleResetTransactions}>重置</Button>
                      <Button onClick={() => void handleExportTransactions()}>导出流水</Button>
                    </Space>
                  </div>

                  <Table
                    rowKey="id"
                    columns={transactionColumns}
                    dataSource={transactionRows}
                    loading={transactionLoading}
                    scroll={{ x: 1400 }}
                    pagination={{
                      current: transactionPageNo,
                      pageSize: transactionPageSize,
                      total: transactionTotal,
                      showSizeChanger: true,
                      onChange: (nextPage, nextPageSize) => {
                        setTransactionPageNo(nextPage);
                        setTransactionPageSize(nextPageSize);
                      },
                    }}
                  />
                </Card>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={editingCard ? '编辑油电卡' : '新增油电卡'}
        open={editorOpen}
        onCancel={() => {
          setEditorOpen(false);
          form.resetFields();
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={form}>
          <Form.Item name="cardNo" label="卡号" rules={[{ required: true, message: '请输入卡号' }]}>
            <Input placeholder="请输入卡号" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="cardType" label="卡类型" rules={[{ required: true, message: '请选择卡类型' }]}>
                <Select options={cardTypeOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select options={statusOptions.filter((item) => item.value !== 'all')} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="providerName" label="发卡方">
                <Input placeholder="请输入发卡方" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgId" label="所属单位">
                <Select options={companyOptions} allowClear showSearch optionFilterProp="label" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="vehicleId" label="绑定车辆">
            <Select options={vehicleOptions} allowClear showSearch optionFilterProp="label" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="balance" label="当前余额(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="totalRecharge" label="累计充值(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="totalConsume" label="累计消费(元)">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={currentCard ? `充值 ${currentCard.cardNo}` : '充值'}
        open={rechargeOpen}
        onCancel={() => {
          setRechargeOpen(false);
          rechargeForm.resetFields();
        }}
        onOk={() => void handleRecharge()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={rechargeForm}>
          <Form.Item name="amount" label="充值金额(元)" rules={[{ required: true, message: '请输入充值金额' }]}>
            <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入充值说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={currentCard ? `消费确认 ${currentCard.cardNo}` : '消费确认'}
        open={consumeOpen}
        onCancel={() => {
          setConsumeOpen(false);
          consumeForm.resetFields();
        }}
        onOk={() => void handleConsume()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={consumeForm}>
          <Form.Item name="amount" label="消费金额(元)" rules={[{ required: true, message: '请输入消费金额' }]}>
            <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入消费说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={currentCard ? `绑定车辆 ${currentCard.cardNo}` : '绑定车辆'}
        open={bindOpen}
        onCancel={() => {
          setBindOpen(false);
          bindForm.resetFields();
        }}
        onOk={() => void handleBind()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form layout="vertical" form={bindForm}>
          <Form.Item name="vehicleId" label="车辆" rules={[{ required: true, message: '请选择车辆' }]}>
            <Select options={vehicleOptions} showSearch optionFilterProp="label" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
    </div>
  );
};

export default VehiclesCards;
