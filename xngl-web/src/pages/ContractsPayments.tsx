import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Input, Select, DatePicker, Tag, Space, Modal, Form, InputNumber, message } from 'antd';
import { SearchOutlined, PlusOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { createContractReceipt, listContractReceipts, type ContractReceipt } from '../utils/contractReceipts';

const { RangePicker } = DatePicker;
const { Option } = Select;

const ContractsPayments: React.FC = () => {
    const navigate = useNavigate();
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [receipts, setReceipts] = useState<ContractReceipt[]>([]);
    const [searchText, setSearchText] = useState('');
    const [statusFilter, setStatusFilter] = useState('all');
    const [dateRange, setDateRange] = useState<[string | null, string | null] | null>(null);
    const [form] = Form.useForm();

    const loadReceipts = async () => {
        setLoading(true);
        try {
            const data = await listContractReceipts();
            setReceipts(data);
        } catch (error) {
            console.error(error);
            message.error('获取合同入账记录失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadReceipts();
    }, []);

    const filteredReceipts = useMemo(() => {
        return receipts.filter((record) => {
            const keyword = searchText.trim().toLowerCase();
            const hitKeyword =
                !keyword ||
                String(record.contractId ?? '').toLowerCase().includes(keyword) ||
                String(record.voucherNo ?? '').toLowerCase().includes(keyword) ||
                String(record.remark ?? '').toLowerCase().includes(keyword);
            const hitStatus = statusFilter === 'all' || String(record.status ?? '').toUpperCase() === statusFilter;
            const hitDate =
                !dateRange ||
                !dateRange[0] ||
                !dateRange[1] ||
                (record.receiptDate &&
                    dayjs(record.receiptDate).isValid() &&
                    !dayjs(record.receiptDate).isBefore(dayjs(dateRange[0]), 'day') &&
                    !dayjs(record.receiptDate).isAfter(dayjs(dateRange[1]), 'day'));
            return hitKeyword && hitStatus && hitDate;
        });
    }, [dateRange, receipts, searchText, statusFilter]);

    const renderStatus = (status?: string) => {
        const normalized = String(status ?? 'UNKNOWN').toUpperCase();
        if (normalized === 'NORMAL') {
            return <Tag color="green">正常</Tag>;
        }
        if (normalized === 'CANCELLED') {
            return <Tag color="red">已冲销</Tag>;
        }
        return <Tag>{status || '未知'}</Tag>;
    };

    const columns = [
        { title: '入账记录ID', dataIndex: 'id', key: 'id', render: (text: string | number) => <span className="font-mono g-text-secondary">{text}</span> },
        { title: '合同ID', dataIndex: 'contractId', key: 'contractId', render: (text: string | number | undefined) => <span className="g-text-primary-link">{text || '-'}</span> },
        { title: '入账金额 (元)', dataIndex: 'amount', key: 'amount', render: (val?: number) => <span className="g-text-success">{Number(val ?? 0).toLocaleString()}</span> },
        { title: '入账日期', dataIndex: 'receiptDate', key: 'receiptDate', render: (text?: string) => text || '-' },
        { title: '凭证号/流水号', dataIndex: 'voucherNo', key: 'voucherNo', render: (text?: string) => text || '-' },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, render: (text?: string) => text || '-' },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: renderStatus,
        },
        {
            title: '操作',
            key: 'action',
            render: (_: unknown, record: ContractReceipt) => (
                <Space size="middle">
                    <Button
                        type="link"
                        size="small"
                        icon={<EyeOutlined />}
                        disabled={!record.contractId}
                        onClick={() => navigate(`/contracts/${record.contractId}?tab=payment`)}
                    >
                        查看合同
                    </Button>
                </Space>
            ),
        },
    ];

    const handleOk = async () => {
        try {
            const values = await form.validateFields();
            setSubmitting(true);
            await createContractReceipt({
                contractId: Number(values.contractId),
                amount: Number(values.amount),
                receiptDate: values.date.format('YYYY-MM-DD'),
                voucherNo: values.receipt,
                remark: values.remark,
                status: 'NORMAL',
            });
            message.success('入账记录已提交');
            setIsModalVisible(false);
            form.resetFields();
            await loadReceipts();
        } catch (error) {
            if (error instanceof Error) {
                message.error(error.message || '提交入账记录失败');
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold g-text-primary m-0">合同入账管理</h1>
            </div>

            <Card className="glass-panel g-border-panel border">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input
                            placeholder="搜索合同ID/凭证号/备注"
                            prefix={<SearchOutlined />}
                            className="bg-white g-border-panel border g-text-primary w-64"
                            value={searchText}
                            onChange={(event) => setSearchText(event.target.value)}
                        />
                        <Select value={statusFilter} className="w-32 bg-white" onChange={setStatusFilter}>
                            <Option value="all">全部状态</Option>
                            <Option value="NORMAL">正常</Option>
                            <Option value="CANCELLED">已冲销</Option>
                        </Select>
                        <RangePicker
                            className="bg-white g-border-panel border"
                            onChange={(values) =>
                                setDateRange(
                                    values
                                        ? [values[0]?.format('YYYY-MM-DD') ?? null, values[1]?.format('YYYY-MM-DD') ?? null]
                                        : null,
                                )
                            }
                        />
                        <Button type="primary" onClick={() => void loadReceipts()}>刷新</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalVisible(true)}>
                        新增入账
                    </Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={filteredReceipts}
                    rowKey={(record) => String(record.id)}
                    loading={loading}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Modal
                title="入账登记"
                open={isModalVisible}
                onOk={handleOk}
                confirmLoading={submitting}
                onCancel={() => { setIsModalVisible(false); form.resetFields(); }}
                className="dark-modal"
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="contractId"
                        label="合同ID"
                        rules={[{ required: true, message: '请输入合同ID' }]}
                    >
                        <Input placeholder="请输入后端合同ID" />
                    </Form.Item>
                    <Form.Item
                        name="amount"
                        label="本次入账金额 (元)"
                        rules={[{ required: true, message: '请输入入账金额' }]}
                    >
                        <InputNumber className="w-full" placeholder="请输入金额" min={0.01} />
                    </Form.Item>
                    <Form.Item
                        name="date"
                        label="入账日期"
                        rules={[{ required: true, message: '请选择入账日期' }]}
                    >
                        <DatePicker className="w-full" />
                    </Form.Item>
                    <Form.Item name="receipt" label="凭证号/流水号">
                        <Input placeholder="请输入银行流水号" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注说明">
                        <Input.TextArea rows={3} placeholder="请输入备注信息" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default ContractsPayments;
