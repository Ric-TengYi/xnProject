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
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CreditCardOutlined, PlusOutlined, SearchOutlined, WalletOutlined } from '@ant-design/icons';
import {
  bindVehicleCard,
  createVehicleCard,
  fetchVehicleCards,
  fetchVehicleCardSummary,
  rechargeVehicleCard,
  unbindVehicleCard,
  updateVehicleCard,
  type VehicleCardRecord,
  type VehicleCardSummaryRecord,
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

type RechargeFormValues = {
  amount: number;
};

type BindFormValues = {
  vehicleId: string;
};

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

const statusColorMap: Record<string, string> = {
  NORMAL: 'success',
  LOW_BALANCE: 'error',
  UNBOUND: 'default',
  DISABLED: 'warning',
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
  const [companyOptions, setCompanyOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [vehicleOptions, setVehicleOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [editorOpen, setEditorOpen] = useState(false);
  const [rechargeOpen, setRechargeOpen] = useState(false);
  const [bindOpen, setBindOpen] = useState(false);
  const [editingCard, setEditingCard] = useState<VehicleCardRecord | null>(null);
  const [currentCard, setCurrentCard] = useState<VehicleCardRecord | null>(null);
  const [form] = Form.useForm<CardFormValues>();
  const [rechargeForm] = Form.useForm<RechargeFormValues>();
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

  useEffect(() => {
    void loadSummary();
  }, [queryParams]);

  useEffect(() => {
    void loadCards();
  }, [pageNo, pageSize, queryParams]);

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [companies, vehiclesPage] = await Promise.all([
          fetchVehicleCompanyCapacity(),
          fetchVehicles({ pageNo: 1, pageSize: 200 }),
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
      } catch (error) {
        console.error(error);
      }
    };

    void loadOptions();
  }, []);

  const handleReset = () => {
    setKeyword('');
    setCardType('all');
    setStatus('all');
    setOrgId(undefined);
    setPageNo(1);
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
      await Promise.all([loadSummary(), loadCards()]);
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
    rechargeForm.setFieldsValue({ amount: 1000 });
    setRechargeOpen(true);
  };

  const handleRecharge = async () => {
    if (!currentCard) {
      return;
    }
    try {
      const values = await rechargeForm.validateFields();
      setSubmitLoading(true);
      await rechargeVehicleCard(currentCard.id, { amount: values.amount });
      message.success('充值成功');
      setRechargeOpen(false);
      rechargeForm.resetFields();
      await Promise.all([loadSummary(), loadCards()]);
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
      await Promise.all([loadSummary(), loadCards()]);
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
        await Promise.all([loadSummary(), loadCards()]);
      },
    });
  };

  const columns: ColumnsType<VehicleCardRecord> = [
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
      render: (_, record) => <Tag color={record.cardType === 'FUEL' ? 'orange' : 'blue'}>{record.cardTypeLabel}</Tag>,
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
      render: (value?: string | null) => value ? <Tag>{value}</Tag> : <span className="g-text-secondary">未绑定</span>,
    },
    {
      title: '当前余额(元)',
      dataIndex: 'balance',
      key: 'balance',
      render: (value: number) => <span className={value < 500 ? 'g-text-error font-semibold' : 'g-text-success font-semibold'}>{value.toFixed(2)}</span>,
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
      render: (_, record) => <Tag color={statusColorMap[record.status] || 'default'}>{record.statusLabel}</Tag>,
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
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => openRecharge(record)}>
            充值
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

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">油电卡管理</h1>
          <p className="g-text-secondary mt-1">对接真实油卡/电卡主数据，支持余额统计、车辆绑定与充值留痕。</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增卡片
        </Button>
      </div>

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
          <Button onClick={handleReset}>重置</Button>
        </div>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={cards}
          loading={loading}
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
        <Form form={form} layout="vertical">
          <Form.Item name="cardNo" label="卡号" rules={[{ required: true, message: '请输入卡号' }]}>
            <Input placeholder="请输入卡号" />
          </Form.Item>
          <Form.Item name="cardType" label="卡类型" rules={[{ required: true, message: '请选择卡类型' }]}>
            <Select
              options={cardTypeOptions.filter((item) => item.value !== 'all')}
              placeholder="请选择卡类型"
            />
          </Form.Item>
          <Form.Item name="providerName" label="发卡方">
            <Input placeholder="如中石化企业车队卡" />
          </Form.Item>
          <Form.Item name="orgId" label="所属单位">
            <Select allowClear options={companyOptions} placeholder="未绑定车辆时可手动指定单位" />
          </Form.Item>
          <Form.Item name="vehicleId" label="绑定车辆">
            <Select allowClear showSearch optionFilterProp="label" options={vehicleOptions} placeholder="可直接绑定平台车辆" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="balance" label="当前余额">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="totalRecharge" label="累计充值">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="totalConsume" label="累计消费">
                <InputNumber className="w-full" min={0} precision={2} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions.filter((item) => item.value !== 'all')} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="补充说明" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="卡片充值"
        open={rechargeOpen}
        onCancel={() => {
          setRechargeOpen(false);
          rechargeForm.resetFields();
        }}
        onOk={() => void handleRecharge()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={rechargeForm} layout="vertical">
          <Form.Item label="当前卡号">
            <Input disabled value={currentCard?.cardNo} />
          </Form.Item>
          <Form.Item label="当前余额">
            <Input disabled value={currentCard ? currentCard.balance.toFixed(2) : ''} />
          </Form.Item>
          <Form.Item name="amount" label="充值金额" rules={[{ required: true, message: '请输入充值金额' }]}>
            <InputNumber className="w-full" min={0.01} precision={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="绑定车辆"
        open={bindOpen}
        onCancel={() => {
          setBindOpen(false);
          bindForm.resetFields();
        }}
        onOk={() => void handleBind()}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={bindForm} layout="vertical">
          <Form.Item label="当前卡号">
            <Input disabled value={currentCard?.cardNo} />
          </Form.Item>
          <Form.Item name="vehicleId" label="选择车辆" rules={[{ required: true, message: '请选择车辆' }]}>
            <Select showSearch optionFilterProp="label" options={vehicleOptions} placeholder="请选择需要绑定的车辆" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default VehiclesCards;
