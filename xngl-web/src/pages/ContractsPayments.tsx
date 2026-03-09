import React, { useState } from 'react';
import { Card, Table, Button, Input, Select, DatePicker, Tag, Space, Modal, Form, InputNumber, Upload } from 'antd';
import { SearchOutlined, PlusOutlined, UploadOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { RangePicker } = DatePicker;
const { Option } = Select;

const paymentsData = [
    { id: 'HT-2024-001', project: '市中心地铁延长线三期工程', site: '东区临时消纳场', totalAmount: 5000000, paidAmount: 3000000, unPaidAmount: 2000000, lastDate: '2024-03-01', status: '执行中' },
    { id: 'HT-2024-002', project: '滨海新区基础建设B标段', site: '南郊复合型消纳中心', totalAmount: 2000000, paidAmount: 2000000, unPaidAmount: 0, lastDate: '2024-02-15', status: '已结清' },
    { id: 'HT-2024-003', project: '老旧小区改造工程综合包', site: '西郊临时周转站', totalAmount: 800000, paidAmount: 500000, unPaidAmount: 300000, lastDate: '2024-02-28', status: '执行中' },
];

const ContractsPayments: React.FC = () => {
    const navigate = useNavigate();
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [currentContract, setCurrentContract] = useState<any>(null);
    const [form] = Form.useForm();

    const columns = [
        { title: '合同编号', dataIndex: 'id', key: 'id', render: (text: string) => <span className="font-mono g-text-secondary">{text}</span> },
        { title: '关联项目', dataIndex: 'project', key: 'project', render: (text: string) => <span className="g-text-primary-link">{text}</span> },
        { title: '关联场地', dataIndex: 'site', key: 'site' },
        { title: '合同总金额 (元)', dataIndex: 'totalAmount', key: 'totalAmount', render: (val: number) => val.toLocaleString() },
        { title: '已入账 (元)', dataIndex: 'paidAmount', key: 'paidAmount', render: (val: number) => <span className="g-text-success">{val.toLocaleString()}</span> },
        { title: '待入账 (元)', dataIndex: 'unPaidAmount', key: 'unPaidAmount', render: (val: number) => <span className={val > 0 ? 'g-text-error' : 'g-text-secondary'}>{val.toLocaleString()}</span> },
        { title: '最近入账时间', dataIndex: 'lastDate', key: 'lastDate' },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '已结清' ? 'green' : 'blue'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <Button type="link" size="small" onClick={() => { setCurrentContract(record); setIsModalVisible(true); }}>入账登记</Button>
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => navigate(`/contracts/${record.id}?tab=payment`)}>查看合同</Button>
                </Space>
            ),
        },
    ];

    const handleOk = () => {
        form.validateFields().then(values => {
            console.log('Received values of form: ', values);
            setIsModalVisible(false);
            form.resetFields();
        });
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold g-text-primary m-0">合同入账管理</h1>
            </div>

            <Card className="glass-panel g-border-panel border">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input placeholder="搜索合同编号/项目" prefix={<SearchOutlined />} className="bg-white g-border-panel border g-text-primary w-64" />
                        <Select defaultValue="all" className="w-32 bg-white">
                            <Option value="all">全部状态</Option>
                            <Option value="执行中">执行中</Option>
                            <Option value="已结清">已结清</Option>
                        </Select>
                        <RangePicker className="bg-white g-border-panel border" />
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />}>批量入账导入</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={paymentsData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Modal
                title="入账登记"
                open={isModalVisible}
                onOk={handleOk}
                onCancel={() => { setIsModalVisible(false); form.resetFields(); }}
                className="dark-modal"
            >
                <Form form={form} layout="vertical">
                    <Form.Item label="合同编号">
                        <Input disabled value={currentContract?.id} className="bg-white g-text-secondary" />
                    </Form.Item>
                    <Form.Item label="待入账金额">
                        <Input disabled value={`¥ ${currentContract?.unPaidAmount?.toLocaleString()}`} className="bg-white g-text-secondary" />
                    </Form.Item>
                    <Form.Item name="amount" label="本次入账金额 (元)" rules={[{ required: true }]}>
                        <InputNumber className="w-full" placeholder="请输入金额" min={0.01} />
                    </Form.Item>
                    <Form.Item name="date" label="入账日期" rules={[{ required: true }]}>
                        <DatePicker className="w-full" />
                    </Form.Item>
                    <Form.Item name="receipt" label="凭证号/流水号">
                        <Input placeholder="请输入银行流水号" />
                    </Form.Item>
                    <Form.Item name="attachment" label="凭证附件">
                        <Upload>
                            <Button icon={<UploadOutlined />}>上传附件</Button>
                        </Upload>
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
