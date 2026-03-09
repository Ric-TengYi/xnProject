import React, { useState } from 'react';
import { Card, Table, Button, Input, DatePicker, Tag, Space, Modal, Form, InputNumber } from 'antd';
import { SearchOutlined, PlusOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { RangePicker } = DatePicker;

const paymentData = [
    { id: '1', project: '市中心地铁延长线三期工程', totalAmount: 5000000, paidAmount: 3000000, debtAmount: 2000000, lastPaymentDate: '2024-03-01', status: '欠款' },
    { id: '2', project: '滨海新区基础建设B标段', totalAmount: 2000000, paidAmount: 2000000, debtAmount: 0, lastPaymentDate: '2024-02-15', status: '结清' },
    { id: '3', project: '老旧小区改造工程综合包', totalAmount: 800000, paidAmount: 500000, debtAmount: 300000, lastPaymentDate: '2024-02-28', status: '欠款' },
];

const ProjectsPayments: React.FC = () => {
    const navigate = useNavigate();
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [form] = Form.useForm();

    const columns = [
        { title: '项目名称', dataIndex: 'project', key: 'project', render: (text: string) => <span className="text-blue-600 dark:text-blue-400 font-medium">{text}</span> },
        { title: '应消纳金额 (元)', dataIndex: 'totalAmount', key: 'totalAmount', render: (val: number) => val.toLocaleString() },
        { title: '累计交款 (元)', dataIndex: 'paidAmount', key: 'paidAmount', render: (val: number) => <span className="text-green-600 dark:text-green-500">{val.toLocaleString()}</span> },
        { title: '欠款 (元)', dataIndex: 'debtAmount', key: 'debtAmount', render: (val: number) => <span className={val > 0 ? 'text-red-600 dark:text-red-500' : 'text-slate-600 dark:text-slate-400'}>{val.toLocaleString()}</span> },
        { title: '最近交款时间', dataIndex: 'lastPaymentDate', key: 'lastPaymentDate' },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '结清' ? 'green' : 'red'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <Button type="link" size="small" onClick={() => setIsModalVisible(true)}>交款登记</Button>
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => navigate(`/projects/${record.id}?tab=payment`)}>查看</Button>
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
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">项目交款管理</h1>
            </div>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input placeholder="搜索项目名称" prefix={<SearchOutlined />} className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white w-64" />
                        <RangePicker className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700" />
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalVisible(true)}>批量交款登记</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={paymentData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/50 transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Modal
                title="交款登记"
                open={isModalVisible}
                onOk={handleOk}
                onCancel={() => setIsModalVisible(false)}
                className="dark-modal"
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="project" label="关联项目" rules={[{ required: true }]}>
                        <Input placeholder="请选择项目" />
                    </Form.Item>
                    <Form.Item name="amount" label="交款金额 (元)" rules={[{ required: true }]}>
                        <InputNumber className="w-full" placeholder="请输入金额" />
                    </Form.Item>
                    <Form.Item name="date" label="交款日期" rules={[{ required: true }]}>
                        <DatePicker className="w-full" />
                    </Form.Item>
                    <Form.Item name="receipt" label="凭证号">
                        <Input placeholder="请输入银行流水号或收据号" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} placeholder="请输入备注信息" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default ProjectsPayments;
